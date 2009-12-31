/*
 * Copyright (c) 2009. Gridshore
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.gridshore.cqrs4j.repository;

import nl.gridshore.cqrs4j.VersionedAggregateRoot;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the {@link LockManager} that uses an optimistic locking strategy. It uses the sequence number of
 * the last committed event to detect concurrent access.
 * <p/>
 * Classes that use a repository with this strategy must implement any retry logic themselves. Use the {@link
 * ConcurrencyException} to detect concurrent access.
 *
 * @author Allard Buijze
 * @see nl.gridshore.cqrs4j.EventSourcedAggregateRoot
 * @see ConcurrencyException
 * @since 0.3
 */
class OptimisticLockManager implements LockManager {

    private final ConcurrentHashMap<UUID, Long> versionMap = new ConcurrentHashMap<UUID, Long>();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateLock(VersionedAggregateRoot aggregate) {
        long newVersion = aggregate.getLastCommittedEventSequenceNumber()
                + aggregate.getUncommittedEventCount();
        return versionMap.replace(aggregate.getIdentifier(),
                                  aggregate.getLastCommittedEventSequenceNumber(),
                                  newVersion)
                || versionMap.replace(aggregate.getIdentifier(),
                                      Long.MIN_VALUE,
                                      newVersion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void obtainLock(UUID aggregateIdentifier) {
        versionMap.putIfAbsent(aggregateIdentifier, Long.MIN_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseLock(UUID aggregateIdentifier) {
    }
}
