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

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.codehaus.plexus.util.FileUtils;
import org.sonatype.sisu.goodies.common.SimpleFormat;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * FS directory related support class. Offers static helper methods for common FS related operations
 * used in Nexus core and plugins for manipulating FS directories (aka "folders"). Goal of this class is
 * to utilize new Java7 NIO Files and related classes for better error detection.
 *
 * @author cstamas
 * @since 2.7.0
 */
public final class DirSupport
{

  private DirSupport() {
    // no instance
  }

  // MKDIR: directory creation resilient to symlinks

  /**
   * Creates a directory. Fails only if directory creation fails, otherwise cleanly returns. If cleanly returns,
   * it is guaranteed that passed in path is created (with all parents as needed) successfully. Unlike Java7
   * {@link Files#createDirectories(Path, FileAttribute[])} method, this method does support paths having last
   * path element a symlink too. In this case, it's verified that symlink points to a directory and is readable.
   */
  public static void mkdir(final File dir) throws IOException {
    try {
      FileUtils.mkdir(dir.getAbsolutePath());
    }
    catch (Exception e) {
      // this happens when last element of path exists, but is a symlink.
      // A simple test with Files.isDirectory should be able to  detect this
      // case as by default, it follows symlinks.
      if (!dir.isDirectory()) {
        throw new IOException(e);
      }
    }
  }


  // CLEAN: remove files recursively of a directory but keeping the directory structure intact

  /**
   * Cleans an existing directory from any (non-directory) files recursively. Accepts only existing
   * directories, and when returns, it's guaranteed that this directory might contain only subdirectories
   * that also might contain only subdirectories (recursively).
   */
  public static void clean(final File dir) throws IOException {
	  FileUtils.cleanDirectory(dir);
  }

  /**
   * Invokes {@link #clean(Path)} if passed in path exists and is a directory. Also, in that case {@code true} is
   * returned, and in any other case (path does not exists) {@code false} is returned.
   */
  public static boolean cleanIfExists(final File dir) throws IOException {
    checkNotNull(dir);
    if (dir.exists()) {
      clean(dir);
      return true;
    }
    else {
      return false;
    }
  }

  // EMPTY: removes directory subtree with directory itself left intact


  // DELETE: removes directory subtree with directory itself recursively

  /**
   * Deletes a file or directory recursively. This method accepts paths denoting regular files and directories. In case
   * of directory, this method will recursively delete all of it siblings and the passed in directory too.
   */
  public static void delete(final File dir) throws IOException {
    delete(dir, null);
  }

  /**
   * Deletes a file or directory recursively. This method accepts paths denoting regular files and directories. In case
   * of directory, this method will recursively delete all of it siblings and the passed in directory too.
   * The passed in filter can leave out a directory and it's complete subtree from operation.
   */
  public static void delete(final File dir, final @Nullable Predicate<File> excludeFilter) throws IOException {
    validateDirectoryOrFile(dir);
    if (dir.isDirectory()) {
      FileUtils.deleteDirectory(dir);
    }
    else {
      FileUtils.forceDelete(dir);
    }
  }

  /**
   * Invokes {@link #delete(Path)} if passed in path exists. Also, in that case {@code true} is
   * returned, and in any other case (path does not exists) {@code false} is returned.
   */
  public static boolean deleteIfExists(final File dir) throws IOException {
    return deleteIfExists(dir, null);
  }

  /**
   * Invokes {@link #delete(Path)} if passed in path exists. Also, in that case {@code true} is
   * returned, and in any other case (path does not exists) {@code false} is returned.
   * The passed in filter can leave out a directory and it's complete subtree from operation.
   */
  public static boolean deleteIfExists(final File dir, final @Nullable Predicate<File> excludeFilter)
      throws IOException
  {
    checkNotNull(dir);
    if (dir.exists()) {
      delete(dir, excludeFilter);
      return true;
    }
    else {
      return false;
    }
  }


  // COPY: recursive copy of whole directory tree

  /**
   * Copies path "from" to path "to". This method accepts both existing regular files and existing directories. If
   * "from" is a directory, a recursive copy happens of the whole subtree with "from" directory as root. Caller may
   * alter behaviour of Copy operation using copy options, as seen on {@link Files#copy(Path, Path, CopyOption...)}.
   */
  public static void copy(final File from, final File to) throws IOException {
    copy(from, to, null);
  }

  /**
   * Copies path "from" to path "to". This method accepts both existing regular files and existing directories. If
   * "from" is a directory, a recursive copy happens of the whole subtree with "from" directory as root. Caller may
   * alter behaviour of Copy operation using copy options, as seen on {@link Files#copy(Path, Path, CopyOption...)}.
   * The passed in filter can leave out a directory and it's complete subtree from operation.
   *
   * @throws IllegalArgumentException if 'from' is a parent directory of the 'to' path, unless an excludeFilter is
   *                                  provided
   */
  public static void copy(final File from, final File to, final @Nullable Predicate<File> excludeFilter)
      throws IOException
  {
    validateDirectoryOrFile(from);
    checkNotNull(to);
    if (from.isDirectory()) {
      // Avoiding recursion: unless there's an exclude filter, the 'to' dir must not be inside the 'from' dir
      checkArgument(!isParentOf(from, to) || excludeFilter != null);
       FileUtils.copyDirectory(from, to);
    }
    else {
      mkdir(to.getParentFile());
      FileUtils.copyFile(from, to);
    }
  }

  /**
   * Invokes {@link #copy(Path, Path)} if passed in "from" path exists and returns {@code true}. If
   * "from" path does not exists, {@code false} is returned.
   */
  public static boolean copyIfExists(final File from, final File to) throws IOException {
    return copyIfExists(from, to, null);
  }

  /**
   * Invokes {@link #copy(Path, Path)} if passed in "from" path exists and returns {@code true}. If
   * "from" path does not exists, {@code false} is returned.
   * The passed in filter can leave out a directory and it's complete subtree from operation.
   */
  public static boolean copyIfExists(final File from, final File to, final @Nullable Predicate<File> excludeFilter)
      throws IOException
  {
    checkNotNull(from);
    if (from.exists()) {
      copy(from, to, excludeFilter);
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Performs a move operation. It will attempt a real move (if source and target are on same file store), but will
   * fallback to a sequence of "copy" and then "delete" (not a real move!). This method accepts
   * existing Paths that might denote a regular file or a directory. It basically delegates to {@link Files#move(Path,
   * Path, CopyOption...)} method with "replace existing" parameter.
   */
  public static void move(final File from, final File to)
      throws IOException
  {
	  copyDeleteMove(from, to);
  }

  /**
   * Invokes {@link #move(Path, Path)} if passed in "from" path exists and returns {@code true}. If
   * "from" path does not exists, {@code false} is returned.
   */
  public static boolean moveIfExists(final File from, final File to)
      throws IOException
  {
    checkNotNull(from);
    if (from.exists()) {
      move(from, to);
      return true;
    }
    else {
      return false;
    }
  }

  public static void copyDeleteMove(final File from, final File to)
	      throws IOException
	  {
	  copyDeleteMove(from, to, null);
	  }
  
  /**
   * Performs a pseudo move operation (copy+delete). This method accepts existing Paths that might denote a regular file
   * or a directory. While this method is not a real move (like {@link #move(Path, Path)} is), it is a bit more capable:
   * it can move a complete directory structure to it's one sub-directory.
   */
  public static void copyDeleteMove(final File from, final File to, final @Nullable Predicate<File> excludeFilter)
      throws IOException
  {
    copy(from, to, excludeFilter);
    delete(from, excludeFilter);
  }

  /**
   * Invokes {@link #copyDeleteMove(Path, Path, Predicate)} if passed in "from" path exists and returns {@code true}. If
   * "from" path does not exists, {@code false} is returned.
   */
  public static boolean copyDeleteMoveIfExists(final File from,
                                               final File to,
                                               final @Nullable Predicate<File> excludeFilter)
      throws IOException
  {
    checkNotNull(from);
    if (from.exists()) {
      copyDeleteMove(from, to, excludeFilter);
      return true;
    }
    else {
      return false;
    }
  }


  // Validation

  /**
   * Determine if one path is a child of another.
   */
  private static boolean isParentOf(File possibleParent, File possibleChild) {
	  String parent = possibleParent.getAbsolutePath();
	  String child = possibleChild.getAbsolutePath();
	  child = child.substring(0, child.lastIndexOf(File.separator));
    return parent.equals(child);
  }

  /**
   * Enforce all passed in paths are non-null and is existing directory.
   */
  private static void validateDirectory(final File... files) {
	  for (File file : files) {
      checkNotNull(file, "Path must be non-null");
      checkArgument(file.isDirectory(), SimpleFormat.template("%s is not a directory", file));
    }
  }

  /**
   * Enforce all passed in paths are non-null and exist.
   */
  private static void validateDirectoryOrFile(final File... files) {
    for (File file : files) {
      checkNotNull(file, "Path must be non-null");
      checkArgument(file.exists(), SimpleFormat.template("%s does not exists", file));
    }
  }

}
