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

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
    private static final String BUNDLE_NAME =
            "org.eclipse.lyo.testsuite.server.trsutils.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE =
            ResourceBundle.getBundle(Messages.BUNDLE_NAME);

    private Messages() {
        // DO Nothing
    }

    public static String getServerString(String key) {
        try {
            return Messages.RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            Messages.checkForWrongKey(key);
            return '!' + key + '!';
        }
    }

    private static void checkForWrongKey(String key) {
        String prefix = "_NoId."; // $NON-NLS-1$
        boolean prefixExists = key.startsWith(prefix);
        String newKey = prefixExists == true ? key.substring(prefix.length()) : prefix + key;
        try {
            Messages.RESOURCE_BUNDLE.getString(newKey);
            String error =
                    "The message key \"%s\" is wrong.  It should be \"%s\"."
                            .formatted(key, newKey); // $NON-NLS-1$
            System.err.println(error);
            throw new RuntimeException(error);
        } catch (MissingResourceException e) {
            // No-op
        }
    }
}
