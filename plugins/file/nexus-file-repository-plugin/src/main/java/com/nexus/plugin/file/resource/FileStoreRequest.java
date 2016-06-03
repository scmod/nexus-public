package com.nexus.plugin.file.resource;

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
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;

import com.nexus.plugin.file.FileRepository;

public class FileStoreRequest extends ResourceStoreRequest {
	private final FileRepository fileRepository;

	private final Gav gav;

	public FileStoreRequest(FileRepository repository, Gav gav,
			boolean localOnly) {
		this(repository, gav, localOnly, false);
	}

	public FileStoreRequest(FileRepository repository, Gav gav) {
		this(repository, gav, true, false);
	}

	public FileStoreRequest(FileRepository repository, Gav gav,
			boolean localOnly, boolean remoteOnly) {
		super(repository.getGavCalculator().gavToPath(gav), true, false);
		this.fileRepository = repository;
		this.gav = gav;
	}

	public FileRepository getFileRepository() {
		return fileRepository;
	}

	public Gav getGav() {
		return gav;
	}

	public String getGroupId() {
		return gav.getGroupId();
	}

	public String getArtifactId() {
		return gav.getArtifactId();
	}

	public String getVersion() {
		return gav.getVersion();
	}

	public String getClassifier() {
		return gav.getClassifier();
	}

	public String getExtension() {
		return gav.getExtension();
	}
	
	public String getFileName() {
		return gav.getName();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("(GAVCE=");
		sb.append(getGroupId());
		sb.append(":");
		sb.append(getArtifactId());
		sb.append(":");
		sb.append(getVersion());
		sb.append(":c=");
		sb.append(getClassifier());
		sb.append(":e=");
		sb.append(getExtension());
		sb.append(":fileName=");
		sb.append(getFileName());
		sb.append(", for ");
		sb.append(RepositoryStringUtils
				.getHumanizedNameString(getFileRepository()));
		sb.append(") ");

		return sb.toString();
	}

}
