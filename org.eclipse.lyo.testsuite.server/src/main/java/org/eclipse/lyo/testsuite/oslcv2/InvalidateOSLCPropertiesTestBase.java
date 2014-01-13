package org.eclipse.lyo.testsuite.oslcv2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.RDFUtils;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;

public abstract class InvalidateOSLCPropertiesTestBase extends TestsBase {

	@SuppressWarnings("rawtypes")
	public static String[] getCreateTemplateTypes() throws FileNotFoundException {
		if (rdfXmlUpdateTemplate == null) {
			return null;
		}

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
	
	public InvalidateOSLCPropertiesTestBase(String thisUrl) {
		super(thisUrl);
	}

	@Test
	public void onePropertyNotSupported() throws Exception {
		queryInvalidOSLCProperties("oslc:mutable");
	}

	@Test
	public void existingPropertyWithInvalidPerfix() throws Exception {
		queryInvalidOSLCProperties("oslb:serviceProvider");
	}

	@Test
	public void multiPropertyNotSupported() throws Exception {
		queryInvalidOSLCProperties("oslc:goat,oslc:mountain");
	}

	@Test
	public void validPropertyAndInvalidProperty() throws Exception {
		queryInvalidOSLCProperties("oslc:serviceProvider,oslc:mutable");
	}

	@Test
	public void directQueryWithFoafProperty() throws Exception {
		queryInvalidOSLCProperties("foaf:givenName");
	}

	@Test
	public void queryWithoutPrefex() throws Exception {
		queryInvalidOSLCProperties("instanceShape");
	}

	@Test
	public void queryWithStrangeCharacter() throws Exception {
		queryInvalidOSLCProperties("*$");
	}

	@Test
	public void queryWithWrongFoafProperty() throws Exception {
		queryInvalidOSLCProperties("dcterms:title,dcterms:creator{foaf:givenName,foaf:familyMame}");
	}

	@Test
	public void putUpperLevelPropertyInWrongplace() throws Exception {
		queryInvalidOSLCProperties("dcterms:creator{dcterms:title}");
	}

	@Test
	public void propertyNotSupporyFoafProperty() throws Exception {
		queryInvalidOSLCProperties("oslc:shortTitle{foaf:givenName,foaf:familyName}");
	}
	
	protected void queryInvalidOSLCProperties(String query) throws Exception {
		HttpResponse resp = createResource(getContentType(), getContentType(),
				getCreateContent());

		Header location = getRequiredLocationHeader(resp);
		assertTrue("Location(" + location + ")" + " must not be null",
				location != null);

		// check whether a POST response body is empty (which is allowed)
		Header contentLength = resp.getFirstHeader("Content-Length");
		boolean hasPayLoad = true;
		if (contentLength != null) {
			String len = contentLength.getValue().toString();
			if (len.equalsIgnoreCase("0")) {
				hasPayLoad = false;
			}
		}

		Header eTag = resp.getFirstHeader("ETag");
		Header lastModified = resp.getFirstHeader("Last-Modified");
		int size = headers.length;
		Header[] putHeaders = new Header[size + 2];

		int i = 0;
		for (; i < size; i++) {
			putHeaders[i] = headers[i];
		}

		if (hasPayLoad) {
			// then fail, because we expect at least one, if not both
			assertTrue("ETag(" + eTag + ") or Last-Modified(" + lastModified
					+ ") must not be null", eTag != null
					|| lastModified != null);
		}

		if (eTag != null) {
			putHeaders[i++] = new BasicHeader("If-Match", eTag.getValue());
		} else {
			putHeaders[i++] = new BasicHeader("bogus1", "bogus1");
		}

		if (lastModified != null) {
			putHeaders[i++] = new BasicHeader("If-Unmodified-Since",
					lastModified.getValue());
		} else {
			putHeaders[i++] = new BasicHeader("bogus1", "bogus1");
		}

		String updateUrl = location.getValue();
		String updateContent = getUpdateContent(updateUrl);

		// We may need to add something to update URL to match the template
		if (query != null && !query.isEmpty())
			updateUrl = updateUrl + "?oslc.properties=" + URLEncoder.encode(query, "UTF-8");

		resp = OSLCUtils.putDataToUrl(updateUrl, basicCreds, getContentType(),
				getContentType(), updateContent, putHeaders);

		if (resp.getEntity() != null)
			EntityUtils.consume(resp.getEntity());
		
		// Assert that an invalidate oslc.property should get status code 409
		assertEquals(HttpStatus.SC_CONFLICT, resp.getStatusLine().getStatusCode());

		// Clean up after the test by attempting to delete the created resource
		if (location != null) {
			resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds,
					"*/*");
			if (resp != null && resp.getEntity() != null)
				EntityUtils.consume(resp.getEntity());
		}
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

	private HttpResponse doPost(String contentType, String accept,
			String content) throws IOException {

		if (authMethod == AuthMethods.FORM) {
			// make sure we have authenticated before the POST call
			TestsBase.formLogin(basicCreds.getUserPrincipal().getName(),
					basicCreds.getPassword());
		}

		// issue the POST call
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds,
				accept, contentType, content, headers);

		if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE) {
			EntityUtils.consume(resp.getEntity());
			String knownIssue = "";
			if (implName.equalsIgnoreCase("RQM")) {
				knownIssue = "Reported as Defect: PUT and POST requests with Content-Type: application/xml should be allowed (72920)";
			}
			throw new AssertionError("Provider " + implName
					+ " does not support POST with " + contentType + ". "
					+ knownIssue);
		}

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

	/**
	 * Gets the content type for these tests. The type will be used for both the
	 * Accept header and the Content-Type header in POST and PUT requests.
	 * 
	 * @return the content type
	 */
	public abstract String getContentType();

	/**
	 * Gets the valid post content for creating resources. Must be in the
	 * content type returned by
	 * {@link CreationAndUpdateBaseTests#getContentType()}.
	 * 
	 * @return the request body to POST to the creation factory
	 * @throws Exception
	 *             on errors
	 */
	public abstract String getCreateContent() throws Exception;

	/**
	 * Gets the content to update a resource with (using a PUT request).
	 * 
	 * @param resourceUri
	 *            the URI of the resource to modify
	 * @return valid request body content for a PUT request using
	 *         {@link CreationAndUpdateBaseTests#getContentType()}
	 * @throws Exception
	 *             on errors
	 */
	public abstract String getUpdateContent(String resourceUri)
			throws Exception;
	
	protected String generateStringValue(Integer maxSize) {
		// Tack on the current time in millis to make the value unique, just in case.
		String s = "Eclispe Lyo Assessment Test " + System.currentTimeMillis();
		if (maxSize != null && s.length() > maxSize) {
			return s.substring(0, maxSize - 1);
		}
		
		return s;
	}

	protected boolean isPropertyRequired(String occursValue) {
		return OSLCConstants.EXACTLY_ONE.equals(occursValue) || OSLCConstants.ONE_OR_MANY.equals(occursValue);
	}
}
