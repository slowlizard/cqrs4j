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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * The ListenerInboxManager is responsible for delegating each incoming event to the relevant EventProcessingScheduler
 * for processing, depending on the serialization identifier of the event.
 *
 * @author Allard Buijze
 * @since 0.3
 */
class ListenerInboxManager {

    private final EventListener eventListener;
    private final ExecutorService executor;
    private final ConcurrentMap<Object, EventProcessingScheduler> transactions = new ConcurrentHashMap<Object, EventProcessingScheduler>();
    private final EventHandlingSerializationPolicy eventHandlingSerializationPolicy;

    /**
     * Initialize the ListenerInboxManager for the given <code>eventListener</code> using the given
     * <code>executor</code>.
     *
     * @param eventListener The event listener this instance manages
     * @param executor      The executor that processes the events
     */
    public ListenerInboxManager(EventListener eventListener, ExecutorService executor) {
        this.eventListener = eventListener;
        this.executor = executor;
        this.eventHandlingSerializationPolicy = eventListener.getEventHandlingSerializationPolicy();
    }

    /**
     * Adds an event to the relevant scheduler.
     *
     * @param event The event to schedule
     */
    public void addEvent(DomainEvent event) {
        if (eventListener.canHandle(event.getClass())) {
            final Object policy = eventHandlingSerializationPolicy.getSerializationIdentifierFor(event);
            if (policy == null) {
                executor.submit(new SingleEventInvocationTask(eventListener, event));
            } else {
                if (!transactions.containsKey(policy)) {
                    transactions.putIfAbsent(policy, new EventProcessingScheduler(eventListener, executor));
                }
                transactions.get(policy).scheduleEvent(event);
            }
        }
    }

    private static class SingleEventInvocationTask implements Runnable {

        private final EventListener eventListener;
        private final DomainEvent event;

        public SingleEventInvocationTask(
                EventListener eventListener, DomainEvent event) {
            this.eventListener = eventListener;
            this.event = event;
        }

        @Override
        public void run() {
            eventListener.handle(event);
        }
    }
}
