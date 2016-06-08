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
import java.nio.charset.Charset;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.annotation.Nullable;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.exists;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.isDirectory;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.isEmptyDirectory;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.isFile;

public class DirSupportTest
    extends TestSupport
{
  private static final byte[] PAYLOAD = "payload".getBytes(Charset.forName("UTF-8"));

  private File root;

  private void createDirectoryStructure(final Path r) throws IOException {
    Files.write(r.resolve("file1.txt"), PAYLOAD);
    Files.write(r.resolve("file2.txt"), PAYLOAD);
    final Path dir1 = Files.createDirectories(r.resolve("dir1"));
    Files.write(dir1.resolve("file11.txt"), PAYLOAD);
    Files.write(dir1.resolve("file12.txt"), PAYLOAD);
    final Path dir2 = Files.createDirectories(r.resolve("dir2"));
    Files.write(dir2.resolve("file21.txt"), PAYLOAD);
    Files.write(dir2.resolve("file22.txt"), PAYLOAD);
    Files.write(dir2.resolve("file23.txt"), PAYLOAD);
    final Path dir21 = Files.createDirectories(dir2.resolve("dir21"));
    Files.write(dir21.resolve("file211.txt"), PAYLOAD);
    Files.write(dir21.resolve("file212.txt"), PAYLOAD);
  }

  @Before
  public void prepare() throws IOException {
    root = util.createTempDir();
    createDirectoryStructure(root);
  }

  @Test
  public void mkdir() throws IOException {
    final File mkdirA = new File(root, "mkdir-a");
    final File mkdirAB = new File(mkdirA, "mkdir-ab");
    final File dir211 = new File(new File(new File(root, "dir2"), "dir21"), "dir211");
    DirSupport.mkdir(mkdirAB); // new
    DirSupport.mkdir(mkdirA); // existing
    DirSupport.mkdir(dir211); // existing structure
    assertThat(mkdirA, isDirectory());
    assertThat(mkdirAB, isDirectory());
    assertThat(dir211, isDirectory());
  }

  @Test
  public void symlinkMkdir() throws IOException {
    final Path dir1link = root.resolve("dir1-link");
    try {
      // not all OSes support symlink creation
      // if symlink creation fails on given OS, just return from this test
      Files.createSymbolicLink(dir1link, root.resolve("dir1"));
    }
    catch (IOException e) {
      return;
    }
    DirSupport.mkdir(dir1link);
  }

  @Test
  public void clean() throws IOException {
    DirSupport.clean(root);
    assertThat(root, exists());
    assertThat(root, isDirectory());
    assertThat(root, not(isEmptyDirectory()));
    assertThat(root.resolve("dir2").resolve("dir21").toFile(), isDirectory());
  }

  @Test
  public void cleanIfExists() throws IOException {
    assertThat(DirSupport.cleanIfExists(root.resolve("not-existing")), is(false));
    assertThat(DirSupport.cleanIfExists(root), is(true));
    assertThat(root, exists());
    assertThat(root, isDirectory());
    assertThat(root, not(isEmptyDirectory()));
    assertThat(root.resolve("dir2").resolve("dir21").toFile(), isDirectory());
  }

  @Test
  public void empty() throws IOException {
    DirSupport.empty(root);
    assertThat(root, exists());
    assertThat(root, isDirectory());
    assertThat(root, isEmptyDirectory());
  }

  @Test
  public void emptyIfExists() throws IOException {
    assertThat(DirSupport.emptyIfExists(root.resolve("not-existing")), is(false));
    assertThat(DirSupport.emptyIfExists(root), is(true));
    assertThat(root, exists());
    assertThat(root, isDirectory());
    assertThat(root, isEmptyDirectory());
  }

  @Test
  public void delete() throws IOException {
    DirSupport.delete(root);
    assertThat(root, not(exists()));
  }

  @Test
  public void deleteIfExists() throws IOException {
    assertThat(DirSupport.deleteIfExists(root.resolve("not-existing")), is(false));
    assertThat(DirSupport.deleteIfExists(root), is(true));
    assertThat(root, not(exists()));
  }

  @Test
  public void copy() throws IOException {
    final Path target = util.createTempDir();
    DirSupport.copy(root, target);
    assertThat(target.toFile(), exists());
    assertThat(target.toFile(), isDirectory());
    assertThat(target.toFile(), not(isEmptyDirectory()));
    assertThat(target.resolve("dir2").resolve("dir21").toFile(), isDirectory());
    assertThat(target.resolve("dir2").resolve("dir21").resolve("file211.txt").toFile(), isFile());
  }

  @Test
  public void copyIfExists() throws IOException {
    final Path target = util.createTempDir();
    assertThat(DirSupport.copyIfExists(root.resolve("not-existing"), target), is(false));
    assertThat(DirSupport.copyIfExists(root, target), is(true));
    assertThat(target.toFile(), exists());
    assertThat(target.toFile(), isDirectory());
    assertThat(target.toFile(), not(isEmptyDirectory()));
    assertThat(target.resolve("dir2").resolve("dir21").toFile(), isDirectory());
    assertThat(target.resolve("dir2").resolve("dir21").resolve("file211.txt").toFile(), isFile());
  }

  @Test
  public void move() throws IOException {
    final Path target = util.createTempDir();
    DirSupport.move(root, target);
    assertThat(root, not(exists()));
    assertThat(target.toFile(), exists());
    assertThat(target.toFile(), isDirectory());
    assertThat(target.toFile(), not(isEmptyDirectory()));
    assertThat(target.resolve("dir2").resolve("dir21").toFile(), isDirectory());
    assertThat(target.resolve("dir2").resolve("dir21").resolve("file211.txt").toFile(), isFile());
  }

  @Test
  public void copyDeleteMoveToSubdir() throws IOException {
    final Path target = root.resolve("dir2/dir21");
    DirSupport.copyDeleteMove(root, target, new Predicate<Path>()
    {
      @Override
      public boolean apply(@Nullable final Path input) {
        return input.startsWith(target);
      }
    });
    assertThat(root, exists());
    assertThat(root.resolve("dir1").toFile(), not(exists()));
    assertThat(root.resolve("dir2").toFile(), exists());
    assertThat(root.resolve("dir2/file21.txt").toFile(), not(exists()));
    assertThat(root.resolve("dir2/file22.txt").toFile(), not(exists()));
    assertThat(root.resolve("dir2/file23.txt").toFile(), not(exists()));
    assertThat(root.resolve("dir2/dir21").toFile(), exists());
    assertThat(root.resolve("dir2/dir21/file211.txt").toFile(), exists());
    assertThat(root.resolve("dir2/dir21/file212.txt").toFile(), exists());

    assertThat(root.resolve("dir2/dir21/dir1").toFile(), exists());
    assertThat(root.resolve("dir2/dir21/dir1/file11.txt").toFile(), exists());
    assertThat(root.resolve("dir2/dir21/dir1/file12.txt").toFile(), exists());
    assertThat(root.resolve("dir2/dir21/dir2").toFile(), exists());
    assertThat(root.resolve("dir2/dir21/dir2/file21.txt").toFile(), exists());
    assertThat(root.resolve("dir2/dir21/dir2/file22.txt").toFile(), exists());
    assertThat(root.resolve("dir2/dir21/dir2/file23.txt").toFile(), exists());

    assertThat(root, exists());
    assertThat(target.toFile(), exists());
    assertThat(target.toFile(), isDirectory());
    assertThat(target.toFile(), not(isEmptyDirectory()));
    assertThat(root.resolve("dir2").resolve("dir21").toFile(), isDirectory());
    assertThat(root.resolve("dir2").resolve("dir21").resolve("file211.txt").toFile(), isFile());
  }

  /**
   * This is what happened when repo root was being deleted: endless cycle in as "manual" copy/move was
   * performed (during copy), as it copied files "ahead" of itself, basically "rolling" files deeper
   * and deeper. {@link FileSystemException} is thrown once file path length reaches OS limit. In case
   * of repo local storage, the root was being moved under "/.nexus/trash".
   */
  @Test(expected = FileSystemException.class)
  public void moveToSubdir() throws IOException {
    final Path target = root.resolve("dir2/dir21");
    DirSupport.move(root, target);
  }

  @Test(expected = IllegalArgumentException.class)
  public void copyingToChildDirDisallowedWithoutFilter() throws IOException {
    final Path target = root.resolve("dir2/dir21");
    DirSupport.copyDeleteMove(root, target, null);
  }

  @Test
  public void moveIfExists() throws IOException {
    final Path target = util.createTempDir();
    assertThat(DirSupport.moveIfExists(root.resolve("not-existing"), target), is(false));
    assertThat(DirSupport.moveIfExists(root, target), is(true));
    assertThat(root, not(exists()));
    assertThat(target.toFile(), exists());
    assertThat(target.toFile(), isDirectory());
    assertThat(target.toFile(), not(isEmptyDirectory()));
    assertThat(target.resolve("dir2").resolve("dir21").toFile(), isDirectory());
    assertThat(target.resolve("dir2").resolve("dir21").resolve("file211.txt").toFile(), isFile());
  }

  @Test
  public void apply() throws IOException {
    final ArrayList<String> fileNames = Lists.newArrayList();
    final ArrayList<String> dirNames = Lists.newArrayList();
    final Function<Path, FileVisitResult> tf = new Function<Path, FileVisitResult>()
    {
      @Override
      public FileVisitResult apply(final Path input) {
        if (Files.isDirectory(input)) {
          dirNames.add(input.getFileName().toString());
        }
        else if (Files.isRegularFile(input)) {
          fileNames.add(input.getFileName().toString());
        }
        return FileVisitResult.CONTINUE;
      }
    };
    DirSupport.apply(root, tf);

    assertThat(fileNames, hasSize(9));
    // root + 3dirs
    assertThat(dirNames, hasSize(4));
  }

  @Test
  public void applyToFiles() throws IOException {
    final ArrayList<String> fileNames = Lists.newArrayList();
    final ArrayList<String> dirNames = Lists.newArrayList();
    final Function<Path, FileVisitResult> tf = new Function<Path, FileVisitResult>()
    {
      @Override
      public FileVisitResult apply(final Path input) {
        if (Files.isDirectory(input)) {
          dirNames.add(input.getFileName().toString());
        }
        else if (Files.isRegularFile(input)) {
          fileNames.add(input.getFileName().toString());
        }
        return FileVisitResult.CONTINUE;
      }
    };
    DirSupport.applyToFiles(root, tf);

    assertThat(fileNames, hasSize(9));
    // func never invoked on dirs
    assertThat(dirNames, hasSize(0));
  }

}
