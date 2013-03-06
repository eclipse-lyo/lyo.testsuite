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
import java.math.BigInteger;
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

import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class ChangeLogTest extends TestCore{
	private static Properties prop = null;
	private static HttpClient httpClient = null;
	private static Resource trsResource = null;
	private static HttpContext httpContext = null;

	@BeforeClass
	public static void setupOnce() {
		try {
			httpClient = new DefaultHttpClient();
			prop = getConfigPropertiesInstance();
			
			String trsEndpoint = prop.getProperty("configTrsEndpoint");
				
			httpContext = 
					new DefaultedHttpContext(new BasicHttpContext(), new SyncBasicHttpContext(null));
				
			trsResource = getResource(trsEndpoint, httpClient, httpContext);	
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
	public void testChangeLogHasChangesProperty() {
		try {
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil)) {
				if (!changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY)) {
					throw new InvalidTRSException(
						Messages.getServerString("validators.missing.trs.changes.property")); //$NON-NLS-1$
				}
				
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
	public void testChangeLogHasExactlyOneChangesProperty() {
		try {
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil)) {
				if (changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY) && getStatementsForProp(changeLogResource,ITRSVocabulary.CHANGES_PROPERTY).toList().size()!=1) {
					throw new InvalidTRSException(
						Messages.getServerString("validators.invalid.trs.changes.property")); //$NON-NLS-1$
				}
				
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
	public void testChangeLogIsList() {
		try {
			
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil)
						&& changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY)) 
			{				
				if (!isList(changeLogResource.getProperty(ITRSVocabulary.CHANGES_PROPERTY).getObject())) {
					throw new InvalidTRSException(
							Messages.getServerString("validators.invalid.trs.changes.property")); //$NON-NLS-1$
				}
				
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
	public void testChangeLogEventIsURIResource() {
		try {
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil) 
						&& changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY)) 
			{
				RDFList changeEvents =
						changeLogResource.getPropertyResourceValue(ITRSVocabulary.CHANGES_PROPERTY).as(RDFList.class);
				
				for (RDFNode changeNode : changeEvents.asJavaList()) {
					if (!changeNode.isURIResource()) {
						throw new InvalidTRSException(
							Messages.getServerString("validators.missing.uri.change.event")); //$NON-NLS-1$
					}
				}
				
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
	public void testChangeLogEventType() {
		try {
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil)
						&& changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY)) 
			{
				RDFList changeEvents =
						changeLogResource.getPropertyResourceValue(ITRSVocabulary.CHANGES_PROPERTY).as(RDFList.class);
				
				for (RDFNode changeNode : changeEvents.asJavaList()) {
					Resource changeEvent = changeNode.asResource();
					
					if (RDF.nil.getURI().equals(changeEvent.getURI()))
						break;
					
					boolean hasChangeType = changeEvent.hasProperty(RDF.type, ITRSVocabulary.CREATION_RESOURCE)
												|| changeEvent.hasProperty(RDF.type, ITRSVocabulary.DELETION_RESOURCE)
												|| changeEvent.hasProperty(RDF.type, ITRSVocabulary.MODIFICATION_RESOURCE);
					
					if (!hasChangeType) {
						throw new InvalidTRSException(
								Messages.getServerString("validators.missing.type.change.event")); //$NON-NLS-1$
					}
				}
				
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
	public void testChangeLogEventHasExactlyOneEventType() {
		try {
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil)
						&& changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY)) 
			{
				RDFList changeEvents =
						changeLogResource.getPropertyResourceValue(ITRSVocabulary.CHANGES_PROPERTY).as(RDFList.class);
				
				for (RDFNode changeNode : changeEvents.asJavaList()) {
					Resource changeEvent = changeNode.asResource();
					
					if (RDF.nil.getURI().equals(changeEvent.getURI()))
						break;
					int count=0;
					if (changeEvent.hasProperty(RDF.type,
							ITRSVocabulary.CREATION_RESOURCE)) {
						count++;
					}
					if (changeEvent.hasProperty(RDF.type,
							ITRSVocabulary.DELETION_RESOURCE)) {
						count++;
					}
					if (changeEvent.hasProperty(RDF.type,
							ITRSVocabulary.MODIFICATION_RESOURCE)) {
						count++;
					}
					
					if (count!=1) {
						throw new InvalidTRSException(
								Messages.getServerString("validators.invalid.type.change.event")); //$NON-NLS-1$
					}
				}
				
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
	public void testChangeLogEventHasChangedProperty() {
		try {
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil)
						&& changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY)) 
			{
				RDFList changeEvents =
						changeLogResource.getPropertyResourceValue(ITRSVocabulary.CHANGES_PROPERTY).as(RDFList.class);
				
				for (RDFNode changeNode : changeEvents.asJavaList()) {
					Resource changeEvent = changeNode.asResource();
					
					if (RDF.nil.getURI().equals(changeEvent.getURI()))
						break;
					
					if (!changeEvent.hasProperty(ITRSVocabulary.CHANGED_PROPERTY)) {
						throw new InvalidTRSException(
								Messages.getServerString("validators.missing.trs.changed.property")); //$NON-NLS-1$
					}
				}
				
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
	public void testChangeLogEventHasExactlyOneChangedProperty() {
		try {
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil)
						&& changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY)) 
			{
				RDFList changeEvents =
						changeLogResource.getPropertyResourceValue(ITRSVocabulary.CHANGES_PROPERTY).as(RDFList.class);
				
				for (RDFNode changeNode : changeEvents.asJavaList()) {
					Resource changeEvent = changeNode.asResource();
					
					if (RDF.nil.getURI().equals(changeEvent.getURI()))
						break;
					
					if (changeEvent.hasProperty(ITRSVocabulary.CHANGED_PROPERTY) && getStatementsForProp(changeEvent,ITRSVocabulary.CHANGED_PROPERTY).toList().size()!=1) {
						throw new InvalidTRSException(
								Messages.getServerString("validators.invalid.trs.changed.property")); //$NON-NLS-1$
					}
				}
				
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
	public void testChangeLogEventChangedPropertyIsURIResource() {
		try {
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil)
						&& changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY)) 
			{
				RDFList changeEvents =
						changeLogResource.getPropertyResourceValue(ITRSVocabulary.CHANGES_PROPERTY).as(RDFList.class);
				
				for (RDFNode changeNode : changeEvents.asJavaList()) {
					Resource changeEvent = changeNode.asResource();
					
					if (RDF.nil.getURI().equals(changeEvent.getURI()))
						break;
					
					if (changeEvent.hasProperty(ITRSVocabulary.CHANGED_PROPERTY)
							&& !changeEvent.getProperty(ITRSVocabulary.CHANGED_PROPERTY).getObject().isURIResource()) {
						throw new InvalidTRSException(
								Messages.getServerString("validators.invalid.trs.changed.property")); //$NON-NLS-1$
					}
				}
				
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
	public void testChangeLogEventHasOrderProperty() {
		try {
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil)
						&& changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY)) 
			{
				RDFList changeEvents =
						changeLogResource.getPropertyResourceValue(ITRSVocabulary.CHANGES_PROPERTY).as(RDFList.class);
				
				for (RDFNode changeNode : changeEvents.asJavaList()) {
					Resource changeEvent = changeNode.asResource();
					
					if (RDF.nil.getURI().equals(changeEvent.getURI()))
						break;
					
					if (!changeEvent.hasProperty(ITRSVocabulary.ORDER_PROPERTY)) {
						throw new InvalidTRSException(
								Messages.getServerString("validators.missing.trs.order.property")); //$NON-NLS-1$
					}
				}
			
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
	public void testChangeLogEventHasExactlyOneOrderProperty() {
		try {
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil)
						&& changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY)) 
			{
				RDFList changeEvents =
						changeLogResource.getPropertyResourceValue(ITRSVocabulary.CHANGES_PROPERTY).as(RDFList.class);
				
				for (RDFNode changeNode : changeEvents.asJavaList()) {
					Resource changeEvent = changeNode.asResource();
					
					if (RDF.nil.getURI().equals(changeEvent.getURI()))
						break;
					
					if (changeEvent.hasProperty(ITRSVocabulary.ORDER_PROPERTY) && getStatementsForProp(changeEvent,ITRSVocabulary.ORDER_PROPERTY).toList().size()!=1) {
						throw new InvalidTRSException(
								Messages.getServerString("validators.invalid.trs.order.property")); //$NON-NLS-1$
					}
				}
			
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
	public void testChangeLogEventOrderPropertyIsPositiveNumber() {
		try {
			Resource changeLogResource =
					trsResource.getPropertyResourceValue(ITRSVocabulary.CHANGELOG_PROPERTY);
				
			if (changeLogResource != null && !changeLogResource.equals(RDF.nil)
						&& changeLogResource.hasProperty(ITRSVocabulary.CHANGES_PROPERTY))
			{
				RDFList changeEvents =
						changeLogResource.getPropertyResourceValue(ITRSVocabulary.CHANGES_PROPERTY).as(RDFList.class);
				
				for (RDFNode changeNode : changeEvents.asJavaList()) {
					Resource changeEvent = changeNode.asResource();
					
					if (RDF.nil.getURI().equals(changeEvent.getURI()))
						break;
					
					try {
						if(changeEvent.hasProperty(ITRSVocabulary.ORDER_PROPERTY)) {
							if(new BigInteger(changeEvent.getProperty(ITRSVocabulary.ORDER_PROPERTY).getObject().asLiteral().getString()).intValue() < 0)
							    throw new Exception();
							
						}
					} catch (Throwable t) {
						throw new InvalidTRSException(
								Messages.getServerString("validators.invalid.trs.order.property"), t); //$NON-NLS-1$
					}
				}
				
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
	
	private static boolean isList(RDFNode node) {
		return node == null ? false : node.asResource().equals(RDF.nil)
			|| node.asResource().hasProperty(RDF.first) || node.asResource().hasProperty(RDF.rest)
			|| node.asResource().hasProperty(RDF.type, RDF.List);
	}
	
	
	
}
