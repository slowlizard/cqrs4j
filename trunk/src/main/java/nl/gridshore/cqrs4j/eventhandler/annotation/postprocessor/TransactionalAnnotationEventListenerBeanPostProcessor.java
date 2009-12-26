package nl.gridshore.cqrs4j.eventhandler.annotation.postprocessor;

import nl.gridshore.cqrs4j.eventhandler.annotation.BufferingAnnotationEventListenerAdapter;
import nl.gridshore.cqrs4j.eventhandler.annotation.TransactionalAnnotationEventListenerAdapter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of the {@link AnnotationEventListenerBeanPostProcessor} that checks event listeners for the
 * availability of a {@link org.springframework.transaction.annotation.Transactional} annotation. If one is found, a
 * {@link nl.gridshore.cqrs4j.eventhandler.annotation.TransactionalAnnotationEventListenerAdapter transaction aware
 * event listener adapter} is used to adapt the bean.
 * <p/>
 * Note: even if only a single method is marked as {@link org.springframework.transaction.annotation.Transactional}, all
 * event handlers will be called within a transaction.
 *
 * @author Allard Buijze
 * @see org.springframework.transaction.annotation.Transactional
 * @see AnnotationEventListenerBeanPostProcessor
 * @see nl.gridshore.cqrs4j.eventhandler.annotation.TransactionalAnnotationEventListenerAdapter
 */
public class TransactionalAnnotationEventListenerBeanPostProcessor extends AnnotationEventListenerBeanPostProcessor {

    private PlatformTransactionManager transactionManager;

    /**
     * Specialized implementation of the adapt method that creates a Transaction aware adapter for transactional event
     * listeners.
     *
     * @param bean the bean to adapt
     */
    @Override
    protected BufferingAnnotationEventListenerAdapter createAdapterFor(Object bean) {
        if (!isTransactional(bean)) {
            return new BufferingAnnotationEventListenerAdapter(bean);
        }
        TransactionalAnnotationEventListenerAdapter adapter = new TransactionalAnnotationEventListenerAdapter(bean);

        if (transactionManager != null) {
            adapter.setTransactionManager(transactionManager);
        }

        return adapter;
    }

    /**
     * Indicates whether the given bean has any @Transactional annotations are present, either on the class or on a
     * method.
     *
     * @param bean The object to search annotations on
     * @return true if bean is marked as transactional
     */
    private boolean isTransactional(Object bean) {
        return transactionalAnnotationOnClass(bean) || transactionalAnnotationOnMethod(bean);
    }

    private boolean transactionalAnnotationOnMethod(Object bean) {
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

    private boolean transactionalAnnotationOnClass(Object bean) {
        return bean.getClass().isAnnotationPresent(Transactional.class);
    }

    /**
     * Sets the transaction manager to use when processing events transactionally
     *
     * @param transactionManager the transaction manager to use when processing events transactionally
     */
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

}
