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

package nl.gridshore.cqrs;

import java.util.UUID;

/**
 * Abstract convenience class to be extended by all aggregate roots. The AbstractAggregateRoot tracks all uncommitted
 * events. It also provides convenience methods to initialize the state of the aggregate root based on an {@link
 * nl.gridshore.cqrs.EventStream}, which can be used for event sourcing.
 *
 * @author Allard Buijze
 */
public abstract class AbstractAggregateRoot implements AggregateRoot {

    private final EventContainer uncommittedEvents;
    private final UUID identifier;

    /**
     * Initializes the aggregate root using a random aggregate identifier.
     */
    protected AbstractAggregateRoot() {
        this(UUID.randomUUID());
    }

    /**
     * Initializes the aggregate root using the provided aggregate identifier.
     *
     * @param identifier the identifier of this aggregate
     */
    protected AbstractAggregateRoot(UUID identifier) {
        uncommittedEvents = new EventContainer(identifier);
        this.identifier = identifier;
    }

    /**
     * Initialize the state of this aggregate using the events in the provided {@link nl.gridshore.cqrs.EventStream}
     *
     * @param eventStream the event stream containing the events that describe the state changes of this aggregate
     */
    protected void initializeState(EventStream eventStream) {
        long lastSequenceNumber = -1;
        while (eventStream.hasNext()) {
            DomainEvent event = eventStream.next();
            lastSequenceNumber = event.getSequenceNumber();
            handle(event);
        }
        uncommittedEvents.setFirstSequenceNumber(lastSequenceNumber + 1);
    }

    /**
     * Apply the provided event. Applying events means they are added to the uncommitted event queue and forwarded to
     * the {@link #handle(DomainEvent) event handler method} for processing.
     *
     * @param event The event to apply
     */
    protected void apply(DomainEvent event) {
        uncommittedEvents.addEvent(event);
        handle(event);
    }

    /**
     * Applies state changes based on the given event.
     * <p/>
     * Note: Implementations of this method should *not* perform validation.
     *
     * @param event The event to handle
     */
    protected abstract void handle(DomainEvent event);

    /**
     * {@inheritDoc}
     */
    @Override
    public EventStream getUncommittedEvents() {
        return uncommittedEvents.getInputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID getIdentifier() {
        return identifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitEvents() {
        uncommittedEvents.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUncommittedEventCount() {
        return uncommittedEvents.size();
    }
}
