package nl.gridshore.cqrs4j.repository.eventsourcing;

/**
 * Enum indicating possible locking strategies for repositories.
 *
 * @author Allard Buijze
 */
public enum LockingStrategy {

    /**
     * Indicator of an optimistic locking strategy. Concurrent access is not prevented. Instead, when concurrent
     * modifications are detected, an exception is thrown.
     *
     * @see nl.gridshore.cqrs4j.repository.eventsourcing.ConcurrencyException
     */
    OPTIMISTIC,

    /**
     * Indicator of a pessimistic locking strategy. This strategy will block any thread that tries to load an aggregate
     * that has already been loaded by another thread. Once the other thread saves the aggregate, the lock is released,
     * giving waiting threads the opportunity to obtain it and load the aggregate.
     */
    PESSIMISTIC
}
