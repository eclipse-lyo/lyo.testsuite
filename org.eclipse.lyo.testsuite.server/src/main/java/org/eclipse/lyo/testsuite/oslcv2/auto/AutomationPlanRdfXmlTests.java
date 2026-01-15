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
package org.eclipse.lyo.testsuite.oslcv2.auto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceRdfXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of a auto plan returned by accessing the auto plan's URL directly.
 * It runs the equality query from the properties file and grabs the first result to test against, checking the
 * relationship of elements in the XML representation of the auto plan.
 */
public class AutomationPlanRdfXmlTests extends CoreResourceRdfXmlTests {

    public void initAutomationPlanRdfXmlTests(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException,
                    NullPointerException {

        setup(thisUrl);
    }

    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        setResourceType(OSLCConstants.AUTO_AUTOMATION_PLAN_TYPE);

        String useThisAutoPlan = setupProps.getProperty("useThisAutoPlan");
        if (useThisAutoPlan != null) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThisAutoPlan);
            return toCollection(results);
        }

        setResourceTypeQuery(OSLCConstants.USAGE_PROP);
        setxpathSubStmt("//oslc_v2:QueryCapability/oslc:resourceType/@rdf:resource");

        return getAllDescriptionUrls(eval);
    }

    public static String eval = OSLCConstants.RDFS_MEMBER;
}
