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

package nl.gridshore.cqrs4j.repository.eventsourcing;

import nl.gridshore.cqrs4j.EventSourcedAggregateRoot;
import org.springframework.util.Assert;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the {@link nl.gridshore.cqrs4j.repository.eventsourcing.LockManager} that uses a pessimistic
 * locking strategy. Calls to obtainLock will block until a lock could be obtained. If a lock is obtained by a thread,
 * that thread has guaranteed unique access.
 *
 * @author Allard Buijze
 */
class PessimisticLockManager implements LockManager {

    private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<UUID, ReentrantLock>();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateLock(EventSourcedAggregateRoot aggregate) {
        return locks.containsKey(aggregate.getIdentifier())
                && locks.get(aggregate.getIdentifier()).isHeldByCurrentThread();
    }

    /**
     * Obtain a lock for an aggregate. This method will block until a lock was successfully obtained.
     *
     * @param aggregateIdentifier the identifier of the aggregate to obtains a lock for.
     */
    @Override
    public void obtainLock(UUID aggregateIdentifier) {
        locks.putIfAbsent(aggregateIdentifier, new ReentrantLock());
        locks.get(aggregateIdentifier).lock();
    }

    /**
     * Release the lock held on the aggregate. If no valid lock is held by the current thread, an exception is thrown.
     *
     * @param aggregateIdentifier the identifier of the aggregate to release the lock for.
     * @throws IllegalStateException        if no lock was ever obtained for this aggregate
     * @throws IllegalMonitorStateException if a lock was obtained, but is not currently held by the current thread
     */
    @Override
    public void releaseLock(UUID aggregateIdentifier) {
        Assert.state(locks.containsKey(aggregateIdentifier), "No lock for this aggregate was ever obtained");
        locks.get(aggregateIdentifier).unlock();
    }
}
