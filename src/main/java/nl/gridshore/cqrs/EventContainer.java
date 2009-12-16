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

import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * @author Allard Buijze
 */
// TODO: Detect concurrent access (i.e. access from multiple threads)
public class EventContainer {

    private final List<DomainEvent> events = new LinkedList<DomainEvent>();
    private final UUID aggregateIdentifier;
    private Long lastSequenceNumber;
    private long firstSequenceNumber = 0;

    public EventContainer(UUID aggregateIdentifier) {
        this.aggregateIdentifier = aggregateIdentifier;
    }

    public void addEvent(DomainEvent event) {
        Assert.isTrue(event.getSequenceNumber() == null
                || lastSequenceNumber == null
                || event.getSequenceNumber().equals(lastSequenceNumber + 1),
                      "The given event's sequence number is discontinuous");

        Assert.isTrue(event.getAggregateIdentifier() == null
                || aggregateIdentifier.equals(event.getAggregateIdentifier()),
                      "The Identifier of the event does not match the Identifier of the EventContainer");

        if (event.getAggregateIdentifier() == null) {
            event.setAggregateIdentifier(aggregateIdentifier);
        }

        if (event.getSequenceNumber() == null) {
            event.setSequenceNumber(newSequenceNumber());
        } else {
            lastSequenceNumber = event.getSequenceNumber();
        }

        events.add(event);
    }

    public EventStream getInputStream() {
        return new SimpleEventStream(events);
    }

    public UUID getAggregateIdentifier() {
        return aggregateIdentifier;
    }

    private long newSequenceNumber() {
        if (lastSequenceNumber == null) {
            lastSequenceNumber = firstSequenceNumber;
        } else {
            lastSequenceNumber++;
        }
        return lastSequenceNumber;
    }

    public void setFirstSequenceNumber(long firstSequenceNumber) {
        Assert.state(events.size() == 0, "Cannot set first sequence number if events have already been added");
        this.firstSequenceNumber = firstSequenceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EventContainer that = (EventContainer) o;

        if (aggregateIdentifier != null ? !aggregateIdentifier.equals(that.aggregateIdentifier) :
                that.aggregateIdentifier != null) {
            return false;
        }
        if (events != null ? !events.equals(that.events) : that.events != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return aggregateIdentifier != null ? aggregateIdentifier.hashCode() : 0;
    }

    public void clear() {
        events.clear();
    }

    public int size() {
        return events.size();
    }
}
