package nl.gridshore.cqrs4j.eventhandler.annotation.postprocessor;

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

    /**
     * {@inheritDoc}
     */
    @Override
    protected AnnotationEventListenerAdapter createEventHandlerAdapter(Object bean) {
        return new AnnotationEventListenerAdapter(bean);
    }
}
