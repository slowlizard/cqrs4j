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

package nl.gridshore.cqrs4j.eventhandler.annotation.postprocessor;

import nl.gridshore.cqrs4j.eventhandler.annotation.BufferingAnnotationEventListenerAdapter;
import org.junit.*;
import org.springframework.core.task.AsyncTaskExecutor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class AnnotationEventListenerBeanPostProcessorTest {

    private AnnotationEventListenerBeanPostProcessor testSubject;

    @Test
    public void testExecutorServiceInjectedProperly() {
        testSubject = spy(new AnnotationEventListenerBeanPostProcessor());
        BufferingAnnotationEventListenerAdapter eventListenerAdapter = spy(testSubject.createAdapterFor(new Object()));
        doReturn(eventListenerAdapter).when(testSubject).createAdapterFor(any());
        AsyncTaskExecutor mockTaskExecutor = mock(AsyncTaskExecutor.class);
        testSubject.setTaskExecutor(mockTaskExecutor);

        testSubject.adapt(new Object());

        verify(eventListenerAdapter).setTaskExecutor(mockTaskExecutor);
    }

}
