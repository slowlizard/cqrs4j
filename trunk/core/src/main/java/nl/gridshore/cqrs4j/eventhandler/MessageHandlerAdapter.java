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
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;

/**
 * Adapter that allows an EventListener to be registered as a Spring Integration {@link
 * org.springframework.integration.message.MessageHandler}.
 *
 * @author Allard Buijze
 * @since 0.1
 */
class MessageHandlerAdapter implements MessageHandler {

    private final EventListener eventListener;

    /**
     * Initialize an adapter for the given <code>eventListener</code>
     *
     * @param eventListener the event listener to adapt
     */
    public MessageHandlerAdapter(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleMessage(Message<?> message) throws MessageHandlingException, MessageDeliveryException {
        DomainEvent event = (DomainEvent) message.getPayload();
        if (eventListener.canHandle(event.getClass())) {
            eventListener.handle(event);
        }
    }
}
