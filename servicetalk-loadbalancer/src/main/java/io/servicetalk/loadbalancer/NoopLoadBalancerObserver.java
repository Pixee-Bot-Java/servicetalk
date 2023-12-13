/*
 * Copyright © 2023 Apple Inc. and the ServiceTalk project authors
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
package io.servicetalk.loadbalancer;

import io.servicetalk.client.api.NoActiveHostException;
import io.servicetalk.client.api.ServiceDiscovererEvent;

import java.util.Collection;

final class NoopLoadBalancerObserver<ResolvedAddress> implements LoadBalancerObserver<ResolvedAddress> {

    private static final LoadBalancerObserver<Object> INSTANCE = new NoopLoadBalancerObserver<>();

    private NoopLoadBalancerObserver() {
        // only private instance
    }

    @Override
    public HostObserver<ResolvedAddress> hostObserver() {
        return (HostObserver<ResolvedAddress>) NoopHostObserver.INSTANCE;
    }

    @Override
    public void onNoHostsAvailable() {
        // noop
    }

    @Override
    public void onNoActiveHostsAvailable(int hostSetSize, NoActiveHostException exn) {
        // noop
    }

    @Override
    public void onServiceDiscoveryEvent(Collection<? extends ServiceDiscovererEvent<ResolvedAddress>> events,
                                        int oldHostSetSize, int newHostSetSize) {
        // noop
    }

    private static final class NoopHostObserver<ResolvedAddress> implements
            LoadBalancerObserver.HostObserver<ResolvedAddress> {

        private static final HostObserver<Object> INSTANCE = new NoopHostObserver<>();

        private NoopHostObserver() {
        }

        @Override
        public void onHostMarkedExpired(ResolvedAddress resolvedAddress, int connectionCount) {
            // noop
        }

        @Override
        public void onExpiredHostRemoved(ResolvedAddress resolvedAddress) {
            // noop
        }

        @Override
        public void onExpiredHostRevived(ResolvedAddress resolvedAddress, int connectionCount) {
            // noop
        }

        @Override
        public void onActiveHostRemoved(ResolvedAddress resolvedAddress, int connectionCount) {
            // noop
        }

        @Override
        public void onHostCreated(ResolvedAddress resolvedAddress) {
            // noop
        }

        @Override
        public void onHostMarkedUnhealthy(ResolvedAddress address, Throwable cause) {
            // noop
        }

        @Override
        public void onHostRevived(ResolvedAddress address) {
            // noop
        }
    }

    public static <T> LoadBalancerObserver<T> instance() {
        return (LoadBalancerObserver<T>) INSTANCE;
    }
}