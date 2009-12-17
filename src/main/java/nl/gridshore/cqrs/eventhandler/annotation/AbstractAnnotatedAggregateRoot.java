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

package nl.gridshore.cqrs.eventhandler.annotation;

import nl.gridshore.cqrs.AbstractAggregateRoot;
import nl.gridshore.cqrs.AggregateRoot;
import nl.gridshore.cqrs.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Convenience super type for aggregate roots that have their event handler methods annotated with the {@link
 * nl.gridshore.cqrs.eventhandler.annotation.EventHandler} annotation.
 * <p/>
 * Implementations can call the {@link #apply(nl.gridshore.cqrs.DomainEvent)} method to have an event applied.
 * <p/>
 * Any events that are passed to the {@link #apply(nl.gridshore.cqrs.DomainEvent)} method for which no event handler can
 * be found will cause an {@link nl.gridshore.cqrs.eventhandler.annotation.UnhandledEventException} to be thrown.
 *
 * @author Allard Buijze
 * @see nl.gridshore.cqrs.eventhandler.annotation.EventHandler
 */
public abstract class AbstractAnnotatedAggregateRoot extends AbstractAggregateRoot implements AggregateRoot {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAnnotatedAggregateRoot.class);
    private final AnnotationEventHandlerInvoker eventHandlerInvoker;

    protected AbstractAnnotatedAggregateRoot() {
        super();
        eventHandlerInvoker = new AnnotationEventHandlerInvoker(this);
    }

    protected AbstractAnnotatedAggregateRoot(UUID identifier) {
        super(identifier);
        eventHandlerInvoker = new AnnotationEventHandlerInvoker(this);
    }

    @Override
    protected void handle(DomainEvent event) {
        eventHandlerInvoker.invokeEventHandlerMethod(event);
    }

    @EventHandler
    protected void onUnhandledEvents(DomainEvent event) {
        String message = String.format("No EventHandler method could be found for [%s] on aggregate [%s]",
                                       event.getClass().getSimpleName(),
                                       getClass().getSimpleName());
        logger.error(message);
        throw new UnhandledEventException(message, event);
    }

}
