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

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;
import nl.gridshore.cqrs4j.eventhandler.EventBus;
import nl.gridshore.cqrs4j.eventhandler.EventListener;
import nl.gridshore.cqrs4j.eventhandler.annotation.AnnotationEventListenerAdapter;
import nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Allard Buijze
 */
public abstract class BaseAnnotationEventListenerBeanPostProcessor
        implements BeanPostProcessor, ApplicationContextAware, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(BaseAnnotationEventListenerBeanPostProcessor.class);

    private final List<DisposableBean> beansToDisposeOfAtShutdown = new LinkedList<DisposableBean>();
    private ApplicationContext applicationContext;
    private EventBus eventBus;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        if (targetClass == null) {
            return bean;
        }
        if (isNotEventHandlerSubclass(targetClass) && hasEventHandlerMethod(targetClass)) {
            AnnotationEventListenerAdapter adapter = initializeAdapterFor(bean);
            beansToDisposeOfAtShutdown.add(adapter);
            return createAdapterProxy(targetClass, bean, adapter);
        }
        return bean;
    }

    /**
     * Create an AnnotationEventListenerAdapter instance of the given {@code bean}. This adapter will receive all event
     * handler calls to be handled by this bean.
     *
     * @param bean The bean that the EventListenerAdapter has to adapt
     * @return an event handler adapter for the given {@code bean}
     */
    private AnnotationEventListenerAdapter initializeAdapterFor(Object bean) {
        AnnotationEventListenerAdapter adapter = adapt(bean);
        adapter.setApplicationContext(applicationContext);
        adapter.setEventBus(eventBus);
        try {
            adapter.afterPropertiesSet();
        } catch (Exception e) {
            String message = String.format("An error occurred while wrapping an event listener: [%s]",
                                           bean.getClass().getSimpleName());
            logger.error(message, e);
            throw new EventListenerAdapterException(message, e);
        }
        return adapter;
    }

    /**
     * Instantiate and perform implementation specific initialisation an event listener adapter for the given bean
     *
     * @param bean The bean for which to create an adapter
     * @return the adapter for the given bean
     */
    protected abstract AnnotationEventListenerAdapter adapt(Object bean);

    private Object createAdapterProxy(Class targetClass, Object eventHandler, AnnotationEventListenerAdapter adapter) {
        Class[] adapterInterfaces = ClassUtils.getAllInterfaces(adapter);

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);
        enhancer.setClassLoader(targetClass.getClassLoader());
        enhancer.setInterfaces(adapterInterfaces);
        enhancer.setCallback(new AdapterInvocationHandler(adapterInterfaces, adapter, eventHandler));
        return enhancer.create();
    }

    private boolean isNotEventHandlerSubclass(Class<?> beanClass) {
        return !EventListener.class.isAssignableFrom(beanClass);
    }

    private boolean hasEventHandlerMethod(Class<?> beanClass) {
        final AtomicBoolean result = new AtomicBoolean(false);
        ReflectionUtils.doWithMethods(beanClass, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                if (method.isAnnotationPresent(EventHandler.class)) {
                    result.set(true);
                }
            }
        });
        return result.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception {
        for (DisposableBean bean : beansToDisposeOfAtShutdown) {
            bean.destroy();
        }
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private static class AdapterInvocationHandler implements InvocationHandler {

        private final Class[] adapterInterfaces;

        private final AnnotationEventListenerAdapter adapter;

        private final Object bean;

        public AdapterInvocationHandler(Class[] adapterInterfaces, AnnotationEventListenerAdapter adapter,
                                        Object bean) {
            this.adapterInterfaces = adapterInterfaces;
            this.adapter = adapter;
            this.bean = bean;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
            // this is where we test the method
            Class declaringClass = method.getDeclaringClass();
            if (arrayContainsEquals(adapterInterfaces, declaringClass)) {
                return method.invoke(adapter, arguments);
            }
            return method.invoke(bean, arguments);
        }

        private <T> boolean arrayContainsEquals(T[] adapterInterfaces, T declaringClass) {
            for (T item : adapterInterfaces) {
                if (declaringClass.equals(item)) {
                    return true;
                }
            }
            return false;
        }
    }
}
