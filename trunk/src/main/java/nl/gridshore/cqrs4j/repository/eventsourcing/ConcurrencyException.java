package nl.gridshore.cqrs4j.repository.eventsourcing;

/**
 * Exception indicating that concurrent access to a repository was detected. Most likely, two threads were modifying the
 * same aggregate.
 *
 * @author Allard Buijze
 */
public class ConcurrencyException extends RuntimeException {

    public ConcurrencyException(String message) {
        super(message);
    }

    public ConcurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
