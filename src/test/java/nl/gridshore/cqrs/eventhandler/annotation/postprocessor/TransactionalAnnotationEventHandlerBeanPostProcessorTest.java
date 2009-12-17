package nl.gridshore.cqrs.eventhandler.annotation.postprocessor;

import nl.gridshore.cqrs.DomainEvent;
import nl.gridshore.cqrs.eventhandler.annotation.BufferingAnnotationEventHandlerAdapter;
import nl.gridshore.cqrs.eventhandler.annotation.TransactionalAnnotationEventHandlerAdapter;
import org.junit.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class TransactionalAnnotationEventHandlerBeanPostProcessorTest {

    private TransactionalAnnotationEventHandlerBeanPostProcessor testSubject;

    @Before
    public void setUp() {
        testSubject = new TransactionalAnnotationEventHandlerBeanPostProcessor();
        testSubject.setTransactionManager(mock(PlatformTransactionManager.class));
    }

    @Test
    public void testAdapt_NonTransactionalBean() {
        BufferingAnnotationEventHandlerAdapter actualResult = testSubject.adapt(new NonTransactionalEventHandler());
        assertEquals(BufferingAnnotationEventHandlerAdapter.class, actualResult.getClass());
    }

    @Test
    public void testAdapt_TransactionOnClass() {
        BufferingAnnotationEventHandlerAdapter actualResult = testSubject.adapt(new TransactionOnClassEventHandler());
        assertEquals(TransactionalAnnotationEventHandlerAdapter.class, actualResult.getClass());
    }

    @Test
    public void testAdapt_TransactionOnMethod() {
        BufferingAnnotationEventHandlerAdapter actualResult = testSubject.adapt(new TransactionOnMethodEventHandler());
        assertEquals(TransactionalAnnotationEventHandlerAdapter.class, actualResult.getClass());
    }

    @Test
    public void testAdapt_TransactionOnSuperClass() {
        BufferingAnnotationEventHandlerAdapter actualResult = testSubject.adapt(new TransactionInheritedEventHandler());
        assertEquals(TransactionalAnnotationEventHandlerAdapter.class, actualResult.getClass());
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
