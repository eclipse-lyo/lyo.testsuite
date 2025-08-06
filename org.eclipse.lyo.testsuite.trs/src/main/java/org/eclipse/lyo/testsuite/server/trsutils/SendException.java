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

import java.io.Serial;

/** A exception that is thrown when creating/updating a resource encounters an exception */
public class SendException extends Exception {

    @Serial private static final long serialVersionUID = -7111348864477190261L;

    public SendException(String message) {
        super(message);
    }

    public SendException(Throwable th) {
        super(th);
    }

    public SendException(String message, Throwable th) {
        super(message, th);
    }
}
