/*
 * Copyright (c) 2011, 2025 IBM Corporation and others
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
package org.eclipse.lyo.testsuite.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class SetupProperties {

    static final String setupProps = "setup.properties";
    static final String setupParam = "props";

    public static Properties setup(String propFile) {
        String propFileName = propFile;
        if (propFileName == null) {
            propFileName = System.getProperty(setupParam);
            if (propFileName == null) propFileName = setupProps;
        }
        try {
            InputStream is = new FileInputStream(propFileName);
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Specify property file via -Dprops= or provide one at "
                    + System.getProperty("user.dir")
                    + "/"
                    + propFileName);
            throw new RuntimeException("Property file not found", e);
        } catch (Exception e) {
            throw new RuntimeException("Error loading properties", e);
        }
    }
}
