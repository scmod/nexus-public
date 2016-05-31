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

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;

import com.nexus.plugin.file.FileContentClass;
import com.nexus.plugin.file.internal.FileMimeRulesSource;

@Named(DefaultFileHostedRepository.ROLE_HINT)
@Typed(Repository.class)
@Description("File registry hosted repo")
public class DefaultFileHostedRepository
    extends AbstractRepository
    implements Repository
{

  public static final String ROLE_HINT = "file-hosted";

  private final ContentClass contentClass;

  private final FileHostedRepositoryConfigurator configurator;

  private final RepositoryKind repositoryKind;

  private final FileMimeRulesSource mimeRulesSource;

  @Inject
  public DefaultFileHostedRepository(final @Named(FileContentClass.ID) ContentClass contentClass,
                                    final FileHostedRepositoryConfigurator configurator)
  {
    this.mimeRulesSource = new FileMimeRulesSource();
    this.contentClass = checkNotNull(contentClass);
    this.configurator = checkNotNull(configurator);
    this.repositoryKind = new DefaultRepositoryKind(FileHostedRepository.class, null);
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
  public MimeRulesSource getMimeRulesSource() {
    return mimeRulesSource;
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<FileHostedRepositoryConfiguration>()
    {
      @Override
      public FileHostedRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
        return new FileHostedRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

}