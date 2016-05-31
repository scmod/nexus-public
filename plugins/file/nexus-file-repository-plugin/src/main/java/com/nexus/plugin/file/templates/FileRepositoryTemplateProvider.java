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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplateProvider;

@Named(FileRepositoryTemplateProvider.PROVIDER_ID)
@Singleton
public class FileRepositoryTemplateProvider
    extends AbstractRepositoryTemplateProvider
{

  public static final String PROVIDER_ID = "file-repository";

  private static final String FILE_HOSTED = "file_hosted";

  public static final String FILE_PROVIDER = "File";

  @Override
  public TemplateSet getTemplates() {
    final TemplateSet templates = new TemplateSet(null);
    templates.add(new FileHostedRepositoryTemplate(this, FILE_HOSTED, FILE_PROVIDER));
    return templates;
  }
}
