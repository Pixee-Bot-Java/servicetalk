/*
 * Copyright © 2018 Apple Inc. and the ServiceTalk project authors
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
package io.servicetalk.redis.netty;

import io.servicetalk.concurrent.api.Single;
import io.servicetalk.transport.api.ExecutionContext;

import static io.servicetalk.redis.netty.DefaultRedisConnectionBuilder.buildForSubscribe;
import static java.util.Objects.requireNonNull;

final class SubscribedLBRedisConnectionFactory<ResolvedAddress> extends
                                                                AbstractLBRedisConnectionFactory<ResolvedAddress> {
    private final ReadOnlyRedisClientConfig config;
    private final ExecutionContext executionContext;

    SubscribedLBRedisConnectionFactory(final ReadOnlyRedisClientConfig config,
                                       final ExecutionContext executionContext,
                                       final RedisConnectionFilterFactory connectionFilterFactory) {
        super(connectionFilterFactory);
        this.config = requireNonNull(config);
        this.executionContext = requireNonNull(executionContext);
    }

    @Override
    Single<LoadBalancedRedisConnection> newConnection(final ResolvedAddress resolvedAddress,
                                          final RedisConnectionFilterFactory connectionFilterFactory) {
        return buildForSubscribe(executionContext, resolvedAddress, config, connectionFilterFactory)
                .map(filteredConnection -> new LoadBalancedRedisConnection(filteredConnection,
                        new RedisSubscribedReservableRequestConcurrencyController()));
    }
}
