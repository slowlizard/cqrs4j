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

import nl.gridshore.cqrs4j.DomainEvent;
import nl.gridshore.cqrs4j.eventhandler.EventBus;
import org.junit.*;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class AnnotationEventListenerAdapterTest {

    @Test
    public void testEventBusIsAutowired() throws Exception {
        AnnotatedEventHandler annotatedEventHandler = new AnnotatedEventHandler();
        AnnotationEventListenerAdapter adapter = new AnnotationEventListenerAdapter(annotatedEventHandler);
        ApplicationContext mockApplicationContext = mock(ApplicationContext.class);
        adapter.setApplicationContext(mockApplicationContext);
        EventBus mockEventBus = mock(EventBus.class);
        when(mockApplicationContext.getBean(EventBus.class)).thenReturn(mockEventBus);

        adapter.afterPropertiesSet();

        verify(mockApplicationContext).getBean(EventBus.class);
        verify(mockEventBus).subscribe(adapter);

        assertSame(annotatedEventHandler, adapter.getTarget());
    }

    private static class AnnotatedEventHandler {

        @EventHandler
        public void handleEvent(DomainEvent event) {
        }

    }

}