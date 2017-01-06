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

import java.util.Map;

import org.slf4j.MDC;

import com.google.common.collect.Maps;

/**
 * Simple helper class to manipulate MDC.
 *
 * @author cstamas
 * @since 2.6
 */
public class MDCUtils
{
  public static final String CONTEXT_NON_INHERITABLE_KEY = "non-inheritable";

  public static Map<String, String> getCopyOfContextMap() {
    final boolean inheritable = MDC.get(CONTEXT_NON_INHERITABLE_KEY) == null;
    Map<String, String> result = null;
    if (inheritable) {
      //noinspection unchecked
      result = MDC.getCopyOfContextMap();
    }
    if (result == null) {
      result = Maps.newHashMap();
    }
    result.remove(CONTEXT_NON_INHERITABLE_KEY);
    return result;
  }

  public static void setContextMap(Map<String, String> context) {
    if (context != null) {
      MDC.setContextMap(context);
    }
    else {
      MDC.clear();
    }
  }
}
