package nl.gridshore.cqrs4j.eventhandler.annotation.postprocessor;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;
import nl.gridshore.cqrs4j.eventhandler.EventListener;
import nl.gridshore.cqrs4j.eventhandler.annotation.AnnotationEventListenerAdapter;
import nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler;
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
public abstract class BaseAnnotationEventListenerBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, DisposableBean {

    private final List<DisposableBean> beansToDisposeOfAtShutdown = new LinkedList<DisposableBean>();
    protected ApplicationContext applicationContext;

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
            AnnotationEventListenerAdapter adapter = createEventHandlerAdapter(bean);
            beansToDisposeOfAtShutdown.add(adapter);
            return createAdapterProxy(targetClass, bean, adapter);
        }
        return bean;
    }

    protected abstract AnnotationEventListenerAdapter createEventHandlerAdapter(Object bean);

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
