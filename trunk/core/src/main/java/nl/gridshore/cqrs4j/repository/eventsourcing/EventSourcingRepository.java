/*
 * Copyright (c) 2010. Gridshore
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
import nl.gridshore.cqrs4j.repository.LockingRepository;
import nl.gridshore.cqrs4j.repository.LockingStrategy;
import org.springframework.beans.factory.annotation.Required;

import java.util.UUID;

/**
 * Abstract repository implementation that allows easy implementation of an Event Sourcing mechanism. It will
 * automatically publish new events to the given {@link nl.gridshore.cqrs4j.eventhandler.EventBus} and delegate event
 * storage to the provided {@link nl.gridshore.cqrs4j.repository.eventsourcing.EventStore}.
 *
 * @author Allard Buijze
 * @param <T> The type of aggregate this repository stores
 * @see nl.gridshore.cqrs4j.EventSourcedAggregateRoot
 * @see nl.gridshore.cqrs4j.AbstractAggregateRoot
 * @see nl.gridshore.cqrs4j.eventhandler.annotation.AbstractAnnotatedAggregateRoot
 * @see nl.gridshore.cqrs4j.repository.eventsourcing.EventStore
 * @see nl.gridshore.cqrs4j.repository.eventsourcing.XStreamFileSystemEventStore
 * @since 0.1
 */
public abstract class EventSourcingRepository<T extends EventSourcedAggregateRoot> extends LockingRepository<T> {

    private EventStore eventStore;

    /**
     * Initializes a repository with the default locking strategy.
     *
     * @see nl.gridshore.cqrs4j.repository.LockingRepository#LockingRepository()
     */
    protected EventSourcingRepository() {
    }

    /**
     * Initialize a repository with the given locking strategy.
     *
     * @param lockingStrategy the locking strategy to apply to this
     */
    protected EventSourcingRepository(final LockingStrategy lockingStrategy) {
        super(lockingStrategy);
    }

    /**
     * Perform the actual saving of the aggregate. All necessary locks have been verified.
     *
     * @param aggregate the aggregate to store
     */
    @Override
    protected void doSave(T aggregate) {
        eventStore.appendEvents(getTypeIdentifier(), aggregate.getUncommittedEvents());
    }

    /**
     * Perform the actual loading of an aggregate. The necessary locks have been obtained.
     *
     * @param aggregateIdentifier the identifier of the aggregate to load
     * @return the fully initialized aggregate
     */
    @Override
    protected T doLoad(UUID aggregateIdentifier) {
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
     * Sets the event store that would physically store the events.
     *
     * @param eventStore the event bus to publish events to
     */
    @Required
    public void setEventStore(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * Returns the type identifier for this aggregate. The type identifier is used by the EventStore to organize data
     * related to the same type of aggregate.
     * <p/>
     * Tip: in most cases, the simple class name would be a good start.
     *
     * @return the type identifier of the aggregates this repository stores
     */
    protected abstract String getTypeIdentifier();

}
