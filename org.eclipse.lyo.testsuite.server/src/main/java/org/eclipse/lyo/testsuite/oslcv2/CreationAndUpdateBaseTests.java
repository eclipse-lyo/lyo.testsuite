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
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase.AuthMethods;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.RDFUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
public abstract class CreationAndUpdateBaseTests extends TestsBase {

	@SuppressWarnings("rawtypes")
	public static String[] getCreateTemplateTypes() throws FileNotFoundException {
		Model m = ModelFactory.createDefaultModel();
		m.read(new StringReader(rdfXmlUpdateTemplate), "http://base.url", OSLCConstants.JENA_RDF_XML);
		RDFUtils.validateModel(m);
		Property rdfType = m.getProperty(OSLCConstants.RDF_TYPE_PROP);
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

	@Test
	public void createResourceWithInvalidContentType() throws IOException {
		// Issue post request using the provided template and an invalid
		// contentType
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds,
				"*/*", "weird/type", xmlCreateTemplate, headers);
		EntityUtils.consume(resp.getEntity());
		assertTrue(resp.getStatusLine().getStatusCode() == 415);
	}

	private HttpResponse doPost (String contentType, String accept, String content) 
		throws IOException {
		
		if (authMethod == AuthMethods.FORM) {
			// make sure we have authenticated before the POST call
			TestsBase.formLogin(basicCreds.getUserPrincipal().getName(), basicCreds.getPassword());
		}
		
		// issue the POST call
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds,
				accept, contentType, content, headers);

		if ( resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE) {
			EntityUtils.consume(resp.getEntity());
			String knownIssue = "";
			if ( implName.equalsIgnoreCase("RQM") ) {
				knownIssue = "Reported as Defect: PUT and POST requests with Content-Type: application/xml should be allowed (72920)";
			}
			throw new AssertionError("Provider "+implName+" does not support POST with "+contentType+". "+knownIssue);
		}
		
		return resp;
	}
	
	protected void createValidResourceUsingTemplate(String contentType,
			String accept, String content) throws IOException {
		
		HttpResponse resp = doPost(contentType, accept, content);
		
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
		
		
		HttpResponse resp = createResource(contentType, accept, newContent);
		
		Header location = getRequiredLocationHeader(resp);
		assertTrue("Location("+location+")"+" must not be null", location != null);
		
		// check whether a POST response body is empty (which is allowed)
		Header contentLength = resp.getFirstHeader("Content-Length");
		boolean hasPayLoad = true;
		if ( contentLength != null ) {
			String len = contentLength.getValue().toString();
			if ( len.equalsIgnoreCase("0") ) {
				hasPayLoad = false;
			}
		}
	
		Header eTag = resp.getFirstHeader("ETag");
		Header lastModified = resp.getFirstHeader("Last-Modified");
		int size = headers.length;
		Header[] putHeaders = new Header[size+2];
		
		int i=0;
		for( ; i<size; i++){
			putHeaders[i] = headers[i];
		}

		if ( hasPayLoad ) {
			// then fail, because we expect at least one, if not both
			assertTrue("ETag("+eTag+") or Last-Modified("+lastModified+") must not be null", eTag != null || lastModified != null ) ;
		}
		
		if( eTag != null ) {
			putHeaders[i++] = new BasicHeader("If-Match", eTag.getValue());
		}
		else {
			putHeaders[i++] = new BasicHeader("bogus1", "bogus1");
		}
		
		if( lastModified != null ) {
			putHeaders[i++] = new BasicHeader("If-Unmodified-Since", lastModified.getValue());
		}
		else {
			putHeaders[i++] = new BasicHeader("bogus1", "bogus1");
		}

		String updateUrl = location.getValue();
		
		// We need to replace the rdf:about in the template with the real url
		if ( updateContent.contains("rdf:about=\"\"") ) {
			String replacement = "rdf:about=\"" + updateUrl+ "\"";		
			updateContent = updateContent.replace("rdf:about=\"\"", replacement);
		}

		// Now, go to the url of the new change request and update it.
		// We may need to add something to update URL to match the template
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
		HttpResponse resp = createResource(contentType, accept, content);
		Header location = getRequiredLocationHeader(resp);
		Header eTag = resp.getFirstHeader("ETag");
		Header lastModified = resp.getFirstHeader("Last-Modified");
		
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
		HttpResponse resp = createResource(contentType, accept, createContent);
		Header location = getRequiredLocationHeader(resp);
		Header eTag = resp.getFirstHeader("ETag");
		Header lastModified = resp.getFirstHeader("Last-Modified");

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
		//resp = OSLCUtils.putDataToUrl(location.getValue(), basicCreds, "*/*",
		resp = OSLCUtils.putDataToUrl(location.getValue(), basicCreds, "application/xml",
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

	private HttpResponse createResource(String contentType, String accept,
			String createContent) throws IOException {
				
		HttpResponse resp = doPost(contentType, accept, createContent);
				
		// Assert the response gave a 201 Created
		EntityUtils.consume(resp.getEntity());
		assertEquals(HttpStatus.SC_CREATED, resp.getStatusLine()
				.getStatusCode());
		return resp;
	}

	private Header getRequiredLocationHeader(HttpResponse resp) {
		Header location = resp.getFirstHeader("Location");
		
		// Assert that we were given a Location header pointing to the resource
		assertNotNull(
				"Expected 201-Created to return non-null Location header",
				location);
		
		return location;
	}
	
	protected void updateCreatedResourceWithFailedPrecondition(String contentType,
			String accept, String createContent, String updateContent) throws IOException {
		
		HttpResponse resp = createResource(contentType, accept, createContent);
		
		Header location = getRequiredLocationHeader(resp);
		assertTrue("Location("+location+")"+" must not be null", location != null);
		
		// check whether a POST response body is empty (which is allowed)
		Header contentLength = resp.getFirstHeader("Content-Length");
		boolean hasPayLoad = true;
		if ( contentLength != null ) {
			String len = contentLength.getValue().toString();
			if ( len.equalsIgnoreCase("0") ) {
				hasPayLoad = false;
			}
		}

		Header eTag = resp.getFirstHeader("ETag");
		Header lastModified = resp.getFirstHeader("Last-Modified");

		if ( hasPayLoad ) {
			assertTrue("Either ETag("+eTag+") or Last-Modified("+lastModified+") must not be null", eTag != null || lastModified != null ) ;
		}

		int size = headers.length + 1;		
		
		Header[] putHeaders = new Header[size];
		int i=0;
		for(;i<headers.length;i++){
			putHeaders[i] = headers[i];
		}
		
		if ( !hasPayLoad || eTag != null ) {
			putHeaders[i++] = new BasicHeader("If-Match", "Bogus");
		} else if( lastModified != null ) {
			putHeaders[i++] = new BasicHeader("If-Unmodified-Since", "Tue, 15 Nov 1994 12:45:26 GMT");
		}

		// Now, go to the url of the new change request and update it.
		resp = OSLCUtils.putDataToUrl(location.getValue(), basicCreds, accept,
				contentType, updateContent, putHeaders);
		if (resp != null && resp.getEntity() != null)
			EntityUtils.consume(resp.getEntity());

		if ( resp.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
			String knownIssue = "";
			if ( implName.equalsIgnoreCase("RQM") ) {
				knownIssue = "Reported as Defect: Updating (PUT) with a failed precondition should result in http status 412 instead of server internal error (73374)";
			}
			
			throw new AssertionError("Known issue for provider "+implName+". "+knownIssue);	
		}
		
		assertEquals(HttpStatus.SC_PRECONDITION_FAILED, resp.getStatusLine()
				.getStatusCode());

		// Clean up after the test by attempting to delete the created resource
		resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds, "");

		if (resp != null && resp.getEntity() != null)
			EntityUtils.consume(resp.getEntity());
	}
}
