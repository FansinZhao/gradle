/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.internal.resource.local.ivy;

import org.gradle.api.internal.artifacts.ivyservice.ArtifactCacheMetadata;
import org.gradle.api.internal.artifacts.mvnsettings.CannotLocateLocalMavenRepositoryException;
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator;
import org.gradle.api.internal.artifacts.repositories.resolver.IvyResourcePattern;
import org.gradle.api.internal.artifacts.repositories.resolver.M2ResourcePattern;
import org.gradle.api.internal.artifacts.repositories.resolver.ResourcePattern;
import org.gradle.internal.Factory;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.gradle.internal.component.external.model.ModuleComponentArtifactMetadata;
import org.gradle.internal.hash.HashValue;
import org.gradle.internal.resource.local.CompositeLocallyAvailableResourceFinder;
import org.gradle.internal.resource.local.FileStoreSearcher;
import org.gradle.internal.resource.local.LocallyAvailableResource;
import org.gradle.internal.resource.local.LocallyAvailableResourceCandidates;
import org.gradle.internal.resource.local.LocallyAvailableResourceFinder;
import org.gradle.internal.resource.local.LocallyAvailableResourceFinderSearchableFileStoreAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class LocallyAvailableResourceFinderFactory implements Factory<LocallyAvailableResourceFinder<ModuleComponentArtifactMetadata>> {
    static final Logger LOGGER = LoggerFactory.getLogger(LocallyAvailableResourceFinderFactory.class);

    private final File rootCachesDirectory;
    private final LocalMavenRepositoryLocator localMavenRepositoryLocator;
    final FileStoreSearcher<ModuleComponentArtifactIdentifier> fileStore;

    public LocallyAvailableResourceFinderFactory(
        ArtifactCacheMetadata artifactCacheMetadata, LocalMavenRepositoryLocator localMavenRepositoryLocator, FileStoreSearcher<ModuleComponentArtifactIdentifier> fileStore) {
        this.rootCachesDirectory = artifactCacheMetadata.getCacheDir().getParentFile();
        this.localMavenRepositoryLocator = localMavenRepositoryLocator;
        this.fileStore = fileStore;
    }

    @Override
    public LocallyAvailableResourceFinder<ModuleComponentArtifactMetadata> create() {
        List<LocallyAvailableResourceFinder<ModuleComponentArtifactMetadata>> finders = new LinkedList<LocallyAvailableResourceFinder<ModuleComponentArtifactMetadata>>();

        // Order is important here, because they will be searched in that order

        // The current filestore
        finders.add(new LocallyAvailableResourceFinderSearchableFileStoreAdapter<ModuleComponentArtifactMetadata>(new FileStoreSearcher<ModuleComponentArtifactMetadata>() {
            @Override
            public Set<? extends LocallyAvailableResource> search(ModuleComponentArtifactMetadata key) {
                return fileStore.search(key.getId());
            }
        }));

        // 1.8
        addForPattern(finders, "artifacts-26/filestore/[organisation]/[module](/[branch])/[revision]/[type]/*/[artifact]-[revision](-[classifier])(.[ext])");

        // 1.5
        addForPattern(finders, "artifacts-24/filestore/[organisation]/[module](/[branch])/[revision]/[type]/*/[artifact]-[revision](-[classifier])(.[ext])");

        // 1.4
        addForPattern(finders, "artifacts-23/filestore/[organisation]/[module](/[branch])/[revision]/[type]/*/[artifact]-[revision](-[classifier])(.[ext])");

        // 1.3
        addForPattern(finders, "artifacts-15/filestore/[organisation]/[module](/[branch])/[revision]/[type]/*/[artifact]-[revision](-[classifier])(.[ext])");

        // 1.1, 1.2
        addForPattern(finders, "artifacts-14/filestore/[organisation]/[module](/[branch])/[revision]/[type]/*/[artifact]-[revision](-[classifier])(.[ext])");

        // rc-1, 1.0
        addForPattern(finders, "artifacts-13/filestore/[organisation]/[module](/[branch])/[revision]/[type]/*/[artifact]-[revision](-[classifier])(.[ext])");

        // Milestone 8 and 9
        addForPattern(finders, "artifacts-8/filestore/[organisation]/[module](/[branch])/[revision]/[type]/*/[artifact]-[revision](-[classifier])(.[ext])");

        // Milestone 7
        addForPattern(finders, "artifacts-7/artifacts/*/[organisation]/[module](/[branch])/[revision]/[type]/[artifact]-[revision](-[classifier])(.[ext])");

        // Milestone 6
        addForPattern(finders, "artifacts-4/[organisation]/[module](/[branch])/*/[type]s/[artifact]-[revision](-[classifier])(.[ext])");
        addForPattern(finders, "artifacts-4/[organisation]/[module](/[branch])/*/pom.originals/[artifact]-[revision](-[classifier])(.[ext])");

        // Milestone 3
        addForPattern(finders, "../cache/[organisation]/[module](/[branch])/[type]s/[artifact]-[revision](-[classifier])(.[ext])");

        // Maven local
        try {
            File localMavenRepository = localMavenRepositoryLocator.getLocalMavenRepository();
            if (localMavenRepository.exists()) {
                addForPattern(finders, localMavenRepository, new M2ResourcePattern("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])"));
            }
        } catch (CannotLocateLocalMavenRepositoryException ex) {
            finders.add(new NoMavenLocalRepositoryResourceFinder(ex));
        }
        return new CompositeLocallyAvailableResourceFinder<ModuleComponentArtifactMetadata>(finders);
    }

    private void addForPattern(List<LocallyAvailableResourceFinder<ModuleComponentArtifactMetadata>> finders, String pattern) {
        int wildcardPos = pattern.indexOf("/*/");
        int patternPos = pattern.indexOf("/[");
        if (wildcardPos < 0 && patternPos < 0) {
            throw new IllegalArgumentException(String.format("Unsupported pattern '%s'", pattern));
        }
        int chopAt;
        if (wildcardPos >= 0 && patternPos >= 0) {
            chopAt = Math.min(wildcardPos, patternPos);
        } else if (wildcardPos >= 0) {
            chopAt = wildcardPos;
        } else {
            chopAt = patternPos;
        }
        String pathPart = pattern.substring(0, chopAt);
        String patternPart = pattern.substring(chopAt + 1);
        addForPattern(finders, new File(rootCachesDirectory, pathPart), new IvyResourcePattern(patternPart));
    }

    private void addForPattern(List<LocallyAvailableResourceFinder<ModuleComponentArtifactMetadata>> finders, File baseDir, ResourcePattern pattern) {
        if (baseDir.exists()) {
            finders.add(new PatternBasedLocallyAvailableResourceFinder(baseDir, pattern));
        }
    }

    private class NoMavenLocalRepositoryResourceFinder implements LocallyAvailableResourceFinder<ModuleComponentArtifactMetadata> {
        private final CannotLocateLocalMavenRepositoryException ex;
        private boolean logged;

        public NoMavenLocalRepositoryResourceFinder(CannotLocateLocalMavenRepositoryException ex) {
            this.ex = ex;
        }

        @Override
        public LocallyAvailableResourceCandidates findCandidates(ModuleComponentArtifactMetadata criterion) {
            if(!logged){
                LOGGER.warn("Unable to locate local Maven repository.");
                LOGGER.debug("Problems while locating local Maven repository.", ex);
                logged = true;
            }
            return new LocallyAvailableResourceCandidates() {
                @Override
                public boolean isNone() {
                    return true;
                }

                @Override
                public LocallyAvailableResource findByHashValue(HashValue hashValue) {
                    return null;
                }
            };
        }
    }
}
