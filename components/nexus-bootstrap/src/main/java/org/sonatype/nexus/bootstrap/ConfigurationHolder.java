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
package org.sonatype.nexus.bootstrap;

import java.util.Map;

/**
 * Holder for bootstrap configuration properties.
 *
 * @since 2.8
 */
public class ConfigurationHolder
{
  private static final InheritableThreadLocal<Map<String, String>> reference = new InheritableThreadLocal<Map<String, String>>();

  public static void set(final Map<String,String> properties) {
    reference.set(properties);
  }

  public static Map<String,String> get() {
    return reference.get();
  }

  public static void unset() {
    reference.remove();
  }
}
