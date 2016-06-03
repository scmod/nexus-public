package com.nexus.plugin.file.resource;

import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.subject.Subject;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.StorageFileItemRepresentation;
import org.sonatype.nexus.rest.artifact.AbstractArtifactPlexusResource;
import org.sonatype.nexus.rest.model.ArtifactCoordinate;
import org.sonatype.security.SecuritySystem;

import com.nexus.plugin.file.FileRepository;

public abstract class AbstractFilePlexusResource extends
		AbstractArtifactPlexusResource {

	private SecuritySystem securitySystem;

	private Pattern validInputPattern = Pattern
			.compile("^[a-zA-Z0-9_\\-\\.]*$");

	@Inject
	public void setSecuritySystem(final SecuritySystem securitySystem) {
		this.securitySystem = securitySystem;
	}

	protected FileStoreRequest getFileStoreRequest(Request request,
			String repositoryId, String g, String a, String v, String c,
			String e, String fileName) throws ResourceException {

		FileRepository fileRepository = getFileRepository(repositoryId);

		// clean up the classifier
		if (StringUtils.isBlank(c)) {
			c = null;
		}

		Gav gav = new Gav(g, a, v, c, e, null, null, fileName, false, null, false,
				null);

		FileStoreRequest result = new FileStoreRequest(fileRepository, gav);

		if (getLogger().isDebugEnabled()) {
			getLogger().debug(
					"Created FileStoreRequest request for "
							+ result.getRequestPath());
		}

		// stuff in the originating remote address
		result.getRequestContext().put(AccessManager.REQUEST_REMOTE_ADDRESS,
				getValidRemoteIPAddress(request));

		// stuff in the user id if we have it in request
		Subject subject = securitySystem.getSubject();
		if (subject != null && subject.getPrincipal() != null) {
			result.getRequestContext().put(AccessManager.REQUEST_USER,
					subject.getPrincipal().toString());
		}
		result.getRequestContext().put(AccessManager.REQUEST_AGENT,
				request.getClientInfo().getAgent());

		// this is HTTPS, get the cert and stuff it too for later
		if (request.isConfidential()) {
			result.getRequestContext().put(AccessManager.REQUEST_CONFIDENTIAL,
					Boolean.TRUE);

			List<?> certs = (List<?>) request.getAttributes().get(
					"org.restlet.https.clientCertificates");

			if (certs != null) {
				result.getRequestContext().put(
						AccessManager.REQUEST_CERTIFICATES, certs);
			}
		}

		// put the incoming URLs
		result.setRequestUrl(request.getOriginalRef().toString());

		return result;
	}

	protected Object getContent(Variant variant, Request request,
			Response response) throws ResourceException {
		Form form = request.getResourceRef().getQueryAsForm();

		String groupId = form.getFirstValue("g");

		String artifactId = form.getFirstValue("a");

		String version = form.getFirstValue("v");

		String classifier = form.getFirstValue("c");

		String repositoryId = form.getFirstValue("r");

		String extension = form.getFirstValue("e");

		String fileName = form.getFirstValue("fileName");

		if (repositoryId == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		}
		FileStoreRequest gavRequest = getFileStoreRequest(request,
				repositoryId, groupId, artifactId, version, classifier,
				extension, fileName);

		gavRequest.setRequestLocalOnly(true);

		try {
			FileRepository fileRepository = getFileRepository(repositoryId);

			FileStoreHelper helper = fileRepository.getFileStoreHelper();

			StorageFileItem file = helper.retrieveFile(gavRequest);

			Representation result = new StorageFileItemRepresentation(file);

			result.setDownloadable(true);

			result.setDownloadName(file.getName());

			return result;

		} catch (Exception e) {
			handleException(request, response, e);
		}

		return null;
	}

	@Override
	public Object upload(Context context, Request request, Response response,
			List<FileItem> files) throws ResourceException {
		final UploadContext uploadContext = createUploadContext();

		try {
			for (FileItem fi : files) {
				if (fi.isFormField()) {
					// parameters are first in "nibble"
					processFormField(request, uploadContext, fi);
				} else {
					InputStream is = null;

					FileStoreRequest gavRequest = null;

					uploadGavParametersAvailable(request, uploadContext);
					is = fi.getInputStream();
					gavRequest = getFileStoreRequest(request,
							uploadContext.getRepositoryId(),
							uploadContext.getGroupId(),
							uploadContext.getArtifactId(),
							uploadContext.getVersion(),
							uploadContext.getClassifier(),
							uploadContext.getExtension(), fi.getName());

					final FileRepository fr = gavRequest.getFileRepository();
					final FileStoreHelper helper = fr.getFileStoreHelper();

					helper.storeFile(gavRequest, is, null);
				}
			}
		} catch (Exception t) {
			return buildUploadFailedHtmlResponse(t, request, response);
		}

		final ArtifactCoordinate coords = new ArtifactCoordinate();
		coords.setGroupId(uploadContext.getGroupId());
		coords.setArtifactId(uploadContext.getArtifactId());
		coords.setVersion(uploadContext.getVersion());

		return coords;
	}

	protected void uploadGavParametersAvailable(final Request request,
			final UploadContext uploadContext) throws ResourceException {
		if (StringUtils.isBlank(uploadContext.getGroupId())
				&& StringUtils.isBlank(uploadContext.getArtifactId())
				&& StringUtils.isBlank(uploadContext.getVersion())) {
			Calendar c = Calendar.getInstance(Locale.CHINA);
			uploadContext.setGroupId(String.valueOf(c.get(Calendar.YEAR)));
			uploadContext.setArtifactId(String.valueOf(c.get(Calendar.MONTH) + 1));
			uploadContext.setVersion(String.valueOf(c.get(Calendar.DAY_OF_MONTH)));
		}
	}

	protected FileRepository getFileRepository(String id)
			throws ResourceException {
		try {
			Repository repository = getUnprotectedRepositoryRegistry()
					.getRepository(id);

			if (!repository.getRepositoryKind().isFacetAvailable(
					FileRepository.class)) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"This is not a Maven repository!");
			}

			return repository.adaptToFacet(FileRepository.class);
		} catch (NoSuchRepositoryException e) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					e.getMessage(), e);
		}
	}

}
