package com.nexus.plugin.file.resource;

import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.codehaus.plexus.util.StringUtils;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;

import com.nexus.plugin.file.FileRepository;

public class FileStoreHelper {

	private final FileRepository repository;

	public FileStoreHelper(FileRepository repo) {
		this.repository = repo;
	}

	public FileRepository getFileRepository() {
		return repository;
	}

	public void storeItemWithChecksums(ResourceStoreRequest request,
			InputStream is, Map<String, String> userAttributes)
			throws UnsupportedStorageOperationException,
			IllegalOperationException, StorageException, AccessDeniedException {
		String originalPath = request.getRequestPath();

		try {
			try {
				getFileRepository().storeItem(request, is, userAttributes);
			} catch (IOException e) {
				throw new LocalStorageException(String.format(
						"Could not store item to repository %s, path %s",
						RepositoryStringUtils
								.getHumanizedNameString(getFileRepository()),
						request), e);
			}

			// NXCM-4861: Doing "local only" lookup, same code should be used as
			// in
			// org.sonatype.nexus.proxy.repository.AbstractProxyRepository#doCacheItem
			// Note: ResourceStoreRequest( ResourceStoreRequest ) creates a
			// "subordinate" request from passed with same
			// path but localOnly=true
			StorageFileItem storedFile = (StorageFileItem) getFileRepository()
					.retrieveItem(false, new ResourceStoreRequest(request));

			String sha1Hash = storedFile.getRepositoryItemAttributes().get(
					DigestCalculatingInspector.DIGEST_SHA1_KEY);

			String md5Hash = storedFile.getRepositoryItemAttributes().get(
					DigestCalculatingInspector.DIGEST_MD5_KEY);

			if (!StringUtils.isEmpty(sha1Hash)) {
				request.setRequestPath(storedFile.getPath() + ".sha1");

				getFileRepository().storeItem(
						false,
						new DefaultStorageFileItem(getFileRepository(),
								request, true, true, new StringContentLocator(
										sha1Hash)));
			}

			if (!StringUtils.isEmpty(md5Hash)) {
				request.setRequestPath(storedFile.getPath() + ".md5");

				getFileRepository().storeItem(
						false,
						new DefaultStorageFileItem(getFileRepository(),
								request, true, true, new StringContentLocator(
										md5Hash)));
			}
		} catch (ItemNotFoundException e) {
			throw new LocalStorageException("Storage inconsistency!", e);
		} finally {
			request.setRequestPath(originalPath);
		}
	}

	public void deleteItemWithChecksums(ResourceStoreRequest request)
			throws UnsupportedStorageOperationException,
			IllegalOperationException, ItemNotFoundException, StorageException,
			AccessDeniedException {
		try {
			getFileRepository().deleteItem(request);
		} catch (ItemNotFoundException e) {
			if (request.getRequestPath().endsWith(".asc")) {
				// Do nothing no guarantee that the .asc files will exist
			} else {
				throw e;
			}
		}

		String originalPath = request.getRequestPath();

		request.setRequestPath(originalPath + ".sha1");

		try {
			getFileRepository().deleteItem(request);
		} catch (ItemNotFoundException e) {
			// ignore not found
		}

		request.setRequestPath(originalPath + ".md5");

		try {
			getFileRepository().deleteItem(request);
		} catch (ItemNotFoundException e) {
			// ignore not found
		}

		// Now remove the .asc files, and the checksums stored with them as well
		// Note this is a recursive call, hence the check for .asc
		if (!originalPath.endsWith(".asc")) {
			request.setRequestPath(originalPath + ".asc");

			deleteItemWithChecksums(request);
		}
	}

	public void storeItemWithChecksums(boolean fromTask,
			AbstractStorageItem item)
			throws UnsupportedStorageOperationException,
			IllegalOperationException, StorageException {
		try {
			try {
				getFileRepository().storeItem(false, item);
			} catch (IOException e) {
				throw new LocalStorageException(
						"Could not get the content from the ContentLocator!", e);
			}

			StorageFileItem storedFile = (StorageFileItem) getFileRepository()
					.retrieveItem(false, new ResourceStoreRequest(item));

			ResourceStoreRequest req = new ResourceStoreRequest(storedFile);

			String sha1Hash = storedFile.getRepositoryItemAttributes().get(
					DigestCalculatingInspector.DIGEST_SHA1_KEY);

			String md5Hash = storedFile.getRepositoryItemAttributes().get(
					DigestCalculatingInspector.DIGEST_MD5_KEY);

			if (!StringUtils.isEmpty(sha1Hash)) {
				req.setRequestPath(item.getPath() + ".sha1");

				getFileRepository()
						.storeItem(
								false,
								new DefaultStorageFileItem(getFileRepository(),
										req, true, true,
										new StringContentLocator(sha1Hash)));
			}

			if (!StringUtils.isEmpty(md5Hash)) {
				req.setRequestPath(item.getPath() + ".md5");

				getFileRepository().storeItem(
						false,
						new DefaultStorageFileItem(getFileRepository(), req,
								true, true, new StringContentLocator(md5Hash)));
			}
		} catch (ItemNotFoundException e) {
			throw new LocalStorageException("Storage inconsistency!", e);
		}
	}

	public void deleteItemWithChecksums(boolean fromTask,
			ResourceStoreRequest request)
			throws UnsupportedStorageOperationException,
			IllegalOperationException, ItemNotFoundException, StorageException {
		try {
			getFileRepository().deleteItem(fromTask, request);
		} catch (ItemNotFoundException e) {
			if (request.getRequestPath().endsWith(".asc")) {
				// Do nothing no guarantee that the .asc files will exist
			} else {
				throw e;
			}
		}

		request.pushRequestPath(request.getRequestPath() + ".sha1");
		try {
			getFileRepository().deleteItem(fromTask, request);

		} catch (ItemNotFoundException e) {
			// ignore not found
		} finally {
			request.popRequestPath();
		}

		request.pushRequestPath(request.getRequestPath() + ".md5");
		try {
			getFileRepository().deleteItem(fromTask, request);
		} catch (ItemNotFoundException e) {
			// ignore not found
		} finally {
			request.popRequestPath();
		}

		// Now remove the .asc files, and the checksums stored with them as well
		// Note this is a recursive call, hence the check for .asc
		if (!request.getRequestPath().endsWith(".asc")) {
			request.pushRequestPath(request.getRequestPath() + ".asc");
			try {
				deleteItemWithChecksums(fromTask, request);
			} finally {
				request.popRequestPath();
			}
		}
	}

	public Gav resolveFile(FileStoreRequest gavRequest)
			throws IllegalOperationException, ItemNotFoundException,
			StorageException, AccessDeniedException {

		Gav gav = repository.resolveFile(gavRequest);

		if (gav == null) {
			throw new ItemNotFoundException(reasonFor(gavRequest, repository,
					"Request %s is not resolvable in repository %s",
					gavRequest.getRequestPath(),
					RepositoryStringUtils.getHumanizedNameString(repository)));
		}

		return gav;
	}

	public StorageFileItem retrieveFile(FileStoreRequest gavRequest)
			throws IllegalOperationException, ItemNotFoundException,
			StorageException, AccessDeniedException {

		Gav gav = resolveFile(gavRequest);

		gavRequest.setRequestPath(repository.getGavCalculator().gavToPath(gav));

		StorageItem item = repository.retrieveItem(gavRequest);

		if (StorageFileItem.class.isAssignableFrom(item.getClass())) {
			return (StorageFileItem) item;
		} else {
			throw new LocalStorageException(
					"The File retrieval returned non-file, path:"
							+ item.getRepositoryItemUid().toString());
		}
	}

	public void storeFile(FileStoreRequest gavRequest, InputStream is,
			Map<String, String> attributes)
			throws UnsupportedStorageOperationException,
			IllegalOperationException, ItemNotFoundException, StorageException,
			AccessDeniedException {

		Gav gav = new Gav(gavRequest.getGroupId(), gavRequest.getArtifactId(),
				gavRequest.getVersion(), gavRequest.getClassifier(),
				gavRequest.getExtension(), null, null, null, false, null,
				false, null);

		gavRequest.setRequestPath(repository.getGavCalculator().gavToPath(gav));

		repository.storeItemWithChecksums(gavRequest, is, attributes);
	}

}
