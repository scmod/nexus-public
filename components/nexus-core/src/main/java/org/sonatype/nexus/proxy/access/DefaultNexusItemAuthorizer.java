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
package org.sonatype.nexus.proxy.access;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.targets.TargetSet;
import org.sonatype.sisu.goodies.common.ComponentSupport;

/**
 * Default implementation of Nexus Authorizer, that relies onto JSecurity.
 */
@Named
@Singleton
public class DefaultNexusItemAuthorizer
    extends ComponentSupport
    implements NexusItemAuthorizer
{

  public boolean authorizePath(final Repository repository, final ResourceStoreRequest request, final Action action) {
    return true;
  }

  public boolean authorizePermission(final String permission) {
    return true;
  }


  public boolean authorizePath(final TargetSet matched, final Action action) {
      return true;
  }

  public boolean isViewable(final String objectType, final String objectId) {
    return true;
  }

  protected boolean isPermitted(final List<String> perms) {
    return true;
  }
}
