package nl.gridshore.cqrs4j.repository.eventsourcing;

import nl.gridshore.cqrs4j.EventSourcedAggregateRoot;

import java.util.UUID;

/**
 * Interface to the lock manager. A lock manager will maintain and validate locks on aggregates of a single type
 *
 * @author Allard Buijze
 */
interface LockManager {

    /**
     * Make sure that the current thread holds a valid lock for the given aggregate.
     *
     * @param aggregate the aggregate to validate the lock for
     * @return true if a valid lock is held, false otherwise
     */
    boolean validateLock(EventSourcedAggregateRoot aggregate);

    /**
     * Obtain a lock for an aggregate with the given <code>aggregateIdentifier</code>. Depending on the strategy, this
     * method may return immediately or block until a lock is held.
     *
     * @param aggregateIdentifier the identifier of the aggregate to obtains a lock for.
     */
    void obtainLock(UUID aggregateIdentifier);

    /**
     * Release the lock held for an aggregate with the given <code>aggregateIdentifier</code>. The caller of this method
     * must ensure a valid lock was requested using {@link #obtainLock(java.util.UUID)}. If no lock was successfully
     * obtained, the behavior of this method is undefined.
     *
     * @param aggregateIdentifier the identifier of the aggregate to release the lock for.
     */
    void releaseLock(UUID aggregateIdentifier);
}
