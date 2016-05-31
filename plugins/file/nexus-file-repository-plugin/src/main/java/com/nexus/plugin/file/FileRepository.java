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

import org.sonatype.nexus.proxy.repository.Repository;

import com.nexus.plugin.file.internal.FileMimeRulesSource;

public interface FileRepository
    extends Repository
{
  /**
   * Mime type used for file metadata downstream. See {@link FileMimeRulesSource}.
   */
  String JSON_MIME_TYPE = "application/json";

  /**
   * Mime type used for file tarballs downstream. See {@link FileMimeRulesSource}.
   */
  String TARBALL_MIME_TYPE = "application/x-gzip";

  /**
   * Registry "escape" character, that is invalid package name or version.
   */
  String FILE_REGISTRY_SPECIAL = "-";

  /**
   * Key for flag used to mark a store request "already serviced" by FILE metadata service.
   */
  String FILE_METADATA_SERVICED = "FileMetadataServiced";

  /**
   * Key for flag used to disable FILE metadata service in repository.
   */
  String FILE_METADATA_NO_SERVICE = "FileMetadataNoService";

}
