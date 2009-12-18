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

import java.util.UUID;

/**
 * @author Allard Buijze
 */
public abstract class EventSourcingRepository<T extends AggregateRoot> implements Repository<T> {

    protected EventStore eventStore = new XStreamFileSystemEventStore();
    protected EventBus eventBus;

    @Override
    public void save(T aggregate) {
        eventStore.appendEvents(getTypeIdentifier(), aggregate.getUncommittedEvents());
        EventStream uncommittedEvents = aggregate.getUncommittedEvents();
        while (uncommittedEvents.hasNext()) {
            eventBus.publish(uncommittedEvents.next());
        }
        aggregate.commitEvents();
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public T load(UUID aggregateIdentifier) {
        EventStream events = eventStore.readEvents(getTypeIdentifier(), aggregateIdentifier);
        return instantiateAggregate(events);
    }

    protected abstract T instantiateAggregate(EventStream events);

    protected abstract String getTypeIdentifier();

    @Autowired
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setEventStore(EventStore eventStore) {
        this.eventStore = eventStore;
    }
}
