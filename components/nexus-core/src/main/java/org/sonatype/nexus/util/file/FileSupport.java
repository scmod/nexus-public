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
package org.sonatype.nexus.util.file;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.sonatype.sisu.goodies.common.SimpleFormat;

import util.Files;

import com.google.common.base.Charsets;

/**
 * FS regular file related support class. Offers static helper methods for common FS related operations
 * used in Nexus core and plugins for manipulating FS files (aka "regular files"). Goal of this class is
 * to utilize new Java7 NIO Files and related classes for better error detection.
 *
 * @author cstamas
 * @since 2.7.0
 */
public final class FileSupport
{
  public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

  private FileSupport() {
    // no instance
  }

  // COPY: stream to file

  /**
   * Invokes {@link #copy(InputStream, File, CopyOption...)} with default copy options of:
   * <pre>
   *   StandardCopyOption.REPLACE_EXISTING
   * </pre>
   * Hence, this method would copy the complete  content of passed input stream into a file at path "to",
   * possibly overwriting it if file already exists. The passed in stream will be attempted to be
   * completely consumed. In any outcome (success or exception), the stream will be closed.
   */
  public static void copy(final InputStream from, final File to) throws IOException {
    // "copy": overwrite if exists + make files appear as "new" + copy as link if link
    copy(from, to);
  }

  /**
   * This method attempts to copy the complete content of passed input stream into a file at path "to". using copy
   * options as passed in by user. For acceptable copy options see {@link Files#copy(InputStream, File, CopyOption...)}
   * method. The passed in stream will be attempted to be completely consumed. In any outcome (success or exception),
   * the stream will be closed.
   */

  // READ: reading them up as String

  /**
   * Shorthand method for method {@link #readFile(File, Charset)} that uses {@link #DEFAULT_CHARSET}.
   */
  public static String readFile(final File file)
      throws IOException
  {
    return readFile(file, DEFAULT_CHARSET);
  }

  /**
   * Reads up the contents of the file, using given charset into a stream. It is the caller liability to
   * ensure this is actually suitable, as file is relatively small. This method does not have any protection
   * to filter out such bad/malicious attempts.
   */
  public static String readFile(final File file, final Charset charset)
      throws IOException
  {
    validateFile(file);
    checkNotNull(charset);
    final BufferedReader reader = Files.newBufferedReader(file, charset);
    try {
      final StringBuilder result = new StringBuilder();
      String line1 = reader.readLine();
      String line2 = reader.readLine();
      while (true) {
        if (line1 == null) {
          break;
        }
        result.append(line1);
        if (line2 != null) {
          result.append("\n");
        }
        line1 = line2;
        line2 = reader.readLine();
      }
      return result.toString();
    }finally {
    	try {
    		if(reader != null)
    			reader.close();
		} catch (Exception e) {
		}
    }
  }

  /**
   * Shorthand method for method {@link #writeFile(File, Charset, String)} that uses {@link #DEFAULT_CHARSET}.
   */
  public static void writeFile(final File file, final String payload)
      throws IOException
  {
    writeFile(file, DEFAULT_CHARSET, payload);
  }

  /**
   * Writes out the content of a string payload into a file using given charset. The file will be overwritten
   * if exists and parent directories will be created if needed.
   */
  public static void writeFile(final File file, final Charset charset, final String payload)
      throws IOException
  {
    checkNotNull(file);
    checkNotNull(charset);
    checkNotNull(payload);
    DirSupport.mkdir(file.getParentFile());
    final BufferedWriter writer = Files.newBufferedWriter(file, charset);
    try {
      writer.write(payload);
      writer.flush();
    }finally {
    	try {
    		if(writer != null)
    			writer.close();
		} catch (Exception e) {
		}
    }
  }

  // Validation

  /**
   * Enforces that passed in paths are non-null and denote an existing regular file.
   */
  private static void validateFile(final File... paths) {
    for (File path : paths) {
      checkNotNull(path, "File must be non-null");
      checkArgument(Files.isRegularFile(path), SimpleFormat.template("%s is not a regular file", path));
    }
  }

}
