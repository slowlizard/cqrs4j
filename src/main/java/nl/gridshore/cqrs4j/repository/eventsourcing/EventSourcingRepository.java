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
import nl.gridshore.cqrs4j.EventStream;
import nl.gridshore.cqrs4j.eventhandler.EventBus;
import nl.gridshore.cqrs4j.repository.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.util.UUID;

/**
 * Abstract repository implementation that allows easy implementation of an Event Sourcing mechanism. It will
 * automatically publish new events to the given {@link nl.gridshore.cqrs4j.eventhandler.EventBus} and delegate event
 * storage to the provided {@link nl.gridshore.cqrs4j.repository.eventsourcing.EventStore}.
 *
 * @author Allard Buijze
 * @see nl.gridshore.cqrs4j.EventSourcedAggregateRoot
 * @see nl.gridshore.cqrs4j.AbstractAggregateRoot
 * @see nl.gridshore.cqrs4j.eventhandler.annotation.AbstractAnnotatedAggregateRoot
 * @see nl.gridshore.cqrs4j.repository.eventsourcing.EventStore
 * @see nl.gridshore.cqrs4j.repository.eventsourcing.XStreamFileSystemEventStore
 */
public abstract class EventSourcingRepository<T extends EventSourcedAggregateRoot> implements Repository<T> {

    private EventStore eventStore;
    private EventBus eventBus;
    private final LockManager lockManager;

    /**
     * Initializes a repository with an optimistic locking strategy.
     */
    protected EventSourcingRepository() {
        this(LockingStrategy.OPTIMISTIC);
    }

    /**
     * Initialize a repository with the given locking strategy.
     *
     * @param lockingStrategy the locking strategy to apply to this
     */
    protected EventSourcingRepository(final LockingStrategy lockingStrategy) {
        switch (lockingStrategy) {
            case PESSIMISTIC:
                lockManager = new PessimisticLockManager();
                break;
            case OPTIMISTIC:
                lockManager = new OptimisticLockManager();
                break;
            default:
                throw new IllegalArgumentException(String.format(
                        "This repository implementation does not support the [%s] locking strategy",
                        lockingStrategy.name()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(T aggregate) {
        // make sure no events were previously committed
        boolean isNewAggregate = (aggregate.getLastCommittedEventSequenceNumber() == null);
        if (!isNewAggregate && !lockManager.validateLock(aggregate)) {
            throw new ConcurrencyException(String.format(
                    "The aggregate if type [%s] with identifier [%s] could not be "
                            + "saved due to concurrent access to the repository",
                    getTypeIdentifier(),
                    aggregate.getIdentifier()));
        }
        // save events
        eventStore.appendEvents(getTypeIdentifier(), aggregate.getUncommittedEvents());
        EventStream uncommittedEvents = aggregate.getUncommittedEvents();
        while (uncommittedEvents.hasNext()) {
            eventBus.publish(uncommittedEvents.next());
        }
        aggregate.commitEvents();
        if (!isNewAggregate) {
            lockManager.releaseLock(aggregate.getIdentifier());
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public T load(UUID aggregateIdentifier) {
        lockManager.obtainLock(aggregateIdentifier);
        EventStream events = eventStore.readEvents(getTypeIdentifier(), aggregateIdentifier);
        T aggregate = instantiateAggregate(events.getAggregateIdentifier());
        aggregate.initializeState(events);
        return aggregate;
    }

    /**
     * Instantiate the aggregate using the given aggregate identifier. Aggregate state should *not* be initialized by
     * this method. That means, no events should be applied by a call to this method.
     *
     * @param aggregateIdentifier the aggregate identifier of the aggregate to instantiate
     * @return a fully initialized aggregate
     */
    protected abstract T instantiateAggregate(UUID aggregateIdentifier);

    /**
     * Returns the type identifier for this aggregate. The type identifier is used by the EventStore to organize data
     * related to the same type of aggregate.
     * <p/>
     * Tip: in most cases, the simple class name would be a good start.
     *
     * @return the type identifier of the aggregates this repository stores
     */
    protected abstract String getTypeIdentifier();

    /**
     * Sets the event bus to which newly stored events should be published. Optional. By default, the repository tries
     * to autwowire the event bus.
     *
     * @param eventBus the event bus to publish events to
     */
    @Autowired
    @Required
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Sets the event store that would physically store the events.
     *
     * @param eventStore the event bus to publish events to
     */
    @Required
    public void setEventStore(EventStore eventStore) {
        this.eventStore = eventStore;
    }
}