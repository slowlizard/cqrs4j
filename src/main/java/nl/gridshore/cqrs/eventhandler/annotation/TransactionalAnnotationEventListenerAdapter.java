package nl.gridshore.cqrs.eventhandler.annotation;

import nl.gridshore.cqrs.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
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
 * @see nl.gridshore.cqrs.eventhandler.annotation.EventHandler
 * @see nl.gridshore.cqrs.eventhandler.annotation.postprocessor.TransactionalAnnotationEventListenerBeanPostProcessor
 */
public class TransactionalAnnotationEventListenerAdapter extends BufferingAnnotationEventListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalAnnotationEventListenerAdapter.class);

    private PlatformTransactionManager transactionManager;

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
        return new TransactionalPoller();
    }

    private class TransactionalPoller implements Runnable {

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
                    if (transactionCommitThreshold < eventBatch.size()) {
                        // the poll cannot return null, as the peek said to and the queue is not shared
                        eventBatch.add(event);
                        transactionCommitThreshold = Math.min(transactionCommitThreshold,
                                                              configuration.commitThreshold());
                    } else {
                        processEventBatch(eventBatch);
                        eventBatch.clear();
                        eventBatch.add(event);
                        transactionCommitThreshold = configuration.commitThreshold();
                    }
                }
            }
            // now, we have some events
            processEventBatch(eventBatch);
        }

        private void processEventBatch(final List<DomainEvent> eventBatch) {
            if (eventBatch.size() == 0) {
                return;
            }
            new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
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

}
