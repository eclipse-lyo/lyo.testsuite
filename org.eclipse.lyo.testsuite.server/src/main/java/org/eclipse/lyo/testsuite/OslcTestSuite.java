/*
 * Copyright (c) 2011, 2013, 2025 IBM Corporation and others
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
package org.eclipse.lyo.testsuite;

import java.io.IOException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

/**
 * An extension to JUnit's Suite, this class's job is to allow DynamicSuiteBuilder to properly create the test suite for
 * the correct version of OSLC.
 *
 * @author Matthew Brown
 */
public class OslcTestSuite extends Suite {
    public OslcTestSuite(Class<?> setupClass) throws InitializationError, IOException {
        super(setupClass, DynamicSuiteBuilder.suitesArray());
    }
}
