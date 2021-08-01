/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
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
 *******************************************************************************/
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
			if (propFileName == null)
				propFileName = setupProps;
		}
		try {
			InputStream is = new FileInputStream(propFileName);
			Properties props = new Properties();
			props.load(is);
			return props;
		} catch (java.io.FileNotFoundException e) {
			System.err.println("Specify property file via -Dprops= or provide one at " + System.getProperty("user.dir") + "/" + propFileName);
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
	    }
	}
}
