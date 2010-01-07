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

package nl.gridshore.cqrs4j.eventhandler.annotation;

import nl.gridshore.cqrs4j.eventhandler.EventHandlingSerializationPolicy;
import nl.gridshore.cqrs4j.eventhandler.FullySerializedPolicy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a class as a concurrency aware EventListener. This optional type-level annotations allows the
 * definition of the concurrency policy for this EventListener. The default values of this annotations will provide the
 * exact same behavior as the omission of the entire annotation, albeit less explicit.
 * <p/>
 * This annotation allows the configuration of any {@link #policyClass() arbitrary class}, as long as it implements the
 * {@link nl.gridshore.cqrs4j.eventhandler.EventHandlingSerializationPolicy} interface. It also needs to have (at least)
 * a no-arg constructor.
 *
 * @author Allard Buijze
 * @since 0.3
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConcurrentEventListener {

    /**
     * Defines the policy type to use for event handling serialization. The provided class must implement {@link
     * nl.gridshore.cqrs4j.eventhandler.EventHandlingSerializationPolicy} and provide an accessible no-arg constructor.
     */
    Class<? extends EventHandlingSerializationPolicy> policyClass() default FullySerializedPolicy.class;

}
