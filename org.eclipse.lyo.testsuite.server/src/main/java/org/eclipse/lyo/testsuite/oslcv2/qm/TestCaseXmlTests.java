/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
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
 *    Yuhong Yin - initial API and implementation
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.qm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.eclipse.lyo.testsuite.oslcv2.CoreResourceXmlTests;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

public class TestCaseXmlTests extends CoreResourceXmlTests {

	public TestCaseXmlTests(String thisUrl) 
		throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, NullPointerException {
		
		super(thisUrl);
		setNode(ns, resource);
	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() 
		throws IOException, ParserConfigurationException, SAXException, XPathException {
				
		staticSetup();
		
		// If a particular TestCase asset is specified, use it 
		String useThis = setupProps.getProperty("useThisTestCase");			
		if ( (useThis != null) && (useThis != "") ) {			
			ArrayList<String> results = new ArrayList<String>();
			results.add(useThis);
			return toCollection(results);
		}
		
		// Otherwise, run a query and pick up one
		setResourceTypeQuery(OSLCConstants.QM_TEST_CASE_QUERY);
		setxpathSubStmt("//oslc_v2:QueryCapability/oslc:resourceType/@rdf:resource");
		return getAllDescriptionUrls(eval);
	}
	
	@Test
	public void TestCaseRelatedChangeRequest() throws XPathExpressionException
	{
		// TestCase specific test
	}

	public static String ns = "oslc_qm_v2";
	public static String resource = "TestCase";
	public static String eval = "//" + ns + ":" + resource + "/@rdf:about";	 
}