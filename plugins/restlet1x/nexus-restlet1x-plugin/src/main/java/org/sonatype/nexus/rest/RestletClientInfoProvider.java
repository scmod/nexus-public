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
package org.sonatype.nexus.rest;

import javax.inject.Named;
import javax.inject.Singleton;

import org.restlet.data.Request;
import org.sonatype.nexus.auth.ClientInfo;
import org.sonatype.nexus.auth.ClientInfoProvider;
import org.sonatype.sisu.goodies.common.ComponentSupport;

/**
 * {@link ClientInfoProvider} implementation that uses Security and Restlet
 * frameworks to obtain informations. Note: in case of indirect authentication
 * (tokens), what will be returned as "userId" depends on the actual Realm
 * implementation used by given indirect authentication layer. So, it might be
 * the indirect principal (the token) or the userId of the indirectly
 * authenticated user. Implementation dependent.
 *
 * @author cstamas
 * @since 2.1
 */
@Named
@Singleton
public class RestletClientInfoProvider extends ComponentSupport implements
		ClientInfoProvider {
	@Override
	public ClientInfo getCurrentThreadClientInfo() {
		final Request current = Request.getCurrent();
		final String currentIp = RemoteIPFinder.findIP(current);
		final String currentUa = current.getClientInfo().getAgent();
		return new ClientInfo(currentIp, currentUa);
	}
}
