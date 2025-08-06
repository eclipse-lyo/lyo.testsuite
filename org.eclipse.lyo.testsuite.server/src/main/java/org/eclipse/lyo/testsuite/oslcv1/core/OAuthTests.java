/*
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
 *    Matthew Brown
 *    Michael Fiedler
 */
package org.eclipse.lyo.testsuite.oslcv1.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Properties;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;
import jakarta.ws.rs.core.Response;
import org.apache.http.auth.Credentials;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.SetupProperties;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the process by which consumers are able to access resources of an OSLC
 * Provider using OAuth authentication to access the provider's resources.
 *
 * @author Matt Brown
 *
 */
public class OAuthTests {
//
//    private HttpClient c;
//    private OAuthServiceProvider provider;
//    private OAuthConsumer consumer;
//    private String baseUrl;
//    private static Credentials basicCreds;
//    private String postParameters;
//
//    public OAuthTests() throws GeneralSecurityException {
//
//        // Setup the HTTP client to properly handle SSL requests
//        Protocol.registerProtocol(
//                "https", new Protocol("https", SSLProtocolSocketFactory.INSTANCE, 443));
//        c = new HttpClient();
//    }
//
//    @Before
//    public void setup() {
//        Properties setupProps = SetupProperties.setup(null);
//        if (setupProps.getProperty("testBackwardsCompatability") != null
//                && Boolean.parseBoolean(setupProps.getProperty("testBackwardsCompatability"))) {
//            setupProps = SetupProperties.setup(setupProps.getProperty("version1Properties"));
//        }
//        baseUrl = setupProps.getProperty("baseUri");
//        String userId = setupProps.getProperty("userId");
//        String password = setupProps.getProperty("pw");
//        basicCreds = new UsernamePasswordCredentials(userId, password);
//        postParameters = setupProps.getProperty("OAuthAuthorizationParameters");
//        String requestUrl = setupProps.getProperty("OAuthRequestTokenUrl");
//        String authorizeUrl = setupProps.getProperty("OAuthAuthorizationUrl");
//        String accessUrl = setupProps.getProperty("OAuthAccessTokenUrl");
//        // Setup the OAuth provider
//        provider = new OAuthServiceProvider(requestUrl, authorizeUrl, accessUrl);
//        // Setup the OAuth consumer
//        String consumerSecret = setupProps.getProperty("OAuthConsumerSecret");
//        String consumerToken = setupProps.getProperty("OAuthConsumerToken");
//        consumer = new OAuthConsumer("", consumerToken, consumerSecret, provider);
//    }
//
//    @Test
//    public void oAuthRequestTokenProperlyRecieved()
//            throws IOException, OAuthException, URISyntaxException {
//        // Setup the client using our HttpClient that is set up with SSL.
//        OAuthClient client = new OAuthClient(new HttpClient3(new TestHttpClientPool()));
//        OAuthAccessor accessor = new OAuthAccessor(consumer);
//        // Attempt to get a request token from the provider
//        client.getRequestToken(accessor);
//        // Make sure we got the request token
//        assertNotNull(accessor.requestToken);
//        assertFalse(accessor.requestToken.isEmpty());
//    }
//
//    @Test
//    public void oAuthAuthorizationHandled() throws IOException, OAuthException, URISyntaxException {
//        // Setup the client using our HttpClient that is set up with SSL.
//        OAuthClient client = new OAuthClient(new HttpClient3(new TestHttpClientPool()));
//        OAuthAccessor accessor = new OAuthAccessor(consumer);
//        // Get the request token.
//        client.getRequestToken(accessor);
//        // Get a response from the base URL to setup cookies (to prevent form redirection)
//        Response resp =
//                OSLCUtils.getResponseFromUrl(
//                        "",
//                        baseUrl,
//                        basicCreds,
//                        OSLCConstants.CT_DISC_CAT_XML + ", " + OSLCConstants.CT_DISC_DESC_XML);
//        resp.close();
//        // Post authorization using user credentials provided.
//        resp =
//                OSLCUtils.postDataToUrl(
//                        provider.userAuthorizationURL + "?oauth_token=" + accessor.requestToken,
//                        basicCreds,
//                        "",
//                        "application/x-www-form-urlencoded",
//                        postParameters + "&oauth_token=" + accessor.requestToken);
//        resp.close();
//        // Make sure the authorization did not return an error.
//        assertTrue(resp.getStatus() < 400);
//    }
//
//    @Test
//    public void oAuthAccessTokenRecieved() throws OAuthException, IOException, URISyntaxException {
//        OAuthClient client = new OAuthClient(new HttpClient3(new TestHttpClientPool()));
//        OAuthAccessor accessor = new OAuthAccessor(consumer);
//        // Get the request token
//        client.getRequestToken(accessor);
//
//        Response resp =
//                OSLCUtils.getResponseFromUrl(
//                        "",
//                        baseUrl,
//                        basicCreds,
//                        OSLCConstants.CT_DISC_CAT_XML + ", " + OSLCConstants.CT_DISC_DESC_XML);
//        resp.close();
//
//        resp =
//                OSLCUtils.postDataToUrl(
//                        provider.userAuthorizationURL + "?oauth_token=" + accessor.requestToken,
//                        basicCreds,
//                        "",
//                        "application/x-www-form-urlencoded",
//                        postParameters + "&oauth_token=" + accessor.requestToken);
//        resp.close();
//        // Provide authorization by the user
//        assertTrue(resp.getStatus() < 400);
//
//        try {
//            // Trade the request token for an access token.
//            client.getAccessToken(accessor, OAuthMessage.POST, null);
//        } catch (OAuthProblemException e) {
//            assertNull("Could not get access token\n" + e.getMessage(), e);
//        }
//        // Make sure we got an access token that is not empty.
//        assertNotNull(accessor.accessToken);
//        assertFalse(accessor.accessToken.isEmpty());
//    }
//
//    private class TestHttpClientPool implements HttpClientPool {
//        @Override
//        public org.apache.commons.httpclient.HttpClient getHttpClient(URL arg0) {
//            return c;
//        }
//    }
}
