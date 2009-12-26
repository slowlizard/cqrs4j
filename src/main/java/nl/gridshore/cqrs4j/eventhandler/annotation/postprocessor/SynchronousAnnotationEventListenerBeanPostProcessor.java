package nl.gridshore.cqrs4j.eventhandler.annotation.postprocessor;

import nl.gridshore.cqrs4j.eventhandler.EventBus;
import nl.gridshore.cqrs4j.eventhandler.annotation.AnnotationEventListenerAdapter;

/**
 * Spring Bean post processor that automatically generates an adapter for each bean containing {@link
 * nl.gridshore.cqrs4j.eventhandler.annotation.EventHandler} annotated methods.
 * <p/>
 * The beans processed by this bean post processor will handle events in the thread that delivers them. This makes this
 * post processor useful in the case of tests or when the dispatching mechanism takes care of asynchronous (but
 * sequential) event delivery.
 *
 * @author Allard Buijze
 */
public class SynchronousAnnotationEventListenerBeanPostProcessor extends BaseAnnotationEventListenerBeanPostProcessor {

    private EventBus eventBus;

    /**
     * {@inheritDoc}
     */
    @Override
    protected AnnotationEventListenerAdapter createEventHandlerAdapter(Object bean) {
        AnnotationEventListenerAdapter adapter = new AnnotationEventListenerAdapter(bean);
        adapter.setApplicationContext(applicationContext);
        adapter.setEventBus(eventBus);
        return adapter;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }
}
