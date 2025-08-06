/*
 * Copyright (c) 2013, 2025 IBM Corporation and others
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
 *    Joseph Leong, Sujeet Mishra - Initial implementation
 */

package org.eclipse.lyo.testsuite.server.trsutils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Properties;
import javax.ws.rs.HttpMethod;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.jena.rdf.model.Model;
import org.apache.xerces.impl.dv.util.Base64;

/**
 * A class that provides a utility methods to fetch HTTP resources as well as process fetch
 * responses
 */
public class FetchUtil {
    /**
     * Fetches a resource. In case of success, this method returns an instance of the {@link Model}.
     *
     * @param uri resource uri to fetch
     * @param httpClient client used to fetch the resource
     * @param httpContext http context to use for the call
     * @param acceptType value to use in the accept header
     * @throws InterruptedException if the thread is interrupted
     * @throws FetchException if an error occurs while fetching the resource.
     * @throws IOException if an error occurs while updating the retryable error information into
     *     the error handler
     */
    public static Model fetchResource(
            String uri, HttpClient httpClient, HttpContext httpContext, String acceptType)
            throws InterruptedException, FetchException {
        if (uri == null)
            throw new IllegalArgumentException(
                    Messages.getServerString("fetch.util.uri.null")); // $NON-NLS-1$
        if (httpClient == null)
            throw new IllegalArgumentException(
                    Messages.getServerString("fetch.util.httpclient.null")); // $NON-NLS-1$

        Model model = null;

        try {
            new URL(uri); // Make sure URL is valid

            HttpGet get = new HttpGet(uri);

            get.setHeader(HttpConstants.ACCEPT, acceptType);

            // Caches must revalidate with origin server. This is to prevent a cache
            // from serving stale data. We may still get a cached response if the
            // origin server responds to the revalidation with a 304.
            // See Unspecified end-to-end revalidation:
            // http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.4
            get.addHeader(HttpConstants.CACHE_CONTROL, "max-age=0"); // $NON-NLS-1$

            RDFModelResponseHandler handler = new RDFModelResponseHandler(uri);

            // Try to access the uri directly.  If this fails attempt to retry
            // using authentication.
            try {
                model = httpClient.execute(get, handler, httpContext);
            } catch (HttpResponseException e1) {
                if (e1.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                    model =
                            attemptAuthentication(
                                    uri, httpClient, httpContext, model, get, handler);
                }
            }
        } catch (Exception e) {
            String uriLocation =
                    Messages.getServerString("fetch.util.uri.unidentifiable"); // $NON-NLS-1$

            if (uri != null && !uri.isEmpty()) {
                uriLocation = uri;
            }

            throw new FetchException(
                    MessageFormat.format(
                            Messages.getServerString("fetch.util.retrieve.error"), // $NON-NLS-1$
                            uriLocation),
                    e);
        }

        return model;
    }

    /**
     * This method performs authentication based on the config.properties' AuthType setting.
     *
     * @param uri
     * @param httpClient
     * @param httpContext
     * @param model
     * @param get
     * @param handler
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClientProtocolException
     * @throws URISyntaxException
     */
    private static Model attemptAuthentication(
            String uri,
            HttpClient httpClient,
            HttpContext httpContext,
            Model model,
            HttpGet get,
            RDFModelResponseHandler handler)
            throws FileNotFoundException, IOException, ClientProtocolException, URISyntaxException {
        // Check the config.properties to see if the user is overriding
        // the WWW-Authenticate header.
        String override = TestCore.getConfigPropertiesInstance().getProperty("AuthType");
        AuthenticationTypes overrideType = AuthenticationTypes.valueOf(override.toUpperCase());

        switch (overrideType) {
            case OAUTH:
                return perform2LeggedOauth(httpClient, httpContext, get, uri);

            case BASIC:
                return performBasicAuthentication(httpClient, httpContext, get, uri);

            case HEADER:
                Header authTypes[] = handler.getAuthTypes();

                // Determine the authentication type to attempt based on the
                // server's response. Attempt the first type encountered that
                // both the server and the tests support.
                for (Header authType : authTypes) {
                    if (authType.getValue().startsWith("OAuth ")) {
                        return perform2LeggedOauth(httpClient, httpContext, get, uri);
                    } else if (authType.getValue().startsWith("Basic ")) {
                        return performBasicAuthentication(httpClient, httpContext, get, uri);
                    }
                }
        }

        throw new InvalidRDFResourceException(
                Messages.getServerString("fetch.util.authentication.unknown"));
    }

    /**
     * This method uses the username and password specified in config.properties to attempt basic
     * authentication against a resource server.
     *
     * @param httpClient
     * @param httpContext
     * @param get
     * @param uri
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static Model performBasicAuthentication(
            HttpClient httpClient, HttpContext httpContext, HttpGet get, String uri)
            throws FileNotFoundException, IOException {
        // Obtain the username and password from the config.properties file
        Properties prop = TestCore.getConfigPropertiesInstance();
        String username = prop.getProperty("username");
        String password = prop.getProperty("password");

        // Construct the authentication header by using Base64 encoding on the
        // supplied username and password
        String authString = username + ":" + password;
        authString = new String(Base64.encode(authString.getBytes(HttpConstants.DEFAULT_ENCODING)));
        authString = "Basic " + authString;

        get.setHeader("Authorization", authString);
        get.setHeader("OSLC-Core-Version", "2.0");

        Model model = null;

        try {
            model = httpClient.execute(get, new RDFModelResponseHandler(uri), httpContext);
        } catch (Exception e) {
            TestCore.terminateTest(
                    Messages.getServerString("fetch.util.authentication.failure"), e);
        }

        return model;
    }

    /**
     * This method attempts to authenticate with a server using OAuth two legged authentication. A
     * functional user with a consumer key and secret must have already been created on the resource
     * server and specified in the config.properties file.
     *
     * @param httpClient
     * @param httpContext
     * @param get
     * @param uri
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    private static Model perform2LeggedOauth(
            HttpClient httpClient, HttpContext httpContext, HttpGet get, String uri)
            throws ClientProtocolException, IOException, URISyntaxException {
        // Get the necessary OAuth values from the config.properties file
        Properties prop = TestCore.getConfigPropertiesInstance();
        String consumerKey = prop.getProperty("consumerKey");
        String consumerSecret = prop.getProperty("consumerSecret");
        String authorizationTokenURL = prop.getProperty("OAuthURL");
        String oAuthRealm = prop.getProperty("OAuthRealm");

        // Using the information from the config.properties file
        // construct the authentication header to use in our GET request
        OAuthServiceProvider provider = new OAuthServiceProvider(null, authorizationTokenURL, null);
        OAuthConsumer consumer = new OAuthConsumer("", consumerKey, consumerSecret, provider);
        OAuthAccessor accessor = new OAuthAccessor(consumer);
        accessor.accessToken = "";

        Model model = null;

        try {
            OAuthMessage message = accessor.newRequestMessage(HttpMethod.GET, uri, null);
            String authHeader = message.getAuthorizationHeader(oAuthRealm);

            get.setHeader("Authorization", authHeader);
            get.setHeader("OSLC-Core-Version", "2.0");

            model = httpClient.execute(get, new RDFModelResponseHandler(uri), httpContext);
        } catch (OAuthException e) {
            TestCore.terminateTest(
                    Messages.getServerString("fetch.util.authentication.failure"), e);
        }

        return model;
    }

    /**
     * A list of the currently supported authentication types. The user specifies the desired type
     * in the config.properties' AuthType property.
     */
    private enum AuthenticationTypes {
        HEADER,
        OAUTH,
        BASIC
    }
}
