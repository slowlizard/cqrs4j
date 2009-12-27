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
import nl.gridshore.cqrs4j.StubDomainEvent;
import nl.gridshore.cqrs4j.eventhandler.EventBus;
import org.junit.*;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class TransactionalAnnotationEventListenerAdapterTest {

    private AnnotatedEventHandler eventHandler = new AnnotatedEventHandler();

    @Test
//(timeout = 5000)
public void testTransactionIsRetried() throws Exception {
        TransactionalAnnotationEventListenerAdapter adapter = new TransactionalAnnotationEventListenerAdapter(
                eventHandler);
        MockPlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
        adapter.setTransactionManager(transactionManager);
        adapter.setRetryDelayMillis(1);
        adapter.setEventBus(mock(EventBus.class));
        adapter.afterPropertiesSet();

        /*
         * these "marker" events are added to make threading more predictable. The first makes the handler
         * thread wait until the main thread is done adding events to the queue.
         * The seconds event prevents the main thread to stop the listener before it has had the chance to retry any
         * transactions.
         */
        StarterEvent initializeEvent = new StarterEvent();
        FinishEvent shutdownEvent = new FinishEvent();

        adapter.handle(initializeEvent);
        for (int t = 0; t < 4; t++) {
            adapter.handle(new StubDomainEvent());
        }
        adapter.handle(shutdownEvent);
        initializeEvent.latch.countDown();
        shutdownEvent.latch.await();

        adapter.destroy();

        // the 3rd invocation failed. Next run had 4 invocations, making 7 in total
        assertEquals(7, eventHandler.invocationCount.get());
        assertEquals(3, transactionManager.commitCount.get());
        assertEquals(1, transactionManager.rollbackCount.get());
        assertEquals(4, transactionManager.transactionCount.get());
    }

    private static class AnnotatedEventHandler {

        private final AtomicInteger invocationCount = new AtomicInteger(0);

        @EventHandler(commitThreshold = 1)
        public void waitAtMarkerEvent(StarterEvent event) throws InterruptedException {
            event.latch.await();
        }

        @EventHandler(commitThreshold = 1)
        public void waitAtMarkerEvent(FinishEvent event) throws InterruptedException {
            event.latch.countDown();
        }

        @EventHandler(commitThreshold = 100)
        public void unreliableEventHandler(DomainEvent event) {
            int count = invocationCount.incrementAndGet();
            if (count == 3) {
                throw new TransientDataAccessResourceException("Our mock resource is not available right now");
            }
        }

        public int getInvocationCount() {
            return invocationCount.get();
        }
    }

    private static class MockPlatformTransactionManager implements PlatformTransactionManager {

        private AtomicInteger transactionCount = new AtomicInteger(0);
        private AtomicInteger commitCount = new AtomicInteger(0);
        private AtomicInteger rollbackCount = new AtomicInteger(0);

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            transactionCount.incrementAndGet();
            return new SimpleTransactionStatus(true);
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
            commitCount.incrementAndGet();
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
            rollbackCount.incrementAndGet();
        }
    }

    private static class StarterEvent extends DomainEvent {

        private final CountDownLatch latch = new CountDownLatch(1);
    }

    private static class FinishEvent extends DomainEvent {

        private final CountDownLatch latch = new CountDownLatch(1);
    }
}
