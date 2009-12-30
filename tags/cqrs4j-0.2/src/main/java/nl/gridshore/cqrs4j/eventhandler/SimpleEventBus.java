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

package nl.gridshore.cqrs4j.eventhandler;

import nl.gridshore.cqrs4j.DomainEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of the {@link nl.gridshore.cqrs4j.eventhandler.EventBus} that directly forwards all published events
 * (in the callers' thread) to all subscribed listeners.
 *
 * @author Allard Buijze
 */
public class SimpleEventBus implements EventBus {

    private final List<EventListener> listeners = new CopyOnWriteArrayList<EventListener>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe(EventListener eventListener) {
        listeners.remove(eventListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(EventListener eventListener) {
        listeners.add(eventListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(DomainEvent event) {
        for (EventListener listener : listeners) {
            if (listener.canHandle(event.getClass())) {
                listener.handle(event);
            }
        }
    }
}