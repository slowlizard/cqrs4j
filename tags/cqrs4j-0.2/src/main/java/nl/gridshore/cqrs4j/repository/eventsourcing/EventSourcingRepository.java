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

import nl.gridshore.cqrs4j.AggregateRoot;
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
 * @see nl.gridshore.cqrs4j.repository.eventsourcing.EventStore
 * @see nl.gridshore.cqrs4j.repository.eventsourcing.XStreamFileSystemEventStore
 */
public abstract class EventSourcingRepository<T extends AggregateRoot> implements Repository<T> {

    protected EventStore eventStore;
    protected EventBus eventBus;

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(T aggregate) {
        eventStore.appendEvents(getTypeIdentifier(), aggregate.getUncommittedEvents());
        EventStream uncommittedEvents = aggregate.getUncommittedEvents();
        while (uncommittedEvents.hasNext()) {
            eventBus.publish(uncommittedEvents.next());
        }
        aggregate.commitEvents();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public T load(UUID aggregateIdentifier) {
        EventStream events = eventStore.readEvents(getTypeIdentifier(), aggregateIdentifier);
        return instantiateAggregate(events);
    }

    /**
     * Instantiate the aggregate using the given event stream. You may use the {@link
     * nl.gridshore.cqrs4j.AbstractAggregateRoot#initializeState(nl.gridshore.cqrs4j.EventStream)} method to initialize
     * the aggregate's state.
     *
     * @param eventStream the event stream containing the historical events of the aggregate
     * @return a fully initialized aggregate
     */
    protected abstract T instantiateAggregate(EventStream eventStream);

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
