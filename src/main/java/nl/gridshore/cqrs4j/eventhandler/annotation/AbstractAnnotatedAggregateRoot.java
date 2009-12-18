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

package nl.gridshore.cqrs4j.eventhandler.annotation;

import nl.gridshore.cqrs4j.AbstractAggregateRoot;
import nl.gridshore.cqrs4j.AggregateRoot;
import nl.gridshore.cqrs4j.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Convenience super type for aggregate roots that have their event handler methods annotated with the {@link
 * nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler} annotation.
 * <p/>
 * Implementations can call the {@link #apply(nl.gridshore.cqrs4j.DomainEvent)} method to have an event applied.
 * <p/>
 * Any events that are passed to the {@link #apply(nl.gridshore.cqrs4j.DomainEvent)} method for which no event handler
 * can be found will cause an {@link nl.gridshore.cqrs4j.eventhandler.annotation.UnhandledEventException} to be thrown.
 *
 * @author Allard Buijze
 * @see nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler
 */
public abstract class AbstractAnnotatedAggregateRoot extends AbstractAggregateRoot implements AggregateRoot {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAnnotatedAggregateRoot.class);
    private final AnnotationEventListenerInvoker eventListenerInvoker;

    /**
     * Initialize the aggregate root with a random identifier
     */
    protected AbstractAnnotatedAggregateRoot() {
        super();
        eventListenerInvoker = new AnnotationEventListenerInvoker(this);
    }

    /**
     * Initializes the aggregate root using the provided aggregate identifier.
     *
     * @param identifier the identifier of this aggregate
     */
    protected AbstractAnnotatedAggregateRoot(UUID identifier) {
        super(identifier);
        eventListenerInvoker = new AnnotationEventListenerInvoker(this);
    }

    /**
     * Calls the appropriate {@link nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler} annotated handler with the
     * provided event.
     *
     * @param event The event to handle
     * @see nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler
     */
    @Override
    protected void handle(DomainEvent event) {
        eventListenerInvoker.invokeEventHandlerMethod(event);
    }

    /**
     * Event Handler that will throw an exception if no better (read any) event handler could be found for the processed
     * event. Throws an {@link nl.gridshore.cqrs4j.eventhandler.annotation.UnhandledEventException}.
     *
     * @param event the event that could not be processed by any other event handler
     * @throws UnhandledEventException when called.
     */
    @EventHandler
    protected void onUnhandledEvents(DomainEvent event) {
        String message = String.format("No EventListener method could be found for [%s] on aggregate [%s]",
                                       event.getClass().getSimpleName(),
                                       getClass().getSimpleName());
        logger.error(message);
        throw new UnhandledEventException(message, event);
    }

}
