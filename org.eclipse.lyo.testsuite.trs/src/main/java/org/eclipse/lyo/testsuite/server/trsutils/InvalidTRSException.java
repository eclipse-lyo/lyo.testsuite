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

/**
 * A exception that is thrown when an invalid TRS, TRS Base or Change Log segment is encountered.
 */
public class InvalidTRSException extends Exception {
    @Serial private static final long serialVersionUID = -5064491774183615219L;

    public InvalidTRSException(String message) {
        super(message);
    }

    public InvalidTRSException(Throwable th) {
        super(th);
    }

    public InvalidTRSException(String message, Throwable th) {
        super(message, th);
    }
}
