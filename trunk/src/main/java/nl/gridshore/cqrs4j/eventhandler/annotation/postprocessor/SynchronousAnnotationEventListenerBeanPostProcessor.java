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

package nl.gridshore.cqrs4j.eventhandler.annotation.postprocessor;

import nl.gridshore.cqrs4j.eventhandler.annotation.AnnotationEventListenerAdapter;

/**
 * Spring Bean post processor that automatically generates an adapter for each bean containing {@link
 * nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler} annotated methods.
 * <p/>
 * The beans processed by this bean post processor will handle events in the thread that delivers them. This makes this
 * post processor useful in the case of tests or when the dispatching mechanism takes care of asynchronous event
 * delivery.
 *
 * @author Allard Buijze
 */
public class SynchronousAnnotationEventListenerBeanPostProcessor extends BaseAnnotationEventListenerBeanPostProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    protected AnnotationEventListenerAdapter adapt(Object bean) {
        return new AnnotationEventListenerAdapter(bean);
    }
}
