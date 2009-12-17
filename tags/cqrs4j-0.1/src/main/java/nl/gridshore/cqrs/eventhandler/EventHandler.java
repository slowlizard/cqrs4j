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

package nl.gridshore.cqrs.eventhandler;

import nl.gridshore.cqrs.DomainEvent;

/**
 * Interface to be implemented by classes that can handle events.
 *
 * @author Allard Buijze
 * @see nl.gridshore.cqrs.eventhandler.EventDispatcher
 * @see nl.gridshore.cqrs.eventhandler.EventBus
 * @see nl.gridshore.cqrs.eventhandler.annotation.EventHandler
 */
public interface EventHandler {

    boolean canHandle(Class<? extends DomainEvent> eventType);

    void handle(DomainEvent event);
}
