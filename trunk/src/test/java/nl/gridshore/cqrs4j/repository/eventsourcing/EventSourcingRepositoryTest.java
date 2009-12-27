package nl.gridshore.cqrs4j.repository.eventsourcing;

import nl.gridshore.cqrs4j.AbstractAggregateRoot;
import nl.gridshore.cqrs4j.DomainEvent;
import nl.gridshore.cqrs4j.EventStream;
import nl.gridshore.cqrs4j.SimpleEventStream;
import nl.gridshore.cqrs4j.StubDomainEvent;
import nl.gridshore.cqrs4j.eventhandler.EventBus;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class EventSourcingRepositoryTest {

    private EventStore mockEventStore;
    private EventBus mockEventBus;
    private EventSourcingRepository<TestAggregate> testSubject;

    @Before
    public void setUp() {
        mockEventStore = mock(EventStore.class);
        mockEventBus = mock(EventBus.class);
        testSubject = new EventSourcingRepositoryImpl();
        testSubject.setEventBus(mockEventBus);
        testSubject.setEventStore(mockEventStore);
    }

    @Test
    public void testLoadAndSaveAggregate() {
        UUID identifier = UUID.randomUUID();
        StubDomainEvent event1 = new StubDomainEvent(identifier, 1);
        StubDomainEvent event2 = new StubDomainEvent(identifier, 2);
        when(mockEventStore.readEvents("test", identifier)).thenReturn(new SimpleEventStream(event1, event2));

        TestAggregate aggregate = testSubject.load(identifier);

        assertEquals(0, aggregate.getUncommittedEventCount());
        assertEquals(2, aggregate.getHandledEvents().size());
        assertSame(event1, aggregate.getHandledEvents().get(0));
        assertSame(event2, aggregate.getHandledEvents().get(1));

        // now the aggregate is loaded (and hopefully correctly locked)
        StubDomainEvent event3 = new StubDomainEvent(identifier);

        aggregate.apply(event3);

        testSubject.save(aggregate);

        verify(mockEventBus).publish(event3);
        verify(mockEventBus, never()).publish(event1);
        verify(mockEventBus, never()).publish(event2);
        verify(mockEventStore, times(1)).appendEvents(eq("test"), isA(EventStream.class));
        assertEquals(0, aggregate.getUncommittedEventCount());
    }

    private static class EventSourcingRepositoryImpl extends EventSourcingRepository<TestAggregate> {

        @Override
        protected TestAggregate instantiateAggregate(UUID aggregateIdentifier) {
            return new TestAggregate(aggregateIdentifier);
        }

        @Override
        protected String getTypeIdentifier() {
            return "test";
        }
    }

    private static class TestAggregate extends AbstractAggregateRoot {

        private List<DomainEvent> handledEvents = new ArrayList<DomainEvent>();

        private TestAggregate(UUID identifier) {
            super(identifier);
        }

        @Override
        protected void apply(DomainEvent event) {
            super.apply(event);
        }

        @Override
        protected void handle(DomainEvent event) {
            handledEvents.add(event);
        }

        public List<DomainEvent> getHandledEvents() {
            return handledEvents;
        }
    }
}
