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

package nl.gridshore.cqrs4j.eventhandler.annotation;

import nl.gridshore.cqrs4j.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.BatchUpdateException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Specialization of the {@link BufferingAnnotationEventListenerAdapter} that allows events to be handled
 * transactionally. This requires the configuration of a {@link org.springframework.transaction.PlatformTransactionManager}.
 * <p/>
 * The event listener classes need to be marked as transactional. A handler is considered marked if the class or any of
 * its methods are annotated with {@link org.springframework.transaction.annotation.Transactional}.
 *
 * @author Allard Buijze
 * @see org.springframework.transaction.annotation.Transactional
 * @see nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler
 * @see nl.gridshore.cqrs4j.eventhandler.annotation.postprocessor.TransactionalAnnotationEventListenerBeanPostProcessor
 */
public class TransactionalAnnotationEventListenerAdapter extends BufferingAnnotationEventListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalAnnotationEventListenerAdapter.class);
    private static final List<Class<? extends Exception>> transientExceptions =
            Arrays.asList(
                    RecoverableDataAccessException.class,
                    TransientDataAccessException.class,
                    SQLTransientException.class,
                    TransactionException.class,
                    BatchUpdateException.class,
                    SQLRecoverableException.class);

    private PlatformTransactionManager transactionManager;
    private long retryDelayMillis = 1000;

    /**
     * Initialize the TransactionalAnnotationEventListenerAdapter for the given <code>annotatedEventListener</code>.
     *
     * @param annotatedEventListener the event listener
     */
    public TransactionalAnnotationEventListenerAdapter(Object annotatedEventListener) {
        super(annotatedEventListener);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Runnable createPoller() {
        return new TransactionalPoller(transactionManager);
    }

    private class TransactionalPoller implements Runnable {

        private final TransactionTemplate transactionTemplate;

        private TransactionalPoller(PlatformTransactionManager transactionManager) {
            transactionTemplate = new TransactionTemplate(transactionManager);
        }

        @Override
        public void run() {
            while (hasEventsInQueue() || isRunning()) {
                pollAndHandle();
            }
        }

        private void pollAndHandle() {
            try {
                DomainEvent event = getNextEventFromQueue(1000, TimeUnit.MILLISECONDS);
                if (event != null && canHandle(event.getClass())) {
                    handleEvent(event);
                }
            } catch (InterruptedException e) {
                // probably the thread executor is stopped
                logger.info("***** Thread executor stopped");
            } catch (Exception ex) {
                logger.info("****############********", ex);
            }
        }

        private void handleEvent(DomainEvent event) {
            EventHandler configuration = getConfigurationFor(event);
            int transactionCommitThreshold = configuration.commitThreshold();
            final List<DomainEvent> eventBatch = new ArrayList<DomainEvent>();
            eventBatch.add(event);
            while ((event = getNextEventFromQueue()) != null) {
                if (canHandle(event.getClass())) {
                    configuration = getConfigurationFor(event);
                    if (eventFitsInBatch(transactionCommitThreshold, configuration.commitThreshold(), eventBatch)) {
                        eventBatch.add(event);
                        transactionCommitThreshold = Math.min(transactionCommitThreshold,
                                                              configuration.commitThreshold());
                    } else {
                        tryProcessEventBatch(eventBatch);
                        eventBatch.clear();
                        eventBatch.add(event);
                        transactionCommitThreshold = configuration.commitThreshold();
                    }
                }
            }
            // now, we have some events
            tryProcessEventBatch(eventBatch);
        }

        private void tryProcessEventBatch(List<DomainEvent> eventBatch) {
            try {
                processEventBatch(eventBatch);
            }
            catch (RuntimeException e) {
                if (isTransientException(e)) {
                    logger.warn("An exception occurred in a transaction.", e);
                    if (retryDelayMillis >= 0) {
                        retryBatch(eventBatch);
                    }
                } else {
                    throw e;
                }
            }
        }

        private boolean eventFitsInBatch(int currentTransactionCommitThreshold,
                                         int nextTransactionCommitThreshold,
                                         List<DomainEvent> eventBatch) {
            int transactionCommitThreshold = Math.min(currentTransactionCommitThreshold,
                                                      nextTransactionCommitThreshold);
            return transactionCommitThreshold >= eventBatch.size() + 1;
        }

        @SuppressWarnings({"SimplifiableIfStatement"})
        private boolean isTransientException(Throwable e) {
            if (isAssignableToAnyOf(e, transientExceptions)) {
                return true;
            }
            if (e.getCause() != null) {
                return isTransientException(e.getCause());
            }
            return false;
        }

        private boolean isAssignableToAnyOf(Throwable exception, List<Class<? extends Exception>> transientExceptions) {
            for (Class exceptionClass : transientExceptions) {
                if (exceptionClass.isInstance(exception)) {
                    return true;
                }
            }
            return false;
        }

        private void retryBatch(List<DomainEvent> eventBatch) {
            boolean shouldContinue = true;
            while (shouldContinue && isRunning()) {
                logger.info("Retrying in {} milliseconds", retryDelayMillis);
                try {
                    Thread.sleep(retryDelayMillis);
                    processEventBatch(eventBatch);
                    shouldContinue = false;
                }
                catch (InterruptedException e) {
                    logger.error("Thread was interrupted while in retry mode. Ignoring failed transaction.");
                    shouldContinue = false;
                }
                catch (RuntimeException ex) {
                    shouldContinue = isTransientException(ex);
                    logger.warn("An exception occurred while retrying a transaction: " + ex.getMessage());
                }
            }
        }

        private void processEventBatch(final List<DomainEvent> eventBatch) {
            if (eventBatch.size() == 0) {
                return;
            }
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    logger.debug("Started transaction for " + eventBatch.size() + " events");
                    for (DomainEvent event : eventBatch) {
                        handleInternal(event);
                    }
                }
            });
        }
    }

    /**
     * Sets the transaction manager to use when handling events transactionally.
     *
     * @param transactionManager the transaction manager to use when handling events transactionally.
     */
    @Required
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

}
