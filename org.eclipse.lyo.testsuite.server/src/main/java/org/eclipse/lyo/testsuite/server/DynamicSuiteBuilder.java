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
 *    Yuhong Yin
 *    Tim Eck II     - asset management test cases
 *******************************************************************************/
package org.eclipse.lyo.testsuite.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.server.oslcv1tests.CreationAndUpdateTests;
import org.eclipse.lyo.testsuite.server.oslcv1tests.QueryTests;
import org.eclipse.lyo.testsuite.server.oslcv1tests.ServiceDescriptionTests;
import org.eclipse.lyo.testsuite.server.oslcv1tests.ServiceProviderCatalogTests;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.SetupProperties;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class determines which version of OSLC the provider is using,
 * and builds the correct set of OSLC Provider tests accordingly.
 * 
 * Also sets up forms authentication if necessary (for things like RTC).
 * 
 */
@RunWith(OslcTestSuite.class)
public class DynamicSuiteBuilder
{	
	public static Class<?>[] suite() throws IOException
	{
		final Logger log = LoggerFactory.getLogger(DynamicSuiteBuilder.class);
		Properties setupProps = SetupProperties.setup(null);
		
		TestsBase.staticSetup();
		
		//If we also want to run v1 tests (assuming this is a v2 provider)
		String testVersions = setupProps.getProperty("testVersions");

		
		ArrayList<Class<?>> testsToRun = new ArrayList<Class<?>>();
		//Determine if this is a v1 or v2 provider
		if (   OSLCConstants.OSLC_CM_V2.equals(testVersions) 
			|| OSLCConstants.OSLC_V2.equals(testVersions) 
			|| OSLCConstants.OSLC_RM_V2.equals(testVersions) 
			|| OSLCConstants.OSLC_QM_V2.equals(testVersions) 
			|| testVersions.equals("both") 
			|| OSLCConstants.OSLC_AM_V2.equals(testVersions)
			|| OSLCConstants.OSLC_ASSET_V2.equals(testVersions)) {
			log.info("Setting up to test Core v2 features");
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.ServiceProviderCatalogRdfXmlTests.class);
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.ServiceProviderRdfXmlTests.class);
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.FetchResourceTests.class);
			
			if (OSLCConstants.OSLC_CM_V2.equals(testVersions) || 
				OSLCConstants.OSLC_QM_V2.equals(testVersions) || 
				OSLCConstants.OSLC_ASSET_V2.equals(testVersions)) {
				testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.ServiceProviderCatalogXmlTests.class);
				testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.ServiceProviderXmlTests.class);
				testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.CreationAndUpdateXmlTests.class);
				testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.SimplifiedQueryXmlTests.class);
				testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.SimplifiedQueryAtomTests.class);		
				
				if (OSLCConstants.OSLC_CM_V2.equals(testVersions)) {
					log.info("Setting up to test CM v2 features");
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.CreationAndUpdateJsonTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.ChangeRequestXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.ChangeRequestRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.ChangeRequestJsonTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.SimplifiedQueryJsonTests.class);
				} else if(OSLCConstants.OSLC_ASSET_V2.equals(testVersions)) {
					log.info("Setting up to test Asset v2 features");
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.CreateAssetJsonTest.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.CreateAssetRdfXmlTest.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.CreateAssetXmlTest.class);
					
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.GetAndUpdateRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.GetAndUpdateXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.GetAndUpdateJsonTests.class);
					
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.UsageCaseXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.UsageCaseRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.UsageCaseJsonTests.class);
					
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.SimplifiedQueryJsonTests.class);
				}
				
				if (OSLCConstants.OSLC_QM_V2.equals(testVersions)) {
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestPlanXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestCaseXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestScriptXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestExecutionRecordXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestResultXmlTests.class);
					
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestPlanRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestCaseRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestScriptRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestExecutionRecordRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestResultRdfXmlTests.class);
				}
			}
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.CreationAndUpdateRdfXmlTests.class);
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.SimplifiedQueryRdfXmlTests.class);
			//testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.OAuthTests.class);
		}
		if (OSLCConstants.OSLC_CM.equals(testVersions) || testVersions.equals("both")) {
			log.info("Setting up to test CM v2 features");
			testsToRun.add(ServiceProviderCatalogTests.class);
			testsToRun.add(ServiceDescriptionTests.class);
			testsToRun.add(CreationAndUpdateTests.class);
			testsToRun.add(QueryTests.class);
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.OAuthTests.class);
		}
		
		//Return array of test classes
		return testsToRun.toArray(new Class<?>[0]);
	}
}
