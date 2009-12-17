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
import org.springframework.util.Assert;

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

    protected boolean isRunning() {
        return running.get();
    }

    protected boolean hasEventsInQueue() {
        return queue.isEmpty();
    }

    protected DomainEvent getNextEventFromQueue(final int timeout, final TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    protected DomainEvent getNextEventFromQueue() {
        return queue.poll();
    }

    protected Runnable getPoller() {
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


    public void setTaskExecutor(AsyncTaskExecutor executor) {
        this.executor = executor;
    }

}
