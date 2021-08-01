/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation.
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
 *    Yuhong Yin    - initial API and implementation
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.qm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class TestExecutionRecordXmlTests extends CoreResourceXmlTests {

	public TestExecutionRecordXmlTests(String thisUrl)
		throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, NullPointerException {

		super(thisUrl);
		setNode(ns, resource);
	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls()
		throws IOException, ParserConfigurationException, SAXException, XPathException {

		staticSetup();

		// If a particular TestExecutionRecord asset is specified, use it
		String useThis = setupProps.getProperty("useThisTestExecutionRecord");
		assumeTrue(useThis != null && !"".equals(useThis));
		ArrayList<String> results = new ArrayList<String>();
		results.add(useThis);

		return toCollection(results);
	}

	@Test
	public void TestExecutionRecordHasOneRunsTestCase() throws XPathExpressionException
	{
		String eval = "//" + getNode() + "/" + "oslc_qm_v2:runsTestCase";

		NodeList results = (NodeList) OSLCUtils.getXPath().evaluate(eval,
	    		doc, XPathConstants.NODESET);

		assertEquals("oslc_qm_v2:runsTestCase"+getFailureMessage(), 1, results.getLength());
	}

	public static String ns = "oslc_qm_v2";
	public static String resource = "TestExecutionRecord";
	public static String eval = "//" + ns + ":" + resource + "/@rdf:about";
}
