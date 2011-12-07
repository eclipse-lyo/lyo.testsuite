/*******************************************************************************
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
package org.eclipse.lyo.testsuite.server.oslcv2tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;


import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.RDFUtils;
import org.eclipse.lyo.testsuite.server.util.SetupProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;


public class TestsBase {
	public enum AuthMethods {BASIC, FORM, OAUTH};

	protected static Credentials basicCreds;
	protected static boolean onlyOnce = true;
	protected static boolean useDefaultUsageForCreation = true;
	protected static Properties setupProps = null;
	protected static String xmlCreateTemplate;
	protected static String xmlUpdateTemplate;
	protected static String rdfXmlCreateTemplate;
	protected static String rdfXmlUpdateTemplate;
	protected static String jsonCreateTemplate;
	protected static String jsonUpdateTemplate;
	protected static String updateParams;
	protected static String useThisServiceProvider;
	protected static Header[] headers;
	protected static AuthMethods authMethod = AuthMethods.BASIC;

	protected String currentUrl = null;      // URL of current service being tested
	protected static String setupBaseUrl = null;  // Configuration baseUrl, think ServiceProvider or ServiceProviderCatalog
	
	public TestsBase(String thisUrl) {
		currentUrl = thisUrl;
	}
	
	public static void staticSetup() {
		if (setupProps == null) {
			setupProps = SetupProperties.setup(null);
			updateParams = setupProps.getProperty("updateParams");
			String userId = setupProps.getProperty("userId");
			String pw = setupProps.getProperty("pw");
			basicCreds = new UsernamePasswordCredentials(userId, pw);
			Header h = new BasicHeader("OSLC-Core-Version", "2.0");
			Header h2 = new BasicHeader("DoorsRP-Request-Type", "private"); // TODO: RRC special sauce
			headers = new Header[] { h, h2 };
			String onlyOnceStr = setupProps.getProperty("runOnlyOnce");
			if (onlyOnceStr != null && onlyOnceStr.equals("false")) {
				onlyOnce = false;
			}
			String defUsageStr = setupProps.getProperty("useDefaultUsageForCreation");
			if (defUsageStr != null && defUsageStr.equals("false")) {
				useDefaultUsageForCreation = false;
			}
			setupBaseUrl = setupProps.getProperty("baseUri");
			String authType = setupProps.getProperty("authMethod");
			if (authType.equalsIgnoreCase("OAUTH")) {
				authMethod = AuthMethods.OAUTH;
			} else if (authType.equalsIgnoreCase("FORM")) {
				authMethod = AuthMethods.FORM;
				formLogin(userId, pw);
			}
			
			useThisServiceProvider = setupProps.getProperty("useThisServiceProvider");

			// First, Setup plain old XML
			String fileName = setupProps.getProperty("createTemplateXmlFile");
			if (fileName != null)
				xmlCreateTemplate = OSLCUtils.readFileByNameAsString(fileName);
			fileName = setupProps.getProperty("updateTemplateXmlFile");
			if (fileName != null)
				xmlUpdateTemplate = OSLCUtils.readFileByNameAsString(fileName);
			// Now RDF/XML
			fileName = setupProps.getProperty("createTemplateRdfXmlFile");
			if (fileName != null)
				rdfXmlCreateTemplate = OSLCUtils.readFileByNameAsString(fileName);
			fileName = setupProps.getProperty("updateTemplateRdfXmlFile");
			if (fileName != null)
				rdfXmlUpdateTemplate = OSLCUtils.readFileByNameAsString(fileName);
			// Now JSON
			fileName = setupProps.getProperty("createTemplateJsonFile");
			if (fileName != null)
				jsonCreateTemplate = OSLCUtils.readFileByNameAsString(fileName);
			fileName = setupProps.getProperty("updateTemplateJsonFile");
			if (fileName != null)
				jsonUpdateTemplate = OSLCUtils.readFileByNameAsString(fileName);
			// Now handle if RDF/XML wasn't given
			if (rdfXmlCreateTemplate == null)
				rdfXmlCreateTemplate = xmlCreateTemplate;
			if (rdfXmlUpdateTemplate == null)
				rdfXmlUpdateTemplate = xmlUpdateTemplate;
		}
	}

	public void setup() throws IOException, ParserConfigurationException,
			SAXException, XPathException {
		staticSetup();
	}

	public static ArrayList<String> getServiceProviderURLsUsingXML(String inBaseURL)
			throws IOException, XPathException, ParserConfigurationException,
			SAXException {
		return getServiceProviderURLsUsingXML(inBaseURL, onlyOnce);
	}
	
	public static Collection<Object[]> toCollection(ArrayList<String> list) {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (String string : list) {
			data.add(new Object[] {string});
		}
		return data;	
	}
	
	public static ArrayList<String> getServiceProviderURLsUsingXML(String inBaseURL, boolean dontGoDeep)
		throws IOException, XPathException, ParserConfigurationException,
		SAXException {
		staticSetup();
		String base=null;
		if (inBaseURL == null) 
			base = setupBaseUrl;
		else
			base = inBaseURL;
		HttpResponse resp = OSLCUtils.getResponseFromUrl(base, base,
				basicCreds, OSLCConstants.CT_XML, headers);

		Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(EntityUtils
				.toString(resp.getEntity()));

		// ArrayList to contain the urls from all SPCs
		ArrayList<String> data = new ArrayList<String>();

		// Get all ServiceProvider urls from the base document in order to
		// recursively add all the capability urls from them as well.
		//   Inlined using oslc:ServiceProvider/@rdf:about
		NodeList sps = (NodeList) OSLCUtils.getXPath().evaluate(
				"//oslc_v2:ServiceProvider/@rdf:about", baseDoc,
				XPathConstants.NODESET);
		for (int i = 0; i < sps.getLength(); i++) {
			if (!sps.item(i).getNodeValue().equals(base) || sps.getLength() == 1) {
				data.add(sps.item(i).getNodeValue());
				if (dontGoDeep)
					return data;
			}
		}
		
		// Get all ServiceProvider urls from the base document in order to
		// recursively add all the capability urls from them as well.
		//   Referenced using oslc:serviceProvider/@rdf:resource
		sps = (NodeList) OSLCUtils.getXPath().evaluate(
				"//oslc_v2:serviceProvider/@rdf:resource", baseDoc,
				XPathConstants.NODESET);
		for (int i = 0; i < sps.getLength(); i++) {
			if (!sps.item(i).getNodeValue().equals(base) || sps.getLength() == 1) {
				data.add(sps.item(i).getNodeValue());
				if (dontGoDeep)
					return data;
			}
		}
		
		// Get all ServiceProviderCatalog urls from the base document in order
		// to recursively add all the capability from ServiceProviders within them as well.
		//   Inlined using oslc:ServiceProviderCatalog/@rdf:about		
		NodeList spcs = (NodeList) OSLCUtils.getXPath().evaluate(
				"//oslc_v2:ServiceProviderCatalog/@rdf:about", baseDoc,
				XPathConstants.NODESET);
		for (int i = 0; i < spcs.getLength(); i++) {
			if (!spcs.item(i).getNodeValue().equals(base)) {
				ArrayList<String> subCollection = getServiceProviderURLsUsingXML(spcs
						.item(i).getNodeValue(), dontGoDeep);
				for (String subUri : subCollection) {
					data.add(subUri);
					if (dontGoDeep)
						return data;
				}
			}
		}
		// Get all ServiceProviderCatalog urls from the base document in order
		// to recursively add all the capability from ServiceProviders within them as well.
		//   Referenced using oslc:serviceProviderCatalog/@rdf:resource		
		spcs = (NodeList) OSLCUtils.getXPath().evaluate(
				"//oslc_v2:serviceProviderCatalog/@rdf:resource", baseDoc,
				XPathConstants.NODESET);
		for (int i = 0; i < spcs.getLength(); i++) {
			if (!spcs.item(i).getNodeValue().equals(base)) {
				ArrayList<String> subCollection = getServiceProviderURLsUsingXML(spcs
						.item(i).getNodeValue(), dontGoDeep);
				for (String subUri : subCollection) {
					data.add(subUri);
					if (dontGoDeep)
						return data;
				}
			}
		}

		return data;
	}
	
	public static ArrayList<Node> getCapabilityDOMNodesUsingXML(String xpathStmt,
			ArrayList<String> serviceUrls) throws IOException,
			ParserConfigurationException, SAXException,
			XPathExpressionException {
		// Collection to contain the creationFactory urls from all SPs
		ArrayList<Node> data = new ArrayList<Node>();
		
		for (String base : serviceUrls) {
			HttpResponse resp = OSLCUtils.getResponseFromUrl(base, base,
					basicCreds, OSLCConstants.CT_XML, headers);

			Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(EntityUtils
					.toString(resp.getEntity()));

			NodeList sDescs = (NodeList) OSLCUtils.getXPath().evaluate(
					xpathStmt,
					baseDoc, XPathConstants.NODESET);
			for (int i = 0; i < sDescs.getLength(); i++) {
				data.add(sDescs.item(i));
				if (onlyOnce)
					return data;
			}
		}
		return data;
	}
	
	public static ArrayList<String> getCapabilityURLsUsingXML(String xpathStmt,
			ArrayList<String> serviceUrls, boolean useDefaultUsage) throws IOException,
			ParserConfigurationException, SAXException,
			XPathExpressionException {
		// Collection to contain the creationFactory urls from all SPs
		ArrayList<String> data = new ArrayList<String>();
		String firstUrl = null;
		
		for (String base : serviceUrls) {
			HttpResponse resp = OSLCUtils.getResponseFromUrl(base, base,
					basicCreds, OSLCConstants.CT_XML, headers);

			Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(EntityUtils
					.toString(resp.getEntity()));

			NodeList sDescs = (NodeList) OSLCUtils.getXPath().evaluate(
					xpathStmt,
					baseDoc, XPathConstants.NODESET);
			String xpathSubStmt = "../../oslc_v2:usage/@rdf:resource";
			for (int i = 0; i < sDescs.getLength(); i++) {
				if (firstUrl == null)
					firstUrl = sDescs.item(i).getNodeValue();
				if (useDefaultUsage) {
					NodeList usages = (NodeList) OSLCUtils.getXPath().evaluate(
							xpathSubStmt,
							sDescs.item(i), XPathConstants.NODESET);
					for (int u=0; u < usages.getLength(); u++) {
						String usageValue = usages.item(u).getNodeValue();
						if (OSLCConstants.USAGE_DEFAULT_URI.equals(usageValue)) {
							data.add(sDescs.item(i).getNodeValue());
							return data;
						}
					}
				} else {
					data.add(sDescs.item(i).getNodeValue());
					if (onlyOnce)
						return data;
				}
			}
		}
		// If we didn't find the default, then just send back the first one we
		// found.
		if (useDefaultUsage && firstUrl != null)
			data.add(firstUrl);
		return data;
	}

	public static ArrayList<String> getServiceProviderURLsUsingRdfXml(String inBaseURL, boolean dontGoDeep)
	throws IOException {
		staticSetup();
		
	    // ArrayList to contain the urls from all SPCs
	    ArrayList<String> data = new ArrayList<String>();
	    
	    // If we are given a shortcut, then use it and skip the rest
	    if (useThisServiceProvider != null && useThisServiceProvider.length() > 0) {
	    	data.add(useThisServiceProvider);
	    	return data;
	    }

	    HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, inBaseURL, basicCreds, OSLCConstants.CT_RDF, headers);
		assertEquals("Did not successfully retrieve ServiceProviders at: "+inBaseURL, HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());

	    // Used to hold RDF from doing service discovery
		Model spModel = ModelFactory.createDefaultModel(); 
		spModel.read(resp.getEntity().getContent(),
				OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, inBaseURL),
				OSLCConstants.JENA_RDF_XML);
		EntityUtils.consume(resp.getEntity());
		RDFUtils.validateModel(spModel);
		
		// Get all the "inlined" definitions for Service Providers, namely
		// all subjects whose rdf:type = oslc:ServiceProvider
		Property rdfType = spModel.createProperty(OSLCConstants.RDF_TYPE_PROP);
		Resource spTypeRes = spModel.getResource(OSLCConstants.SERVICE_PROVIDER_TYPE);
		Selector select = new SimpleSelector(null, rdfType, spTypeRes); 
		StmtIterator statements = spModel.listStatements(select);
		// Since resources can have multiple types, iterate over all
		while (statements.hasNext()) {
			Statement st = statements.nextStatement();
			data.add(st.getSubject().getURI());
			if (dontGoDeep)	return data;
		}

		// Get all the "referenced" definitions for Service Providers, namely
		// of form: <oslc:serviceProvider rdf:resource="url" />
		Property spProp = spModel.createProperty(OSLCConstants.SERVICE_PROVIDER_PROP);
		select = new SimpleSelector(null, spProp, (RDFNode)null); 
		statements = spModel.listStatements(select);
		// Since resources can have multiple types, iterate over all
		while (statements.hasNext()) {
			Statement st = statements.nextStatement();
			data.add(st.getObject().toString());
			if (dontGoDeep)	return data;
		}
		
		// Chase any ServiceProviderCatalogs, looking for ServiceProviders definitions.
		Property spcPredicate = spModel.createProperty(OSLCConstants.SERVICE_PROVIDER_CATALOG_PROP);
		select = new SimpleSelector(null, spcPredicate, (RDFNode)null); 
		statements = spModel.listStatements(select);
		while (statements.hasNext()) {
			ArrayList<String> results = getServiceProviderURLsUsingRdfXml(statements.nextStatement().getObject().toString(), dontGoDeep);
			data.addAll(results);
			if (dontGoDeep) return data;
		}		
	    
	    return data;		
	}

	public static ArrayList<String> getCapabilityURLsUsingRdfXml(String propertyUri,
			ArrayList<String> serviceUrls, boolean useDefaultUsage) throws IOException {
		return getCapabilityURLsUsingRdfXml(propertyUri, serviceUrls, useDefaultUsage, null);
	}
	
	public static ArrayList<String> getCapabilityURLsUsingRdfXml(String propertyUri,
			ArrayList<String> serviceUrls, boolean useDefaultUsage, String[] types) throws IOException {
		// Collection to contain the creationFactory urls from all SPs
		ArrayList<String> data = new ArrayList<String>();
		String firstUrl = null;
		for (String base : serviceUrls) {
			HttpResponse resp = OSLCUtils.getResponseFromUrl(base, base,
					basicCreds, OSLCConstants.CT_RDF, headers);
			
			Model spModel = ModelFactory.createDefaultModel();
			spModel.read(resp.getEntity().getContent(), base, OSLCConstants.JENA_RDF_XML);
			RDFUtils.validateModel(spModel);
			
			Property capProp = spModel.createProperty(propertyUri);
			Property usageProp = spModel.createProperty(OSLCConstants.USAGE_PROP);
			Selector select = new SimpleSelector(null, capProp, (RDFNode)null);
			StmtIterator statements = spModel.listStatements(select);
			while (statements.hasNext()) {
				Statement stmt = statements.nextStatement();
				if (firstUrl == null)
					firstUrl = stmt.getObject().toString();
				if (useDefaultUsage) {
					StmtIterator usages = stmt.getSubject().listProperties(usageProp);
					while (usages.hasNext()) {
						Statement usageStmt = usages.nextStatement();
						if (OSLCConstants.USAGE_DEFAULT_URI.equals(usageStmt.getObject().toString())) {
							data.add(stmt.getObject().toString());
							return data;
						}
					}
				} else {
					// Now if we have types, we match the capability for the given types
					if (types != null && types.length > 0) {
						Property typeProp = spModel.getProperty(OSLCConstants.RESOURCE_TYPE_PROP);
						StmtIterator typeIter = stmt.getSubject().listProperties(typeProp);
						while (typeIter.hasNext()) {
							String typeName = typeIter.nextStatement().getObject().toString();
							for (String t: types) {
								if (t.equals(typeName)) {
									data.add(stmt.getObject().toString());
									if (onlyOnce) return data;
								}
							}
						}
					} else {
						data.add(stmt.getObject().toString());
						if (onlyOnce) return data;
					}
				}
			}			
		}
		// If no default usage was found, then just return first one
		if (useDefaultUsage && firstUrl != null)
			data.add(firstUrl);
		return data;
	}

	public static boolean formLogin(String userId, String pw) {
		String formUri = setupProps.getProperty("formUri");
		// Get cookies for forms login procedure (ie: get redirected to login
		// page.
		HttpResponse resp;
		try {
			resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, setupBaseUrl, null, "*/*");
			if (resp.getEntity() != null) {
				EntityUtils.consume(resp.getEntity());
			}
			// Post info to forms auth page
			OSLCUtils.setupFormsAuth(formUri, userId, pw);
		} catch (ClientProtocolException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public static void printRdfModel(Model model) {
		StmtIterator listStatements = model.listStatements();
		while (listStatements.hasNext()) {
			Statement s = listStatements.nextStatement();
			System.out.println(s.toString());
		}
	}

}
