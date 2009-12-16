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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * @author Allard Buijze
 */
public class SimpleEventStream implements EventStream {

    private DomainEvent nextEvent;
    private Iterator<DomainEvent> iterator;
    private UUID identifier;

    public SimpleEventStream(List<DomainEvent> domainEvents) {
        this.iterator = domainEvents.iterator();
        if (iterator.hasNext()) {
            nextEvent = iterator.next();
            identifier = nextEvent.getAggregateIdentifier();
        } else {
            throw new IllegalArgumentException("Must provide at least one event");
        }
    }

    public SimpleEventStream(DomainEvent... events) {
        this(Arrays.asList(events));
    }

    @Override
    public UUID getAggregateIdentifier() {
        return identifier;
    }

    @Override
    public boolean hasNext() {
        return nextEvent != null;
    }

    @Override
    public DomainEvent next() {
        DomainEvent next = nextEvent;
        if (iterator.hasNext()) {
            nextEvent = iterator.next();
        } else {
            nextEvent = null;
        }
        return next;
    }
}
