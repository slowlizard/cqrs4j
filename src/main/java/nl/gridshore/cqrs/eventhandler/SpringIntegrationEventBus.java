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
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Allard Buijze
 */
public class SpringIntegrationEventBus implements EventBus {

    private SubscribableChannel channel;
    private ConcurrentMap<EventListener, MessageHandler> handlers = new ConcurrentHashMap<EventListener, MessageHandler>();

    @Override
    public void unsubscribe(EventListener eventListener) {
        MessageHandler messageHandler = handlers.remove(eventListener);
        channel.unsubscribe(messageHandler);
    }

    @Override
    public void subscribe(EventListener eventListener) {
        MessageHandler messagehandler = new MessageHandlerAdapter(eventListener);
        handlers.putIfAbsent(eventListener, messagehandler);
        channel.subscribe(messagehandler);
    }

    @Override
    public void publish(DomainEvent event) {
        channel.send(new GenericMessage<Object>(event));
    }

    private class MessageHandlerAdapter implements MessageHandler {

        private final EventListener eventListener;

        public MessageHandlerAdapter(EventListener eventListener) {
            this.eventListener = eventListener;
        }

        @Override
        public void handleMessage(Message<?> message)
                throws MessageHandlingException, MessageDeliveryException {
            eventListener.handle((DomainEvent) message.getPayload());
        }
    }

    public void setChannel(SubscribableChannel channel) {
        this.channel = channel;
    }
}
