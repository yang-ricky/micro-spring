package org.microspring.webflux;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.BiFunction;

/**
 * A non-blocking HTTP server implementation using Netty
 */
public class ReactiveHttpServer {
    private final int port;
    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public ReactiveHttpServer(int port) {
        this.port = port;
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
                                // Convert ByteBuf to String properly
                                ByteBuf content = request.content();
                                String requestBody = content.toString(CharsetUtil.UTF_8);
                                
                                ReactiveServerRequest reactiveRequest = new ReactiveServerRequest(
                                    request.method(),
                                    URI.create(request.uri()),
                                    request.headers(),
                                    Mono.just(requestBody)
                                );

                                ReactiveServerResponse reactiveResponse = new ReactiveServerResponse();
                                handler.apply(reactiveRequest, reactiveResponse)
                                    .subscribe(response -> {
                                        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(
                                            request.protocolVersion(),
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

                                        ctx.writeAndFlush(nettyResponse)
                                           .addListener(ChannelFutureListener.CLOSE);
                                    });
                            }
                        });
                    }
                });

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            System.out.println("Server started on port " + port);
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