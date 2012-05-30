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
 *    Yuhong Yin
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.RDFUtils;
import org.eclipse.lyo.testsuite.server.util.SetupProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;


/**
 * This class provides JUnit tests for the validation of OSLCv2 ServiceProvider documents
 * 
 */
@RunWith(Parameterized.class)
public class ServiceProviderRdfXmlTests extends TestsBase {
	
	protected HttpResponse response;
	protected String fContentType = OSLCConstants.CT_RDF;
	private Model fRdfModel = ModelFactory.createDefaultModel();
	private Resource fServiceProvider = null;
	

	public ServiceProviderRdfXmlTests(String url)
	{
		super(url);
	}
	
	@Before
	public void setup() throws IOException, ParserConfigurationException, SAXException, XPathException
	{
		super.setup();
        response = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, basicCreds, fContentType,
        		headers);
		assertEquals("Did not successfully retrieve ServiceProvider at: "+currentUrl, HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

		fRdfModel.read(response.getEntity().getContent(),
				OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
				OSLCConstants.JENA_RDF_XML);
		RDFUtils.validateModel(fRdfModel);
		fServiceProvider = (Resource) fRdfModel.getResource(currentUrl);

		assertTrue("Failed to read ServiceProvider resource at URI: "
				+ currentUrl, fRdfModel.contains(fServiceProvider, RDF.type,
				fRdfModel.createResource(OSLCConstants.SERVICE_PROVIDER_TYPE)));
	}
	
	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() throws IOException
	{
		//Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order to grab all urls
		//to other ServiceProvidersCatalogs contained within it, recursively, in order to find the URLs of all
		//service description documents of the REST service.
		Properties setupProps = SetupProperties.setup(null);
		Collection<Object[]> coll = getReferencedUrls(setupProps.getProperty("baseUri"));
		return coll;
	}
	
	public static Collection<Object[]> getReferencedUrls(String base) throws IOException
	{
	    //ArrayList to contain the urls from all SPCs
	    Collection<Object[]> data = new ArrayList<Object[]>();
	    
	    ArrayList<String> serviceURLs = TestsBase.getServiceProviderURLsUsingRdfXml(base, true);
	    
	    for (String serviceURL : serviceURLs) {
			data.add(new Object[] {serviceURL});
			if (onlyOnce)
				return data;
		}
	    
	    return data;
	}
	
	@Test
	public void currentUrlIsValid()
	{
		assertNotNull("Could not locate a service provider document", currentUrl);
		
        //Get the status, make sure 200 OK
        assertTrue("Expected 200-Ok but received "+response.getStatusLine().toString(), response.getStatusLine().getStatusCode() == 200);
	}

	@Test
	public void rootAboutElementPointsToSelf() throws XPathException, IOException
	{
		assertEquals(currentUrl, fServiceProvider.getURI());
	}

	@Test
	public void typeIsServiceProvider() throws XPathException 
	{		
		Property rdfType = fRdfModel.createProperty(OSLCConstants.RDF_TYPE_PROP);
		StmtIterator iter = fServiceProvider.listProperties(rdfType);
		boolean matches = false;
		// Since resources can have multiple types, iterate over all
		while (iter.hasNext() && !matches) {
			Statement st = iter.nextStatement();
			matches = OSLCConstants.SERVICE_PROVIDER_TYPE.equals(st.getObject().toString());
		}
		assertTrue("Expected rdf:type="+OSLCConstants.SERVICE_PROVIDER_TYPE, matches);
	}

	
	@Test 
	// OSLC: Optional
	public void invalidContentTypeGivesNotSupportedOPTIONAL() throws IOException
	{
		HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, basicCreds, "invalid/content-type", 
				headers);
		String respType =  (resp.getEntity().getContentType() == null) ? "" : resp.getEntity().getContentType().getValue();
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected 406 but received "+resp.getStatusLine()+",Content-type='invalid/content-type' but received "+respType, resp.getStatusLine().getStatusCode() == 406 || respType.contains("invalid/content-type"));
	}
	
	
	@Test
	public void responseContentTypeIsXML() throws IOException
	{
		HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, basicCreds,
				fContentType, headers);
		//Make sure the response to this URL was of valid type
		EntityUtils.consume(resp.getEntity());
		String contentType = resp.getEntity().getContentType().getValue();
		String contentTypeSplit[] = contentType.split(";");
		contentType = contentTypeSplit[0];
		assertTrue(contentType.equalsIgnoreCase("application/xml") ||
				   contentType.equalsIgnoreCase("application/rdf+xml") || 
				   contentType.equalsIgnoreCase("text/xml"));
	}
	
	@Test
	public void misplacedParametersDoNotEffectResponse() throws IOException
	{
		Model baseRespModel = this.fRdfModel;
        String badParmUrl = currentUrl+"?oslc_cm:query";
        
		HttpResponse parameterResp = OSLCUtils.getResponseFromUrl(setupBaseUrl,
				badParmUrl, basicCreds, fContentType,
				headers);
		assertEquals("Did not successfully retrieve catalog at: " 
				+ badParmUrl, HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
	
		Model badParmModel = ModelFactory.createDefaultModel();
		badParmModel.read(parameterResp.getEntity().getContent(),
				OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, badParmUrl),
				OSLCConstants.JENA_RDF_XML);
		RDFUtils.validateModel(fRdfModel);
	
		assertTrue(baseRespModel.isIsomorphicWith(badParmModel));
	}
	
	@Test
	public void serviceProviderHasAtMostOneTitle()
	{
		Property dcTitle = fRdfModel.createProperty(OSLCConstants.DC_TITLE_PROP);
		assertTrue(fServiceProvider.listProperties(dcTitle).toList().size() <= 1);
	}
	
	@Test
	public void serviceProviderHasService()
	{
		Property service = fRdfModel.createProperty(OSLCConstants.SERVICE_PROP);
		List<?> lst = fServiceProvider.listProperties(service).toList();
		assertTrue(lst.size() >= 1);
	}

	/* TODO: Complete ServiceProvider RDF/XML test validation

	@Test
	public void serviceProviderHasValidDetails()
	{
	}
	
	@Test 
	public void prefixDefinitionsAreValid()
	{
	}
	
	@Test
	public void serviceProviderHasAtMostOnePublisher()
	{
	}
	
	@Test
	public void publisherElementsAreValid()
	{
	}
	
	@Test
	public void serviceProviderHasAtMostOneOAuthElement()
	{
	}
	
	@Test
	public void oAuthElementsAreValid()
	{
	}
	
	@Test
	public void eachServiceHasOneDomain()
	{
	}
	
	@Test
	public void creationFactoriesAreValid()
	{
	}
	
	@Test
	public void queryCapabilityBlocksAreValid()
	{
	}
	
	@Test
	public void dialogsAreValid()
	{
	}
	*/
}
