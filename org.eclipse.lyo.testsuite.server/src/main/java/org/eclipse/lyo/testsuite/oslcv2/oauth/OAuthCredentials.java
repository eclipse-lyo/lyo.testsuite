/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution. 
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *    Samuel Padgett - initial API and implementation
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.oauth;

import java.security.Principal;

import org.apache.http.auth.Credentials;

public class OAuthCredentials implements Credentials {
	
	private Principal principal;
	private String secret;
	
	public OAuthCredentials(OAuthConsumerPrincipal principal, String secret) {
		this.principal = principal;
		this.secret = secret;
	}

	@Override
	public Principal getUserPrincipal() {
		return principal;
	}

	@Override
	public String getPassword() {
		return secret;
	}

}
