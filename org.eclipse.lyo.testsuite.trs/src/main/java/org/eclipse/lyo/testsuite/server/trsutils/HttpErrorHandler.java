/*
 * Copyright (c) 2013, 2025 IBM Corporation and others
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

package org.eclipse.lyo.testsuite.server.trsutils;

import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class HttpErrorHandler {

    /**
     * Handle a possible HTTP error response.
     *
     * @param response The HTTP response to handle; must not be <code>null</code>
     * @throws HttpResponseException if the response status code maps to an exception class
     */
    public static void responseToException(HttpResponse response) throws HttpResponseException {
        if (response == null)
            throw new IllegalArgumentException(
                    Messages.getServerString("http.error.handler.null.argument")); // $NON-NLS-1$

        Integer status = Integer.valueOf(response.getStatusLine().getStatusCode());

        // Create detail message from response status line and body
        String reasonPhrase = response.getStatusLine().getReasonPhrase();

        StringBuilder message =
                new StringBuilder(reasonPhrase == null ? "" : reasonPhrase); // $NON-NLS-1$

        if (response.getEntity() != null) {
            try {
                String body = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
                if (body != null && body.length() != 0) {
                    message.append('\n');
                    message.append(body);
                }
            } catch (IOException e) {
            } // ignore, since the original error needs to be reported
        }

        throw new HttpResponseException(status, message.toString());
    }
}
