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
package org.sonatype.nexus.threads;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ScheduledExecutorService;

import javax.security.auth.Subject;

import concurrent.SubjectAwareScheduledExecutorService;

/**
 * A modification of Shiro's {@link SubjectAwareScheduledExecutorService} that in turn returns always the same,
 * supplied
 * {@link Subject} to bind threads with.
 *
 * @author cstamas
 * @since 2.6
 */
public class NexusScheduledExecutorService
    extends SubjectAwareScheduledExecutorService
{

  public static NexusScheduledExecutorService newService(final ScheduledExecutorService target) {
    return new NexusScheduledExecutorService(target);
  }

  public NexusScheduledExecutorService(final ScheduledExecutorService target) {
    super(checkNotNull(target));
  }
}
