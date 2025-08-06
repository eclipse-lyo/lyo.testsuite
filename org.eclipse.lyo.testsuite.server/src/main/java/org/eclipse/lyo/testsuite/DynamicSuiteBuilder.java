/*
 * Copyright (c) 2021 Contributors to the Eclipse Foundation.
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
package org.eclipse.lyo.testsuite;

import java.util.ArrayList;
import java.util.Properties;
import org.eclipse.lyo.testsuite.oslcv1.core.CreationAndUpdateTests;
import org.eclipse.lyo.testsuite.oslcv1.core.QueryTests;
import org.eclipse.lyo.testsuite.oslcv1.core.ServiceDescriptionTests;
import org.eclipse.lyo.testsuite.oslcv1.core.ServiceProviderCatalogTests;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.oslcv2.core.*;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.SetupProperties;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class determines which version of OSLC the provider is using, and builds the correct set of OSLC Provider tests
 * accordingly.
 *
 * <p>Also sets up forms authentication if necessary (for things like RTC).
 */
@RunWith(OslcTestSuite.class)
public class DynamicSuiteBuilder {
    public static Class<?>[] suitesArray() {
        final Logger log = LoggerFactory.getLogger(DynamicSuiteBuilder.class);
        Properties setupProps = SetupProperties.setup(null);

        log.info("Test");
        log.debug("Test debug");

        TestsBase.staticSetup();

        // If we also want to run v1 tests (assuming this is a v2 provider)
        String testVersions = setupProps.getProperty("testVersions");

        // Does the provide support JSON format?
        boolean supportOslcJson = true;
        if (setupProps.getProperty("supportJSON") != null) {
            supportOslcJson = !setupProps.getProperty("supportJSON").equalsIgnoreCase("false");
        }

        // Does the provide support creation factory?
        boolean supportCreationFactory = true;
        if (setupProps.getProperty("supportCreationFactory") != null) {
            supportCreationFactory =
                    !setupProps.getProperty("supportCreationFactory").equalsIgnoreCase("false");
        }

        // Does the provide support JSON format?
        boolean supportQuery = true;
        if (setupProps.getProperty("supportQuery") != null) {
            supportQuery = !setupProps.getProperty("supportQuery").equalsIgnoreCase("false");
        }

        // Does the provider support RDF/XML abbreviated format?
        boolean supportRdfXmlAbbrev = true;
        if (setupProps.getProperty("supportRdfXmlAbbrev") != null) {
            supportRdfXmlAbbrev = Boolean.parseBoolean(setupProps.getProperty("supportRdfXmlAbbrev"));
        }

        ArrayList<Class<?>> testsToRun = new ArrayList<>();
        // Determine if this is a v1 or v2 provider
        if (OSLCConstants.OSLC_CM_V2.equals(testVersions)
                || OSLCConstants.OSLC_V2.equals(testVersions)
                || OSLCConstants.OSLC_RM_V2.equals(testVersions)
                || OSLCConstants.OSLC_QM_V2.equals(testVersions)
                || "both".equals(testVersions)
                || OSLCConstants.OSLC_AM_V2.equals(testVersions)
                || OSLCConstants.OSLC_ASSET_V2.equals(testVersions)
                || OSLCConstants.OSLC_AUTO_V2.equals(testVersions)
                || OSLCConstants.OSLC_PM_V2.equals(testVersions)) {
            log.info("Setting up to test Core v2 features");
            testsToRun.add(ServiceProviderCatalogRdfXmlTests.class);
            testsToRun.add(ServiceProviderRdfXmlTests.class);
            if (supportOslcJson) {
                testsToRun.add(FetchResourceJsonTests.class);
            } else {
                testsToRun.add(FetchResourceTests.class);
            }

            if (OSLCConstants.OSLC_CM_V2.equals(testVersions)
                    || OSLCConstants.OSLC_QM_V2.equals(testVersions)
                    || OSLCConstants.OSLC_RM_V2.equals(testVersions)
                    || OSLCConstants.OSLC_ASSET_V2.equals(testVersions)
                    || OSLCConstants.OSLC_AUTO_V2.equals(testVersions)
                    || OSLCConstants.OSLC_PM_V2.equals(testVersions)) {
                if (supportRdfXmlAbbrev) {
                    testsToRun.add(ServiceProviderCatalogXmlTests.class);
                    testsToRun.add(ServiceProviderXmlTests.class);
                }

                if (supportCreationFactory) {
                    testsToRun.add(CreationAndUpdateRdfXmlTests.class);
                    if (supportRdfXmlAbbrev) {
                        testsToRun.add(CreationAndUpdateXmlTests.class);
                    }
                }

                if (supportQuery) {
                    testsToRun.add(SimplifiedQueryRdfXmlTests.class);
                    if (supportRdfXmlAbbrev) {
                        testsToRun.add(SimplifiedQueryXmlTests.class);
                    }
                }

                if (OSLCConstants.OSLC_CM_V2.equals(testVersions)) {
                    log.info("Setting up to test CM v2 features");

                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.ChangeRequestRdfXmlTests.class);
                    if (supportRdfXmlAbbrev) {
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.ChangeRequestXmlTests.class);
                    }

                    if (supportOslcJson) {
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.cm.ChangeRequestJsonTests.class);

                        if (supportQuery) {
                            testsToRun.add(SimplifiedQueryJsonTests.class);
                        }

                        if (supportCreationFactory) {
                            testsToRun.add(CreationAndUpdateJsonTests.class);
                        }
                    }
                } else if (OSLCConstants.OSLC_ASSET_V2.equals(testVersions)) {
                    log.info("Setting up to test Asset v2 features");

                    if (supportOslcJson) {
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.CreateAssetJsonTest.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.GetAndUpdateJsonTests.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.UsageCaseJsonTests.class);
                    }

                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.CreateAssetRdfXmlTest.class);
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.GetAndUpdateRdfXmlTests.class);
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.UsageCaseRdfXmlTests.class);

                    if (supportRdfXmlAbbrev) {
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.CreateAssetXmlTest.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.GetAndUpdateXmlTests.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.asset.UsageCaseXmlTests.class);
                    }
                } else if (OSLCConstants.OSLC_RM_V2.equals(testVersions)) {
                    log.info("Setting up to test RM v2 features");
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.rm.RequirementRdfXmlTests.class);
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.rm.RequirementCollectionRdfXmlTests.class);

                    if (supportRdfXmlAbbrev) {
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.rm.RequirementXmlTests.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.rm.RequirementCollectionXmlTests.class);
                    }
                } else if (OSLCConstants.OSLC_AUTO_V2.equals(testVersions)) {
                    log.info("Setting up to test Automation v2 features");
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationPlanRdfXmlTests.class);
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationRequestRdfXmlTests.class);
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationResultRdfXmlTests.class);

                    if (supportRdfXmlAbbrev) {
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationRequestXmlTests.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationPlanXmlTests.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationResultXmlTests.class);
                    }

                    if (supportOslcJson) {
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationPlanJsonTests.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationRequestJsonTests.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.auto.AutomationResultJsonTests.class);
                    }
                } else if (OSLCConstants.OSLC_QM_V2.equals(testVersions)) {
                    log.info("Setting up to test QM v2 features");
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestPlanRdfXmlTests.class);
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestCaseRdfXmlTests.class);
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestScriptRdfXmlTests.class);
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestExecutionRecordRdfXmlTests.class);
                    testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestResultRdfXmlTests.class);

                    if (supportRdfXmlAbbrev) {
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestPlanXmlTests.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestCaseXmlTests.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestScriptXmlTests.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestExecutionRecordXmlTests.class);
                        testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.qm.TestResultXmlTests.class);
                    }
                } else if (OSLCConstants.OSLC_PM_V2.equals(testVersions)) {
                    log.info("Setting up to test PM v2 features");
                    testsToRun.add(
                            org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordComputerSystemRdfXmlTests
                                    .class);
                    testsToRun.add(
                            org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordAgentRdfXmlTests.class);
                    testsToRun.add(
                            org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordDatabaseRdfXmlTests.class);
                    testsToRun.add(
                            org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordProcessRdfXmlTests.class);
                    testsToRun.add(
                            org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordSoftwareModuleRdfXmlTests
                                    .class);
                    testsToRun.add(
                            org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordSoftwareServerRdfXmlTests
                                    .class);
                    testsToRun.add(
                            org.eclipse.lyo.testsuite.oslcv2.pm.PerformanceMonitoringRecordStorageVolumeRdfXmlTests
                                    .class);
                }
            }
            // testsToRun.add(org.eclipse.lyo.testsuite.oslcv2.core.OAuthTests.class);
        }
        if (OSLCConstants.OSLC_CM.equals(testVersions)
                || OSLCConstants.OSLC_ASSET.equals(testVersions)
                || testVersions.equals("both")) {
            log.info("Setting up to test v1 features");
            testsToRun.add(ServiceProviderCatalogTests.class);
            testsToRun.add(ServiceDescriptionTests.class);
            testsToRun.add(CreationAndUpdateTests.class);
            testsToRun.add(QueryTests.class);
            testsToRun.add(OAuthTests.class);
        }

        // Return array of test classes
        return testsToRun.toArray(new Class<?>[0]);
    }
}
