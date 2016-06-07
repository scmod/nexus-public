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
package org.sonatype.nexus.configuration.application.upgrade;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;
import org.sonatype.configuration.upgrade.SingleVersionUpgrader;
import org.sonatype.configuration.upgrade.UpgradeMessage;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Upgrades configuration model from version 2.7.0 to 2.8.0.
 *
 * @since 2.8
 */
@Singleton
@Named("2.7.0")
public class Upgrade270to280
    extends ComponentSupport
    implements SingleVersionUpgrader
{
  @Override
  public Object loadConfiguration(File file)
      throws IOException, ConfigurationIsCorruptedException
  {
    org.sonatype.nexus.configuration.model.v2_7_0.Configuration conf = null;
	FileReader fr = new FileReader(file);
    try {
      // reading without interpolation to preserve user settings as variables
      org.sonatype.nexus.configuration.model.v2_7_0.io.xpp3.NexusConfigurationXpp3Reader reader =
          new org.sonatype.nexus.configuration.model.v2_7_0.io.xpp3.NexusConfigurationXpp3Reader();

      conf = reader.read(fr);
    }
    catch (XmlPullParserException e) {
      throw new ConfigurationIsCorruptedException(file.getAbsolutePath(), e);
    } finally {
    	try {
    		if(fr != null)
    			fr.close();
		} catch (Exception e) {
		}
    }

    return conf;
  }

  @Override
  public void upgrade(UpgradeMessage message)
      throws ConfigurationIsCorruptedException
  {
    org.sonatype.nexus.configuration.model.v2_7_0.Configuration oldc =
        (org.sonatype.nexus.configuration.model.v2_7_0.Configuration) message.getConfiguration();

    org.sonatype.nexus.configuration.model.v2_8_0.upgrade.BasicVersionUpgrade versionConverter = new org.sonatype.nexus.configuration.model.v2_8_0.upgrade.BasicVersionUpgrade();

    Configuration newc = versionConverter.upgradeConfiguration(oldc);
    newc.setVersion(Configuration.MODEL_VERSION);

    // no structural change sofar
    // TODO: remove this comment above if we do some

    message.setModelVersion(Configuration.MODEL_VERSION);
    message.setConfiguration(newc);
  }
}
