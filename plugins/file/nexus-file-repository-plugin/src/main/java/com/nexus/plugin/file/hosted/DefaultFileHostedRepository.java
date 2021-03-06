/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.nexus.plugin.file.hosted;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.util.Map;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import com.nexus.plugin.file.FileContentClass;
import com.nexus.plugin.file.resource.FileStoreHelper;
import com.nexus.plugin.file.resource.FileStoreRequest;

@Named(DefaultFileHostedRepository.ROLE_HINT)
@Typed(Repository.class)
@Description("File registry hosted repo")
/* 这个不是直接实现Repository接口的话会加不进仓库类型里面去... 不知道为啥 */
public class DefaultFileHostedRepository extends AbstractRepository implements
		Repository, FileHostedRepository {

	public static final String ROLE_HINT = "file-hosted";

	private final ContentClass contentClass;

	private final FileHostedRepositoryConfigurator configurator;

	private final RepositoryKind repositoryKind;

	private final GavCalculator gavCalculator;
	
	private final FileStoreHelper fileStoreHelper;

	@Inject
	public DefaultFileHostedRepository(
			final @Named(FileContentClass.ID) ContentClass contentClass,
			final @Named("file") GavCalculator gavCalculator,
			final FileHostedRepositoryConfigurator configurator) {
		this.contentClass = checkNotNull(contentClass);
		this.gavCalculator = checkNotNull(gavCalculator);
		this.configurator = checkNotNull(configurator);
		this.repositoryKind = new DefaultRepositoryKind(
				FileHostedRepository.class, null);
		this.fileStoreHelper = new FileStoreHelper(this);
	}

	@Override
	protected Configurator getConfigurator() {
		return this.configurator;
	}

	@Override
	public RepositoryKind getRepositoryKind() {
		return this.repositoryKind;
	}

	@Override
	public ContentClass getRepositoryContentClass() {
		return this.contentClass;
	}

	@Override
	protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
		return new CRepositoryExternalConfigurationHolderFactory<FileHostedRepositoryConfiguration>() {
			@Override
			public FileHostedRepositoryConfiguration createExternalConfigurationHolder(
					final CRepository config) {
				return new FileHostedRepositoryConfiguration(
						(Xpp3Dom) config.getExternalConfiguration());
			}
		};
	}

	@Override
	public Gav resolveFile(FileStoreRequest gavRequest) {
		String version = gavRequest.getVersion();

		Gav gav = null;

		gav = new Gav(gavRequest.getGroupId(), gavRequest.getArtifactId(),
				version, gavRequest.getClassifier(), gavRequest.getExtension(),
				null, null, gavRequest.getFileName(), false, null, false, null);

		return gav;
	}

	@Override
	public GavCalculator getGavCalculator() {
		return gavCalculator;
	}

	@Override
	public FileStoreHelper getFileStoreHelper() {
		return fileStoreHelper;
	}

	@Override
	public RepositoryPolicy getRepositoryPolicy() {
		return RepositoryPolicy.MIXED;
	}

	@Override
	public void setRepositoryPolicy(RepositoryPolicy repositoryPolicy) {
	}

	@Override
	public void storeItemWithChecksums(ResourceStoreRequest request,
			InputStream is, Map<String, String> userAttributes)
			throws UnsupportedStorageOperationException, ItemNotFoundException,
			IllegalOperationException, StorageException, AccessDeniedException {
		if (log.isDebugEnabled()) {
			log.debug("storeItemWithChecksums() :: " + request.getRequestPath());
		}
		fileStoreHelper.storeItemWithChecksums(request, is, userAttributes);
	}

	@Override
	public void deleteItemWithChecksums(ResourceStoreRequest request)
			throws UnsupportedStorageOperationException, ItemNotFoundException,
			IllegalOperationException, StorageException, AccessDeniedException {
		if (log.isDebugEnabled()) {
			log.debug("deleteItemWithChecksums() :: " + request.getRequestPath());
		}
		fileStoreHelper.deleteItemWithChecksums(request);
	}

}