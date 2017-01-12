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
package org.sonatype.nexus.rest.global;

import java.util.ArrayList;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.AuthenticationInfoConverter;
import org.sonatype.nexus.configuration.application.GlobalRemoteConnectionSettings;
import org.sonatype.nexus.configuration.application.GlobalRemoteProxySettings;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.configuration.model.CRemoteConnectionSettings;
import org.sonatype.nexus.configuration.model.CRemoteHttpProxySettings;
import org.sonatype.nexus.configuration.model.CRemoteProxySettings;
import org.sonatype.nexus.configuration.model.CRestApiSettings;
import org.sonatype.nexus.configuration.model.CSmtpConfiguration;
import org.sonatype.nexus.notification.NotificationCheat;
import org.sonatype.nexus.notification.NotificationManager;
import org.sonatype.nexus.notification.NotificationTarget;
import org.sonatype.nexus.proxy.repository.DefaultRemoteHttpProxySettings;
import org.sonatype.nexus.proxy.repository.RemoteHttpProxySettings;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.model.RemoteConnectionSettings;
import org.sonatype.nexus.rest.model.RemoteHttpProxySettingsDTO;
import org.sonatype.nexus.rest.model.RemoteProxySettingsDTO;
import org.sonatype.nexus.rest.model.RestApiSettings;
import org.sonatype.nexus.rest.model.SmtpSettings;
import org.sonatype.nexus.rest.model.SystemNotificationSettings;

/**
 * The base class for global configuration resources.
 *
 * @author cstamas
 */
public abstract class AbstractGlobalConfigurationPlexusResource extends
		AbstractNexusPlexusResource {
	public static final String SECURITY_OFF = "off";

	public static final String SECURITY_SIMPLE = "simple";

	public static final String SECURITY_CUSTOM = "custom";

	private GlobalRemoteProxySettings globalRemoteProxySettings;

	private GlobalRemoteConnectionSettings globalRemoteConnectionSettings;

	private GlobalRestApiSettings globalRestApiSettings;

	private AuthenticationInfoConverter authenticationInfoConverter;


	@Inject
	public void setGlobalRemoteProxySettings(
			final GlobalRemoteProxySettings globalRemoteProxySettings) {
		this.globalRemoteProxySettings = globalRemoteProxySettings;
	}

	@Inject
	public void setGlobalRemoteConnectionSettings(
			final GlobalRemoteConnectionSettings globalRemoteConnectionSettings) {
		this.globalRemoteConnectionSettings = globalRemoteConnectionSettings;
	}

	@Inject
	public void setGlobalRestApiSettings(
			final GlobalRestApiSettings globalRestApiSettings) {
		this.globalRestApiSettings = globalRestApiSettings;
	}

	@Inject
	public void setAuthenticationInfoConverter(
			final AuthenticationInfoConverter authenticationInfoConverter) {
		this.authenticationInfoConverter = authenticationInfoConverter;
	}

	protected GlobalRemoteProxySettings getGlobalRemoteProxySettings() {
		return globalRemoteProxySettings;
	}

	protected GlobalRemoteConnectionSettings getGlobalRemoteConnectionSettings() {
		return globalRemoteConnectionSettings;
	}

	protected GlobalRestApiSettings getGlobalRestApiSettings() {
		return globalRestApiSettings;
	}

	protected AuthenticationInfoConverter getAuthenticationInfoConverter() {
		return authenticationInfoConverter;
	}


	public static SystemNotificationSettings convert(NotificationManager manager) {
		if (manager == null) {
			return null;
		}

		SystemNotificationSettings settings = new SystemNotificationSettings();
		settings.setEnabled(manager.isEnabled());

		NotificationTarget target = manager
				.readNotificationTarget(NotificationCheat.AUTO_BLOCK_NOTIFICATION_GROUP_ID);

		if (target == null) {
			return settings;
		}

		settings.getRoles().addAll(target.getTargetRoles());

		StringBuilder sb = new StringBuilder();

		for (String email : target.getExternalTargets()) {
			sb.append(email).append(",");
		}

		// drop last comma
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}

		settings.setEmailAddresses(sb.toString());

		return settings;
	}

	/**
	 * Externalized Nexus object to DTO's conversion.
	 */
	public static RemoteConnectionSettings convert(
			GlobalRemoteConnectionSettings settings) {
		if (settings == null) {
			return null;
		}

		RemoteConnectionSettings result = new RemoteConnectionSettings();

		result.setConnectionTimeout(settings.getConnectionTimeout() / 1000);

		result.setRetrievalRetryCount(settings.getRetrievalRetryCount());

		result.setQueryString(settings.getQueryString());

		result.setUserAgentString(settings.getUserAgentCustomizationString());

		return result;
	}

	/**
	 * Externalized Nexus object to DTO's conversion.
	 */
	public static RemoteProxySettingsDTO convert(
			GlobalRemoteProxySettings settings) {
		if (settings == null) {
			return null;
		}

		RemoteProxySettingsDTO result = new RemoteProxySettingsDTO();

		result.setHttpProxySettings(convert(settings.getHttpProxySettings()));

		result.setHttpsProxySettings(convert(settings.getHttpsProxySettings()));

		result.setNonProxyHosts(new ArrayList<String>(settings
				.getNonProxyHosts()));

		return result;
	}

	/**
	 * Externalized Nexus object to DTO's conversion.
	 */
	public static RemoteHttpProxySettingsDTO convert(
			RemoteHttpProxySettings settings) {
		if (settings == null) {
			return null;
		}

		final RemoteHttpProxySettingsDTO result = new RemoteHttpProxySettingsDTO();

		result.setProxyHostname(settings.getHostname());

		result.setProxyPort(settings.getPort());

		return result;
	}

	public static RestApiSettings convert(GlobalRestApiSettings settings) {
		if (settings == null || !settings.isEnabled()) {
			return null;
		}

		RestApiSettings result = new RestApiSettings();

		result.setBaseUrl(settings.getBaseUrl());

		result.setForceBaseUrl(settings.isForceBaseUrl());

		result.setUiTimeout(settings.getUITimeout() / 1000);

		return result;
	}


	/**
	 * Externalized Nexus object to DTO's conversion.
	 */
	public static RemoteConnectionSettings convert(
			CRemoteConnectionSettings settings) {
		if (settings == null) {
			return null;
		}

		RemoteConnectionSettings result = new RemoteConnectionSettings();

		result.setConnectionTimeout(settings.getConnectionTimeout() / 1000);

		result.setRetrievalRetryCount(settings.getRetrievalRetryCount());

		result.setQueryString(settings.getQueryString());

		result.setUserAgentString(settings.getUserAgentCustomizationString());

		return result;
	}

	/**
	 * Externalized Nexus object to DTO's conversion.
	 */
	public static RemoteProxySettingsDTO convert(CRemoteProxySettings settings) {
		if (settings == null) {
			return null;
		}

		final RemoteProxySettingsDTO result = new RemoteProxySettingsDTO();

		result.setHttpProxySettings(convert(settings.getHttpProxySettings()));

		result.setHttpsProxySettings(convert(settings.getHttpsProxySettings()));

		result.setNonProxyHosts(settings.getNonProxyHosts());

		return result;
	}

	/**
	 * Externalized Nexus object to DTO's conversion.
	 */
	public static RemoteHttpProxySettingsDTO convert(
			CRemoteHttpProxySettings settings) {
		if (settings == null) {
			return null;
		}

		final RemoteHttpProxySettingsDTO result = new RemoteHttpProxySettingsDTO();

		result.setProxyHostname(settings.getProxyHostname());

		result.setProxyPort(settings.getProxyPort());

		return result;
	}

	public static RestApiSettings convert(CRestApiSettings settings) {
		if (settings == null) {
			return null;
		}

		RestApiSettings result = new RestApiSettings();

		result.setBaseUrl(settings.getBaseUrl());

		result.setForceBaseUrl(settings.isForceBaseUrl());

		result.setUiTimeout(settings.getUiTimeout() / 1000);

		return result;
	}

	public RemoteHttpProxySettings convert(
			final RemoteHttpProxySettingsDTO settings, final String oldPassword)
			throws ConfigurationException {
		if (settings == null
				|| StringUtils.isEmpty(settings.getProxyHostname())) {
			return null;
		}

		final RemoteHttpProxySettings result = new DefaultRemoteHttpProxySettings();

		result.setHostname(settings.getProxyHostname());
		result.setPort(settings.getProxyPort());
		result.setProxyAuthentication(null);

		return result;
	}

}
