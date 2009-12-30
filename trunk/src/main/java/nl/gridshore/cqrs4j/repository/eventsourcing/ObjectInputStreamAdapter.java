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

import nl.gridshore.cqrs4j.DomainEvent;
import nl.gridshore.cqrs4j.EventStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.UUID;

/**
 * {@link nl.gridshore.cqrs4j.EventStream} implementation that takes an ObjectInputStream as input for events. This
 * implementation uses read-ahead to initialize the aggregateIdentifier upon initialization of the adapter. If there are
 * no events in the ObjectInputStream, the aggregate identifier cannot be resolved and will return null.
 *
 * @author Allard Buijze
 */
public class ObjectInputStreamAdapter implements EventStream {

    private final ObjectInputStream objectInputStream;
    private final UUID aggregateIdentifier;
    private volatile DomainEvent nextEvent;

    /**
     * Construct a EventStream using the provided ObjectInputStream as event provider.
     *
     * @param objectInputStream the backing input stream that provides the events
     */
    public ObjectInputStreamAdapter(final ObjectInputStream objectInputStream) {
        this.objectInputStream = objectInputStream;
        try {
            nextEvent = readIfAvailable(objectInputStream);
        } catch (IOException e) {
            throw new IllegalStateException("The provided objectInputStream is not ready for reading");
        }
        aggregateIdentifier = (nextEvent == null ? null : nextEvent.getAggregateIdentifier());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return nextEvent != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DomainEvent next() {
        DomainEvent currentEvent = nextEvent;
        try {
            nextEvent = readIfAvailable(objectInputStream);
        } catch (IOException e) {
            throw new IllegalStateException("The provided objectInputStream is not ready for reading");

        }
        return currentEvent;
    }

    /**
     * Returns the Aggregate Identifier that the events in this stream apply to. May return null if no events are
     * available in the stream.
     *
     * @return the aggregate identifier of the events in this stream
     */
    @Override
    public UUID getAggregateIdentifier() {
        return aggregateIdentifier;
    }

    private DomainEvent readIfAvailable(ObjectInputStream objectStream) throws IOException {
        try {
            return (DomainEvent) objectStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            // move to the next event
            return readIfAvailable(objectStream);
        } catch (EOFException e) {
            return null;
            // what an ugly way to escape a loop, but that's how the ObjectInputStream works
        }
    }
}
