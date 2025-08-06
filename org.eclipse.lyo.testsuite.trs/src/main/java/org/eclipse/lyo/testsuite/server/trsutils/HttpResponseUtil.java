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
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

public class HttpResponseUtil {
    /**
     * Marks an HTTP response as final and disposes of any system resources referenced by the
     * response.
     *
     * <p>Clients should aggressively call this method as soon as they no longer need the response
     * to reduce contention over possibly scarce system resources.
     *
     * <p>Clients should <strong>not</strong> attempt to access the HTTP response after calling this
     * method.
     *
     * @param response the HTTP response to finalize
     */
    public static void finalize(final HttpResponse response) {
        if (response == null) return;
        HttpEntity entity = response.getEntity();
        try {
            if (entity != null) {
                InputStream is = entity.getContent();
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            /* ignored */
        }
    }

    private HttpResponseUtil() {
        super();
        throw new UnsupportedOperationException(
                Messages.getServerString("http.response.util.no.instance")); // $NON-NLS-1$
    }
}
