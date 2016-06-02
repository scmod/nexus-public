package com.nexus.plugin.file.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.subject.Subject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.IllegalRequestException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.RemoteStorageTransportOverloadedException;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.AbstractResourceStoreContentPlexusResource;
import org.sonatype.nexus.rest.StorageFileItemRepresentation;
import org.sonatype.nexus.rest.model.ArtifactCoordinate;
import org.sonatype.security.SecuritySystem;

import com.nexus.plugin.file.FileRepository;

public abstract class AbstractFilePlexusResource extends
		AbstractNexusPlexusResource {

	private SecuritySystem securitySystem;

	private Pattern validInputPattern = Pattern
			.compile("^[a-zA-Z0-9_\\-\\.]*$");

	@Inject
	public void setSecuritySystem(final SecuritySystem securitySystem) {
		this.securitySystem = securitySystem;
	}

	protected FileStoreRequest getResourceStoreRequest(Request request,
			boolean localOnly, boolean remoteOnly, String repositoryId,
			String g, String a, String v, String p, String c, String e)
			throws ResourceException {

		FileRepository fileRepository = getFileRepository(repositoryId);

		// if extension is not given, fall-back to packaging and apply mapper
		if (StringUtils.isBlank(e)) {
			// e = fileRepository.getArtifactPackagingMapper()
			// .getExtensionForPackaging(p);
		}

		// clean up the classifier
		if (StringUtils.isBlank(c)) {
			c = null;
		}

		Gav gav = new Gav(g, a, v, c, e, null, null, null, false, null, false,
				null);

		FileStoreRequest result = new FileStoreRequest(fileRepository, gav,
				localOnly, remoteOnly);

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

	protected Object getContent(Variant variant, boolean redirectTo,
			Request request, Response response) throws ResourceException {
		Form form = request.getResourceRef().getQueryAsForm();

		String groupId = form.getFirstValue("g");

		String artifactId = form.getFirstValue("a");

		String version = form.getFirstValue("v");

		String packaging = form.getFirstValue("p");

		String classifier = form.getFirstValue("c");

		String repositoryId = form.getFirstValue("r");

		String extension = form.getFirstValue("e");

		if (groupId == null || artifactId == null || version == null
				|| repositoryId == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		}

		FileStoreRequest gavRequest = getResourceStoreRequest(request, false,
				false, repositoryId, groupId, artifactId, version, packaging,
				classifier, extension);

		gavRequest.setRequestLocalOnly(isLocal(request,
				gavRequest.getRequestPath()));

		try {
			FileRepository fileRepository = getFileRepository(repositoryId);

			FileStoreHelper helper = fileRepository.getFileStoreHelper();

			StorageFileItem file = helper.retrieveFile(gavRequest);

			if (redirectTo) {
				Reference fileReference = createRepositoryReference(request,
						file.getRepositoryItemUid().getRepository().getId(),
						file.getRepositoryItemUid().getPath());

				response.setLocationRef(fileReference);

				response.setStatus(Status.REDIRECTION_TEMPORARY);

				String redirectMessage = "If you are not automatically redirected use this url: "
						+ fileReference.toString();
				return redirectMessage;
			} else {
				Representation result = new StorageFileItemRepresentation(file);

				result.setDownloadable(true);

				result.setDownloadName(file.getName());

				return result;
			}

		} catch (Exception e) {
			handleException(request, response, e);
		}

		return null;
	}

	// == Upload related stuff

	/**
	 * Method accepting artifact uploads in special form (HTTP POST multipart
	 * requests). This resource processes uploads in a special way, unlike the
	 * content and other related resources does in Nexus (where an "upload" is
	 * basically a HTTP PUT with URL containing the targeted path and with body
	 * carrying the content being uploaded). Description of upload to this
	 * resource follows.
	 * <p>
	 * Every file selected for upload will generate a SEPARATE upload HTTP POST
	 * request that is multipart form upload basically. Each upload request has
	 * form like: (params, file1, [file2]). we have two cases, either POM is
	 * present for upload too, or user filled in GAVP fields in upload form and
	 * wants us to generate a POM for him. Params ALWAYS have param "r"=repoId
	 * as first element (unless we talk about staging, where this is NOT the
	 * case)
	 * <p>
	 * First case, when POM is present (POM file is selected), file1 is always
	 * POM and file2 is the (main or classified) artifact (UI does not validate,
	 * but cases when packaging=pom, or user error when packaging=jar but no
	 * main artifact selected are possible!) params are then "r", "hasPom"=TRUE,
	 * "c"=C, "e"=E Interestingly, subsequent requests (those for "extra"
	 * artifacts with classifiers follows the "second case" pattern below, as
	 * they get GAV from ArtifactCoordinate response. The "c" param if null
	 * means file2 is classified artifact or main artifact. The "e" param might
	 * be null, if pom is the only upload (ie. packaging=pom) Still, in case
	 * POM.packaging=pom plus one classified artifact, parameters "c" and "e"
	 * will belong to the classified artifact!
	 * <p>
	 * Second case, when POM is not present (we need to generate it), user has
	 * to fill in the GAVP fields in form on UI. In that case, nibble is always
	 * in form of (params, file1), so it will strictly have only one file
	 * appended as last content part of multipart form upload, where params will
	 * contain "extra" fields: "r"=repoId, "g"=G, "a"=A, "v"=V, "p"=P, "c"=C,
	 * "e"=E
	 * <p>
	 * This resource will lay the content on proper paths nased on the GAV
	 * coordinates it gets in corresponding repository (either sent as
	 * parameter, or got by some other means).
	 */
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
					gavRequest = getResourceStoreRequest(request, true, false,
							uploadContext.getRepositoryId(),
							uploadContext.getGroupId(),
							uploadContext.getArtifactId(),
							uploadContext.getVersion(),
							uploadContext.getPackaging(),
							uploadContext.getClassifier(),
							uploadContext.getExtension());

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
		coords.setPackaging(uploadContext.getPackaging());
		return coords;
	}

	/**
	 * Upload context that is used to carry state across FileItem processing
	 * iterations.
	 */
	protected static class UploadContext {
		private String repositoryId = null;

		private boolean pomAvailable = false;

		private String extension = null;

		private String classifier = null;

		private String groupId = null;

		private String artifactId = null;

		private String version = null;

		private String packaging = null;

		public String getRepositoryId() {
			return repositoryId;
		}

		public void setRepositoryId(String repositoryId) {
			this.repositoryId = repositoryId;
		}

		public boolean isPomAvailable() {
			return pomAvailable;
		}

		public void setPomAvailable(boolean pomAvailable) {
			this.pomAvailable = pomAvailable;
		}

		public String getExtension() {
			return extension;
		}

		public void setExtension(String extension) {
			this.extension = extension;
		}

		public String getClassifier() {
			return classifier;
		}

		public void setClassifier(String classifier) {
			this.classifier = classifier;
		}

		public String getGroupId() {
			return groupId;
		}

		public void setGroupId(String groupId) {
			this.groupId = groupId;
		}

		public String getArtifactId() {
			return artifactId;
		}

		public void setArtifactId(String artifactId) {
			this.artifactId = artifactId;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public String getPackaging() {
			return packaging;
		}

		public void setPackaging(String packaging) {
			this.packaging = packaging;
		}
	}

	/**
	 * Creates instance of {@link UploadContext} to be used throughout of upload
	 * process.
	 */
	protected UploadContext createUploadContext() {
		return new UploadContext();
	}

	/**
	 * Invoked for every form field that upload is receiving.
	 */
	protected void processFormField(final Request request,
			final UploadContext uploadContext, final FileItem fi)
			throws ResourceException {
		// Ensure valid characters in field
		if (!validInputPattern.matcher(fi.getString()).matches()) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Only letters, digits, underscores(_), hyphens(-), and dots(.) are allowed");
		}

		if ("r".equals(fi.getFieldName())) {
			uploadContext.setRepositoryId(fi.getString());
		} else if ("g".equals(fi.getFieldName())) {
			uploadContext.setGroupId(fi.getString());
		} else if ("a".equals(fi.getFieldName())) {
			uploadContext.setArtifactId(fi.getString());
		} else if ("v".equals(fi.getFieldName())) {
			uploadContext.setVersion(fi.getString());
		} else if ("p".equals(fi.getFieldName())) {
			uploadContext.setPackaging(fi.getString());
		} else if ("c".equals(fi.getFieldName())) {
			uploadContext.setClassifier(fi.getString());
		} else if ("e".equals(fi.getFieldName())) {
			uploadContext.setExtension(fi.getString());
		}
	}

	/**
	 * Invoked once from upload method, when all the coordinates are ready
	 * (either all form params are processed or POM is parsed).
	 */
	protected void uploadGavParametersAvailable(final Request request,
			final UploadContext uploadContext) throws ResourceException {
		// nop
	}

	// ==

	protected String buildUploadFailedHtmlResponse(Throwable t,
			Request request, Response response) {
		try {
			handleException(request, response, t);
		} catch (ResourceException e) {
			getLogger().debug("Got error while uploading artifact", t);

			StringBuilder resp = new StringBuilder();
			resp.append("<html><body><error>");
			resp.append(StringEscapeUtils.escapeHtml(e.getMessage()));
			resp.append("</error></body></html>");

			String forceSuccess = request.getResourceRef().getQueryAsForm()
					.getFirstValue("forceSuccess");

			if (!"true".equals(forceSuccess)) {
				response.setStatus(e.getStatus());
			}

			return resp.toString();
		}

		// We have an error at this point, can't get here
		return null;
	}

	protected void handleException(Request request, Response res, Throwable t)
			throws ResourceException {
		if (t instanceof ResourceException) {
			throw (ResourceException) t;
		} else if (t instanceof IllegalArgumentException) {
			getLogger().info(
					"ResourceStoreContentResource, illegal argument:"
							+ t.getMessage());

			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					t.getMessage());
		} else if (t instanceof RemoteStorageTransportOverloadedException) {
			throw new ResourceException(
					Status.SERVER_ERROR_SERVICE_UNAVAILABLE, t);
		} else if (t instanceof RepositoryNotAvailableException) {
			throw new ResourceException(
					Status.SERVER_ERROR_SERVICE_UNAVAILABLE, t.getMessage());
		} else if (t instanceof IllegalRequestException) {
			getLogger().info(
					"ResourceStoreContentResource, illegal request:"
							+ t.getMessage());

			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					t.getMessage());
		} else if (t instanceof IllegalOperationException) {
			getLogger().info(
					"ResourceStoreContentResource, illegal operation:"
							+ t.getMessage());

			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					t.getMessage());
		} else if (t instanceof StorageException) {
			getLogger().warn("IO problem!", t);

			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					t.getMessage());
		} else if (t instanceof UnsupportedStorageOperationException) {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
					t.getMessage());
		} else if (t instanceof NoSuchResourceStoreException) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					t.getMessage());
		} else if (t instanceof ItemNotFoundException) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					t.getMessage());
		} else if (t instanceof AccessDeniedException) {
			AbstractResourceStoreContentPlexusResource.challengeIfNeeded(
					request, res, (AccessDeniedException) t);
			if (Status.CLIENT_ERROR_FORBIDDEN.equals(res.getStatus())) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						t.getMessage());
			}
		} else if (t instanceof XmlPullParserException) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					t.getMessage());
		} else if (t instanceof IOException) {
			getLogger().warn("IO error!", t);

			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					t.getMessage());
		} else {
			getLogger().warn(t.getMessage(), t);

			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					t.getMessage());
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
