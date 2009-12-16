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
 * @author Allard Buijze
 */
public abstract class AbstractAggregateRoot implements AggregateRoot {

    private final EventContainer uncommittedEvents;
    private final UUID identifier;

    protected AbstractAggregateRoot() {
        this(UUID.randomUUID());
    }

    protected AbstractAggregateRoot(UUID identifier) {
        uncommittedEvents = new EventContainer(identifier);
        this.identifier = identifier;
    }

    protected void initializeState(EventStream eventStream) {
        long lastSequenceNumber = -1;
        while (eventStream.hasNext()) {
            DomainEvent event = eventStream.next();
            lastSequenceNumber = event.getSequenceNumber();
            handle(event);
        }
        uncommittedEvents.setFirstSequenceNumber(lastSequenceNumber + 1);
    }

    protected void apply(DomainEvent event) {
        uncommittedEvents.addEvent(event);
        handle(event);
    }

    /**
     * Applies state changes based on the given event.
     *
     * @param event The event to handle
     */
    protected abstract void handle(DomainEvent event);

    @Override
    public EventStream getUncommittedEvents() {
        return uncommittedEvents.getInputStream();
    }

    @Override
    public UUID getIdentifier() {
        return identifier;
    }

    @Override
    public void commitEvents() {
        uncommittedEvents.clear();
    }

    @Override
    public int getUncommittedEventCount() {
        return uncommittedEvents.size();
    }
}
