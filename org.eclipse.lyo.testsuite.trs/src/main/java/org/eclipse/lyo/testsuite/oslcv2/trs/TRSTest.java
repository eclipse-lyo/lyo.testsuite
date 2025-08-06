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

public class TRSTest extends TestCore {
    private static Properties prop = null;
    private static HttpClient httpClient = null;
    private static Resource trsResource = null;
    private static HttpContext httpContext = null;

    @BeforeClass
    public static void setupOnce() {
        try {
            prop = getConfigPropertiesInstance();

            String trsEndpoint = prop.getProperty("configTrsEndpoint");
            String acceptType = prop.getProperty("acceptType");

            httpClient = new EasySSLClient().getClient();

            httpContext =
                    new DefaultedHttpContext(
                            new BasicHttpContext(), new SyncBasicHttpContext(null));

            trsResource = getResource(trsEndpoint, httpClient, httpContext, acceptType);
        } catch (FileNotFoundException e) {
            terminateTest(Messages.getServerString("tests.general.config.properties.missing"), e);
        } catch (InterruptedException e) {
            terminateTest(null, e);
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
    public void testTRSHasType() {
        try {
            if (!trsResource.hasProperty(RDF.type, ITRSVocabulary.TRS_RESOURCE)) {
                throw new InvalidTRSException(
                        Messages.getServerString("validators.missing.rdf.type.oslc.trs"));
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
    public void testTRSHasBaseProperty() {
        try {
            if (!trsResource.hasProperty(ITRSVocabulary.BASE_PROPERTY)) {
                throw new InvalidTRSException(
                        Messages.getServerString(
                                "validators.missing.trs.base.property")); //$NON-NLS-1$
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
    public void testTRSBasePropertyIsResource() {
        try {
            if (trsResource.hasProperty(ITRSVocabulary.BASE_PROPERTY)
                    && !trsResource
                            .getProperty(ITRSVocabulary.BASE_PROPERTY)
                            .getObject()
                            .isResource()) {
                throw new InvalidTRSException(
                        Messages.getServerString(
                                "validators.invalid.trs.base.property")); //$NON-NLS-1$
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
    public void testTRSHasExactlyOneBaseProperty() {
        try {
            if (trsResource.hasProperty(ITRSVocabulary.BASE_PROPERTY)
                    && getStatementsForProp(trsResource, ITRSVocabulary.BASE_PROPERTY)
                                    .toList()
                                    .size()
                            != 1) {
                throw new InvalidTRSException(
                        Messages.getServerString(
                                "validators.invalid.trs.base.property")); //$NON-NLS-1$
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
    public void testTRSHasChangeLogProperty() {
        try {
            if (!trsResource.hasProperty(ITRSVocabulary.CHANGELOG_PROPERTY)) {
                throw new InvalidTRSException(
                        Messages.getServerString(
                                "validators.missing.trs.changelog.property")); //$NON-NLS-1$
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
    public void testTRSChangeLogPropertyIsResource() {
        try {
            if (trsResource.hasProperty(ITRSVocabulary.CHANGELOG_PROPERTY)
                    && !trsResource
                            .getProperty(ITRSVocabulary.CHANGELOG_PROPERTY)
                            .getObject()
                            .isResource()) {
                throw new InvalidTRSException(
                        Messages.getServerString(
                                "validators.invalid.trs.changelog.property")); //$NON-NLS-1$
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
    public void testTRSHasExactlyOneChangeLogProperty() {
        try {
            if (trsResource.hasProperty(ITRSVocabulary.CHANGELOG_PROPERTY)
                    && getStatementsForProp(trsResource, ITRSVocabulary.CHANGELOG_PROPERTY)
                                    .toList()
                                    .size()
                            != 1) {
                throw new InvalidTRSException(
                        Messages.getServerString(
                                "validators.invalid.trs.changelog.property")); //$NON-NLS-1$
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
