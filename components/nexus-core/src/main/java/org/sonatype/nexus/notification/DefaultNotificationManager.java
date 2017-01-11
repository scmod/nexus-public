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
package org.sonatype.nexus.notification;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.AbstractLastingConfigurable;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CNotification;
import org.sonatype.nexus.configuration.model.CNotificationConfiguration;
import org.sonatype.sisu.goodies.eventbus.EventBus;

@Singleton
@Named
public class DefaultNotificationManager
    extends AbstractLastingConfigurable<CNotification>
    implements NotificationManager
{


  @Inject
  public DefaultNotificationManager(EventBus eventBus, ApplicationConfiguration applicationConfiguration)
  {
    super("Notification Manager", eventBus, applicationConfiguration);
  }

  @Override
  protected void initializeConfiguration()
      throws ConfigurationException
  {
    if (getApplicationConfiguration().getConfigurationModel() != null) {
      configure(getApplicationConfiguration());
    }
  }

  @Override
  protected CoreConfiguration<CNotification> wrapConfiguration(Object configuration)
      throws ConfigurationException
  {
    if (configuration instanceof ApplicationConfiguration) {
      return new CNotificationConfiguration(getApplicationConfiguration());
    }
    else {
      throw new ConfigurationException("The passed configuration object is of class \""
          + configuration.getClass().getName() + "\" and not the required \""
          + ApplicationConfiguration.class.getName() + "\"!");
    }
  }

  // ==

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public void setEnabled(boolean val) {
    getCurrentConfiguration(true).setEnabled(val);
  }

  @Override
  public NotificationTarget readNotificationTarget(String targetId) {
    return null;
  }

  @Override
  public void updateNotificationTarget(NotificationTarget target) {
  }

  @Override
  public void notifyTargets(NotificationRequest request) {
  }

}
