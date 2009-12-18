package nl.gridshore.cqrs4j.eventhandler.annotation.postprocessor;

import nl.gridshore.cqrs4j.DomainEvent;
import nl.gridshore.cqrs4j.eventhandler.annotation.BufferingAnnotationEventListenerAdapter;
import nl.gridshore.cqrs4j.eventhandler.annotation.TransactionalAnnotationEventListenerAdapter;
import org.junit.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class TransactionalAnnotationEventHandlerBeanPostProcessorTest {

    private TransactionalAnnotationEventListenerBeanPostProcessor testSubject;

    @Before
    public void setUp() {
        testSubject = new TransactionalAnnotationEventListenerBeanPostProcessor();
        testSubject.setTransactionManager(mock(PlatformTransactionManager.class));
    }

    @Test
    public void testAdapt_NonTransactionalBean() {
        BufferingAnnotationEventListenerAdapter actualResult = testSubject.adapt(new NonTransactionalEventHandler());
        assertEquals(BufferingAnnotationEventListenerAdapter.class, actualResult.getClass());
    }

    @Test
    public void testAdapt_TransactionOnClass() {
        BufferingAnnotationEventListenerAdapter actualResult = testSubject.adapt(new TransactionOnClassEventHandler());
        assertEquals(TransactionalAnnotationEventListenerAdapter.class, actualResult.getClass());
    }

    @Test
    public void testAdapt_TransactionOnMethod() {
        BufferingAnnotationEventListenerAdapter actualResult = testSubject.adapt(new TransactionOnMethodEventHandler());
        assertEquals(TransactionalAnnotationEventListenerAdapter.class, actualResult.getClass());
    }

    @Test
    public void testAdapt_TransactionOnSuperClass() {
        BufferingAnnotationEventListenerAdapter actualResult = testSubject
                .adapt(new TransactionInheritedEventHandler());
        assertEquals(TransactionalAnnotationEventListenerAdapter.class, actualResult.getClass());
    }

    private static class NonTransactionalEventHandler {

        public void handleSomeEvent(DomainEvent event) {
            // we don't care
        }
    }

    @Transactional
    private static class TransactionOnClassEventHandler {

        public void handleSomeEvent(DomainEvent event) {
            // we don't care
        }
    }

    private static class TransactionOnMethodEventHandler {

        @Transactional
        public void handleSomeEvent(DomainEvent event) {
            // we don't care
        }
    }

    private static class TransactionInheritedEventHandler extends TransactionOnClassEventHandler {

        public void handleSomeEvent(DomainEvent event) {
            // we don't care
        }
    }


}
