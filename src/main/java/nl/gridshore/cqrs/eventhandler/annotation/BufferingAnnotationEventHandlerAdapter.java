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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Allard Buijze
 */
public class BufferingAnnotationEventHandlerAdapter extends AnnotationEventHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(BufferingAnnotationEventHandlerAdapter.class);

    private final BlockingQueue<DomainEvent> queue = new LinkedBlockingQueue<DomainEvent>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private AsyncTaskExecutor executor;
    private Future<?> runningTask;
    private PlatformTransactionManager transactionManager;

    public BufferingAnnotationEventHandlerAdapter(Object annotatedEventHandler) {
        super(annotatedEventHandler);
    }

    @Override
    public void handle(DomainEvent event) {
        Assert.state(running.get(), "EventHandler must be running to accept events");
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to add event to queue", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        running.set(false);
        // force the thread to wait until the last event is processed
        runningTask.get();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if (executor == null) {
            executor = new SimpleAsyncTaskExecutor(getClass().getSimpleName() + "-");
        }
        running.set(true);
        runningTask = executor.submit(getPoller());
    }

    protected void handleInternal(DomainEvent event) {
        super.handle(event);
    }

    protected Runnable getPoller() {
        if (transactionManager != null) {
            return new TransactionalPoller();
        }
        return new Poller();
    }

    private class Poller implements Runnable {

        @Override
        public void run() {
            while (!queue.isEmpty() || running.get()) {
                try {
                    DomainEvent event = queue.poll(1000, TimeUnit.MILLISECONDS);
                    if (event != null) {
                        handleInternal(event);
                    }
                } catch (InterruptedException e) {
                    // probably the thread executor is stopped
                    logger.info("***** Thread executor stopped");
                }
            }
        }
    }

    private class TransactionalPoller implements Runnable {

        @Override
        public void run() {
            while (!queue.isEmpty() || running.get()) {
                pollAndHandle();
            }
        }

        private void pollAndHandle() {
            try {
                DomainEvent event = queue.poll(1000, TimeUnit.MILLISECONDS);
                if (event != null && canHandle(event.getClass())) {
                    handleEvent(event);
                }
            } catch (InterruptedException e) {
                // probably the thread executor is stopped
                logger.info("***** Thread executor stopped");
            } catch (Exception ex) {
                logger.info("****############********", ex);
            }
        }

        private void handleEvent(DomainEvent event) {
            EventHandler configuration = getConfigurationFor(event);
            int transactionCommitThreshold = configuration.commitThreshold();
            final List<DomainEvent> eventBatch = new ArrayList<DomainEvent>();
            eventBatch.add(event);
            while ((event = queue.poll()) != null) {
                if (canHandle(event.getClass())) {
                    configuration = getConfigurationFor(event);
                    if (transactionCommitThreshold < eventBatch.size()) {
                        // the poll cannot return null, as the peek said to and the queue is not shared
                        eventBatch.add(event);
                        transactionCommitThreshold = Math.min(transactionCommitThreshold,
                                                              configuration.commitThreshold());
                    } else {
                        processEventBatch(eventBatch);
                        eventBatch.clear();
                        eventBatch.add(event);
                        transactionCommitThreshold = configuration.commitThreshold();
                    }
                }
            }
            // now, we have some events
            processEventBatch(eventBatch);
        }

        private void processEventBatch(final List<DomainEvent> eventBatch) {
            if (eventBatch.size() == 0) {
                return;
            }
            new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    logger.debug("Started transaction for " + eventBatch.size() + " events");
                    for (DomainEvent event : eventBatch) {
                        handleInternal(event);
                    }
                }
            });
        }
    }


    public void setTaskExecutor(AsyncTaskExecutor executor) {
        this.executor = executor;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
