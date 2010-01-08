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

package nl.gridshore.cqrs4j.eventhandler;

import nl.gridshore.cqrs4j.DomainEvent;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * The EventProcessingScheduler is responsible for scheduling all events within the same SerializationIdentifier in an
 * ExecutorService. It will only handle events that were present in the queue at the moment processing started. Any
 * events added later will be rescheduled automatically.
 *
 * @author Allard Buijze
 * @since 0.3
 */
class SimpleEventProcessingScheduler implements EventProcessingScheduler {

    private final Queue<DomainEvent> events = new LinkedList<DomainEvent>();
    private final EventListener eventListener;
    private boolean isScheduled = false;
    private final ExecutorService executorService;

    /**
     * Initialize a scheduler for the given <code>eventListener</code> using the given <code>executorService</code>.
     *
     * @param eventListener   The event listener for which this scheduler schedules events
     * @param executorService The executor service that will process the events
     */
    public SimpleEventProcessingScheduler(EventListener eventListener, ExecutorService executorService) {
        this.eventListener = eventListener;
        this.executorService = executorService;
    }

    /**
     * Schedules an event for processing. Will schedule a new invoker task (see {@link #newEventHandlerInvokerTask()} if
     * none is currently active.
     * <p/>
     * This method is thread safe
     *
     * @param event the event to schedule
     */
    @Override
    public synchronized void scheduleEvent(DomainEvent event) {
        events.add(event);
        scheduleIfNecessary();
    }

    /**
     * Returns the next event in the queue, if available. If returns false if no further events are available for
     * processing. In that case, it will also set the scheduled status to false.
     * <p/>
     * This method is thread safe
     *
     * @return the next DomainEvent for processing, of null if none is available
     */
    protected synchronized DomainEvent nextEvent() {
        DomainEvent event = events.poll();
        if (event == null) {
            isScheduled = false;
        }
        return event;
    }

    /**
     * Tries to yield to other threads be rescheduling processing of any further queued events. If rescheduling fails,
     * this call returns false, indicating that processing should continue in the current thread.
     * <p/>
     * This method is thread safe
     *
     * @return true if yielding succeeded, false otherwise.
     */
    protected synchronized boolean yield() {
        if (events.size() > 0) {
            isScheduled = true;
            try {
                executorService.submit(newEventHandlerInvokerTask());
            }
            catch (RejectedExecutionException e) {
                return false;
            }
        } else {
            isScheduled = false;
        }
        return true;
    }

    /**
     * Will look at the current scheduling status and schedule an EventHandlerInvokerTask if none is already active.
     * <p/>
     * This method is thread safe
     */
    protected synchronized void scheduleIfNecessary() {
        if (!isScheduled) {
            isScheduled = true;
            executorService.submit(newEventHandlerInvokerTask());
        }
    }

    /**
     * Constructs a new Runnable that invokes a the event handler for the next events in the queue.
     *
     * @return a new instance that will perform the necessary method invocations on event handlers.
     */
    protected Runnable newEventHandlerInvokerTask() {
        return new EventHandlerInvoker();
    }

    private class EventHandlerInvoker implements Runnable {

        @Override
        public void run() {
            DomainEvent event;
            int transactionSize = 0;
            int maxTransactionSize = events.size();
            while ((event = nextEvent()) != null) {
                transactionSize++;
                eventListener.handle(event);
                if (transactionSize >= maxTransactionSize && yield()) {
                    break;
                }
            }
        }
    }
}
