/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;


import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.RDFUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;

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
public class CreationAndUpdateBaseTests extends TestsBase {

	@SuppressWarnings("rawtypes")
	public static String[] getCreateTemplateTypes() throws FileNotFoundException {
		Model m = ModelFactory.createDefaultModel();
		m.read(new StringReader(rdfXmlUpdateTemplate), "http://base.url", "RDF/XML");
		Property rdfType = m.getProperty(OSLCConstants.RDF_TYPE_PROP);
		RDFUtils.printModel(m);
		Selector select = new SimpleSelector(null, rdfType, (RDFNode)null); 
		List l = m.listStatements(select).toList();
		String[] types = new String[l.size()];
		for (int i=0; i<l.size(); i++) {
			types[i] = ((Statement)l.get(i)).getObject().toString();
		}
		return types;
	}

	public CreationAndUpdateBaseTests(String url) {
		super(url);
	}

	@Before
	public void setup() throws IOException, ParserConfigurationException,
			SAXException, XPathException {
		super.setup();
	}

	@Test
	public void createResourceWithInvalidContentType() throws IOException {
		// Issue post request using the provided template and an invalid
		// contentType
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds,
				"*/*", "weird/type", xmlCreateTemplate, headers);
		EntityUtils.consume(resp.getEntity());
		assertTrue(resp.getStatusLine().getStatusCode() == 415);
	}

	protected void createValidResourceUsingTemplate(String contentType,
			String accept, String content) throws IOException {
		// Issue post request using the provided template
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds,
				accept, contentType, content, headers);

		// Assert the response gave a 201 Created
		String responseBody = EntityUtils.toString(resp.getEntity());
		EntityUtils.consume(resp.getEntity());
		assertEquals(responseBody, HttpStatus.SC_CREATED, resp.getStatusLine()
				.getStatusCode());
		Header location = resp.getFirstHeader("Location");
		// Assert that we were given a Location header pointing to the resource,
		// which is not a MUST according to oslc v2, but probably should be
		// present
		// none the less.
		assertFalse(location == null);
		// Attempt to clean up after the test by calling delete on the given
		// url,
		// which is not a MUST according to the oslc cm spec
		resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds, "*/*");
		if (resp.getEntity() != null) {
			EntityUtils.consume(resp.getEntity());
		}
	}

	protected void createResourceWithInvalidContent(String contentType,
			String accept, String content) throws IOException {
		// Issue post request using valid content type but invalid content
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds,
				accept, accept, content, headers);
		EntityUtils.consume(resp.getEntity());
		// TODO: What is right sc forbidden?
		assertFalse("Expecting error but received OK", HttpStatus.SC_OK == resp
				.getStatusLine().getStatusCode());
	}

	protected void createResourceAndUpdateIt(String contentType, String accept,
			String newContent, String updateContent) throws IOException {
		// Issue post request using the provided template
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds,
				accept, contentType, newContent, headers);

		EntityUtils.consume(resp.getEntity());
		assertEquals(HttpStatus.SC_CREATED, resp.getStatusLine()
				.getStatusCode());
		Header location = resp.getFirstHeader("Location");
		assertFalse(location == null);
		
		Header eTag = resp.getFirstHeader("ETag");
		Header lastModified = resp.getFirstHeader("Last-Modified");
		
		// then fail, because we expect at least one, if not both
		assertTrue("ETag("+eTag+") and Last-Modified("+lastModified+") must not be null", eTag != null || lastModified != null ) ;
		int size = headers.length;
		if( eTag != null ) size++;
		if( lastModified != null ) size++;
		Header[] putHeaders = new Header[size];
		int i=0;
		for(;i<headers.length;i++){
			putHeaders[i] = headers[i];
		}
		if( eTag != null ) {
			putHeaders[i++] = new BasicHeader("If-Match", eTag.getValue());
		}
		if( lastModified != null ) {
			putHeaders[i++] = new BasicHeader("If-Unmodified-Since", lastModified.getValue());
		}

		// Now, go to the url of the new change request and update it.
		// We may need to add something to update URL to match the template
		String updateUrl = location.getValue();
		if (updateParams != null && !updateParams.isEmpty())
			updateUrl = updateUrl + updateParams;
		resp = OSLCUtils.putDataToUrl(updateUrl, basicCreds, accept,
				contentType, updateContent, putHeaders);
		String responseBody = EntityUtils.toString(resp.getEntity());
		if (resp.getEntity() != null)
			EntityUtils.consume(resp.getEntity());
		// Assert that a proper PUT resulted in a 200 OK
		assertEquals("HTTP Response body: \n " + responseBody,
				HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());

		// Clean up after the test by attempting to delete the created resource
		if (location != null) {
			resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds,
					"*/*");
			if (resp != null && resp.getEntity() != null)
				EntityUtils.consume(resp.getEntity());
		}
	}

	protected void updateCreatedResourceWithInvalidContent(String contentType,
			String accept, String content, String invalidContent)
			throws IOException {
		// Issue post request using the provided template
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds,
				accept, contentType, content, headers);

		// Assert the response gave a 201 Created
		EntityUtils.consume(resp.getEntity());
		assertEquals(HttpStatus.SC_CREATED, resp.getStatusLine()
				.getStatusCode());
		Header location = resp.getFirstHeader("Location");
		Header eTag = resp.getFirstHeader("ETag");
		Header lastModified = resp.getFirstHeader("Last-Modified");
		
		// Assert that we were given a Location header pointing to the resource
		assertNotNull(
				"Expected 201-Created to return non-null Location header",
				location);
		
		// Ignore ETag and Last-Modified for these tests
		int size = headers.length;
		if( eTag != null ) size++;
		if( lastModified != null ) size++;
		Header[] putHeaders = new Header[size];
		int i=0;
		for(;i<headers.length;i++){
			putHeaders[i] = headers[i];
		}
		if( eTag != null ) {
			putHeaders[i++] = new BasicHeader("If-Match", eTag.getValue());
		}
		if( lastModified != null ) {
			putHeaders[i++] = new BasicHeader("If-Unmodified-Since", lastModified.getValue());
		}		

		// Now, go to the url of the new change request and update it.
		resp = OSLCUtils.putDataToUrl(location.getValue(), basicCreds, accept,
				contentType, invalidContent, putHeaders);
		if (resp.getEntity() != null) {
			EntityUtils.consume(resp.getEntity());
		}
		// Assert that an invalid PUT resulted in a 400 BAD REQUEST
		assertEquals(HttpStatus.SC_BAD_REQUEST, resp.getStatusLine()
				.getStatusCode());

		// Clean up after the test by attempting to delete the created resource
		if (location != null)
			resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds, "");

		if (resp != null && resp.getEntity() != null)
			EntityUtils.consume(resp.getEntity());
	}

	protected void updateCreatedResourceWithBadType(String contentType,
			String accept, String createContent, String updateContent,
			String badType) throws IOException {
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds,
				accept, contentType, createContent, headers);

		// Assert the response gave a 201 Created
		EntityUtils.consume(resp.getEntity());
		assertEquals(HttpStatus.SC_CREATED, resp.getStatusLine()
				.getStatusCode());
		Header location = resp.getFirstHeader("Location");
		Header eTag = resp.getFirstHeader("ETag");
		Header lastModified = resp.getFirstHeader("Last-Modified");

		// Assert that we were given a Location header pointing to the resource
		assertFalse("Expected Location header on 201-Created", location == null);

		// Ignore eTag and Last-Modified for this test
		int size = headers.length;
		if( eTag != null ) size++;
		if( lastModified != null ) size++;
		Header[] putHeaders = new Header[size];
		int i=0;
		for(;i<headers.length;i++){
			putHeaders[i] = headers[i];
		}
		if( eTag != null ) {
			putHeaders[i++] = new BasicHeader("If-Match", eTag.getValue());
		}
		if( lastModified != null ) {
			putHeaders[i++] = new BasicHeader("If-Unmodified-Since", lastModified.getValue());
		}

		// Now, go to the url of the new change request and update it.
		resp = OSLCUtils.putDataToUrl(location.getValue(), basicCreds, "*/*",
				badType, updateContent, putHeaders);
		if (resp != null && resp.getEntity() != null)
			EntityUtils.consume(resp.getEntity());

		assertEquals(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, resp.getStatusLine()
				.getStatusCode());

		// Clean up after the test by attempting to delete the created resource
		if (location != null)
			resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds, "");

		if (resp != null && resp.getEntity() != null)
			EntityUtils.consume(resp.getEntity());
	}

}
