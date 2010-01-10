/*
 * Copyright (c) 2010. Gridshore
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

import nl.gridshore.cqrs4j.DomainEvent;
import nl.gridshore.cqrs4j.eventhandler.EventBus;
import nl.gridshore.cqrs4j.eventhandler.EventListener;
import nl.gridshore.cqrs4j.eventhandler.EventSequencingPolicy;
import nl.gridshore.cqrs4j.eventhandler.SequentialPolicy;
import org.springframework.core.annotation.AnnotationUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Adapter that turns any bean with {@link nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler} annotated methods
 * into an {@link nl.gridshore.cqrs4j.eventhandler.EventListener}.
 * <p/>
 * Optionally, this adapter may be configured with an {@link EventBus} at which the adapter should register for events.
 * If none is configured, one is autowired (requiring that exactly one {@link EventBus} is present in the
 * ApplicationContext.
 *
 * @author Allard Buijze
 * @since 0.1
 */
public class AnnotationEventListenerAdapter implements EventListener {

    private EventBus eventBus;

    private final Object target;
    private final AnnotationEventHandlerInvoker eventHandlerInvoker;
    private final EventSequencingPolicy eventSequencingPolicy;

    /**
     * Initialize the AnnotationEventListenerAdapter for the given <code>annotatedEventListener</code>.
     *
     * @param annotatedEventListener the event listener
     */
    public AnnotationEventListenerAdapter(Object annotatedEventListener) {
        eventHandlerInvoker = new AnnotationEventHandlerInvoker(annotatedEventListener);
        eventSequencingPolicy = getSequencingPolicyFor(annotatedEventListener);
        this.target = annotatedEventListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(Class<? extends DomainEvent> eventType) {
        return eventHandlerInvoker.hasHandlerFor(eventType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(DomainEvent event) {
        eventHandlerInvoker.invokeEventHandlerMethod(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventSequencingPolicy getEventSequencingPolicy() {
        return eventSequencingPolicy;
    }

    /**
     * Returns the configuration of the event handler that would process the given <code>event</code>. Returns
     * <code>null</code> if no event handler is found for the given event.
     *
     * @param event the event for which to search configuration.
     * @return the annotation on the event handler method
     */
    public nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler getConfigurationFor(DomainEvent event) {
        return eventHandlerInvoker.findEventHandlerConfiguration(event);
    }

    /**
     * {@inheritDoc}
     */
    @PreDestroy
    public void shutdown() throws Exception {
        if (eventBus != null) {
            eventBus.unsubscribe(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @PostConstruct
    public void initialize() throws Exception {
        eventBus.subscribe(this);
    }

    private EventSequencingPolicy getSequencingPolicyFor(Object annotatedEventListener) {
        ConcurrentEventListener annotation = AnnotationUtils.findAnnotation(annotatedEventListener.getClass(),
                                                                            ConcurrentEventListener.class);
        if (annotation == null) {
            return new SequentialPolicy();
        }

        Class<? extends EventSequencingPolicy> policyClass = annotation.sequencingPolicyClass();
        try {
            return policyClass.newInstance();
        } catch (InstantiationException e) {
            throw new UnsupportedPolicyException(String.format(
                    "Could not initialize an instance of the given policy: [%s]. "
                            + "Does it have an accessible no-arg constructor?",
                    policyClass.getSimpleName()), e);
        } catch (IllegalAccessException e) {
            throw new UnsupportedPolicyException(String.format(
                    "Could not initialize an instance of the given policy: [%s]. "
                            + "Is the no-arg constructor accessible?",
                    policyClass.getSimpleName()), e);
        }
    }

    /**
     * Returns the event listener to which events are forwarded
     *
     * @return the targeted event listener
     */
    public Object getTarget() {
        return target;
    }

    /**
     * Sets the eventbus that this adapter should subscribe to. If none is provided, the adapter will autowire the
     * eventBus from the application context. This only works if there is exactly one {@link EventBus} defined in the
     * application context.
     *
     * @param eventBus the EventBus to subscribe to
     */
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

}
