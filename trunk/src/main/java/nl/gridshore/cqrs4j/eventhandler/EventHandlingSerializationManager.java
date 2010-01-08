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
 * The EventHandlingSerializationManager is responsible for delegating each incoming event to the relevant {@link
 * EventProcessingScheduler} for processing, depending on the serialization identifier of the event.
 *
 * @author Allard Buijze
 * @since 0.3
 */
public class EventHandlingSerializationManager {

    private final EventListener eventListener;
    private final ExecutorService executorService;
    private final ConcurrentMap<Object, EventProcessingScheduler> transactions =
            new ConcurrentHashMap<Object, EventProcessingScheduler>();
    private final EventHandlingSerializationPolicy eventHandlingSerializationPolicy;

    /**
     * Initialize the EventHandlingSerializationManager for the given <code>eventListener</code> using the given
     * <code>executorService</code>.
     *
     * @param eventListener   The event listener this instance manages
     * @param executorService The executorService that processes the events
     */
    public EventHandlingSerializationManager(EventListener eventListener, ExecutorService executorService) {
        this.eventListener = eventListener;
        this.executorService = executorService;
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
                executorService.submit(new EventInvocationTask(eventListener, event));
            } else {
                if (!transactions.containsKey(policy)) {
                    transactions.putIfAbsent(policy, newProcessingScheduler());
                }
                transactions.get(policy).scheduleEvent(event);
            }
        }
    }

    /**
     * Creates a new scheduler instance for the eventListener that schedules events on the executor service for the
     * managed EventListener.
     *
     * @return a new scheduler instance
     */
    protected EventProcessingScheduler newProcessingScheduler() {
        return new SimpleEventProcessingScheduler(getEventListener(), getExecutorService());
    }

    /**
     * Returns the event listener for which this manager manages event handler invocations.
     *
     * @return the event listener for which this manager manages event handler invocations.
     */
    protected EventListener getEventListener() {
        return eventListener;
    }

    /**
     * Returns the executor service that peforms the actual event handler invocations
     *
     * @return the executor service that peforms the actual event handler invocations
     */
    protected ExecutorService getExecutorService() {
        return executorService;
    }
}
