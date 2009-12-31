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

import nl.gridshore.cqrs4j.EventStream;

import java.util.UUID;

/**
 * Abstraction of the event storage mechanism. Events are stored and read as {@link nl.gridshore.cqrs4j.EventStream
 * streams}.
 *
 * @author Allard Buijze
 * @since 0.1
 */
public interface EventStore {

    /**
     * Append the events in the given {@link nl.gridshore.cqrs4j.EventStream stream} to the events available.
     *
     * @param type   The type descriptor of the object to store
     * @param events The event stream containing the events to store
     * @throws EventStorageException if an error occurs while storing the events in the event stream
     */
    void appendEvents(String type, EventStream events);

    /**
     * Read the events of the aggregate identified by the given type and identifier.
     *
     * @param type       The type descriptor of the object to retrieve
     * @param identifier The unique aggregate identifier of the events to load
     * @return an event stream containing the events of the aggregate
     */
    EventStream readEvents(String type, UUID identifier);
}
