/*
 * Copyright © 2019 Apple Inc. and the ServiceTalk project authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.servicetalk.http.netty;

import io.servicetalk.concurrent.api.CompositeCloseable;
import io.servicetalk.concurrent.api.DefaultThreadFactory;
import io.servicetalk.concurrent.api.Executor;
import io.servicetalk.concurrent.api.Single;
import io.servicetalk.http.api.BlockingHttpClient;
import io.servicetalk.http.api.HttpExecutionStrategy;
import io.servicetalk.http.api.HttpExecutionStrategyAdapter;
import io.servicetalk.http.api.HttpServerBuilder;
import io.servicetalk.http.api.HttpServiceContext;
import io.servicetalk.http.api.StreamingHttpRequest;
import io.servicetalk.http.api.StreamingHttpResponse;
import io.servicetalk.http.api.StreamingHttpResponseFactory;
import io.servicetalk.http.api.StreamingHttpService;
import io.servicetalk.transport.api.HostAndPort;
import io.servicetalk.transport.api.IoExecutor;
import io.servicetalk.transport.api.ServerContext;

import org.junit.After;
import org.junit.Test;

import java.net.InetSocketAddress;
import javax.annotation.Nullable;

import static io.servicetalk.concurrent.api.AsyncCloseables.newCompositeCloseable;
import static io.servicetalk.concurrent.api.Executors.immediate;
import static io.servicetalk.http.api.HttpExecutionStrategies.noOffloadsStrategy;
import static io.servicetalk.transport.netty.NettyIoExecutors.createIoExecutor;
import static java.lang.Thread.NORM_PRIORITY;
import static java.lang.Thread.currentThread;
import static java.net.InetAddress.getLoopbackAddress;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

public class NoOffloadsStrategyTest {

    private static final String IO_EXECUTOR_NAME = "io-executor";
    private static final InetSocketAddress LISTEN_ADDRESS = new InetSocketAddress(getLoopbackAddress(), 0);
    private final HttpServerBuilder serverBuilder;
    private final IoExecutor ioExecutor;
    @Nullable
    private ServerContext context;
    @Nullable
    private BlockingHttpClient client;

    public NoOffloadsStrategyTest() {
        ioExecutor = createIoExecutor(new DefaultThreadFactory(IO_EXECUTOR_NAME, true, NORM_PRIORITY));
        serverBuilder = HttpServers.forAddress(LISTEN_ADDRESS).ioExecutor(ioExecutor);
    }

    @After
    public void tearDown() throws Exception {
        CompositeCloseable closeables = newCompositeCloseable();
        if (client != null) {
            client.close();
        }
        if (context != null) {
            closeables.append(context);
        }
        closeables.append(ioExecutor).closeAsync().toFuture().get();
    }

    @Test
    public void noOffloadsStillUsesAServerExecutor() throws Exception {
        StreamingHttpServiceImpl svc = new StreamingHttpServiceImpl(noOffloadsStrategy());
        BlockingHttpClient client = initServerAndClient(svc);
        client.request(client.get("/"));
        assertThat("Unexpected thread for the server executor.", svc.executorThread.getName(),
                not(startsWith(IO_EXECUTOR_NAME)));
    }

    @Test
    public void turnOffAllExecutors() throws Exception {
        StreamingHttpServiceImpl svc =
                new StreamingHttpServiceImpl(new HttpExecutionStrategyAdapter(noOffloadsStrategy()) {
                    @Override
                    public Executor executor() {
                        return immediate();
                    }
                });
        BlockingHttpClient client = initServerAndClient(svc);
        client.request(client.get("/"));
        assertThat("Unexpected thread for the server executor.", svc.executorThread.getName(),
                startsWith(IO_EXECUTOR_NAME));
    }

    private BlockingHttpClient initServerAndClient(final StreamingHttpService service) throws Exception {
        context = serverBuilder.listenStreamingAndAwait(service);
        client = HttpClients.forSingleAddress(HostAndPort.of((InetSocketAddress) context.listenAddress()))
                .buildBlocking();
        return requireNonNull(client);
    }

    private static final class StreamingHttpServiceImpl extends StreamingHttpService {

        private final HttpExecutionStrategy strategy;
        private volatile Thread executorThread;

        StreamingHttpServiceImpl(final HttpExecutionStrategy strategy) {
            this.strategy = strategy;
        }

        @Override
        public Single<StreamingHttpResponse> handle(final HttpServiceContext ctx,
                                                    final StreamingHttpRequest request,
                                                    final StreamingHttpResponseFactory responseFactory) {
            return ctx.executionContext().executor().submit(() -> {
                executorThread = currentThread();
                return responseFactory.ok();
            });
        }

        @Override
        public HttpExecutionStrategy executionStrategy() {
            return strategy;
        }
    }
}
