package nl.gridshore.cqrs.eventhandler.annotation.postprocessor;

import org.springframework.beans.BeansException;

/**
 * Exception indicating that an error occurred while creating an EventListenerAdapter for an event listener
 *
 * @author Allard Buijze
 */
public class EventListenerAdapterException extends BeansException {

    public EventListenerAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
