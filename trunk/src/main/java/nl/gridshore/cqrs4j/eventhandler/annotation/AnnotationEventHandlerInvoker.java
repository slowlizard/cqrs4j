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

import nl.gridshore.cqrs4j.DomainEvent;
import nl.gridshore.cqrs4j.eventhandler.EventListener;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.Arrays;

import static java.security.AccessController.doPrivileged;

/**
 * Utility class that supports invocation of specific handler methods for a given event. See {@link
 * nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler} for the rules for resolving the appropriate method.
 *
 * @author Allard Buijze
 * @see nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler
 * @since 0.1
 */
class AnnotationEventHandlerInvoker {

    private final Object target;

    /**
     * Initialize an event handler invoker that invokes handlers on the given <code>target</code>
     *
     * @param target the bean on which to invoke event handlers
     */
    public AnnotationEventHandlerInvoker(Object target) {
        this.target = target;
        validateHandlerMethods(target);
    }

    /**
     * Checks the validity of all event handler methods on the given <code>annotatedEventListener</code>.
     *
     * @param annotatedEventListener the event listener to validate handler methods on
     * @throws UnsupportedHandlerMethodException
     *          if an invalid handler is found
     * @see nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler
     */
    public static void validateHandlerMethods(Object annotatedEventListener) {
        validateHandlerMethods(annotatedEventListener.getClass());
    }

    /**
     * Checks the validity of all event handler methods on the given <code>annotatedEventListenerType</code>.
     *
     * @param annotatedEventListenerType the event listener type to validate handler methods on
     * @throws UnsupportedHandlerMethodException
     *          if an invalid handler is found
     * @see nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler
     */
    public static void validateHandlerMethods(Class<?> annotatedEventListenerType) {
        ReflectionUtils.doWithMethods(annotatedEventListenerType, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                if (method.isAnnotationPresent(EventHandler.class)) {
                    if (method.getParameterTypes().length != 1) {
                        throw new UnsupportedHandlerMethodException(String.format(
                                "Event Handling class %s contains method %s that has more than one parameter. "
                                        + "Either remove @EventListener annotation or reduce to a single parameter.",
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
                    Method[] forbiddenMethods = EventListener.class.getDeclaredMethods();
                    for (Method forbiddenMethod : forbiddenMethods) {
                        if (method.getName().equals(forbiddenMethod.getName())
                                && Arrays.equals(method.getParameterTypes(), forbiddenMethod.getParameterTypes())) {
                            throw new UnsupportedHandlerMethodException(String.format(
                                    "Event Handling class %s contains method %s that has a naming conflict with a method on"
                                            + "the EventHandler interface. Please rename the method.",
                                    method.getDeclaringClass().getSimpleName(),
                                    method.getName()),
                                                                        method);
                        }
                    }
                }
            }
        });
    }

    /**
     * Invoke the event handler on the target for the given <code>event</code>
     *
     * @param event the event to handle
     */
    protected void invokeEventHandlerMethod(DomainEvent event) {
        final Method m = findEventHandlerMethod(event.getClass());
        if (m == null) {
            // event listener doesn't support this type of event
            return;
        }
        try {
            if (!m.isAccessible()) {
                doPrivileged(new PrivilegedAccessibilityAction(m));
            }
            m.invoke(target, event);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(String.format(
                    "An error occurred when applying an event of type [%s]",
                    event.getClass().getSimpleName()), e);
        } catch (InvocationTargetException e) {
            throw new UnsupportedOperationException(String.format(
                    "An error occurred when applying an event of type [%s]",
                    event.getClass().getSimpleName()), e.getCause() != null ? e.getCause() : e);
        }
    }

    /**
     * Find the configuration for the event handler that would handle the given <code>event</code>
     *
     * @param event the event for which to find handler configuration
     * @return the configuration for the event handler that would handle the given <code>event</code>
     */
    protected EventHandler findEventHandlerConfiguration(DomainEvent event) {
        Method m = findEventHandlerMethod(event.getClass());
        if (m != null && m.isAnnotationPresent(EventHandler.class)) {
            return m.getAnnotation(EventHandler.class);
        }
        return null;
    }

    /**
     * Indicates whether the target event listener has a handler for the given <code>eventClass</code>.
     *
     * @param eventClass the event class to find an handler for
     * @return true if an event handler is found, false otherwise
     */
    protected boolean hasHandlerFor(Class<? extends DomainEvent> eventClass) {
        return findEventHandlerMethod(eventClass) != null;
    }

    private Method findEventHandlerMethod(final Class<? extends DomainEvent> eventClass) {
        MostSuitableEventHandlerCallback callback = new MostSuitableEventHandlerCallback(eventClass);
        ReflectionUtils.doWithMethods(target.getClass(), callback, callback);
        return callback.foundHandler();
    }

    private static class PrivilegedAccessibilityAction implements PrivilegedAction<Object> {

        private final Method m;

        public PrivilegedAccessibilityAction(Method m) {
            this.m = m;
        }

        @Override
        public Object run() {
            m.setAccessible(true);
            return Void.class;
        }
    }

    /**
     * MethodCallback and MethodFilter implementation that finds the most suitable event handler method for an event of
     * given type.
     * <p/>
     * Note that this callback must used both as MethodCallback and MethodCallback.
     * <p/>
     * Example:<br/> <code>MostSuitableEventHandlerCallback callback = new MostSuitableEventHandlerCallback(eventType)<br/>
     * ReflectionUtils.doWithMethods(eventListenerClass, callback, callback);</code>
     */
    private static class MostSuitableEventHandlerCallback
            implements ReflectionUtils.MethodCallback, ReflectionUtils.MethodFilter {

        private final Class<? extends DomainEvent> eventClass;
        private Method bestMethodSoFar;

        /**
         * Initialize this callback for the given event class. The callback will find the most suitable method for an
         * event of given type.
         *
         * @param eventClass The type of event to find the handler for
         */
        public MostSuitableEventHandlerCallback(Class<? extends DomainEvent> eventClass) {
            this.eventClass = eventClass;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean matches(Method method) {
            Method foundSoFar = bestMethodSoFar;
            Class<?> classUnderInvestigation = method.getDeclaringClass();
            boolean bestInClassFound =
                    foundSoFar != null
                            && !classUnderInvestigation.equals(foundSoFar.getDeclaringClass())
                            && classUnderInvestigation.isAssignableFrom(foundSoFar.getDeclaringClass());
            return !bestInClassFound && method.isAnnotationPresent(EventHandler.class)
                    && method.getParameterTypes()[0].isAssignableFrom(eventClass);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
            // method is eligible, but is it the best?
            if (bestMethodSoFar == null) {
                // if we have none yet, this one is the best
                bestMethodSoFar = method;
            } else if (bestMethodSoFar.getDeclaringClass().equals(method.getDeclaringClass())
                    && bestMethodSoFar.getParameterTypes()[0].isAssignableFrom(
                    method.getParameterTypes()[0])) {
                // this one is more specific, so it wins
                bestMethodSoFar = method;
            }
        }

        public Method foundHandler() {
            return bestMethodSoFar;
        }
    }
}
