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
import nl.gridshore.cqrs4j.eventhandler.EventSequencingPolicy;
import nl.gridshore.cqrs4j.eventhandler.FullConcurrencyPolicy;
import nl.gridshore.cqrs4j.eventhandler.SequentialPolicy;
import org.junit.*;

import static org.junit.Assert.*;

/**
 * @author Allard Buijze
 */
public class AnnotationEventListenerAdapterTest {

    @Test
    public void testHandlingPolicy_Default() throws Exception {
        AnnotatedEventHandler annotatedEventHandler = new AnnotatedEventHandler();
        AnnotationEventListenerAdapter adapter = new AnnotationEventListenerAdapter(annotatedEventHandler);

        EventSequencingPolicy actualPolicy = adapter.getEventSequencingPolicy();

        assertEquals(SequentialPolicy.class, actualPolicy.getClass());
    }

    @Test
    public void testHandlingPolicy_FromAnnotation() throws Exception {
        ConcurrentAnnotatedEventHandler annotatedEventHandler = new ConcurrentAnnotatedEventHandler();
        AnnotationEventListenerAdapter adapter = new AnnotationEventListenerAdapter(annotatedEventHandler);

        EventSequencingPolicy actualPolicy = adapter.getEventSequencingPolicy();

        assertEquals(FullConcurrencyPolicy.class, actualPolicy.getClass());
    }

    @Test
    public void testHandlingPolicy_IllegalClassFromAnnotation() throws Exception {
        IllegalConcurrentAnnotatedEventHandler annotatedEventHandler = new IllegalConcurrentAnnotatedEventHandler();
        try {
            new AnnotationEventListenerAdapter(annotatedEventHandler);
            fail("Expected UnsupportedPolicyException");
        }
        catch (UnsupportedPolicyException e) {
            assertTrue("Incomplete message:" + e.getMessage(), e.getMessage().contains("IllegalConcurrencyPolicy"));
        }

    }

    private static class AnnotatedEventHandler {

        @EventHandler
        public void handleEvent(DomainEvent event) {
        }

    }

    @ConcurrentEventListener(sequencingPolicyClass = FullConcurrencyPolicy.class)
    private static class ConcurrentAnnotatedEventHandler {

        @EventHandler
        public void handleEvent(DomainEvent event) {
        }

    }

    @ConcurrentEventListener(sequencingPolicyClass = IllegalConcurrencyPolicy.class)
    private static class IllegalConcurrentAnnotatedEventHandler {

        @EventHandler
        public void handleEvent(DomainEvent event) {
        }

    }

    private static class IllegalConcurrencyPolicy implements EventSequencingPolicy {

        public IllegalConcurrencyPolicy(String name) {
            // just for the sake of not having a default constructor
        }

        @Override
        public Object getSequenceIdentifierFor(DomainEvent event) {
            return null;
        }
    }
}
