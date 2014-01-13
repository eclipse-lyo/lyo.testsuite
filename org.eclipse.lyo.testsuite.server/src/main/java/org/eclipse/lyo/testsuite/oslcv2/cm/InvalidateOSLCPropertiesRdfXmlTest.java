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
package org.eclipse.lyo.testsuite.oslcv2.cm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.lyo.testsuite.oslcv2.InvalidateOSLCPropertiesRdfTestBase;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class InvalidateOSLCPropertiesRdfXmlTest extends InvalidateOSLCPropertiesRdfTestBase {

	public InvalidateOSLCPropertiesRdfXmlTest(String thisUrl) {
		super(thisUrl);
	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls()
			throws IOException {
		
		staticSetup();
		
		ArrayList<String> capabilityURLsUsingRdfXml = new ArrayList<String>();
		String useThisCapability = setupProps.getProperty("useThisCapability");
		
		if ( useThisCapability != null ) {
			capabilityURLsUsingRdfXml.add(useThisCapability);
		}
		else {
			ArrayList<String> serviceUrls = getServiceProviderURLsUsingRdfXml(setupProps.getProperty("baseUri"),
				onlyOnce);
			String [] types = getCreateTemplateTypes();
			capabilityURLsUsingRdfXml = getCapabilityURLsUsingRdfXml(OSLCConstants.CREATION_PROP,
						serviceUrls, useDefaultUsageForCreation, types);
		}
		
		return toCollection(capabilityURLsUsingRdfXml);
	}	

	@Override
    public String getContentType() {
	    return OSLCConstants.CT_RDF;
    }

	@Override
    public String getCreateContent() throws IOException {
	    return getCreateContent(rdfXmlCreateTemplate);
    }
	
	@Override
    public String getUpdateContent(String resourceUri) throws IOException {
	    return getUpdateContent(resourceUri, rdfXmlUpdateTemplate);
    }
}
