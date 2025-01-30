package org.microspring.webflux;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.microspring.webflux.exception.ExceptionHandlerRegistry;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * A non-blocking HTTP server implementation using Netty
 */
public class ReactiveHttpServer {
    private final int port;
    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final List<WebFilter> filters = new ArrayList<>();
    private final List<WebExceptionHandler> exceptionHandlers = new ArrayList<>();
    private final ExceptionHandlerRegistry exceptionHandlerRegistry = new ExceptionHandlerRegistry();

    public ReactiveHttpServer(int port) {
        this.port = port;
    }

    /**
     * Add a filter to the chain
     */
    public void addFilter(WebFilter filter) {
        filters.add(filter);
    }

    /**
     * Add an exception handler
     */
    public void addExceptionHandler(WebExceptionHandler handler) {
        exceptionHandlers.add(handler);
    }

    /**
     * Register a @RestControllerAdvice bean for exception handling
     */
    public void registerExceptionHandler(Object adviceBean) {
        exceptionHandlerRegistry.registerExceptionHandler(adviceBean);
    }

    private void sendResponse(ChannelHandlerContext ctx, HttpVersion version, ReactiveServerResponse response) {
        if (response.isCommitted()) {
            return;
        }
        response.markCommitted();
        
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(
            version,
            response.getStatus()
        );

        if (response.getBody() != null) {
            nettyResponse.content().writeBytes(response.getBody().getBytes());
        }

        response.getHeaders().forEach(entry -> 
            nettyResponse.headers().set(entry.getKey(), entry.getValue())
        );

        nettyResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, 
            nettyResponse.content().readableBytes());
        
        nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        // Write the response first
        ChannelFuture writeFuture = ctx.writeAndFlush(nettyResponse);
        
        // Add listener to close the connection after writing is complete
        writeFuture.addListener(future -> {
            if (future.isSuccess()) {
                // Ensure we close the connection after the response is written
                if (ctx.channel().isActive()) {
                    ctx.close().addListener(closeFuture -> {
                        if (closeFuture.isSuccess()) {
                        }
                    });
                }
            } else {
                future.cause().printStackTrace();
                if (ctx.channel().isActive()) {
                    ctx.close();
                }
            }
        });
    }

    private Mono<Void> handleError(Throwable ex, ReactiveServerRequest request, ReactiveServerResponse response, 
            ChannelHandlerContext ctx, HttpVersion version) {
        
        // First try @ExceptionHandler methods
        return Mono.justOrEmpty(exceptionHandlerRegistry.findHandler(ex))
            .flatMap(handler -> handler.invoke(ex))
            .map(result -> {
                if (result instanceof ReactiveServerResponse) {
                    return (ReactiveServerResponse) result;
                } else {
                    response.write(result.toString());
                    return response;
                }
            })
            .flatMap(resp -> {
                if (!resp.isCommitted()) {
                    sendResponse(ctx, version, resp);
                }
                return Mono.empty();
            })
            .onErrorResume(error -> {
                // If exception handler fails, try WebExceptionHandlers
                ExceptionHandlerChain exceptionHandlerChain = new ExceptionHandlerChain(exceptionHandlers);
                return exceptionHandlerChain.handle(request, response, ex)
                    .doOnSuccess(v -> {
                        if (!response.isCommitted()) {
                            sendResponse(ctx, version, response);
                        }
                    })
                    .onErrorResume(e -> {
                        // If all handlers fail, return 500
                        if (!response.isCommitted()) {
                            response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                                .write("Internal Server Error");
                            sendResponse(ctx, version, response);
                        }
                        return Mono.empty();
                    });
            })
            .then();
    }

    public void start(BiFunction<ReactiveServerRequest, ReactiveServerResponse, Mono<ReactiveServerResponse>> handler) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        pipeline.addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
                                
                                ByteBuf content = request.content();
                                String requestBody = content.toString(CharsetUtil.UTF_8);
                                
                                ReactiveServerRequest reactiveRequest = new ReactiveServerRequest(
                                    request.method(),
                                    URI.create(request.uri()),
                                    request.headers(),
                                    Mono.just(requestBody)
                                );

                                ReactiveServerResponse reactiveResponse = new ReactiveServerResponse();

                                try {
                                    // Create the final handler that will process the request
                                    WebHandler webHandler = (req, resp) -> 
                                        handler.apply(req, resp)
                                            .doOnSubscribe(s -> {})
                                            .flatMap(response -> {
                                                if (!response.isCommitted()) {
                                                    sendResponse(ctx, request.protocolVersion(), response);
                                                }
                                                return Mono.empty();
                                            })
                                            .doOnError(e -> {})
                                            .doOnSuccess(response -> {})
                                            .then();

                                    // Create filter chain with the handler
                                    DefaultWebFilterChain filterChain = new DefaultWebFilterChain(filters, webHandler);

                                    // Execute the filter chain with exception handling
                                    filterChain.filter(reactiveRequest, reactiveResponse)
                                        .doOnSubscribe(s -> {})
                                        .doOnError(ex -> {
                                        })
                                        .onErrorResume(ex -> 
                                            handleError(ex, reactiveRequest, reactiveResponse, ctx, request.protocolVersion())
                                        )
                                        .doFinally(signal -> {
                                            if (!reactiveResponse.isCommitted()) {
                                                sendResponse(ctx, request.protocolVersion(), reactiveResponse);
                                            }
                                        })
                                        .subscribe();
                                } catch (Exception e) {
                                    handleError(e, reactiveRequest, reactiveResponse, ctx, request.protocolVersion())
                                        .subscribe(
                                            null,
                                            error -> {},
                                            () -> {}
                                        );
                                }
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                cause.printStackTrace();
                                if (ctx.channel().isActive()) {
                                    ctx.close();
                                }
                            }
                        });
                    }
                });

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
        } catch (Exception e) {
            shutdown();
            throw new RuntimeException("Failed to start server", e);
        }
    }

    public void shutdown() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
} 