/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
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
 *******************************************************************************/

package org.eclipse.lyo.testsuite.oslcv2.trs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.DefaultedHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;
import org.eclipse.lyo.testsuite.server.trsutils.FetchException;
import org.eclipse.lyo.testsuite.server.trsutils.ITRSVocabulary;
import org.eclipse.lyo.testsuite.server.trsutils.InvalidTRSException;
import org.eclipse.lyo.testsuite.server.trsutils.Messages;
import org.eclipse.lyo.testsuite.server.trsutils.TestCore;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Resource;

public class BaseTest extends TestCore{
	private static Properties prop = null;
	private static HttpClient httpClient = null;
	private static Resource trsBaseResource = null;
	private static Resource trsResource = null;
	private static HttpContext httpContext = null;

	@BeforeClass
	public static void setupOnce() {
		try {
			httpClient = new DefaultHttpClient();
			prop = getConfigPropertiesInstance();
			
			//Read the base end point from the TRS Resource
			String trsEndpoint = prop.getProperty("configTrsEndpoint");
			httpContext = 
					new DefaultedHttpContext(new BasicHttpContext(), new SyncBasicHttpContext(null));
			trsResource = getResource(trsEndpoint, httpClient, httpContext);
			String 	trsEndpointBase =trsResource.getProperty(ITRSVocabulary.BASE_PROPERTY).getObject().toString();
			trsBaseResource = getResource(trsEndpointBase, httpClient, httpContext);	
		} catch (FileNotFoundException e) {
			terminateTest(Messages.getServerString("tests.general.config.properties.missing"), e);
		} catch (InterruptedException e) {
			terminateTest(null, e);
		} catch (IOException e) {
			terminateTest(Messages.getServerString("tests.general.config.properties.unreadable"), e);
		} catch (FetchException e) {
			terminateTest(Messages.getServerString("tests.general.trs.fetch.error"), e);
		}
	}

	@Test
	public void testBaseHasCutoffProperty() {
		try {			
			if (!trsBaseResource.hasProperty(ITRSVocabulary.CUTOFFEVENT_PROPERTY)) {
				throw new InvalidTRSException(
					Messages.getServerString("validators.missing.trs.cutoffevent.property")); //$NON-NLS-1$
			}
		} catch (InvalidTRSException e) {
			e.printStackTrace();
			Assert.fail(e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(MessageFormat.format(
					Messages.getServerString("tests.general.error"),
					e.getLocalizedMessage()));
		} 
	}
	
	@Test
	public void testBaseCutoffPropertyIsResource() {
		try {			
			if (trsBaseResource.hasProperty(ITRSVocabulary.CUTOFFEVENT_PROPERTY) 
					&& !trsBaseResource.getProperty(ITRSVocabulary.CUTOFFEVENT_PROPERTY).getObject().isURIResource()) {
				throw new InvalidTRSException(
					Messages.getServerString("validators.invalid.trs.cutoffevent.property")); //$NON-NLS-1$
			}
		} catch (InvalidTRSException e) {
			e.printStackTrace();
			Assert.fail(e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(MessageFormat.format(
					Messages.getServerString("tests.general.error"),
					e.getLocalizedMessage()));
		} 
	}
	
	@Test
	public void testBaseHasExactlyOneCutoffProperty() {
		try {
			if (trsBaseResource.hasProperty(ITRSVocabulary.CUTOFFEVENT_PROPERTY) && getStatementsForProp(trsBaseResource,ITRSVocabulary.CUTOFFEVENT_PROPERTY).toList().size()!=1) {
				throw new InvalidTRSException(
					Messages.getServerString("validators.invalid.trs.cutoffevent.property")); //$NON-NLS-1$
			}	
		} catch (InvalidTRSException e) {
			e.printStackTrace();
			Assert.fail(e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(MessageFormat.format(
					Messages.getServerString("tests.general.error"),
					e.getLocalizedMessage()));
		} 
	}

}
