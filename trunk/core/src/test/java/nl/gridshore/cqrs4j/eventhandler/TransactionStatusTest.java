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

package nl.gridshore.cqrs4j.eventhandler;

import org.junit.*;

import static nl.gridshore.cqrs4j.eventhandler.YieldPolicy.DO_NOT_YIELD;
import static nl.gridshore.cqrs4j.eventhandler.YieldPolicy.YIELD_AFTER_TRANSACTION;
import static org.junit.Assert.*;

/**
 * @author Allard Buijze
 */
public class TransactionStatusTest {

    private TransactionStatus status;

    @Before
    public void setUp() {
        status = new TransactionStatus() {
        };
    }

    @Test
    public void testStaticAccessors() {
        assertNull(TransactionStatus.current());

        TransactionStatus.set(status);
        assertSame(status, TransactionStatus.current());
        TransactionStatus.clear();
        assertNull(TransactionStatus.current());
    }

    @Test
    public void testEventCountInTransaction() {
        assertEquals(0, status.getEventsProcessedInTransaction());
        assertEquals(0, status.getEventsProcessedSinceLastYield());

        status.recordEventProcessed();

        assertEquals(1, status.getEventsProcessedInTransaction());
        assertEquals(1, status.getEventsProcessedSinceLastYield());

        status.resetTransactionStatus();

        assertEquals(0, status.getEventsProcessedInTransaction());
        assertEquals(1, status.getEventsProcessedSinceLastYield());
    }

    @Test
    public void testYieldEnforcement() {
        assertEquals(YIELD_AFTER_TRANSACTION, status.getYieldPolicy());
        status.setYieldPolicy(DO_NOT_YIELD);
        assertEquals(DO_NOT_YIELD, status.getYieldPolicy());
        status.setMaxTransactionSize(10);

        status.requestImmediateYield();

        assertEquals(0, status.getMaxTransactionSize());
        assertEquals(YIELD_AFTER_TRANSACTION, status.getYieldPolicy());
    }

    @Test
    public void testCommitEnforcement() {
        status.setYieldPolicy(DO_NOT_YIELD);
        assertEquals(DO_NOT_YIELD, status.getYieldPolicy());
        status.setMaxTransactionSize(10);

        status.requestImmediateCommit();

        assertEquals(0, status.getMaxTransactionSize());
        assertEquals(DO_NOT_YIELD, status.getYieldPolicy());
    }

}
