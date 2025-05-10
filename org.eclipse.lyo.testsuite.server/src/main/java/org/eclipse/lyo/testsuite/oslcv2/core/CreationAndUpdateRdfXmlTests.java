/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation.
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
 *    Steve Speicher
 *    Yuhong Yin
 *    Samuel Padgett
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This class provides JUnit tests for the validation of the OSLCv2 creation and
 * updating of change requests. It uses the template files specified in
 * setup.properties as the entity to be POST or PUT, for creation and updating
 * respectively.
 *
 * After each test, it attempts to perform a DELETE call on the resource that
 * was presumably created, but this DELETE call is not technically required in
 * the OSLC spec, so the created change request may still exist for some service
 * providers.
 */
@RunWith(Parameterized.class)
public class CreationAndUpdateRdfXmlTests extends AbstractCreationAndUpdateRdfTests {
    private static Logger logger = Logger.getLogger(CreationAndUpdateRdfXmlTests.class);

    public CreationAndUpdateRdfXmlTests(String url) {
        super(url);
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        ArrayList<String> capabilityURLsUsingRdfXml = new ArrayList<String>();
        String useThisCapability = setupProps.getProperty("useThisCapability");

        if (useThisCapability != null) {
            capabilityURLsUsingRdfXml.add(useThisCapability);
        } else {
            ArrayList<String> serviceUrls =
                    getServiceProviderURLsUsingRdfXml(setupProps.getProperty("baseUri"), onlyOnce);
            String[] types = getCreateTemplateTypes();
            capabilityURLsUsingRdfXml =
                    getCapabilityURLsUsingRdfXml(
                            OSLCConstants.CREATION_PROP,
                            serviceUrls,
                            useDefaultUsageForCreation,
                            types);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Running RDF/XML creation tests on the following creation factories:");
            for (String capability : capabilityURLsUsingRdfXml) {
                logger.debug(capability);
            }
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
