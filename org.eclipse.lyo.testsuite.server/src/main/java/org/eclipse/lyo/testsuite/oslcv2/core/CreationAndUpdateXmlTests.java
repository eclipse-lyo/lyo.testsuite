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
package org.eclipse.lyo.testsuite.oslcv2.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of the OSLCv2 creation and updating of change requests. It uses
 * the template files specified in setup.properties as the entity to be POST or PUT, for creation and updating
 * respectively.
 *
 * <p>After each test, it attempts to perform a DELETE call on the resource that was presumably created, but this DELETE
 * call is not technically required in the OSLC spec, so the created change request may still exist for some service
 * providers.
 */
@RunWith(Parameterized.class)
public class CreationAndUpdateXmlTests extends AbstractCreationAndUpdateRdfTests {

    public CreationAndUpdateXmlTests(String url) {
        super(url);
    }

    @Override
    public String getContentType() {
        return OSLCConstants.CT_XML;
    }

    @Override
    protected String getHeaderString(String s) {
        return headers.get(s);
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {

        staticSetup();

        ArrayList<String> capabilityURLsUsingXml = new ArrayList<String>();

        String useThisCapability = setupProps.getProperty("useThisCapability");

        if (useThisCapability != null) {
            capabilityURLsUsingXml.add(useThisCapability);
        } else {
            String v = "//oslc_v2:CreationFactory/oslc_v2:creation/@rdf:resource";
            ArrayList<String> serviceUrls = getServiceProviderURLsUsingXML(null);
            capabilityURLsUsingXml = getCapabilityURLsUsingXML(v, serviceUrls, true);
        }

        return toCollection(capabilityURLsUsingXml);
    }

    @Override
    public String getCreateContent() throws IOException {
        return getCreateContent(xmlCreateTemplate);
    }

    @Override
    public String getUpdateContent(String resourceUri) throws IOException {
        return getUpdateContent(resourceUri, xmlUpdateTemplate);
    }
}
