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
 * ExecutorService. It will yield to other threads when necessary.
 *
 * @author Allard Buijze
 * @since 0.3
 */
class EventProcessingScheduler {

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
    public EventProcessingScheduler(EventListener eventListener, ExecutorService executorService) {
        this.eventListener = eventListener;
        this.executorService = executorService;
    }

    /**
     * Schedule an event for processing
     *
     * @param event the event to schedule
     */
    public synchronized void scheduleEvent(DomainEvent event) {
        events.add(event);
        scheduleIfNecessary();
    }

    private synchronized DomainEvent nextEvent() {
        DomainEvent event = events.poll();
        if (event == null) {
            isScheduled = false;
        }
        return event;
    }

    private synchronized boolean yield() {
        if (events.size() > 0) {
            isScheduled = true;
            try {
                executorService.submit(new EventHandlerInvoker());
            }
            catch (RejectedExecutionException e) {
                return false;
            }
        } else {
            isScheduled = false;
        }
        return true;
    }

    private void scheduleIfNecessary() {
        if (!isScheduled) {
            isScheduled = true;
            executorService.submit(new EventHandlerInvoker());
        }
    }

    private class EventHandlerInvoker implements Runnable {

        @Override
        public void run() {
            DomainEvent event;
            int transactionSize = 0;
            while ((event = nextEvent()) != null) {
                transactionSize++;
                eventListener.handle(event);
                // TODO: Add transaction support
                // TODO: Add configurable yield policy
                if (transactionSize >= 50 && yield()) {
                    break;
                }
            }
        }

    }
}
