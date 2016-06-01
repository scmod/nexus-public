/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
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

import org.sonatype.nexus.plugin.support.StaticSecurityResourceSupport;

import com.nexus.plugin.file.FilePlugin;

/**
 * 实现这个@ {@link StaticSecurityResourceSupport}来获取默认名为"{artifact}-security.xml"中的权限属性
 * @author John Smith
 */
@Named
@Singleton
public class FileSecurityResource
    extends StaticSecurityResourceSupport
{
  @Inject
  public FileSecurityResource(final FilePlugin plugin) {
    super(plugin);
  }
}
