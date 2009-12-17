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

package nl.gridshore.cqrs.eventhandler.annotation.postprocessor;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;
import nl.gridshore.cqrs.eventhandler.annotation.AnnotationEventHandlerAdapter;
import nl.gridshore.cqrs.eventhandler.annotation.BufferingAnnotationEventHandlerAdapter;
import nl.gridshore.cqrs.eventhandler.annotation.EventHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Allard Buijze
 */
public class AnnotationEventHandlerBeanPostProcessor
        implements BeanPostProcessor, ApplicationContextAware, DisposableBean {

    private final List<DisposableBean> beansToDisposeOfAtShutdown = new LinkedList<DisposableBean>();
    private AsyncTaskExecutor taskExecutor;
    private ApplicationContext applicationContext;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        if (targetClass == null) {
            return bean;
        }
        if (isNotEventHandlerSubclass(targetClass) && hasEventHandlerMethod(targetClass)) {
            BufferingAnnotationEventHandlerAdapter adapter = createEventHandlerAdapter(bean);
            beansToDisposeOfAtShutdown.add(adapter);
            return createAdapterProxy(targetClass, bean, adapter);
        }
        return bean;
    }

    private BufferingAnnotationEventHandlerAdapter createEventHandlerAdapter(Object bean) {
        BufferingAnnotationEventHandlerAdapter adapter = adapt(bean);
        adapter.setApplicationContext(applicationContext);
        if (taskExecutor != null) {
            adapter.setTaskExecutor(taskExecutor);
        }
        try {
            adapter.afterPropertiesSet();
        } catch (Exception e) {
            throw new BeansException("Error occurred while wrapping an event handler", e) {
            };
        }
        return adapter;
    }

    protected BufferingAnnotationEventHandlerAdapter adapt(Object bean) {
        return new BufferingAnnotationEventHandlerAdapter(bean);
    }

    private Object createAdapterProxy(Class targetClass, Object eventHandler, AnnotationEventHandlerAdapter adapter) {
        Class[] adapterInterfaces = ClassUtils.getAllInterfaces(adapter);

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);
        enhancer.setClassLoader(targetClass.getClassLoader());
        enhancer.setInterfaces(adapterInterfaces);
        enhancer.setCallback(new AdapterInvocationHandler(adapterInterfaces, adapter, eventHandler));
        return enhancer.create();
    }

    private boolean isNotEventHandlerSubclass(Class<?> beanClass) {
        return !nl.gridshore.cqrs.eventhandler.EventHandler.class.isAssignableFrom(beanClass);
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

    @Override
    public void destroy() throws Exception {
        for (DisposableBean bean : beansToDisposeOfAtShutdown) {
            bean.destroy();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    private static class AdapterInvocationHandler implements InvocationHandler {

        private final Class[] adapterInterfaces;

        private final AnnotationEventHandlerAdapter adapter;

        private final Object bean;

        public AdapterInvocationHandler(Class[] adapterInterfaces, AnnotationEventHandlerAdapter adapter, Object bean) {
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
