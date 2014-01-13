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
 *    Yuhong Yin
 *    Tim Eck II     - asset management test cases
 *    Julie Bielski  - performance management test cases
 *    Samuel Padgett - fix suite error when using Ant
 *******************************************************************************/
package org.eclipse.lyo.testsuite.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.lyo.testsuite.oslcv2.OSLCCoreVersionJsonTest;
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
	public static Class<?>[] suitesArray() throws IOException
	{
		final Logger log = LoggerFactory.getLogger(DynamicSuiteBuilder.class);
		Properties setupProps = SetupProperties.setup(null);
		
		TestsBase.staticSetup();
		
		//If we also want to run v1 tests (assuming this is a v2 provider)
		String testVersions = setupProps.getProperty("testVersions");

		// Does the provide support JSON format?
		boolean supportJSON = true;
		if ( setupProps.getProperty("supportJSON") != null ) {
			supportJSON = !setupProps.getProperty("supportJSON").equalsIgnoreCase("false");
		}
				
		// Does the provide support creation factory?
		boolean supportCreationFactory = true;
		if ( setupProps.getProperty("supportCreationFactory") != null ) {
			supportCreationFactory = !setupProps.getProperty("supportCreationFactory").equalsIgnoreCase("false");
		}
		
		// Does the provide support JSON format?
		boolean supportQuery = true;
		if ( setupProps.getProperty("supportQuery") != null ) {
			supportQuery = !setupProps.getProperty("supportQuery").equalsIgnoreCase("false");
		}

		// Does the provider support ATOM query
		boolean supportATOMQuery = true;
		if ( setupProps.getProperty("supportATOMQuery") != null ) {
			supportATOMQuery = !setupProps.getProperty("supportATOMQuery").equalsIgnoreCase("false");
		}
		
		ArrayList<Class<?>> testsToRun = new ArrayList<Class<?>>();
		//Determine if this is a v1 or v2 provider
		if (   OSLCConstants.OSLC_CM_V2.equals(testVersions) 
			|| OSLCConstants.OSLC_V2.equals(testVersions) 
			|| OSLCConstants.OSLC_RM_V2.equals(testVersions) 
			|| OSLCConstants.OSLC_QM_V2.equals(testVersions) 
			|| testVersions.equals("both") 
			|| OSLCConstants.OSLC_AM_V2.equals(testVersions)
			|| OSLCConstants.OSLC_ASSET_V2.equals(testVersions)
			|| OSLCConstants.OSLC_AUTO_V2.equals(testVersions)
			|| OSLCConstants.OSLC_PM_V2.equals(testVersions)) {
			log.info("Setting up to test Core v2 features");
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.ServiceProviderCatalogRdfXmlTests.class);
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.ServiceProviderRdfXmlTests.class);
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.FetchResourceTests.class);
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.OSLCCoreVersionJsonTest.class);
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.OSLCCoreVersionRdfXmlTest.class);
			testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.OSLCCoreVersionXmlTest.class);
			
			if (OSLCConstants.OSLC_CM_V2.equals(testVersions) || 
				OSLCConstants.OSLC_QM_V2.equals(testVersions) || 
				OSLCConstants.OSLC_RM_V2.equals(testVersions) ||
				OSLCConstants.OSLC_ASSET_V2.equals(testVersions) ||
				OSLCConstants.OSLC_AUTO_V2.equals(testVersions) ||
				OSLCConstants.OSLC_PM_V2.equals(testVersions)) {
				testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.ServiceProviderCatalogXmlTests.class);
				testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.ServiceProviderXmlTests.class);
				
				if ( supportCreationFactory ) {
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.CreationAndUpdateXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.CreationAndUpdateRdfXmlTests.class);			
				}
	
				if ( supportQuery ) {
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.SimplifiedQueryXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.SimplifiedQueryRdfXmlTests.class);
				
					if ( supportATOMQuery ) {
						testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.SimplifiedQueryAtomTests.class);
					}
				}
				
				if (OSLCConstants.OSLC_CM_V2.equals(testVersions)) {
					log.info("Setting up to test CM v2 features");
					if ( supportCreationFactory && supportJSON ) {
						testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.CreationAndUpdateJsonTests.class);
					}
	
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.ChangeRequestXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.ChangeRequestRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.ChangeRequestJsonTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.InvalidateOSLCPropertiesJsonTest.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.InvalidateOSLCPropertiesRdfXmlTest.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.InvalidateOSLCPropertiesXmlTest.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.SimplifiedQueryJsonTests.class);
					
				} else if(OSLCConstants.OSLC_ASSET_V2.equals(testVersions)) {
					log.info("Setting up to test Asset v2 features");
					
					if ( supportJSON ) {
						testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.CreateAssetJsonTest.class);
						testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.GetAndUpdateJsonTests.class);
						testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.UsageCaseJsonTests.class);
					}
					
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.CreateAssetRdfXmlTest.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.CreateAssetXmlTest.class);
					
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.GetAndUpdateRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.GetAndUpdateXmlTests.class);

					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.UsageCaseXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.UsageCaseRdfXmlTests.class);										
				} else if (OSLCConstants.OSLC_RM_V2.equals(testVersions)) {
					log.info("Setting up to test RM v2 features");
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.rm.RequirementXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.rm.RequirementCollectionXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.rm.RequirementRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.rm.RequirementCollectionRdfXmlTests.class);

				} else if (OSLCConstants.OSLC_AUTO_V2.equals(testVersions)) {
					log.info("Setting up to test Automation v2 features");
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationPlanRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationPlanXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationPlanJsonTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationRequestRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationRequestXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationRequestJsonTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationResultRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationResultXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationResultJsonTests.class);
			
				} else if (OSLCConstants.OSLC_QM_V2.equals(testVersions)) {
					log.info("Setting up to test QM v2 features");
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
				} else if(OSLCConstants.OSLC_PM_V2.equals(testVersions)) {
					log.info("Setting up to test PM v2 features");
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordComputerSystemRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordAgentRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordDatabaseRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordProcessRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordSoftwareModuleRdfXmlTests.class);
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordSoftwareServerRdfXmlTests.class);					
					testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordStorageVolumeRdfXmlTests.class);					
				}
			}
			//testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.OAuthTests.class);
		}
		if (OSLCConstants.OSLC_CM.equals(testVersions) || 
			OSLCConstants.OSLC_ASSET.equals(testVersions) ||
			testVersions.equals("both")) {
			log.info("Setting up to test v1 features");
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