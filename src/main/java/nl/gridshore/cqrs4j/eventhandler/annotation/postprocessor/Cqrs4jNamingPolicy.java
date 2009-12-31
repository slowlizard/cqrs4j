/*
 * Copyright (c) 2009. Gridshore
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

import net.sf.cglib.core.DefaultNamingPolicy;

/**
 * CGLib naming policy for classes that tags generated classed with "cqrs4j". This helps identify which classes were
 * generated for cqrs4j, and which cglib classes were generated for other purposes.
 *
 * @author Allard Buijze
 * @since 0.3
 */
class Cqrs4jNamingPolicy extends DefaultNamingPolicy {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getTag() {
        return "cqrs4j";
    }
}
