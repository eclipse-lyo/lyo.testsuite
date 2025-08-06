/*
 * Copyright (c) 2011, 2013 IBM Corporation.
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
 *    Steve Speicher - initial API and implementation
 *    Matthew Brown
 *    Samuel Padgett - fix suite error when using Ant
 */
package org.eclipse.lyo.testsuite;

import java.io.IOException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

/**
 * An extension to JUnit's Suite, this class's job
 * is to allow DynamicSuiteBuilder to properly create
 * the test suite for the correct version of OSLC.
 *
 * @author Matthew Brown
 *
 */
public class OslcTestSuite extends Suite {
    public OslcTestSuite(Class<?> setupClass) throws InitializationError, IOException {
        super(setupClass, DynamicSuiteBuilder.suitesArray());
    }
}
