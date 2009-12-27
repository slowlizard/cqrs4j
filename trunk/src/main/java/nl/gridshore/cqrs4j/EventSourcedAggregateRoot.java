package nl.gridshore.cqrs4j;

/**
 * Aggregate that can be initialized using an {@link nl.gridshore.cqrs4j.EventStream}. Aggregates that are initialized
 * using Event Sourcing should implement this interface.
 *
 * @author Allard Buijze
 * @see nl.gridshore.cqrs4j.repository.eventsourcing.EventSourcingRepository
 */
public interface EventSourcedAggregateRoot extends AggregateRoot {


    /**
     * Initialize the state of this aggregate using the events in the provided {@link EventStream}. A call to this
     * method on an aggregate that has already been initialized will result in an {@link IllegalStateException}.
     *
     * @param eventStream the event stream containing the events that describe the state changes of this aggregate
     * @throws IllegalStateException if this aggregate was already initialized.
     */
    void initializeState(EventStream eventStream);

    /**
     * Returns the sequence number of the last committed event on this aggregate or <code>null</code> if events were
     * ever committed. This sequence number can be used to implement optimistic locking strategies.
     *
     * @return the sequence number of the last committed event or <code>null</code> if no events were ever committed
     */
    Long getLastCommittedEventSequenceNumber();
}
