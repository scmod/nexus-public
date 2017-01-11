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
package org.sonatype.nexus.web.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.sisu.EagerSingleton;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.nexus.security.FilterChain;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.Subscribe;

/**
 * Installs configured {@link FilterChain}s with {@link ProtectedPathManager}.
 *
 * @since 2.5
 */
@Named
@EagerSingleton
public class FilterChainInstaller
{

  private final EventBus eventBus;


  private final List<FilterChain> filterChains;

  @Inject
  public FilterChainInstaller(final EventBus eventBus,
                              final List<FilterChain> filterChains)
  {
    this.eventBus = checkNotNull(eventBus);
    this.filterChains = checkNotNull(filterChains);

    eventBus.register(this);
  }

  @Subscribe
  public void onEvent(final NexusStartedEvent event) {
  }

  @Subscribe
  public void onEvent(final NexusStoppedEvent event) {
    eventBus.unregister(this);
  }

}
