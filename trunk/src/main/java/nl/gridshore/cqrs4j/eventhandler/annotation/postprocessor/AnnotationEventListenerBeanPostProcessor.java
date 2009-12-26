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

import nl.gridshore.cqrs4j.eventhandler.annotation.BufferingAnnotationEventListenerAdapter;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Spring Bean post processor that automatically generates an adapter for each bean containing {@link
 * nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler} annotated methods.
 * <p/>
 * Optionally, this bean can be configured with an {@link org.springframework.core.task.AsyncTaskExecutor} that will be
 * used by the adapters to register their pollers. This task executor must contain at least one thread per adapter
 * created (meaning one per bean with {@link nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler} annotation).
 * <p/>
 * Beans that already implement the {@link nl.gridshore.cqrs4j.eventhandler.EventListener} interface are skipped, even
 * if they contain a method with the {@link nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler} annotation.
 *
 * @author Allard Buijze
 * @see TransactionalAnnotationEventListenerBeanPostProcessor
 * @see nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler
 * @see nl.gridshore.cqrs4j.eventhandler.annotation.BufferingAnnotationEventListenerAdapter
 */
public class AnnotationEventListenerBeanPostProcessor extends BaseAnnotationEventListenerBeanPostProcessor {

    private AsyncTaskExecutor taskExecutor;

    /**
     * Creates a BufferingAnnotationEventListenerAdapter for the given bean. For implementation specific modifications,
     * subclasses should implement the {@link #createAdapterFor(Object)} method.
     *
     * @param bean The bean for which to create an adapter
     * @return a BufferingAnnotationEventListenerAdapter for the given bean
     */
    @Override
    protected BufferingAnnotationEventListenerAdapter adapt(Object bean) {
        BufferingAnnotationEventListenerAdapter adapter = createAdapterFor(bean);
        if (taskExecutor != null) {
            adapter.setTaskExecutor(taskExecutor);
        }
        return adapter;
    }

    /**
     * Create a BufferingAnnotationEventListenerAdapter (or subclass) for the given bean. Specialized implementations
     * should override this method if they wish to create a different type of adapter for the given bean.
     *
     * @param bean the bean for which to create an adapter
     * @return the adapter for the given bean
     */
    protected BufferingAnnotationEventListenerAdapter createAdapterFor(Object bean) {
        return new BufferingAnnotationEventListenerAdapter(bean);
    }

    /**
     * The task executor that the adapters should use to host the pollers thread. Optional. By default, a new single
     * thread task executor is created.
     *
     * @param taskExecutor the task executor that should host the adapters poller thread
     */
    public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }


}
