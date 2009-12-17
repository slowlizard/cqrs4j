package nl.gridshore.cqrs.eventhandler.annotation.postprocessor;

import net.sf.cglib.proxy.Enhancer;
import nl.gridshore.cqrs.DomainEvent;
import nl.gridshore.cqrs.eventhandler.EventBus;
import nl.gridshore.cqrs.eventhandler.annotation.EventHandler;
import org.junit.*;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class AnnotationEventHandlerBeanPostProcessorTest {

    private AnnotationEventHandlerBeanPostProcessor testSubject;
    private ApplicationContext mockApplicationContext;
    private EventBus mockEventBus;

    @Before
    public void setUp() {
        testSubject = spy(new AnnotationEventHandlerBeanPostProcessor());
        mockApplicationContext = mock(ApplicationContext.class);
        testSubject.setApplicationContext(mockApplicationContext);
        mockEventBus = mock(EventBus.class);
        when(mockApplicationContext.getBean(EventBus.class)).thenReturn(mockEventBus);
    }

    @Test
    public void testPostProcessBean_ProxyLifeCycle() throws Exception {
        Object actualResult = testSubject.postProcessAfterInitialization(new SimpleEventHandler(), "beanName");

        assertTrue(Enhancer.isEnhanced(actualResult.getClass()));
        assertTrue(actualResult instanceof SimpleEventHandler);
        assertTrue(actualResult instanceof nl.gridshore.cqrs.eventhandler.EventHandler);

        verify(mockEventBus, times(1)).subscribe(isA(nl.gridshore.cqrs.eventhandler.EventHandler.class));

        testSubject.destroy();

        verify(mockEventBus, times(1)).unsubscribe(isA(nl.gridshore.cqrs.eventhandler.EventHandler.class));
    }

    @Test
    public void testPostProcessBean_AlreadyHandlerIsNotEnhanced() {
        RealEventHandler eventHandler = new RealEventHandler();
        Object actualResult = testSubject.postProcessAfterInitialization(eventHandler, "beanName");
        assertFalse(Enhancer.isEnhanced(actualResult.getClass()));
        assertSame(eventHandler, actualResult);
    }

    @Test
    public void testPostProcessBean_PlainObjectIsIgnored() {
        NotAnEventHandler eventHandler = new NotAnEventHandler();
        Object actualResult = testSubject.postProcessAfterInitialization(eventHandler, "beanName");
        assertFalse(Enhancer.isEnhanced(actualResult.getClass()));
        assertSame(eventHandler, actualResult);
    }


    public static class NotAnEventHandler {

    }

    public static class SimpleEventHandler {

        @EventHandler
        public void handleEvent(DomainEvent event) {
            // not relevant
        }
    }

    public static class RealEventHandler implements nl.gridshore.cqrs.eventhandler.EventHandler {

        @Override
        public boolean canHandle(Class<? extends DomainEvent> eventType) {
            return true;
        }

        @Override
        public void handle(DomainEvent event) {
            // not relevant
        }

        @EventHandler
        public void handleEvent(DomainEvent event) {

        }
    }
}
