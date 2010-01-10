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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * EventBus implementation that uses an ExecutorService to dispatch events asynchronously. This dispatcher takes into
 * account the {@link EventSequencingPolicy} provided by the {@link EventListener} for sequential handling
 * requirements.
 *
 * @author Allard Buijze
 * @see EventSequencingPolicy
 * @see nl.gridshore.cqrs4j.eventhandler.EventListener
 * @since 0.3
 */
public class AsyncEventBus implements EventBus {

    private final static int DEFAULT_CORE_POOL_SIZE = 5;
    private final static int DEFAULT_MAX_POOL_SIZE = 25;
    private final static long DEFAULT_KEEP_ALIVE_TIME = 5;
    private final static TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MINUTES;

    private ExecutorService executorService;
    private final ConcurrentMap<EventListener, EventHandlingSequenceManager> listenerManagers =
            new ConcurrentHashMap<EventListener, EventHandlingSequenceManager>();
    private boolean stopExecutorServiceOnShutdown = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(DomainEvent event) {
        for (EventHandlingSequenceManager eventHandlingSequencing : listenerManagers.values()) {
            eventHandlingSequencing.addEvent(event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(EventListener eventListener) {
        if (!listenerManagers.containsKey(eventListener)) {
            listenerManagers.putIfAbsent(eventListener, newEventHandlingSequenceManager(eventListener));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe(EventListener eventListener) {
        listenerManagers.remove(eventListener);
    }

    /**
     * Will configure a default executor service if none has been wired. This method must be called after initialization
     * of all properties.
     * <p/>
     * {@inheritDoc}
     */
    @PostConstruct
    public void initialize() throws Exception {
        if (executorService == null) {
            stopExecutorServiceOnShutdown = true;
            executorService = new ThreadPoolExecutor(DEFAULT_CORE_POOL_SIZE,
                                                     DEFAULT_MAX_POOL_SIZE,
                                                     DEFAULT_KEEP_ALIVE_TIME,
                                                     DEFAULT_TIME_UNIT,
                                                     new LinkedBlockingQueue<Runnable>());
        }
    }

    /**
     * {@inheritDoc}
     */
    @PreDestroy
    public void destroy() throws Exception {
        if (executorService != null && stopExecutorServiceOnShutdown) {
            executorService.shutdown();
        }
    }

    /**
     * Creates a new EventHandlingSequenceManager for the given event listener.
     *
     * @param eventListener The event listener that the EventHandlingSequenceManager should manage
     * @return a new EventHandlingSequenceManager instance
     */
    protected EventHandlingSequenceManager newEventHandlingSequenceManager(EventListener eventListener) {
        return new EventHandlingSequenceManager(eventListener, getExecutorService());
    }

    /**
     * Accessor for the executor service used to dispatch events in this event bus
     *
     * @return the executor service used to dispatch events in this event bus
     */
    protected ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Sets the ExecutorService instance to use to handle events. Typically, this will be a ThreadPoolExecutor
     * implementation with an unbounded blocking queue.
     * <p/>
     * Defaults to a ThreadPoolExecutor with 5 core threads and a max pool size of 25 threads with a timeout of 5
     * minutes.
     *
     * @param executorService the executor service to use for event handling
     */
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Defines whether or not to shutdown the executor service when the EventBus is stopped. Defaults to
     * <code>true</code> if the default executor service is used. Defaults to <code>false</code> if a custom executor
     * service is defined using the {@link #setExecutorService(java.util.concurrent.ExecutorService)} method.
     *
     * @param stopExecutorServiceOnShutdown Whether or not to shutdown the executor service when the event bus is
     *                                      stopped
     */
    public void setShutdownExecutorServiceOnShutdown(boolean stopExecutorServiceOnShutdown) {
        this.stopExecutorServiceOnShutdown = stopExecutorServiceOnShutdown;
    }
}
