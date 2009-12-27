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

import nl.gridshore.cqrs4j.DomainEvent;
import nl.gridshore.cqrs4j.eventhandler.annotation.BufferingAnnotationEventListenerAdapter;
import nl.gridshore.cqrs4j.eventhandler.annotation.TransactionalAnnotationEventListenerAdapter;
import org.junit.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class TransactionalAnnotationEventHandlerBeanPostProcessorTest {

    private TransactionalAnnotationEventListenerBeanPostProcessor testSubject;

    @Before
    public void setUp() {
        testSubject = new TransactionalAnnotationEventListenerBeanPostProcessor();
        testSubject.setTransactionManager(mock(PlatformTransactionManager.class));
    }

    @Test
    public void testAdapt_NonTransactionalBean() {
        BufferingAnnotationEventListenerAdapter actualResult = testSubject.adapt(new NonTransactionalEventHandler());
        assertEquals(BufferingAnnotationEventListenerAdapter.class, actualResult.getClass());
    }

    @Test
    public void testAdapt_TransactionOnClass() {
        BufferingAnnotationEventListenerAdapter actualResult = testSubject.adapt(new TransactionOnClassEventHandler());
        assertEquals(TransactionalAnnotationEventListenerAdapter.class, actualResult.getClass());
    }

    @Test
    public void testAdapt_TransactionOnMethod() {
        BufferingAnnotationEventListenerAdapter actualResult = testSubject.adapt(new TransactionOnMethodEventHandler());
        assertEquals(TransactionalAnnotationEventListenerAdapter.class, actualResult.getClass());
    }

    @Test
    public void testAdapt_TransactionOnSuperClass() {
        BufferingAnnotationEventListenerAdapter actualResult = testSubject
                .adapt(new TransactionInheritedEventHandler());
        assertEquals(TransactionalAnnotationEventListenerAdapter.class, actualResult.getClass());
    }

    private static class NonTransactionalEventHandler {

        public void handleSomeEvent(DomainEvent event) {
            // we don't care
        }
    }

    @Transactional
    private static class TransactionOnClassEventHandler {

        public void handleSomeEvent(DomainEvent event) {
            // we don't care
        }
    }

    private static class TransactionOnMethodEventHandler {

        @Transactional
        public void handleSomeEvent(DomainEvent event) {
            // we don't care
        }
    }

    private static class TransactionInheritedEventHandler extends TransactionOnClassEventHandler {

        public void handleSomeEvent(DomainEvent event) {
            // we don't care
        }
    }


}
