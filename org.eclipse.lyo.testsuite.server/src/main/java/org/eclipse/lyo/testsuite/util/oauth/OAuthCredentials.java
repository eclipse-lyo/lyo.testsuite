/*
 * Copyright (c) 2014, 2025 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Eclipse Distribution License 1.0
 * which is available at http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */
package org.eclipse.lyo.testsuite.util.oauth;

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
