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
package com.nexus.plugin.file.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.ui.contribution.UiContributorSupport;

import com.nexus.plugin.file.FilePlugin;

/**
 * This tells Nexus that plugin has static resources (js and css)
 *
 * @author Georgy Bolyuba (georgy@bolyuba.com)
 */
/**
 * 实现这个@ {@link UiContributorSupport}来获取css,js资源文件
 */
@Named
@Singleton
public class FileUiContributor
    extends UiContributorSupport
{
  @Inject
  public FileUiContributor(FilePlugin owner) {
    super(owner);
  }
}
