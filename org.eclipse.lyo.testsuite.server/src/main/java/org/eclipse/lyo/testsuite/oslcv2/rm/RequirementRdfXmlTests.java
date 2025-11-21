/*
 * Copyright (c) 2011, 2012, 2025 IBM Corporation and others
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
package org.eclipse.lyo.testsuite.oslcv2.rm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceRdfXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.xml.sax.SAXException;

public class RequirementRdfXmlTests extends CoreResourceRdfXmlTests {

    public void initRequirementRdfXmlTests(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException,
                    NullPointerException {

        super(thisUrl);
    }

    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        setResourceType(OSLCConstants.RM_REQUIREMENT_TYPE);

        // If a particular Requirement is specified, use it
        String useThis = setupProps.getProperty("useThisRequirement");
        if ((useThis != null) && (useThis != "")) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThis);
            return toCollection(results);
        }
        setResourceTypeQuery(OSLCConstants.RESOURCE_TYPE_PROP);
        setxpathSubStmt(OSLCConstants.RM_REQUIREMENT_TYPE);

        return getAllDescriptionUrls(eval);
    }

    public static String eval = OSLCConstants.RDFS_MEMBER;
}
