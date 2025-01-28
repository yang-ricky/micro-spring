package org.microspring.webflux;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

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

    public void start(ReactiveHttpHandler handler) {
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
                                    // Convert Netty request to our ReactiveServerRequest
                                    ReactiveServerRequest reactiveRequest = new ReactiveServerRequest(
                                            request.method(),
                                            URI.create(request.uri()),
                                            request.headers(),
                                            Mono.just(request.content().toString(StandardCharsets.UTF_8))
                                    );

                                    ReactiveServerResponse reactiveResponse = new ReactiveServerResponse() {
                                        @Override
                                        public Mono<Void> end() {
                                            // Create Netty response
                                            FullHttpResponse response = new DefaultFullHttpResponse(
                                                    HttpVersion.HTTP_1_1,
                                                    getStatus(),
                                                    io.netty.buffer.Unpooled.copiedBuffer(getBody(), StandardCharsets.UTF_8)
                                            );

                                            // Copy headers
                                            response.headers().add(getHeaders());
                                            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                                            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

                                            // Write and flush
                                            return Mono.fromRunnable(() -> ctx.writeAndFlush(response));
                                        }
                                    };

                                    // Handle the request
                                    handler.handle(reactiveRequest, reactiveResponse).subscribe();
                                }
                            });
                        }
                    });

            // Start server
            serverChannel = bootstrap.bind(port).sync().channel();
            System.out.println("ReactiveHttpServer started on port " + port);

        } catch (InterruptedException e) {
            shutdown();
            Thread.currentThread().interrupt();
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