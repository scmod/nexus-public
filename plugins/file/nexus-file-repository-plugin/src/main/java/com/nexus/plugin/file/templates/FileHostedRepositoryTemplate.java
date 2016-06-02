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
package com.nexus.plugin.file.templates;

import java.io.IOException;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

import com.nexus.plugin.file.FileContentClass;
import com.nexus.plugin.file.hosted.DefaultFileHostedRepository;
import com.nexus.plugin.file.hosted.FileHostedRepository;
import com.nexus.plugin.file.hosted.FileHostedRepositoryConfiguration;

public class FileHostedRepositoryTemplate
    extends AbstractRepositoryTemplate
{

  public FileHostedRepositoryTemplate(final FileRepositoryTemplateProvider provider, final String id,
                                     final String description)
  {
    super(provider, id, description, new FileContentClass(), FileHostedRepository.class);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    final CRepository repo = new DefaultCRepository();
    repo.setId("file");
    repo.setName("file");

    repo.setProviderRole(Repository.class.getName());
    repo.setProviderHint(DefaultFileHostedRepository.ROLE_HINT);

    final Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    final FileHostedRepositoryConfiguration exConf = new FileHostedRepositoryConfiguration(ex);

    repo.externalConfigurationImple = exConf;

    repo.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());
    repo.setNotFoundCacheTTL(1440);
    repo.setIndexable(true);
    repo.setSearchable(true);

    return new CRepositoryCoreConfiguration(getTemplateProvider().getApplicationConfiguration(), repo,
        new CRepositoryExternalConfigurationHolderFactory<FileHostedRepositoryConfiguration>()
        {
          @Override
          public FileHostedRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
            return new FileHostedRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
          }
        });
  }

  @Override
  public FileHostedRepository create() throws ConfigurationException, IOException {
    return (FileHostedRepository) super.create();
  }
}
