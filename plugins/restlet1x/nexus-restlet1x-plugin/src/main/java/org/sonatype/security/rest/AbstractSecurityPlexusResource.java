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
package org.sonatype.security.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import org.apache.commons.lang.StringEscapeUtils;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.rest.model.AliasingListConverter;
import org.sonatype.plexus.rest.ReferenceFactory;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;
import org.sonatype.security.rest.model.UserResource;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * Base class of SecurityPlexusResources. Contains error handling util methods
 * and conversion between DTO and persistence model.
 *
 * @author bdemers
 */
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public abstract class AbstractSecurityPlexusResource extends
		AbstractPlexusResource {

	protected static final String DEFAULT_SOURCE = "default";

	@Inject
	protected ReferenceFactory referenceFactory;

	protected ErrorResponse getErrorResponse(String id, String msg) {
		ErrorResponse ner = new ErrorResponse();
		ErrorMessage ne = new ErrorMessage();
		ne.setId(id);
		ne.setMsg(msg);
		ner.addError(ne);
		return ner;
	}

	protected void handleInvalidConfigurationException(
			InvalidConfigurationException e) throws PlexusResourceException {
		getLogger().debug("Configuration error!", e);

		ErrorResponse errorResponse;

		ValidationResponse vr = e.getValidationResponse();

		if (vr != null && vr.getValidationErrors().size() > 0) {
			ValidationMessage vm = vr.getValidationErrors().get(0);
			errorResponse = getErrorResponse(vm.getKey(), vm.getShortMessage());
		} else {
			errorResponse = getErrorResponse("*", e.getMessage());
		}

		throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
				"Configuration error.", errorResponse);
	}

	protected UserResource securityToRestModel(Request request,
			boolean appendResourceId) {

		return null;
	}


//	protected PlexusRoleResource securityToRestModel(Role role) {
//		if (role == null) {
//			return null;
//		}
//
//		PlexusRoleResource roleResource = new PlexusRoleResource();
//		roleResource.setRoleId(role.getRoleId());
//		roleResource.setName(role.getName());
//		roleResource.setSource(role.getSource());
//
//		return roleResource;
//	}


	protected Reference createChildReference(Request request, String childPath) {
		return this.referenceFactory.createChildReference(request, childPath);
	}

	protected void checkUsersStatus(String status)
			throws InvalidConfigurationException {
	}

	protected String getRequestAttribute(final Request request, final String key) {
		return getRequestAttribute(request, key, true);
	}

	protected String getRequestAttribute(final Request request,
			final String key, final boolean decode) {
		final String value = request.getAttributes().get(key).toString();

		if (decode) {
			try {
				return URLDecoder.decode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				getLogger().warn("Failed to decode URL attribute.", e);
			}
		}

		return value;
	}

	@Override
	public void configureXStream(final XStream xstream) {
		super.configureXStream(xstream);
	}

	private static class HtmlUnescapeStringCollectionConverter extends
			AliasingListConverter {

		public HtmlUnescapeStringCollectionConverter(String alias) {
			super(String.class, alias);
		}

		@Override
		public Object unmarshal(final HierarchicalStreamReader reader,
				final UnmarshallingContext context) {
			final List<Object> unmarshal = (List<Object>) super.unmarshal(
					reader, context);

			// return value needs to be a "real" List
			return Lists.newArrayList(Collections2.transform(unmarshal,
					new Function() {
						@Nullable
						@Override
						public Object apply(@Nullable final Object input) {
							if (input instanceof String) {
								return StringEscapeUtils
										.unescapeHtml((String) input);
							}

							return input;
						}
					}));
		}
	}
}
