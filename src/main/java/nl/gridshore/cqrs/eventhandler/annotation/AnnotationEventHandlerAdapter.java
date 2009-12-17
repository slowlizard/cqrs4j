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
import nl.gridshore.cqrs.eventhandler.EventHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Adapter that turns any bean with {@link nl.gridshore.cqrs.eventhandler.annotation.EventHandler} annotated methods
 * into an {@link nl.gridshore.cqrs.eventhandler.EventHandler}.
 * <p/>
 * Optionally, this adapter may be configured with an {@link EventBus} at which the adapter should register for events.
 * If none is configured, one is autowired (requiring that exactly one {@link EventBus} is present in the
 * ApplicationContext.
 *
 * @author Allard Buijze
 */
public class AnnotationEventHandlerAdapter
        implements EventHandler, ApplicationContextAware, InitializingBean, DisposableBean {

    private EventBus eventBus;

    private transient ApplicationContext applicationContext;
    private final Object target;
    private final AnnotationEventHandlerInvoker eventHandlerInvoker;

    public AnnotationEventHandlerAdapter(Object annotatedEventHandler) {
        eventHandlerInvoker = new AnnotationEventHandlerInvoker(annotatedEventHandler);
        this.target = annotatedEventHandler;
    }

    @Override
    public boolean canHandle(Class<? extends DomainEvent> eventType) {
        return eventHandlerInvoker.canHandle(eventType);
    }

    @Override
    public void handle(DomainEvent event) {
        eventHandlerInvoker.invokeEventHandlerMethod(event);
    }

    public nl.gridshore.cqrs.eventhandler.annotation.EventHandler getConfigurationFor(DomainEvent event) {
        return eventHandlerInvoker.findEventHandlerConfiguration(event);
    }

    @Override
    public void destroy() throws Exception {
        if (eventBus != null) {
            eventBus.unsubscribe(this);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (eventBus == null) {
            eventBus = applicationContext.getBean(EventBus.class);
        }
        eventBus.subscribe(this);
    }

    public Object getTarget() {
        return target;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }


}
