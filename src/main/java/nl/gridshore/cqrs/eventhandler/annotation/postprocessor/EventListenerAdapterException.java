package nl.gridshore.cqrs.eventhandler.annotation.postprocessor;

import org.springframework.beans.BeansException;

/**
 * @author Allard Buijze
 */
class EventListenerAdapterException extends BeansException {

    public EventListenerAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
