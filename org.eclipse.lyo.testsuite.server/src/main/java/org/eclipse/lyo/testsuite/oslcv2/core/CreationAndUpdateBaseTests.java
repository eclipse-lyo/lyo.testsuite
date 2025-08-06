/*
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
 *    Samuel Padgett - create and update resources using shapes
 *    Samuel Padgett - add logging
 *    Samuel Padgett - relax status code assertion for updateCreatedResourceWithInvalidContent
 */
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * This class provides JUnit tests for the validation of the OSLCv2 creation and
 * updating of change requests. It uses the template files specified in
 * setup.properties as the entity to be POST or PUT, for creation and updating
 * respectively. If these files are not defined, it uses the creation factory
 * resource shape for creation and instance shape for update.
 *
 * After each test, it attempts to perform a DELETE call on the resource that
 * was presumably created, but this DELETE call is not technically required in
 * the OSLC spec, so the created change request may still exist for some service
 * providers.
 */
@RunWith(Parameterized.class)
public abstract class CreationAndUpdateBaseTests extends TestsBase {
    private Logger logger = Logger.getLogger(CreationAndUpdateBaseTests.class);

    @SuppressWarnings("rawtypes")
    public static String[] getCreateTemplateTypes() throws FileNotFoundException {
        if (rdfXmlCreateTemplate == null) {
            return null;
        }

        Model m = ModelFactory.createDefaultModel();
        m.read(
                new StringReader(rdfXmlCreateTemplate),
                "http://base.url",
                OSLCConstants.JENA_RDF_XML);
        RDFUtils.validateModel(m);
        Property rdfType = m.getProperty(OSLCConstants.RDF_TYPE_PROP);
        List l = m.listStatements(null, rdfType, (RDFNode) null).toList();
        String[] types = new String[l.size()];
        for (int i = 0; i < l.size(); i++) {
            types[i] = ((Statement) l.get(i)).getObject().toString();
        }
        return types;
    }

    public CreationAndUpdateBaseTests(String url) {
        super(url);
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
    public abstract String getUpdateContent(String resourceUri) throws Exception;

    @Test
    public void createResourceWithInvalidContentType() throws Exception {
        // Issue post request using the provided template and an invalid
        // contentType
        Response resp =
                OSLCUtils.postDataToUrl(
                        currentUrl, creds, "*/*", "weird/type", getCreateContent(), headers);
        resp.close();
        assertEquals(415, resp.getStatus());
    }

    private Response doPost(String contentType, String accept, String content)
            throws IOException {

        // issue the POST call
        Response resp =
                OSLCUtils.postDataToUrl(currentUrl, creds, accept, contentType, content, headers);

        if (resp.getStatus() == 415) { // Unsupported Media Type
            resp.close();
            String knownIssue = "";
            if (implName.equalsIgnoreCase("RQM")) {
                knownIssue =
                        "Reported as Defect: PUT and POST requests with Content-Type:"
                                + " application/xml should be allowed (72920)";
            }
            throw new AssertionError(
                    "Provider "
                            + implName
                            + " does not support POST with "
                            + contentType
                            + ". "
                            + knownIssue);
        }

        return resp;
    }

    @Test
    public void createValidResourceUsingTemplate() throws Exception {
        Response resp = doPost(getContentType(), getContentType(), getCreateContent());

        // Assert the response gave a 201 Created
        String responseBody = resp.readEntity(String.class);
        resp.close();
        assertEquals(responseBody, 201, resp.getStatus());
        String location = resp.getHeaderString("Location");
        // Assert that we were given a Location header pointing to the resource,
        // which is not a MUST according to oslc v2, but probably should be
        // present
        // none the less.
        assertFalse(location == null);
        // Attempt to clean up after the test by calling delete on the given
        // url,
        // which is not a MUST according to the oslc cm spec
        resp = OSLCUtils.deleteFromUrl(location, creds, "*/*");
        if (resp.getEntity() != null) {
            resp.close();
        }
    }

    @Test
    public void createResourceWithInvalidContent() throws IOException {
        // Issue post request using valid content type but invalid content
        Response resp =
                OSLCUtils.postDataToUrl(
                        currentUrl,
                        creds,
                        getContentType(),
                        getContentType(),
                        "invalid content",
                        headers);
        resp.close();

        // An error status code should be at least 400.
        assertTrue(
                "Expecting error but received successful status code",
                resp.getStatus() >= 400);

        // Malformed content should not result in a 500 internal server error.
        assertFalse(
                "Server should not return an internal server error",
                resp.getStatus() == 500);
    }

    @Test
    public void createResourceAndUpdateIt() throws Exception {

        Response resp = createResource(getContentType(), getContentType(), getCreateContent());

        String location = getRequiredLocationHeader(resp);
        assertTrue("Location(" + location + ")" + " must not be null", location != null);

        // check whether a POST response body is empty (which is allowed)
        String contentLength = resp.getHeaderString("Content-Length");
        boolean hasPayLoad = true;
        if (contentLength != null) {
            if (contentLength.equalsIgnoreCase("0")) {
                hasPayLoad = false;
            }
        }

        String eTag = resp.getHeaderString("ETag");
        String lastModified = resp.getHeaderString("Last-Modified");
        int size = headers.size();
        Map<String, String> putHeaders = new HashMap<>(size + 2);

        for (Map.Entry<String, String> header : headers.entrySet()) {
            putHeaders.put(header.getKey(), header.getValue());
        }

        if (hasPayLoad) {
            // then fail, because we expect at least one, if not both
            assertTrue(
                    "ETag(" + eTag + ") or Last-Modified(" + lastModified + ") must not be null",
                    eTag != null || lastModified != null);
        }

        if (eTag != null) {
            putHeaders.put("If-Match", eTag);
        } else {
            putHeaders.put("bogus1", "bogus1");
        }

        if (lastModified != null) {
            putHeaders.put("If-Unmodified-Since", lastModified);
        } else {
            putHeaders.put("bogus1", "bogus1");
        }

        String updateUrl = location;
        String updateContent = getUpdateContent(updateUrl);

        // We may need to add something to update URL to match the template
        if (updateParams != null && !updateParams.isEmpty()) updateUrl = updateUrl + updateParams;

        resp =
                OSLCUtils.putDataToUrl(
                        updateUrl,
                        creds,
                        getContentType(),
                        getContentType(),
                        updateContent,
                        putHeaders);
        String responseBody = resp.readEntity(String.class);
        if (resp.getEntity() != null) resp.close();
        // Assert that a proper PUT resulted in a 200 OK
        assertEquals(
                "HTTP Response body: \n " + responseBody,
                Response.Status.OK.getStatusCode(),
                resp.getStatus());

        // Clean up after the test by attempting to delete the created resource
        if (location != null) {
            resp = OSLCUtils.deleteFromUrl(location, creds, "*/*");
            if (resp != null && resp.getEntity() != null) resp.close();
        }
    }

    @Test
    public void updateCreatedResourceWithInvalidContent() throws Exception {
        Response resp = createResource(getContentType(), getContentType(), getCreateContent());
        String location = getRequiredLocationHeader(resp);
        String eTag = resp.getHeaderString("ETag");
        String lastModified = resp.getHeaderString("Last-Modified");

        // Ignore ETag and Last-Modified for these tests
        int size = headers.size();
        if (eTag != null) size++;
        if (lastModified != null) size++;
        Map<String, String> putHeaders = new HashMap<>(size);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            putHeaders.put(header.getKey(), header.getValue());
        }
        if (eTag != null) {
            putHeaders.put("If-Match", eTag);
        }
        if (lastModified != null) {
            putHeaders.put("If-Unmodified-Since", lastModified);
        }

        // Now, go to the url of the new change request and update it.
        resp =
                OSLCUtils.putDataToUrl(
                        location,
                        creds,
                        getContentType(),
                        getContentType(),
                        "invalid content",
                        putHeaders);
        if (resp.getEntity() != null) {
            resp.close();
        }
        // Assert that an invalid PUT resulted in a 4xx status
        final int status = resp.getStatus();
        assertTrue(
                "Expected a 4xx status code, but got %s.%n".formatted(resp.getStatus()),
                status >= 400 && status <= 499);

        // Clean up after the test by attempting to delete the created resource
        if (location != null) resp = OSLCUtils.deleteFromUrl(location, creds, "");

        if (resp != null && resp.getEntity() != null) resp.close();
    }

    @Test
    public void updateCreatedResourceWithBadType() throws Exception {
        Response resp = createResource(getContentType(), getContentType(), getCreateContent());
        String location = getRequiredLocationHeader(resp);
        String eTag = resp.getHeaderString("ETag");
        String lastModified = resp.getHeaderString("Last-Modified");

        // Ignore eTag and Last-Modified for this test
        int size = headers.size();
        if (eTag != null) size++;
        if (lastModified != null) size++;
        Map<String, String> putHeaders = new HashMap<>(size);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            putHeaders.put(header.getKey(), header.getValue());
        }
        if (eTag != null) {
            putHeaders.put("If-Match", eTag);
        }
        if (lastModified != null) {
            putHeaders.put("If-Unmodified-Since", lastModified);
        }

        String updateContent = getUpdateContent(location);

        // Now, go to the url of the new change request and update it.
        // resp = OSLCUtils.putDataToUrl(location.getValue(), creds, "*/*",
        resp =
                OSLCUtils.putDataToUrl(
                        location,
                        creds,
                        "application/xml",
                        "application/invalid",
                        updateContent,
                        putHeaders);
        if (resp != null && resp.getEntity() != null) resp.close();

        assertEquals(415, resp.getStatus()); // Unsupported Media Type

        // Clean up after the test by attempting to delete the created resource
        resp = OSLCUtils.deleteFromUrl(location, creds, "");

        if (resp != null && resp.getEntity() != null) resp.close();
    }

    private Response createResource(String contentType, String accept, String createContent)
            throws IOException {

        Response resp = doPost(contentType, accept, createContent);

        if (logger.isDebugEnabled()) {
            logger.debug("HTTP Response: %s".formatted(resp.getStatus()));
            try (InputStream is = resp.readEntity(InputStream.class)) {
                byte[] content = is.readAllBytes();
                logger.debug(new String(content, StandardCharsets.UTF_8));
            }
        } else {
            resp.close();
        }

        // Assert the response gave a 201 Created
        assertEquals(201, resp.getStatus());
        return resp;
    }

    private String getRequiredLocationHeader(Response resp) {
        String location = resp.getHeaderString("Location");

        // Assert that we were given a Location header pointing to the resource
        assertNotNull("Expected 201-Created to return non-null Location header", location);

        return location;
    }

    @Test
    public void updateCreatedResourceWithFailedPrecondition() throws Exception {

        Response resp = createResource(getContentType(), getContentType(), getCreateContent());

        String location = getRequiredLocationHeader(resp);
        assertTrue("Location(" + location + ")" + " must not be null", location != null);

        // check whether a POST response body is empty (which is allowed)
        String contentLength = resp.getHeaderString("Content-Length");
        boolean hasPayLoad = true;
        if (contentLength != null) {
            if (contentLength.equalsIgnoreCase("0")) {
                hasPayLoad = false;
            }
        }

        String eTag = resp.getHeaderString("ETag");
        String lastModified = resp.getHeaderString("Last-Modified");

        if (hasPayLoad) {
            assertTrue(
                    "Either ETag("
                            + eTag
                            + ") or Last-Modified("
                            + lastModified
                            + ") must not be null",
                    eTag != null || lastModified != null);
        }

        int size = headers.size() + 1;
        Map<String, String> putHeaders = new HashMap<>(size);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            putHeaders.put(header.getKey(), header.getValue());
        }

        if (!hasPayLoad || eTag != null) {
            putHeaders.put("If-Match", "\"Bogus\"");
        } else if (lastModified != null) {
            putHeaders.put("If-Unmodified-Since", "Tue, 15 Nov 1994 12:45:26 GMT");
        }

        String updateContent = getUpdateContent(location);

        // Now, go to the url of the new change request and update it.
        resp =
                OSLCUtils.putDataToUrl(
                        location,
                        creds,
                        getContentType(),
                        getContentType(),
                        updateContent,
                        putHeaders);
        if (resp != null && resp.getEntity() != null) resp.close();

        if (resp.getStatus() == 500) { // Internal Server Error
            String knownIssue = "";
            if (implName.equalsIgnoreCase("RQM")) {
                knownIssue =
                        "Reported as Defect: Updating (PUT) with a failed precondition should"
                            + " result in http status 412 instead of server internal error (73374)";
            }

            throw new AssertionError("Known issue for provider " + implName + ". " + knownIssue);
        }

        assertEquals(412, resp.getStatus()); // Precondition Failed

        // Clean up after the test by attempting to delete the created resource
        resp = OSLCUtils.deleteFromUrl(location, creds, "");

        if (resp != null && resp.getEntity() != null) resp.close();
    }

    @Test
    @Ignore("This is OPTIONAL in OSLC 2.0 (and HTTP)")
    public void updateCreatedResourceWithEmptyPrecondition() throws Exception {

        Response resp = createResource(getContentType(), getContentType(), getCreateContent());

        String location = getRequiredLocationHeader(resp);
        assertTrue("Location(" + location + ")" + " must not be null", location != null);

        // check whether a POST response body is empty (which is allowed)
        String contentLength = resp.getHeaderString("Content-Length");
        boolean hasPayLoad = true;
        if (contentLength != null) {
            if (contentLength.equalsIgnoreCase("0")) {
                hasPayLoad = false;
            }
        }

        String eTag = resp.getHeaderString("ETag");
        String lastModified = resp.getHeaderString("Last-Modified");

        if (hasPayLoad) {
            assertTrue(
                    "Either ETag("
                            + eTag
                            + ") or Last-Modified("
                            + lastModified
                            + ") must not be null",
                    eTag != null || lastModified != null);
        }

        int size = headers.size();
        Map<String, String> putHeaders = new HashMap<>(size);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            putHeaders.put(header.getKey(), header.getValue());
        }

        // Now, go to the url of the new change request and update it.
        String updateContent = getUpdateContent(location);
        resp =
                OSLCUtils.putDataToUrl(
                        location,
                        creds,
                        getContentType(),
                        getContentType(),
                        updateContent,
                        putHeaders);
        if (resp != null && resp.getEntity() != null) resp.close();

        assertEquals(400, resp.getStatus()); // Bad Request

        // Clean up after the test by attempting to delete the created resource
        resp = OSLCUtils.deleteFromUrl(location, creds, "");

        if (resp != null && resp.getEntity() != null) resp.close();
    }

    protected String generateStringValue(Integer maxSize) {
        // Tack on the current time in millis to make the value unique, just in case.
        String s = "Eclispe Lyo Assessment Test " + System.currentTimeMillis();
        if (maxSize != null && s.length() > maxSize) {
            return s.substring(0, maxSize - 1);
        }

        return s;
    }

    protected boolean isPropertyRequired(String occursValue) {
        return OSLCConstants.EXACTLY_ONE.equals(occursValue)
                || OSLCConstants.ONE_OR_MANY.equals(occursValue);
    }
}
