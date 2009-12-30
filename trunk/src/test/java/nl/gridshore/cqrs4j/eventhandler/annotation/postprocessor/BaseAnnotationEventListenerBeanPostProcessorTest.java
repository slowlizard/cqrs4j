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

package nl.gridshore.cqrs4j.eventhandler.annotation.postprocessor;

import net.sf.cglib.proxy.Enhancer;
import nl.gridshore.cqrs4j.DomainEvent;
import nl.gridshore.cqrs4j.StubDomainEvent;
import nl.gridshore.cqrs4j.eventhandler.EventBus;
import nl.gridshore.cqrs4j.eventhandler.EventListener;
import nl.gridshore.cqrs4j.eventhandler.annotation.AnnotationEventListenerAdapter;
import nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler;
import org.junit.*;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class BaseAnnotationEventListenerBeanPostProcessorTest {

    private AnnotationEventListenerAdapter mockAdapter;

    private BaseAnnotationEventListenerBeanPostProcessor testSubject;
    private EventBus mockEventBus;
    private ApplicationContext mockApplicationContext;

    @Before
    public void setUp() {
        mockAdapter = mock(AnnotationEventListenerAdapter.class);
        mockEventBus = mock(EventBus.class);
        mockApplicationContext = mock(ApplicationContext.class);
        testSubject = new BaseAnnotationEventListenerBeanPostProcessor() {
            @Override
            protected AnnotationEventListenerAdapter adapt(Object bean) {
                return mockAdapter;
            }
        };
        testSubject.setApplicationContext(mockApplicationContext);
        testSubject.setEventBus(mockEventBus);
    }

    @Test
    public void testEventHandlerCallsRedirectToAdapter() {
        Object result1 = testSubject.postProcessBeforeInitialization(new AnnotatedEventListener(), "beanName");
        Object postProcessedBean = testSubject.postProcessAfterInitialization(result1, "beanName");

        assertTrue(Enhancer.isEnhanced(postProcessedBean.getClass()));
        assertTrue(postProcessedBean instanceof EventListener);
        assertTrue(postProcessedBean instanceof AnnotatedEventListener);

        EventListener eventListener = (EventListener) postProcessedBean;
        AnnotatedEventListener annotatedEventListener = (AnnotatedEventListener) postProcessedBean;
        eventListener.canHandle(StubDomainEvent.class);
        StubDomainEvent domainEvent = new StubDomainEvent();
        eventListener.handle(domainEvent);

        verify(mockAdapter).canHandle(StubDomainEvent.class);
        verify(mockAdapter).handle(domainEvent);
        reset(mockAdapter);
        annotatedEventListener.handleEvent(new StubDomainEvent());
        verifyZeroInteractions(mockAdapter);
    }

    @Test
    public void testEventHandlerAdapterIsInitializedAndDestroyedProperly() throws Exception {
        Object result1 = testSubject.postProcessBeforeInitialization(new AnnotatedEventListener(), "beanName");
        Object postProcessedBean = testSubject.postProcessAfterInitialization(result1, "beanName");

        verify(mockAdapter).setEventBus(mockEventBus);
        verify(mockAdapter).setApplicationContext(mockApplicationContext);
        verify(mockAdapter).afterPropertiesSet();

        verify(mockAdapter, never()).destroy();

        testSubject.postProcessBeforeDestruction(postProcessedBean, "beanName");

        verify(mockAdapter).destroy();
    }

    public static class AnnotatedEventListener {

        private int invocationCount;

        @EventHandler
        public void handleEvent(DomainEvent event) {
            invocationCount++;
        }
    }
}
