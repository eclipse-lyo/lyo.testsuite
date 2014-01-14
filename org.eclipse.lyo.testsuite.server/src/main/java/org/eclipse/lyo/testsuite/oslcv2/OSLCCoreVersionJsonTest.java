/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation.
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
 *    Wu Kai
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;

import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class OSLCCoreVersionJsonTest extends OSLCCoreVersionTestBase{

	public OSLCCoreVersionJsonTest(String thisUrl) {
		super(thisUrl);
	}
	
	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls()
			throws IOException, ParserConfigurationException, SAXException,
			XPathException {
		// Checks the ServiceProviderCatalog at the specified baseUrl of the
		// REST service in order to grab all urls
		// to other ServiceProvidersCatalogs contained within it, recursively,
		// in order to find the URLs of all
		// query factories of the REST service.
		String v = "//oslc_v2:QueryCapability/oslc_v2:queryBase/@rdf:resource";
		ArrayList<String> serviceUrls = getServiceProviderURLsUsingXML(null);
		ArrayList<String> capabilityURLsUsingXML = TestsBase
				.getCapabilityURLsUsingXML(v, serviceUrls, true);
		return toCollection(capabilityURLsUsingXML);
	}

	@Override
	public String getContentType() {
		return OSLCConstants.CT_JSON;
	}
}
