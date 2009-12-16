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

import org.joda.time.LocalDateTime;

import java.util.UUID;

/**
 * @author Allard Buijze
 */
public abstract class DomainEvent {

    private Long sequenceNumber;
    private UUID aggregateIdentifier;

    private final LocalDateTime createDate;
    private final UUID eventIdentifier;

    protected DomainEvent() {
        createDate = new LocalDateTime();
        eventIdentifier = UUID.randomUUID();
    }

    public UUID getEventIdentifier() {
        return eventIdentifier;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    void setSequenceNumber(long sequenceNumber) {
        if (this.sequenceNumber != null) {
            throw new IllegalStateException("Sequence number may not be applied more than once.");
        }
        this.sequenceNumber = sequenceNumber;
    }

    public UUID getAggregateIdentifier() {
        return aggregateIdentifier;
    }

    void setAggregateIdentifier(UUID aggregateIdentifier) {
        if (this.aggregateIdentifier != null) {
            throw new IllegalStateException("An aggregateIdentifier can not be applied more than once.");
        }
        this.aggregateIdentifier = aggregateIdentifier;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DomainEvent that = (DomainEvent) o;

        if (!createDate.equals(that.createDate)) {
            return false;
        }
        if (aggregateIdentifier != null ? !aggregateIdentifier.equals(that.aggregateIdentifier) :
                that.aggregateIdentifier
                        != null) {
            return false;
        }
        if (sequenceNumber != null ? !sequenceNumber.equals(that.sequenceNumber) : that.sequenceNumber != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return createDate.hashCode();
    }
}
