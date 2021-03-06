/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.plugins.ruby.proxy;

import java.io.IOException;

import org.sonatype.nexus.plugins.ruby.RubyRepository;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

/**
 * Rubygems proxy repository.
 *
 * @since 2.11
 */
public interface ProxyRubyRepository
    extends RubyRepository, ProxyRepository
{
  int getArtifactMaxAge();

  void setArtifactMaxAge(int maxAge);

  int getMetadataMaxAge();

  void setMetadataMaxAge(int metadataMaxAge);

  boolean isOld(StorageItem item);

  void syncMetadata() throws IllegalOperationException, ItemNotFoundException, IOException;

  void purgeBrokenMetadataFiles() throws IllegalOperationException, ItemNotFoundException, IOException;
}
