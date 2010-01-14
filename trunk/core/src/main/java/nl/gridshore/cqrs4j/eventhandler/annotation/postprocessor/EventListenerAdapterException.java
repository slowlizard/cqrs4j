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

package nl.gridshore.cqrs4j.eventhandler.annotation.postprocessor;

/**
 * Exception indicating that an error occurred while creating an EventListenerAdapter for an event listener
 *
 * @author Allard Buijze
 * @since 0.2
 */
public class EventListenerAdapterException extends RuntimeException {

    /**
     * Initialize an EventListenerAdapter
     *
     * @param message The message describing the cause of the exception
     * @param cause   The exception causing this one
     */
    public EventListenerAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
