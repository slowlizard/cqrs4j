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

package nl.gridshore.cqrs4j.eventhandler.annotation.postprocessor;

import nl.gridshore.cqrs4j.eventhandler.annotation.BufferingAnnotationEventListenerAdapter;
import nl.gridshore.cqrs4j.eventhandler.annotation.TransactionalBufferingAnnotationEventListenerAdapter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of the {@link BufferingAnnotationEventListenerBeanPostProcessor} that checks event listeners for the
 * availability of a {@link org.springframework.transaction.annotation.Transactional} annotation. If one is found, a
 * {@link nl.gridshore.cqrs4j.eventhandler.annotation.TransactionalBufferingAnnotationEventListenerAdapter transaction
 * aware event listener adapter} is used to adapt the bean.
 * <p/>
 * Note: even if only a single method is marked as {@link org.springframework.transaction.annotation.Transactional}, all
 * event handlers will be called within a transaction.
 *
 * @author Allard Buijze
 * @see org.springframework.transaction.annotation.Transactional
 * @see BufferingAnnotationEventListenerBeanPostProcessor
 * @see nl.gridshore.cqrs4j.eventhandler.annotation.TransactionalBufferingAnnotationEventListenerAdapter
 * @since 0.1
 */
public class TransactionalBufferingAnnotationEventListenerBeanPostProcessor extends
                                                                            BufferingAnnotationEventListenerBeanPostProcessor {

    private PlatformTransactionManager transactionManager;
    private long retryDelayMillis = 1000;

    /**
     * Specialized implementation of the adapt method that creates a Transaction aware adapter for transactional event
     * listeners.
     *
     * @param bean the bean to adapt
     * @return an annotation event listener adapter for the given bean
     */
    @Override
    protected BufferingAnnotationEventListenerAdapter createAdapterFor(Object bean) {
        if (!isTransactional(bean)) {
            return new BufferingAnnotationEventListenerAdapter(bean);
        }
        TransactionalBufferingAnnotationEventListenerAdapter adapter = new TransactionalBufferingAnnotationEventListenerAdapter(
                bean);

        adapter.setTransactionManager(transactionManager);
        adapter.setRetryDelayMillis(retryDelayMillis);

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
        ReflectionUtils.doWithMethods(bean.getClass(), new IsTransactionalMethodCallback(found));
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

    /**
     * Set the amount of milliseconds the poller should wait before retrying a transaction when one has failed. If the
     * value if negative, retrying is disabled on any transactional event handlers.
     * <p/>
     * Transactions that failed due to one of the following exceptions are retried: <ul> <li>{@link
     * org.springframework.dao.RecoverableDataAccessException} <li>{@link org.springframework.dao.TransientDataAccessException}
     * <li>{@link java.sql.SQLTransientException} <li>{@link org.springframework.transaction.TransactionException}
     * <li>{@link java.sql.BatchUpdateException} <li>{@link java.sql.SQLRecoverableException} </ul>
     * <p/>
     * Defaults to 1000 (1 second)
     *
     * @param retryDelayMillis the amount of milliseconds to wait beforer retrying a transaction
     */
    public void setRetryDelayMillis(long retryDelayMillis) {
        this.retryDelayMillis = retryDelayMillis;
    }

    private static class IsTransactionalMethodCallback implements ReflectionUtils.MethodCallback {

        private final AtomicBoolean found;

        public IsTransactionalMethodCallback(AtomicBoolean found) {
            this.found = found;
        }

        @Override
        public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
            if (AnnotationUtils.getAnnotation(method, Transactional.class) != null) {
                found.set(true);
            }
        }
    }
}
