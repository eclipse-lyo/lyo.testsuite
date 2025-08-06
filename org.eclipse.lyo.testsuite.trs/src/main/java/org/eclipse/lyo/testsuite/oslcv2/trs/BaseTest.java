/*
 * Copyright (c) 2013, 2025 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Eclipse Distribution License 1.0
 * which is available at http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

package org.eclipse.lyo.testsuite.oslcv2.trs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.DefaultedHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.lyo.testsuite.server.trsutils.EasySSLClient;
import org.eclipse.lyo.testsuite.server.trsutils.FetchException;
import org.eclipse.lyo.testsuite.server.trsutils.ITRSVocabulary;
import org.eclipse.lyo.testsuite.server.trsutils.InvalidTRSException;
import org.eclipse.lyo.testsuite.server.trsutils.Messages;
import org.eclipse.lyo.testsuite.server.trsutils.TestCore;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BaseTest extends TestCore {
    private static Properties prop = null;
    private static HttpClient httpClient = null;
    private static Resource trsBaseResource = null;
    private static Resource trsResource = null;
    private static HttpContext httpContext = null;

    @BeforeClass
    public static void setupOnce() {
        try {
            prop = getConfigPropertiesInstance();

            // Read the base end point from the TRS Resource
            String trsEndpoint = prop.getProperty("configTrsEndpoint");
            String acceptType = prop.getProperty("acceptType");

            httpClient = new EasySSLClient().getClient();

            httpContext =
                    new DefaultedHttpContext(
                            new BasicHttpContext(), new SyncBasicHttpContext(null));
            trsResource = getResource(trsEndpoint, httpClient, httpContext, acceptType);
            String trsEndpointBase =
                    trsResource.getProperty(ITRSVocabulary.BASE_PROPERTY).getObject().toString();
            trsBaseResource = getResource(trsEndpointBase, httpClient, httpContext, acceptType);
        } catch (FileNotFoundException e) {
            terminateTest(Messages.getServerString("tests.general.config.properties.missing"), e);
        } catch (IOException e) {
            terminateTest(
                    Messages.getServerString("tests.general.config.properties.unreadable"), e);
        } catch (FetchException e) {
            terminateTest(Messages.getServerString("tests.general.trs.fetch.error"), e);
        } catch (Exception e) {
            terminateTest(null, e);
        }
    }

    @Test
    public void testBaseHasCutoffProperty() {
        try {
            if (!trsBaseResource.hasProperty(ITRSVocabulary.CUTOFFEVENT_PROPERTY)) {
                throw new InvalidTRSException(
                        Messages.getServerString(
                                "validators.missing.trs.cutoffevent.property")); //$NON-NLS-1$
            }
        } catch (InvalidTRSException e) {
            e.printStackTrace();
            Assert.fail(e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(
                    MessageFormat.format(
                            Messages.getServerString("tests.general.error"),
                            e.getLocalizedMessage()));
        }
    }

    @Test
    public void testBaseCutoffPropertyIsResource() {
        try {
            if (trsBaseResource.hasProperty(ITRSVocabulary.CUTOFFEVENT_PROPERTY)
                    && !trsBaseResource
                            .getProperty(ITRSVocabulary.CUTOFFEVENT_PROPERTY)
                            .getObject()
                            .isURIResource()) {
                throw new InvalidTRSException(
                        Messages.getServerString(
                                "validators.invalid.trs.cutoffevent.property")); //$NON-NLS-1$
            }
        } catch (InvalidTRSException e) {
            e.printStackTrace();
            Assert.fail(e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(
                    MessageFormat.format(
                            Messages.getServerString("tests.general.error"),
                            e.getLocalizedMessage()));
        }
    }

    @Test
    public void testBaseHasExactlyOneCutoffProperty() {
        try {
            if (trsBaseResource.hasProperty(ITRSVocabulary.CUTOFFEVENT_PROPERTY)
                    && getStatementsForProp(trsBaseResource, ITRSVocabulary.CUTOFFEVENT_PROPERTY)
                                    .toList()
                                    .size()
                            != 1) {
                throw new InvalidTRSException(
                        Messages.getServerString(
                                "validators.invalid.trs.cutoffevent.property")); //$NON-NLS-1$
            }
        } catch (InvalidTRSException e) {
            e.printStackTrace();
            Assert.fail(e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(
                    MessageFormat.format(
                            Messages.getServerString("tests.general.error"),
                            e.getLocalizedMessage()));
        }
    }

    /**
     * Validates that the RDF graph contains a ldp:Page resource with the appropriate properties
     * when the base resource is requested.
     */
    @Test
    public void testBaseHasLdpPage() {
        // Get the model the trsBaseResource belongs to.  This is because the
        // page resource we are interested in should exist outside the base resource
        // but at the same level as the base resource.
        Model baseModel = trsBaseResource.getModel();

        // Query the model for rdf:type ldp:Page
        ResIterator iter =
                baseModel.listResourcesWithProperty(RDF.type, ITRSVocabulary.PAGE_RESOURCE);

        try {
            if (iter == null || iter.hasNext() != true) {
                System.out.println(
                        Messages.getServerString("validators.missing.rdf.type.ldp.page"));
                return;
            }

            Resource page = iter.nextResource();

            Assert.assertTrue(
                    "Exactly one page resource should exist in the graph",
                    page != null && iter.hasNext() == false);

            // This should always pass since we queried the model for this but
            // validate it again just to be certain.
            if (!page.hasProperty(RDF.type, ITRSVocabulary.PAGE_RESOURCE)) {
                throw new InvalidTRSException(
                        Messages.getServerString("validators.missing.rdf.type.ldp.page"));
            }

            // Verify the ldp:Page has a ldp:nextPage property
            if (!page.hasProperty(ITRSVocabulary.NEXT_PAGE_PROPERTY)) {
                throw new InvalidTRSException(
                        Messages.getServerString("validators.missing.ldp.next.page"));
            }

            // Verify the ldp:Page has a ldp:pageOf property that points back to
            // the trsBaseResource aggregate container.
            if (!page.hasProperty(ITRSVocabulary.PAGE_OF_RESOURCE, trsBaseResource)) {
                throw new InvalidTRSException(
                        Messages.getServerString("validators.missing.ldp.page.of"));
            }
        } catch (InvalidTRSException e) {
            e.printStackTrace();
            Assert.fail(e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(
                    MessageFormat.format(
                            Messages.getServerString("tests.general.error"),
                            e.getLocalizedMessage()));
        }
    }

    /** Determine if the base resource contains the rdf:type ldp:Container property. */
    @Test
    public void testBaseHasType() {
        try {
            if (!trsBaseResource.hasProperty(RDF.type, ITRSVocabulary.CONTAINER_RESOURCE)) {
                throw new InvalidTRSException(
                        Messages.getServerString("validators.missing.rdf.type.ldp.container"));
            }
        } catch (InvalidTRSException e) {
            e.printStackTrace();
            Assert.fail(e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(
                    MessageFormat.format(
                            Messages.getServerString("tests.general.error"),
                            e.getLocalizedMessage()));
        }
    }
}
