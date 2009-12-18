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

package nl.gridshore.cqrs4j.eventhandler.annotation;

import java.lang.reflect.Method;

/**
 * Thrown when an {@link nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler} annotated method was found that does
 * not conform to the rules that apply to those methods.
 *
 * @author Allard Buijze
 * @see nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler
 */
public class UnsupportedHandlerMethodException extends RuntimeException {

    private final Method violatingMethod;

    public UnsupportedHandlerMethodException(String message, Method violatingMethod) {
        super(message);
        this.violatingMethod = violatingMethod;
    }

    /**
     * A reference to the method that violated the event handler rules
     *
     * @return the method that violated the event handler rules
     *
     * @see nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler
     */
    public Method getViolatingMethod() {
        return violatingMethod;
    }
}
