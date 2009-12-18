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

package nl.gridshore.cqrs.repository.eventsourcing;

/**
 * Indicates that the given events stream could not be stored or read due to an underlying exception.
 *
 * @author Allard Buijze
 */
public class EventStorageException extends RuntimeException {

    public EventStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
