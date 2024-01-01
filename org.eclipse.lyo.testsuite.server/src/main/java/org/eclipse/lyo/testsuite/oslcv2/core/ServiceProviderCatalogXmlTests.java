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
 *    Steve Speicher - initial API and implementation
 *    Yuhong Yin
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * This class provides JUnit tests for the validation of OSLC Service Provider
 * Catalogs, for the 2.0 version of the OSLC standard, as defined by the OSLC
 * Core Spec.
 */
@RunWith(Parameterized.class)
public class ServiceProviderCatalogXmlTests extends
		ServiceProviderCatalogBaseTests {

	// Base URL of the OSLC Service Provider Catalog to be tested
	protected String responseBody;
	protected Document doc;
	protected HttpResponse response = null;

	public ServiceProviderCatalogXmlTests(String url)
		throws IOException, ParserConfigurationException, SAXException
	{
		super(url);

		fContentType = OSLCConstants.CT_XML;

		response = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl,
				creds, fContentType, headers);
		responseBody = EntityUtils.toString(response.getEntity());
		// Get XML Doc from response
		doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
	}

	@Parameters
	public static Collection<Object[]> getAllServiceProviderCatalogUrls()
			throws IOException, ParserConfigurationException, SAXException,
			XPathException {
		// Checks the ServiceProviderCatalog at the specified baseUrl of the
		// REST service in order to grab all urls
		// to other ServiceProviders contained within it, recursively.

		staticSetup();

		Collection<Object[]> coll = getReferencedCatalogUrlsUsingXML(setupProps
				.getProperty("baseUri"));
		return coll;
	}

	public static Collection<Object[]> getReferencedCatalogUrlsUsingXML(
			String base) throws IOException, ParserConfigurationException,
			SAXException, XPathException {

		staticSetup();

		HttpResponse resp = OSLCUtils.getResponseFromUrl(base, base,
				creds, OSLCConstants.CT_XML, headers);

        try {
            int statusCode = resp.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK != statusCode) {
                throw new IllegalStateException("Response code: " + statusCode + " for " + base + " (" + resp.getStatusLine().getReasonPhrase() + ")");
            }

            String respBody = EntityUtils.toString(resp.getEntity());
            Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(respBody);

            // ArrayList to contain the urls from all SPCs
            Collection<Object[]> data = new ArrayList<Object[]>();
            Node rootElement = (Node) OSLCUtils.getXPath().evaluate("/rdf:RDF/*",
                    baseDoc, XPathConstants.NODE);
            if (rootElement.getNamespaceURI().equals(OSLCConstants.OSLC_V2)
                    && rootElement.getLocalName().equals("ServiceProviderCatalog")) {
                data.add(new Object[] { base });
            }

            // Get all ServiceProviderCatalog urls from the base document in order
            // to test them as well,
            // recursively checking them for other ServiceProviderCatalogs further
            // down.
            NodeList spcs = (NodeList) OSLCUtils
                    .getXPath()
                    .evaluate(
                            "//oslc_v2:serviceProviderCatalog/oslc_v2:ServiceProviderCatalog/@rdf:about",
                            baseDoc, XPathConstants.NODESET);
            for (int i = 0; i < spcs.getLength(); i++) {
                if (!spcs.item(i).getNodeValue().equals(base)) {
                    Collection<Object[]> subCollection = getReferencedCatalogUrlsUsingXML(spcs
                        .item(i).getNodeValue());
                    Iterator<Object[]> iter = subCollection.iterator();
                    while (iter.hasNext()) {
                        data.add(iter.next());
                    }
                }
            }
            return data;
        } finally {
            EntityUtils.consume(resp.getEntity());
        }
    }

	@Test
	public void baseUrlIsValid() throws IOException {
		// Get the status, make sure 200 OK
		assertTrue(response.getStatusLine().toString(), response
				.getStatusLine().getStatusCode() == HttpStatus.SC_OK);

		// Verify we got a response
		assertNotNull(responseBody);
	}

	@Test
	public void catalogRootIsRdfNamespaceDeclaration() throws XPathException {
		// Make sure our root element is the RDF namespace declarations
		Node rootNode = (Node) OSLCUtils.getXPath().evaluate("/rdf:RDF", doc,
				XPathConstants.NODE);
		assertNotNull(rootNode);
	}

	@Test
	public void catalogRootAboutElementPointsToSelf() throws XPathException,
			IOException {
		// Make sure that we our root element has an rdf:about that points the
		// same server provider catalog
		Node aboutRoot = (Node) OSLCUtils.getXPath().evaluate(
				"/*/oslc_v2:ServiceProviderCatalog/@rdf:about", doc,
				XPathConstants.NODE);
		assertNotNull(aboutRoot);
		assertTrue(currentUrl.equals(aboutRoot.getNodeValue()));

	}

	@Test
	public void serviceProviderCatalogsHaveAtMostOneTitle()
			throws XPathException {
		// Check root to make sure it has at most one title.
		NodeList rootChildren = (NodeList) OSLCUtils.getXPath().evaluate(
				"/rdf:RDF/oslc_v2:ServiceProviderCatalog/*", doc,
				XPathConstants.NODESET);
		int numTitles = 0;
		for (int i = 0; i < rootChildren.getLength(); i++) {
			if (rootChildren.item(i).getNamespaceURI().equals(OSLCConstants.DC)
					&& rootChildren.item(i).getLocalName().equals("title")) {
				numTitles++;
			}
		}
		assert (numTitles <= 1);

		// Get all service provider catalogs listed
		NodeList nestedSPCs = (NodeList) OSLCUtils.getXPath().evaluate(
				"/*/*//oslc_v2:serviceProviderCatalog", doc,
				XPathConstants.NODESET);

		for (int i = 0; i < nestedSPCs.getLength(); i++) {
			NodeList spcChildren = (NodeList) OSLCUtils.getXPath().evaluate(
					"/*/*//oslc_v2:serviceProviderCatalog[" + i + "]/*/*", doc,
					XPathConstants.NODESET);
			int titleCount = 0;
			// Go through the service provider catalog's children, make sure it
			// contains at most one title.
			for (int j = 0; j < spcChildren.getLength(); j++) {
				if (spcChildren.item(j).getNamespaceURI().equals(
						OSLCConstants.DC)
						&& spcChildren.item(j).getLocalName().equals("title")) {
					titleCount++;
				}
			}
			assert (titleCount <= 1);
		}
	}

	@Test
	public void serviceProvidersHaveAtMostOneTitle() throws XPathException {
		// Get all service providers listed
		NodeList nestedSPCs = (NodeList) OSLCUtils.getXPath().evaluate(
				"//oslc_v2:serviceProvider", doc, XPathConstants.NODESET);

		for (int i = 0; i < nestedSPCs.getLength(); i++) {
			NodeList spcChildren = (NodeList) OSLCUtils.getXPath().evaluate(
					"./*/*", nestedSPCs.item(i), XPathConstants.NODESET);
			int titleCount = 0;

			// Go through the service provider's children, make sure it contains
			// at most one title.
			for (int j = 0; j < spcChildren.getLength(); j++) {
				if (spcChildren.item(j).getNamespaceURI().equals(
						OSLCConstants.DC)
						&& spcChildren.item(j).getLocalName().equals("title")) {
					titleCount++;
				}
			}
			assert (titleCount <= 1);
		}
	}

	@Test
	public void serviceProviderCatalogsHaveAtMostOnePublisher()
			throws XPathExpressionException {
		// Check root for Publisher, make sure it only has at most one
		NodeList rootChildren = (NodeList) OSLCUtils.getXPath().evaluate(
				"/rdf:RDF/oslc_v2:ServiceProviderCatalog/*", doc,
				XPathConstants.NODESET);
		int numPublishers = 0;
		for (int i = 0; i < rootChildren.getLength(); i++) {
			if (rootChildren.item(i).getNamespaceURI().equals(OSLCConstants.DC)
					&& rootChildren.item(i).getLocalName().equals("publisher")) {
				numPublishers++;
			}
		}
		assert (numPublishers <= 1);

		// Get list of other ServiceProviderCatalog elements
		NodeList nestedSPCs = (NodeList) OSLCUtils.getXPath().evaluate(
				"/*/*//oslc_v2:serviceProviderCatalog", doc,
				XPathConstants.NODESET);
		// Go through the children of each catalog
		for (int i = 0; i < nestedSPCs.getLength(); i++) {
			NodeList spcChildren = (NodeList) OSLCUtils.getXPath().evaluate(
					"/*/*//oslc_v2:serviceProviderCatalog[" + i + "]/*/*", doc,
					XPathConstants.NODESET);
			int publisherCount = 0;
			// Make sure there's at most one Publisher blocks
			for (int j = 0; j < spcChildren.getLength(); j++) {
				if (spcChildren.item(j).getNamespaceURI().equals(
						OSLCConstants.DC)
						&& spcChildren.item(j).getLocalName().equals(
								"publisher")) {
					publisherCount++;
				}
			}
			assert (publisherCount <= 1);
		}
	}

	@Test
	public void serviceProvidersHaveAtMostOnePublisher()
			throws XPathExpressionException {
		// Get the listed ServiceProvider elements
		NodeList nestedSPCs = (NodeList) OSLCUtils.getXPath().evaluate(
				"/*/*//oslc_v2:serviceProvider", doc, XPathConstants.NODESET);

		// Make sure that for each one it only has at most one Publisher block
		for (int i = 0; i < nestedSPCs.getLength(); i++) {
			NodeList spcChildren = (NodeList) OSLCUtils.getXPath().evaluate(
					"/*/*//oslc_v2:serviceProvider[" + i + "]/*/*", doc,
					XPathConstants.NODESET);
			int publisherCount = 0;
			for (int j = 0; j < spcChildren.getLength(); j++) {
				if (spcChildren.item(j).getNamespaceURI().equals(
						OSLCConstants.DC)
						&& spcChildren.item(j).getLocalName().equals(
								"publisher")) {
					publisherCount++;
				}
			}
			assert (publisherCount <= 1);
		}
	}

	@Test
	public void publisherElementsAreValid() throws XPathExpressionException {
		// Get all Publisher xml blocks
		NodeList publishers = (NodeList) OSLCUtils.getXPath().evaluate(
				"//dc:publisher/*", doc, XPathConstants.NODESET);

		// Verify that each block contains a title and identifier, and at most
		// one icon and label
		for (int i = 0; i < publishers.getLength(); i++) {
			NodeList publisherElements = publishers.item(i).getChildNodes();
			int titleCount = 0;
			int identifierCount = 0;
			int iconCount = 0;
			int labelCount = 0;
			for (int j = 0; j < publisherElements.getLength(); j++) {
				Node ele = publisherElements.item(j);
				if (ele.getLocalName() == null) {
					continue;
				}
				if (ele.getNamespaceURI().equals(OSLCConstants.DC)
						&& ele.getLocalName().equals("title")) {
					titleCount++;
				}
				if (ele.getNamespaceURI().equals(OSLCConstants.DC)
						&& ele.getLocalName().equals("identifier")) {
					identifierCount++;
				}
				if (ele.getNamespaceURI().equals(OSLCConstants.OSLC_V2)
						&& ele.getLocalName().equals("label")) {
					labelCount++;
				}
				if (ele.getNamespaceURI().equals(OSLCConstants.OSLC_V2)
						&& ele.getLocalName().equals("icon")) {
					iconCount++;
				}
			}
			assertTrue(titleCount == 1);
			assertTrue(identifierCount == 1);
			assertTrue(iconCount <= 1);
			assertTrue(labelCount <= 1);
		}
	}

	@Test
	public void serviceProviderCatalogsHaveAtMostOneOAuthElement()
			throws XPathExpressionException {
		// Check root for OAuth block, make sure it only has at most one
		NodeList rootChildren = (NodeList) OSLCUtils.getXPath().evaluate(
				"/rdf:RDF/oslc_v2:ServiceProviderCatalog/*", doc,
				XPathConstants.NODESET);
		int numOAuthElements = 0;
		for (int i = 0; i < rootChildren.getLength(); i++) {
			if (rootChildren.item(i).getNamespaceURI().equals(
					OSLCConstants.OSLC_V2)
					&& rootChildren.item(i).getLocalName().equals(
							"oauthConfiguration")) {
				numOAuthElements++;
			}
		}
		assert (numOAuthElements <= 1);

		// Get list of other ServiceProviderCatalog elements
		NodeList nestedSPCs = (NodeList) OSLCUtils.getXPath().evaluate(
				"/*/*//oslc_v2:serviceProviderCatalog", doc,
				XPathConstants.NODESET);
		// Go through the children of each catalog
		for (int i = 0; i < nestedSPCs.getLength(); i++) {
			NodeList spcChildren = (NodeList) OSLCUtils.getXPath().evaluate(
					"/*/*//oslc_v2:serviceProviderCatalog[" + i + "]/*/*", doc,
					XPathConstants.NODESET);
			int oAuthCount = 0;
			// Make sure there's at most one OAuth blocks
			for (int j = 0; j < spcChildren.getLength(); j++) {
				if (spcChildren.item(j).getNamespaceURI().equals(
						OSLCConstants.OSLC_V2)
						&& spcChildren.item(j).getLocalName().equals(
								"oauthConfiguration")) {
					oAuthCount++;
				}
			}
			assert (oAuthCount <= 1);
		}
	}

	@Test
	public void serviceProvidersHaveAtMostOneOAuthElement()
			throws XPathExpressionException {
		// Get list of other service provider elements
		NodeList nestedSPCs = (NodeList) OSLCUtils.getXPath().evaluate(
				"/*/*//oslc_v2:serviceProvider", doc, XPathConstants.NODESET);
		// Go through the children of each provider
		for (int i = 0; i < nestedSPCs.getLength(); i++) {
			NodeList spcChildren = (NodeList) OSLCUtils.getXPath().evaluate(
					"/*/*//oslc_v2:serviceProvider[" + i + "]/*/*", doc,
					XPathConstants.NODESET);
			int oAuthCount = 0;
			// Make sure there's at most one OAuth blocks
			for (int j = 0; j < spcChildren.getLength(); j++) {
				if (spcChildren.item(j).getNamespaceURI().equals(
						OSLCConstants.OSLC_V2)
						&& spcChildren.item(j).getLocalName().equals(
								"oauthConfiguration")) {
					oAuthCount++;
				}
			}
			assert (oAuthCount <= 1);
		}
	}

	@Test
	public void oAuthElementsAreValid() throws XPathExpressionException {
		// Get all oauthAuthorization xml blocks
		NodeList oAuthElement = (NodeList) OSLCUtils.getXPath().evaluate(
				"//oslc_v2:oauthConfiguration/*", doc, XPathConstants.NODESET);

		// Verify the block contains the required expected elements
		for (int i = 0; i < oAuthElement.getLength(); i++) {
			NodeList oAuthChildren = oAuthElement.item(i).getChildNodes();
			int reqTokenCount = 0;
			int authCount = 0;
			int accessCount = 0;
			for (int j = 0; j < oAuthChildren.getLength(); j++) {
				Node oAuthNode = oAuthChildren.item(j);
				if (oAuthNode.getLocalName() == null) {
					continue;
				}
				if (oAuthNode.getLocalName().equals("oauthRequestTokenURI")
						&& oAuthNode.getNamespaceURI().equals(
								OSLCConstants.OSLC_V2)) {
					reqTokenCount++;
				}
				if (oAuthNode.getLocalName().equals("authorizationURI")
						&& oAuthNode.getNamespaceURI().equals(
								OSLCConstants.OSLC_V2)) {
					authCount++;
				}
				if (oAuthNode.getLocalName().equals("oauthAccessTokenURI")
						&& oAuthNode.getNamespaceURI().equals(
								OSLCConstants.OSLC_V2)) {
					accessCount++;
				}
			}
			assertTrue(reqTokenCount == 1);
			assertTrue(authCount == 1);
			assertTrue(accessCount == 1);
		}
	}

	@Test
	public void serviceProviderCatalogsHaveValidResourceUrl()
			throws XPathException, IOException {
		// Get all ServiceProviderCatalog elements and their rdf:about
		// attributes, making sure we have a URL
		// to the resource for each catalog
		NodeList catalogAbouts = (NodeList) OSLCUtils.getXPath().evaluate(
				"//oslc_v2:ServiceProviderCatalog/@rdf:about", doc,
				XPathConstants.NODESET);
		NodeList catalogs = (NodeList) OSLCUtils.getXPath()
				.evaluate("//oslc_v2:ServiceProviderCatalog", doc,
						XPathConstants.NODESET);
		assertTrue(catalogAbouts.getLength() == catalogs.getLength());

		// Verify the urls are valid
		for (int i = 0; i < catalogAbouts.getLength(); i++) {
			String url = catalogAbouts.item(i).getNodeValue();
			assertFalse(url.isEmpty());
			HttpResponse response = OSLCUtils.getResponseFromUrl(setupBaseUrl, url,
					creds, "*/*");
			EntityUtils.consume(response.getEntity());
			assertFalse(response.getStatusLine().getStatusCode() == 404);
		}
	}

	@Test
	public void servicesProvidersHaveValidResourceUrl() throws XPathException,
			IOException {
		// Get all ServiceProvider elements
		NodeList services = (NodeList) OSLCUtils.getXPath().evaluate(
				"//oslc_v2:ServiceProvider", doc, XPathConstants.NODESET);
		// Get all resource attributes from the ServiceProviders
		NodeList resources = (NodeList) OSLCUtils.getXPath().evaluate(
				"//oslc_v2:ServiceProvider/@rdf:about", doc,
				XPathConstants.NODESET);
		// Make sure each ServiceProvider element has an attribute to reference
		// it
		assertTrue(services.getLength() == resources.getLength());
		// Verify that the resource urls are valid
		for (int i = 0; i < resources.getLength(); i++) {
			String url = resources.item(i).getNodeValue();
			assertNotNull(url);
			HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, url,
					creds, "*/*");
			EntityUtils.consume(resp.getEntity());
			assertFalse(resp.getStatusLine().getStatusCode() == 404);
		}
	}

	@Test
	public void detailsElementsHaveValidResourceAttribute() throws IOException,
			XPathException {
		// Get all details elements
		NodeList detailsElements = (NodeList) OSLCUtils.getXPath().evaluate(
				"//oslc_v2:details", doc, XPathConstants.NODESET);
		// Get all resource attributes of the details elements
		NodeList resources = (NodeList) OSLCUtils.getXPath().evaluate(
				"//oslc_v2:details/@rdf:resource", doc, XPathConstants.NODESET);
		// Make sure they match up 1-to-1
		assertTrue(detailsElements.getLength() == resources.getLength());
		// Verify that the resource has a url
		for (int i = 0; i < resources.getLength(); i++) {
			String url = resources.item(i).getNodeValue();
			assertNotNull(url);
		}
	}

	@Test
	public void misplacedParametersDoNotEffectResponse() throws IOException {
		HttpResponse baseResp = OSLCUtils.getResponseFromUrl(setupBaseUrl,
				currentUrl, creds, fContentType, headers);

		Model baseRespModel = ModelFactory.createDefaultModel();
		baseRespModel.read(baseResp.getEntity().getContent(),
				OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
				OSLCConstants.JENA_RDF_XML);
		RDFUtils.validateModel(baseRespModel);

		String badParmUrl = currentUrl+"?oslc_cm:query";

		HttpResponse parameterResp = OSLCUtils.getResponseFromUrl(setupBaseUrl,
				badParmUrl, creds, fContentType,
				headers);

		Model badParmModel = ModelFactory.createDefaultModel();
		badParmModel.read(parameterResp.getEntity().getContent(),
				OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, badParmUrl),
				OSLCConstants.JENA_RDF_XML);
		RDFUtils.validateModel(badParmModel);

		assertTrue(baseRespModel.isIsomorphicWith(badParmModel));
	}
}
