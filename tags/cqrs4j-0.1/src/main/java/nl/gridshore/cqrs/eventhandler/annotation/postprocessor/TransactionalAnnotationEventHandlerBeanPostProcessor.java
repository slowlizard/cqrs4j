package nl.gridshore.cqrs.eventhandler.annotation.postprocessor;

import nl.gridshore.cqrs.eventhandler.annotation.BufferingAnnotationEventHandlerAdapter;
import nl.gridshore.cqrs.eventhandler.annotation.TransactionalAnnotationEventHandlerAdapter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of the {@link AnnotationEventHandlerBeanPostProcessor} that checks event handlers for the availability
 * of a {@link org.springframework.transaction.annotation.Transactional} annotation. If one is found, a {@link
 * nl.gridshore.cqrs.eventhandler.annotation.TransactionalAnnotationEventHandlerAdapter transaction aware event handler
 * adapter} is used to adapt the bean.
 *
 * @author Allard Buijze
 * @see org.springframework.transaction.annotation.Transactional
 * @see nl.gridshore.cqrs.eventhandler.annotation.postprocessor.AnnotationEventHandlerBeanPostProcessor
 * @see nl.gridshore.cqrs.eventhandler.annotation.TransactionalAnnotationEventHandlerAdapter
 */
public class TransactionalAnnotationEventHandlerBeanPostProcessor extends AnnotationEventHandlerBeanPostProcessor {

    private PlatformTransactionManager transactionManager;

    @Override
    protected BufferingAnnotationEventHandlerAdapter adapt(Object bean) {
        if (!isTransactional(bean)) {
            return new BufferingAnnotationEventHandlerAdapter(bean);
        }
        TransactionalAnnotationEventHandlerAdapter adapter = new TransactionalAnnotationEventHandlerAdapter(bean);

        if (transactionManager != null) {
            adapter.setTransactionManager(transactionManager);
        }

        return adapter;
    }

    private boolean isTransactional(Object bean) {
        return annotationOnClass(bean) || annotationOnMethod(bean);
    }

    private boolean annotationOnMethod(Object bean) {
        final AtomicBoolean found = new AtomicBoolean(false);
        ReflectionUtils.doWithMethods(bean.getClass(), new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                if (AnnotationUtils.getAnnotation(method, Transactional.class) != null) {
                    found.set(true);
                }
            }
        });
        return found.get();
    }

    private boolean annotationOnClass(Object bean) {
        return bean.getClass().isAnnotationPresent(Transactional.class);
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

}