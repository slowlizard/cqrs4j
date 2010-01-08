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

/**
 * A Runnable instance that (eventually) invokes an event handler method on an event listener.
 *
 * @author Allard Buijze
 * @since 0.3
 */
public class EventInvocationTask implements Runnable {

    private final EventListener eventListener;
    private final DomainEvent event;

    /**
     * Initializes this task to eventually invoke given <code>eventListener</code> with given <code>event</code> .
     *
     * @param eventListener the event listener to invoke
     * @param event         the event the listener should handle
     */
    public EventInvocationTask(EventListener eventListener, DomainEvent event) {
        this.eventListener = eventListener;
        this.event = event;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        eventListener.handle(event);
    }
}
