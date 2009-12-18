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

import nl.gridshore.cqrs.DomainEvent;
import nl.gridshore.cqrs.eventhandler.EventBus;
import nl.gridshore.cqrs.eventhandler.EventListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Adapter that turns any bean with {@link nl.gridshore.cqrs.eventhandler.annotation.EventHandler} annotated methods
 * into an {@link nl.gridshore.cqrs.eventhandler.EventListener}.
 * <p/>
 * Optionally, this adapter may be configured with an {@link EventBus} at which the adapter should register for events.
 * If none is configured, one is autowired (requiring that exactly one {@link EventBus} is present in the
 * ApplicationContext.
 *
 * @author Allard Buijze
 */
public class AnnotationEventListenerAdapter
        implements EventListener, ApplicationContextAware, InitializingBean, DisposableBean {

    private EventBus eventBus;

    private transient ApplicationContext applicationContext;
    private final Object target;
    private final AnnotationEventListenerInvoker eventListenerInvoker;

    /**
     * Initialize the AnnotationEventListenerAdapter for the given <code>annotatedEventListener</code>.
     *
     * @param annotatedEventListener the event listener
     */
    public AnnotationEventListenerAdapter(Object annotatedEventListener) {
        eventListenerInvoker = new AnnotationEventListenerInvoker(annotatedEventListener);
        this.target = annotatedEventListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(Class<? extends DomainEvent> eventType) {
        return eventListenerInvoker.canHandle(eventType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(DomainEvent event) {
        eventListenerInvoker.invokeEventHandlerMethod(event);
    }

    /**
     * Returns the configuration of the event handler that would process the given <code>event</code>. Returns
     * </code>null</code> if no event handler is found for the given event.
     *
     * @param event the event for which to search configuration.
     */
    public nl.gridshore.cqrs.eventhandler.annotation.EventHandler getConfigurationFor(DomainEvent event) {
        return eventListenerInvoker.findEventHandlerConfiguration(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception {
        if (eventBus != null) {
            eventBus.unsubscribe(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (eventBus == null) {
            eventBus = applicationContext.getBean(EventBus.class);
        }
        eventBus.subscribe(this);
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
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
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
