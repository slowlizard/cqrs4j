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

package nl.gridshore.cqrs4j.repository;

import org.junit.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @author Allard Buijze
 */
public class PessimisticLockManagerTest {

    @Test
    public void testLockReferenceCleanedUpAtUnlock() throws NoSuchFieldException, IllegalAccessException {
        PessimisticLockManager manager = new PessimisticLockManager();
        UUID identifier = UUID.randomUUID();
        manager.obtainLock(identifier);
        manager.releaseLock(identifier);

        Field locksField = manager.getClass().getDeclaredField("locks");
        locksField.setAccessible(true);
        Map locks = (Map) locksField.get(manager);
        assertEquals("Expected lock to be cleaned up", 0, locks.size());
    }
}
