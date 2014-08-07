/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation.
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
 *    Yuhong Yin - revised
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;

import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.junit.BeforeClass;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the basic validation of query factories
 * as specified in the OSLC version 2 spec. This version of the query tests only
 * tests the basic status code and form of the query responses, as without
 * shapes implemented it is difficult to represent the needed various templates
 * of different change request types and to query for the templates.
 */
public abstract class SimplifiedQueryBaseTests extends TestsBase {

	protected static String queryProperty;
	protected static String queryPropertyValue;
	protected static String queryComparisonProperty;
	protected static String queryComparisonValue;
	protected static String fullTextSearchTerm;
	protected static String additionalParameters;
	protected static boolean fullTextSearch = true;

	public SimplifiedQueryBaseTests(String thisUri) {
		super(thisUri);
	}

	@BeforeClass
	public static void mysetup() throws IOException, ParserConfigurationException,
			SAXException, XPathException {
		
		staticSetup();
		
		queryProperty = setupProps.getProperty("queryEqualityProperty");
		queryPropertyValue = setupProps.getProperty("queryEqualityValue");
		queryComparisonProperty = setupProps
				.getProperty("queryComparisonProperty");
		queryComparisonValue = setupProps.getProperty("queryComparisonValue");
		fullTextSearchTerm = setupProps.getProperty("fullTextSearchTerm");
		additionalParameters = setupProps
				.getProperty("queryAdditionalParameters");
		if (additionalParameters == null)
			additionalParameters = "";
		
		if (setupProps.getProperty("fullTextSearch") != null ) {
			fullTextSearch = setupProps.getProperty("fullTextSearch").equalsIgnoreCase("true");
		}
	}

	protected String getQueryBase() {
		String query = (additionalParameters.length() == 0) ? "?" : "?"
				+ additionalParameters + "&";
		return query;
	}

	protected String getQueryUrlForValidEqualsQueryContainsExpectedResources()
			throws UnsupportedEncodingException {
		String query = getQueryBase() + "oslc.where="
				+ URLEncoder.encode(queryProperty + "=\"" + queryPropertyValue + "\"", "UTF-8")
				+ "&oslc.select=" + URLEncoder.encode(queryProperty, "UTF-8");
		return query;
	}

	protected String getQueryUrlForValidNotEqualQueryContainsExpectedResources()
			throws UnsupportedEncodingException {
		return getQueryBase()
				+ "oslc.where="
				+ URLEncoder
						.encode(queryProperty + "!=\"" + queryPropertyValue + "\"", "UTF-8")
				+ "&oslc.select=" + URLEncoder.encode(queryProperty, "UTF-8");
	}

	protected String getQueryUrlForValidLessThanQueryContainsExpectedResources()
			throws UnsupportedEncodingException {
		return getQueryBase()
				+ "oslc.where="
				+ URLEncoder.encode(queryComparisonProperty + "<\"" + queryComparisonValue + "\"",
						"UTF-8") + "&oslc.select=" + URLEncoder.encode(queryComparisonProperty, "UTF-8");
	}

	protected String getQueryUrlForValidGreaterThanQueryContainsExpectedResources()
			throws UnsupportedEncodingException {
		return getQueryBase()
				+ "oslc.where="
				+ URLEncoder.encode(queryComparisonProperty + ">=\"" + queryComparisonValue + "\"",
						"UTF-8") + "&oslc.select=" + URLEncoder.encode(queryComparisonProperty, "UTF-8");
	}

	protected String getQueryUrlForValidCompoundQueryContainsExpectedResources()
			throws UnsupportedEncodingException {
		return getQueryBase()
				+ "oslc.where="
				+ URLEncoder.encode(queryProperty + "=\"" + queryPropertyValue + "\" and "
						+ queryComparisonProperty + ">=\""
						+ queryComparisonValue + "\"", "UTF-8")
				+ "&oslc.select=" + URLEncoder.encode(queryProperty + ","
				+ queryComparisonProperty, "UTF-8");
	}

	protected String getQueryUrlForFullTextSearchContainsExpectedResults()
			throws UnsupportedEncodingException {
		return getQueryBase() + "oslc.searchTerms="
				+ URLEncoder.encode("\"" + fullTextSearchTerm + "\"", "UTF-8");
	}
	
	protected boolean getFullTextSearch() {
		return fullTextSearch;
	}
}