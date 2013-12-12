/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation.
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
 *    Samuel Padgett - create and update resources using shapes
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashSet;
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.hp.hpl.jena.datatypes.xsd.impl.XMLLiteralType;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

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

	public CreationAndUpdateBaseTests(String url) {
		super(url);
	}

	@Test
	public void createResourceWithInvalidContentType() throws IOException {
		// Issue post request using the provided template and an invalid
		// contentType
		String entity = getEntity(xmlCreateTemplate, OSLCConstants.CT_XML);
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds,
				"*/*", "weird/type", entity, headers);
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
		
		String entity = getEntity(content, contentType);
		HttpResponse resp = doPost(contentType, accept, entity);
		
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
			putHeaders[i++] = new BasicHeader("If-Match", eTag.getValue() );
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

		if (updateContent == null && !OSLCConstants.CT_JSON.equals(contentType)) {
			updateContent = updateResourceFromShape(updateUrl, contentType);
		} else {
			if ( updateContent.contains("rdf:about=\"\"") ) {
				// We need to replace the rdf:about in the template with the real url
				String replacement = "rdf:about=\"" + updateUrl+ "\"";		
				updateContent = updateContent.replace("rdf:about=\"\"", replacement);
			}
			
			// Now, go to the url of the new change request and update it.
			// We may need to add something to update URL to match the template
			if (updateParams != null && !updateParams.isEmpty())
				updateUrl = updateUrl + updateParams;
		}
	
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

		if (updateContent == null && !OSLCConstants.CT_JSON.equals(contentType)) {
			updateContent = updateResourceFromShape(location.getValue(), contentType);
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
	
	/*
	 * Use the template or use a shape to define the post body content depending
	 * on the configuration options.
	 */
	private String getEntity(String template, String contentType) throws IOException {
		if (template == null && !OSLCConstants.CT_JSON.equals(contentType)) {
			// ignore content, generate the content instead
			String shapeUri = getShapeUriForCapability(currentUrl);
			assertNotNull("No shape for creation factory: " + currentUrl, shapeUri);
			template = createResourceFromShape(shapeUri, contentType);
		}
		
		return template;
	}

	private HttpResponse createResource(String contentType, String accept,
			String createContent) throws IOException {

		String entity = getEntity(createContent, contentType);
		HttpResponse resp = doPost(contentType, accept, entity);
				
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
			putHeaders[i++] = new BasicHeader("If-Match", "\"Bogus\"");
		} else if( lastModified != null ) {
			putHeaders[i++] = new BasicHeader("If-Unmodified-Since", "Tue, 15 Nov 1994 12:45:26 GMT");
		}

		if (updateContent == null && !OSLCConstants.CT_JSON.equals(contentType)) {
			updateContent = updateResourceFromShape(location.getValue(), contentType);
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
	
	/**
	 * Change a resource using its instance shape. Will only attempt to change
	 * one property to limit the chance of errors.
	 * 
	 * @param resource
	 *            the resource to modify
	 * 
	 * @throws IOException
	 *             on errors requesting the instance shape
	 */
	protected String updateResourceFromShape(String uri, String contentType) throws IOException {
		Model resourceModel = getModel(uri);
		Resource resource = resourceModel.getResource(uri);
		Property instanceShapeProp = resourceModel.createProperty(OSLCConstants.INSTANCE_SHAPE);
		Statement instanceShapeStatement = resource.getProperty(instanceShapeProp);
		assertNotNull("The resource does not have an instance shape.", instanceShapeStatement);

		String shapeUri = instanceShapeStatement.getResource().getURI();
		Model shapeModel = getModel(shapeUri);

		Property propertyProp = shapeModel.createProperty(OSLCConstants.PROPERTY);
		Property propertyDefinitionProp = shapeModel.createProperty(OSLCConstants.PROPERTY_DEFINITION);
		Resource shape = shapeModel.getResource(shapeUri);
		StmtIterator propertyIterator = shape.listProperties(propertyProp);
		
		// Get the list of properties.
		//List<Statement> propertyStatements = shape.listProperties(propertyProp).toList();
		while (propertyIterator.hasNext()) {
			Resource property = propertyIterator.next().getResource();

			// Skip read-only properties that we can't change.
			if (isPropertyReadOnly(property)) {
				continue;
			}
			
			/*
			 * For now, let's keep things simple and try to find a string value
			 * we can update. This will fail (incorrectly) if the resource does
			 * not have any modifiable strings. We could make this more
			 * sophisticated and look for any type that might be modifiable.
			 */
			if (isStringType(property)) {
				String propertyDefinition = property.getRequiredProperty(propertyDefinitionProp).getResource().getURI();
				Property propertyToChange = resourceModel.createProperty(propertyDefinition);
				resource.removeAll(propertyToChange);
				resource.addLiteral(propertyToChange, generateStringValue(getMaxSize(propertyToChange)));
				
				// Updating one field should be good enough.
				break;
			}
		}
		
		return toString(resourceModel, contentType);
	}

	public String createResourceFromShape(String shapeUri, String contentType) throws IOException {
		Model m = ModelFactory.createDefaultModel();
		createResourceFromShape(m, shapeUri, 1);

		return toString(m, contentType);
	}

	// Max depth is used to detect cycles.
	private final static int MAX_DEPTH = Integer.parseInt(System.getProperty("org.eclipse.lyo.testsuite.oslcv2.createResource.maxDepth", "10"));
	private Resource createResourceFromShape(Model requestModel, String shapeUri, int depth) throws IOException {
		assertTrue("Detected possible circular reference in shape while creating resource.", depth < MAX_DEPTH);

		// Get the shape.
		Model shapeModel = getModel(shapeUri);
		Resource toCreate = requestModel.createResource();
		
		Resource shapeResource = shapeModel.getResource(shapeUri);
		StmtIterator typeIter = shapeResource.listProperties(shapeModel.createProperty(OSLCConstants.DESCRIBES));
		
		// Use the first rdf:type if defined.
		if (typeIter.hasNext()) {
			Resource type = typeIter.next().getResource();
			toCreate.addProperty(RDF.type, type);
		}

		final Property propertyProp = shapeModel.createProperty(OSLCConstants.PROPERTY);

		// Try to create a resource based on the properties in the shape.
		StmtIterator propIter = shapeResource.listProperties(propertyProp);
		while (propIter.hasNext()) {
			Resource nextProperty = propIter.next().getResource();

			// Only try to fill in required properties to minimize the chance of errors.
			if (isPropertyRequired(nextProperty)) {
				fillInProperty(shapeModel, toCreate, nextProperty, depth);
			}
		}
		
		return toCreate;
	}

	private void fillInProperty(Model shapeModel, Resource toCreate, Resource propertyToFill, int depth)
			throws IOException {
		final Property propertyDefinitionProp = shapeModel.createProperty(OSLCConstants.PROPERTY_DEFINITION);
		final Property allowedValueProp = shapeModel.createProperty(OSLCConstants.ALLOWED_VALUE);
		final Property allowedValuesProp = shapeModel.createProperty(OSLCConstants.ALLOWED_VALUES);

		String propertyDefinition = propertyToFill.getRequiredProperty(propertyDefinitionProp).getResource().getURI();
		Property requestProp = toCreate.getModel().createProperty(propertyDefinition);

		/*
		 * Don't attempt to use the default value. Some providers make the
		 * default value something that is not allowed (for instance, Filed
		 * Against: Unassigned in RTC).
		 */
		
//		final Property defaultValueProp = shapeModel.createProperty(OSLCConstants.DEFAULT_VALUE);
//		if (propertyToFill.hasProperty(defaultValueProp)) {
//			RDFNode defaultValue = propertyToFill.getProperty(defaultValueProp).getObject();
//			// Make sure it's not just the empty string.
//			if (defaultValue.isResource() || !"".equals(defaultValue.asLiteral().getLexicalForm())) {
//				toCreate.addProperty(requestProp, defaultValue);
//				return;
//			}
//		}

		// Check for a list of allowed values that we can use.
		if (propertyToFill.hasProperty(allowedValueProp)) {
			RDFNode randomAllowedValue = getAllowedValue(propertyToFill);
			toCreate.addProperty(requestProp, randomAllowedValue);
			return;
		}

		if (propertyToFill.hasProperty(allowedValuesProp)) {
			// The allowed values are not inline. Make another request to get the list.
			String allowedValuesUri = propertyToFill.getProperty(allowedValuesProp).getResource().getURI();
			Model allowedValuesModel = getModel(allowedValuesUri);
			Resource allowedValuesResource = allowedValuesModel.getResource(allowedValuesUri);
			RDFNode randomAllowedValue = getAllowedValue(allowedValuesResource);
			toCreate.addProperty(requestProp, randomAllowedValue);
			return;
		}

		// No allowed values. Fill some some data appropriate to the type.
		fillInPropertyFromValueType(toCreate, propertyToFill, requestProp, depth);
	}
	
	/*
	 * Try to find an acceptable allowed value. Really we should be able to
	 * select any, but some providers give an empty or unassigned value as the
	 * first item in the list. Technically, order is not guaranteed from
	 * listProperties(), but in practice it seems to be preserved. Thus let's
	 * try to avoid the first item if possible to minimize errors.
	 */
	private RDFNode getAllowedValue(Resource r) {
		final Property allowedValueProp = r.getModel().createProperty(OSLCConstants.ALLOWED_VALUE);
		List<Statement> allowedValues = r.listProperties(allowedValueProp).toList();
		if (allowedValues.isEmpty()) {
			return null;
		}
		
		if (allowedValues.size() == 1) {
			return allowedValues.get(0).getObject();
		}

		return allowedValues.get(1).getObject();
	}

	/*
	 * Attempt to add a value for this property using its value type.
	 */
	private void fillInPropertyFromValueType(Resource toCreate, Resource propertyResource, Property requestProp, int depth) throws IOException {
		Model requestModel = toCreate.getModel();
		Model shapeModel = propertyResource.getModel();
		final Property valueTypeProp = shapeModel.createProperty(OSLCConstants.VALUE_TYPE);

		if (propertyResource.hasProperty(valueTypeProp)) {
			final Property rangeProp = shapeModel.createProperty(OSLCConstants.RANGE);
			final Property valueShapeProp = shapeModel.createProperty(OSLCConstants.VALUE_SHAPE_PROP);
			HashSet<String> valueTypes = new HashSet<String>();
			StmtIterator valueTypeIter = propertyResource.listProperties(valueTypeProp);
			while (valueTypeIter.hasNext()) {
				String typeUri = valueTypeIter.next().getResource().getURI();
				valueTypes.add(typeUri);
			}

			/*
			 * Look at each type. Try to fill in something reasonable.
			 */
			if (valueTypes.contains(OSLCConstants.STRING_TYPE)) {
				String string = generateStringValue(getMaxSize(propertyResource));
				toCreate.addProperty(requestProp, string);
			} else if (valueTypes.contains(OSLCConstants.XML_LITERAL_TYPE)) {
				String string = generateStringValue(getMaxSize(propertyResource));
				Literal literal = requestModel.createTypedLiteral(string, XMLLiteralType.theXMLLiteralType);
				toCreate.addLiteral(requestProp, literal);
			} else if (valueTypes.contains(OSLCConstants.BOOLEAN_TYPE)) {
				toCreate.addLiteral(requestProp, true);
			} else if (valueTypes.contains(OSLCConstants.INTEGER_TYPE)) {
				toCreate.addLiteral(requestProp, 1);
			} else if (valueTypes.contains(OSLCConstants.DOUBLE_TYPE)) {
				toCreate.addLiteral(requestProp, 1.0d);
			} else if (valueTypes.contains(OSLCConstants.FLOAT_TYPE)) {
				toCreate.addLiteral(requestProp, 1.0f);
			} else if (valueTypes.contains(OSLCConstants.DECIMAL_TYPE)) {
				Literal literal = requestModel.createTypedLiteral(1, OSLCConstants.DECIMAL_TYPE);
				toCreate.addLiteral(requestProp, literal);
			} else if (valueTypes.contains(OSLCConstants.DATE_TIME_TYPE)) {
				toCreate.addLiteral(requestProp, requestModel.createTypedLiteral(Calendar.getInstance()));
			} else {
				// It appears to be a resource.
				Statement valueShapeStatement = propertyResource.getProperty(valueShapeProp);
				if (valueShapeStatement == null) {
					// We have no shape, so this will likely fail. We can try, though.
					// Create an empty resource. Add an rdf:type if the property has a range.
					Resource valueResource = requestModel.createResource();
					StmtIterator rangeIter = propertyResource.listProperties(rangeProp);
					if (rangeIter.hasNext()) {
						valueResource.addProperty(RDF.type, rangeIter.next().getResource());
					}
					toCreate.addProperty(requestProp, valueResource);
				} else {
					Resource nested = createResourceFromShape(requestModel, valueShapeStatement.getResource().getURI(), depth + 1);
					toCreate.addProperty(requestProp, nested);
				}
			}
		}
		else {
			// We have no hints. Try to set a string value. This may fail.
			String string = generateStringValue(getMaxSize(propertyResource));
			toCreate.addProperty(requestProp, string);
		}
	}

	private String toString(Model model, String contentType) {
	    String lang = (OSLCConstants.CT_XML.equals(contentType)) ? "RDF/XML-ABBREV" : "RDF/XML";
		StringWriter writer = new StringWriter();
		model.write(writer, lang, "");
		
		return writer.toString();
    }
	
	private String generateStringValue(Integer maxSize) {
		// Tack on the current time in millis to make the value unique, just in case.
		String s = "Eclispe Lyo Assessment Test " + System.currentTimeMillis();
		if (maxSize != null && s.length() > maxSize) {
			return s.substring(0, maxSize - 1);
		}
		
		return s;
	}
	
	/*
	 * Is this property from a resource shape required?
	 */
	private boolean isPropertyRequired(Resource property) {
		Statement statement = property.getRequiredProperty(property.getModel().createProperty(OSLCConstants.OCCURS));
		String occursValue = statement.getResource().getURI();
	
		return OSLCConstants.EXACTLY_ONE.equals(occursValue) || OSLCConstants.ONE_OR_MANY.equals(occursValue);
	}
	
	/*
	 * Is this property from a resource shape read only?
	 */
	private boolean isPropertyReadOnly(Resource property) {
		Statement statement = property.getProperty(property.getModel().createProperty(OSLCConstants.READ_ONLY));
		if (statement == null) {
			return false;
		}
		
		return statement.getBoolean();
	}
	
	/*
	 * Is this property from a resource shape a string?
	 */
	private boolean isStringType(Resource property) {
		Property valueTypeProp = property.getModel().getProperty(OSLCConstants.VALUE_TYPE);
		if (!property.hasProperty(valueTypeProp)) {
			// We don't know, but assume it's not.
			return false;
		}
		
		StmtIterator iter = property.listProperties(valueTypeProp);
		while (iter.hasNext()) {
			String valueType = iter.next().getResource().getURI();
			if (OSLCConstants.STRING_TYPE.equals(valueType)) {
				return true;
			}
		}
		
		return false;
	}
	
	/*
	 * Get the max size for this property if defined. Return null otherwise.
	 */
	private Integer getMaxSize(Resource property) {
		Property maxSizeProp = property.getModel().createProperty(OSLCConstants.MAX_SIZE_PROP);
		Statement maxSize = property.getProperty(maxSizeProp);
		if (maxSize == null) {
			return null;
		}
		
		return maxSize.getInt();
	}

	private Model getModel(String uri) throws IOException {
	    HttpResponse resp = OSLCUtils.getResponseFromUrl(uri, null, basicCreds, OSLCConstants.CT_RDF, headers);
		Model model = ModelFactory.createDefaultModel();
		model.read(resp.getEntity().getContent(), uri, OSLCConstants.JENA_RDF_XML);
		RDFUtils.validateModel(model);
		
		return model;
    }
}
