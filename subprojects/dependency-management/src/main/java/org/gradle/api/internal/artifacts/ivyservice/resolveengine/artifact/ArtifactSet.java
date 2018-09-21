/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact;

import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.internal.artifacts.transform.VariantSelector;
import org.gradle.api.specs.Spec;
import org.gradle.internal.operations.BuildOperationQueue;
import org.gradle.internal.operations.RunnableBuildOperation;

/**
 * Represents a container of artifacts, possibly made up of several different variants.
 *
 * Instances are retained during the lifetime of a build, so should avoid retaining unnecessary state.
 */
public interface ArtifactSet {
    ArtifactSet NO_ARTIFACTS = new ArtifactSet() {
        @Override
        public ResolvedArtifactSet select(Spec<? super ComponentIdentifier> componentFilter, VariantSelector selector) {
            return new ResolvedArtifactSet() {
                @Override
                public Completion startVisit(BuildOperationQueue<RunnableBuildOperation> actions, AsyncArtifactListener listener) {
                    return EMPTY_RESULT;
                }

                @Override
                public void collectBuildDependencies(BuildDependenciesVisitor visitor) {
                }
            };
        }
    };

    /**
     * Selects the artifacts of this set that meet the given criteria. Implementation should be eager where possible, so that selection happens immediately, but may be lazy.
     */
    ResolvedArtifactSet select(Spec<? super ComponentIdentifier> componentFilter, VariantSelector selector);
}
