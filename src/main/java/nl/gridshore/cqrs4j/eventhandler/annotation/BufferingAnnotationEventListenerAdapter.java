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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link AnnotationEventListenerAdapter} implementation that buffers all incoming events into an internally managed
 * queue. This allows the event providing thread to return immediately and continue processing. Event Handling is forced
 * to run in another thread.
 * <p/>
 * This adapter is configured such that events are processed in exactly the order they are received in.
 * <p/>
 * On application context shutdown, this event listener will refuse any incoming events, but will continue processing
 * until all events in the queue have been processed.
 * <p/>
 * By default, an internal task executor (with a single thread) is created for the event handling thread to run in.
 * Alternatively, you may configure another TaskExecutor. You need to make sure there are sufficient threads available
 * in the executor.
 *
 * @author Allard Buijze
 */
public class BufferingAnnotationEventListenerAdapter extends AnnotationEventListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(BufferingAnnotationEventListenerAdapter.class);

    private final BlockingQueue<DomainEvent> queue = new LinkedBlockingQueue<DomainEvent>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private AsyncTaskExecutor executor;
    private Future<?> runningTask;

    /**
     * Initialize the AnnotationEventListenerAdapter for the given <code>annotatedEventListener</code>.
     *
     * @param annotatedEventListener the event listener
     */
    public BufferingAnnotationEventListenerAdapter(Object annotatedEventListener) {
        super(annotatedEventListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(DomainEvent event) {
        Assert.state(running.get(), "EventListener must be running to accept events");
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to add event to queue", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception {
        super.destroy();
        running.set(false);
        // force the thread to wait until the last event is processed
        runningTask.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if (executor == null) {
            executor = new SimpleAsyncTaskExecutor(getClass().getSimpleName() + "-");
        }
        running.set(true);
        runningTask = executor.submit(createPoller());
    }

    /**
     * Process the event in the callers' thread.
     *
     * @param event the event to process
     */
    protected void handleInternal(DomainEvent event) {
        super.handle(event);
    }

    /**
     * Indicates whether the poller is operational.
     *
     * @return true if the poller is active (either processing events or waiting for new ones).
     */
    protected boolean isRunning() {
        return running.get();
    }

    /**
     * Indicates whether there are any events in the queue
     *
     * @return true if there are events in the queue
     */
    protected boolean hasEventsInQueue() {
        return !queue.isEmpty();
    }

    /**
     * Returns the next event from the queue, blocking the call if there are none.
     *
     * @param timeout the time to wait for an incoming event
     * @param unit    the unit of the <code>timeout</code>
     * @return the next event from the queue or <code>null</code> if a timeout occurred.
     *
     * @throws InterruptedException if the thread was interrupted while waiting for incoming events
     */
    protected DomainEvent getNextEventFromQueue(final int timeout, final TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    /**
     * Returns the next event from the queue. This method will never block
     *
     * @return the next event from the queue or <code>null</code> if no events are available.
     */
    protected DomainEvent getNextEventFromQueue() {
        return queue.poll();
    }

    /**
     * Creates a new Poller.
     *
     * @return a newly instantiated Poller
     */
    protected Runnable createPoller() {
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

    /**
     * Sets the TaskExecutor that should host the thread of the poller. If not set, a simple one-thread task executor is
     * created.
     *
     * @param executor the executor to use for the Poller
     */
    public void setTaskExecutor(AsyncTaskExecutor executor) {
        this.executor = executor;
    }

}
