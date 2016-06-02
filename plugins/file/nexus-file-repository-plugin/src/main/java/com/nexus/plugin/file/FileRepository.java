/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.nexus.plugin.file;

import java.io.InputStream;
import java.util.Map;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import com.nexus.plugin.file.resource.FileStoreHelper;
import com.nexus.plugin.file.resource.FileStoreRequest;
/**
 * 文件服务器的话似乎并没有什么metadata的概念,因为有也获取不到版本信息之类的,
 * 最多一个上传日期(可以作为默认的gav)
 * @author John Smith
 *
 */
public interface FileRepository
    extends Repository
{
  
  Gav resolveFile(FileStoreRequest request);
	
  GavCalculator getGavCalculator();

  FileStoreHelper getFileStoreHelper();

  RepositoryPolicy getRepositoryPolicy();

  void setRepositoryPolicy(RepositoryPolicy repositoryPolicy);

  // == "Public API" (JSec protected)

  void storeItemWithChecksums(ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
             StorageException, AccessDeniedException;

  void deleteItemWithChecksums(ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
             StorageException, AccessDeniedException;

  // == "Insider API" (unprotected)

  void storeItemWithChecksums(boolean fromTask, AbstractStorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException;

  void deleteItemWithChecksums(boolean fromTask, ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException;

}
