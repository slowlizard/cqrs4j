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
import nl.gridshore.cqrs4j.StubDomainEvent;
import nl.gridshore.cqrs4j.eventhandler.EventBus;
import org.junit.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class BufferingAnnotationEventListenerAdapterTest {

    @Test
    public void testHandlingIsRefusedWhenOnThreadInterrupt() throws Exception {
        BufferingAnnotationEventListenerAdapter testSubject = new BufferingAnnotationEventListenerAdapter(new AnnotatedEventHandler());
        testSubject.setEventBus(mock(EventBus.class));
        testSubject.initialize();
        Thread.currentThread().interrupt();

        try {
            testSubject.handle(new StubDomainEvent());
            fail("Expected adapter to react on interrupt with an UnhandledEventExceptionTest.");
        } catch (EventHandlingRejectedException e) {
            assertTrue(e.getMessage().contains("interrupt"));
        }

    }

    private static class AnnotatedEventHandler {

        @EventHandler
        public void handleEvent(DomainEvent event) {
        }

    }
}
