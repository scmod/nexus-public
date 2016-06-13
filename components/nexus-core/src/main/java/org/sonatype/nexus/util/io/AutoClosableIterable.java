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
package org.sonatype.nexus.util.io;

import java.io.Closeable;
import java.util.Iterator;

import lang.AutoCloseable;
import util.Collections;

/**
 * {@link AutoCloseable} {@link Iterable}.
 *
 * @since 2.13
 */
public interface AutoClosableIterable<T>
    extends Closeable, AutoCloseable, Iterable<T>
{
  /**
   * Helper to create {@link AutoClosableIterable}.
   */
  class Factory
  {
    private Factory() {
      // empty
    }

    private static final AutoClosableIterable EMPTY_ITERABLE = new AutoClosableIterable<Object>()
    {
      @Override
      public void close() {
        // empty
      }

      @Override
      public Iterator<Object> iterator() {
        return Collections.emptyIterator();
      }
    };

    /**
     * Create empty/nop iterable.
     */
    @SuppressWarnings("unchecked")
    public static <T> AutoClosableIterable<T> emptyIterable() {
      return EMPTY_ITERABLE;
    }
  }
}
