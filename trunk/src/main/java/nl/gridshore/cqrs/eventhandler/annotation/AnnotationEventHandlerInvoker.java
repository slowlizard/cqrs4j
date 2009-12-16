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

package nl.gridshore.cqrs.eventhandler.annotation;

import nl.gridshore.cqrs.DomainEvent;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Allard Buijze
 */
class AnnotationEventHandlerInvoker {

    // guarded by "this"
    private final transient Map<Class, Method> eventHandlers = new WeakHashMap<Class, Method>();
    private final Object target;

    public AnnotationEventHandlerInvoker(Object target) {
        this.target = target;
        validateHandlerMethods(target);
    }

    public static void validateHandlerMethods(Object annotatedEventHandler) {
        validateHandlerMethods(annotatedEventHandler.getClass());
    }

    public static void validateHandlerMethods(Class<?> clazz) {
        ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                if (method.isAnnotationPresent(EventHandler.class)) {
                    if (method.getParameterTypes().length != 1) {
                        throw new UnsupportedHandlerMethodException(String.format(
                                "Event Handling class %s contains method %s that has more than one parameter. "
                                        + "Either remove @EventHandler annotation or reduce to a single parameter.",
                                method.getDeclaringClass().getSimpleName(),
                                method.getName()),
                                                                    method);
                    }
                    if (!DomainEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                        throw new UnsupportedHandlerMethodException(String.format(
                                "Event Handling class %s contains method %s that has an invalid parameter. "
                                        + "Parameter must extend from DomainEvent",
                                method.getDeclaringClass().getSimpleName(),
                                method.getName()),
                                                                    method);
                    }
                }
            }
        });
    }

    protected void invokeEventHandlerMethod(DomainEvent event) {
        Method m = findEventHandlerMethod(event.getClass());
        if (m == null) {
            // event handler doesn't support this type of event
            return;
        }
        try {
            if (!m.isAccessible()) {
                m.setAccessible(true);
            }
            m.invoke(target, event);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(String.format(
                    "An error occurred when applying an event of type [%s]",
                    event.getClass().getSimpleName()), e);
        } catch (InvocationTargetException e) {
            throw new UnsupportedOperationException(String.format(
                    "An error occurred when applying an event of type [%s]",
                    event.getClass().getSimpleName()), e);
        }
    }

    protected EventHandler findEventHandlerConfiguration(DomainEvent event) {
        Method m = findEventHandlerMethod(event.getClass());
        if (m != null && m.isAnnotationPresent(EventHandler.class)) {
            return m.getAnnotation(EventHandler.class);
        }
        return null;
    }

    protected boolean canHandle(Class<? extends DomainEvent> eventClass) {
        return findEventHandlerMethod(eventClass) != null;
    }

    private synchronized Method findEventHandlerMethod(Class<? extends DomainEvent> eventClass) {
        // there is no synchronized implementation of the WeakHashMap
        if (!eventHandlers.containsKey(eventClass)) {
            scanHierarchyForEventHandler(eventClass);
        }
        return eventHandlers.get(eventClass);
    }

    private void scanHierarchyForEventHandler(final Class<? extends DomainEvent> eventClass) {
        final AtomicReference<Method> bestMethodSoFar = new AtomicReference<Method>(null);
        ReflectionUtils.doWithMethods(target.getClass(), new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                if (method.isAnnotationPresent(EventHandler.class)
                        && method.getParameterTypes()[0].isAssignableFrom(eventClass)) {
                    // method is eligible, but is it the best?
                    if (bestMethodSoFar.get() == null) {
                        // if we have none yet, this one is the best
                        bestMethodSoFar.set(method);
                    } else if (bestMethodSoFar.get().getDeclaringClass().equals(method.getDeclaringClass())
                            && bestMethodSoFar.get().getParameterTypes()[0].isAssignableFrom(
                            method.getParameterTypes()[0])) {
                        // this one is more specific, so it wins
                        bestMethodSoFar.set(method);
                    }
                }
            }
        });
        eventHandlers.put(eventClass, bestMethodSoFar.get());
    }


}
