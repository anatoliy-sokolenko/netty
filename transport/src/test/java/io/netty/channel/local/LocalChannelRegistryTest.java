/*
 * Copyright 2011 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel.local;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;

import org.junit.Assert;
import org.junit.Test;

public class LocalChannelRegistryTest {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(LocalChannelRegistryTest.class);

    private static String LOCAL_ADDR_ID = "test.id";

    @Test
    public void testLocalAddressReuse() throws Exception {

        for (int i = 0; i < 2; i ++) {
            LocalAddress addr = new LocalAddress(LOCAL_ADDR_ID);
            Bootstrap cb = new Bootstrap();
            ServerBootstrap sb = new ServerBootstrap();

            cb.eventLoop(new LocalEventLoop())
              .channel(new LocalChannel())
              .remoteAddress(addr)
              .handler(new TestHandler());

            sb.eventLoop(new LocalEventLoop(), new LocalEventLoop())
              .channel(new LocalServerChannel())
              .localAddress(addr)
              .childHandler(new ChannelInitializer<LocalChannel>() {
                  @Override
                  public void initChannel(LocalChannel ch) throws Exception {
                      ch.pipeline().addLast(new TestHandler());
                  }
              });


            // Start server
            sb.bind().sync();

            // Connect to the server
            Channel cc = cb.connect().sync().channel();

            // Send a message event up the pipeline.
            cc.pipeline().inboundMessageBuffer().add("Hello, World");
            cc.pipeline().fireInboundBufferUpdated();

            // Close the channel
            cc.close().sync();

            sb.shutdown();
            cb.shutdown();

            Assert.assertTrue(String.format(
                    "Expected null, got channel '%s' for local address '%s'",
                    LocalChannelRegistry.get(addr), addr),
                    LocalChannelRegistry.get(addr) == null);
        }
    }

    static class TestHandler extends ChannelInboundMessageHandlerAdapter<String> {
        @Override
        public void messageReceived(ChannelInboundHandlerContext<String> ctx, String msg) throws Exception {
            logger.info(String.format("Received mesage: %s", msg));
        }
    }
}