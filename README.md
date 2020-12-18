# Eclipse Lyo OSLC Test Suite

[![](https://img.shields.io/badge/project-Eclipse%20Lyo-blue?color=418eeb)](https://github.com/eclipse/lyo)
[![Discourse users](https://img.shields.io/discourse/users?color=28bd84&server=https%3A%2F%2Fforum.open-services.net%2F)](https://forum.open-services.net/)


## Goals

The goal of the Lyo OSLC Test Suite is to provide a suite of tests which
will test OSLC domain provider implementations against the
specification. A JUnit-based test suite is now available in the project
Git repository. The suite is under development and does not provide full
coverage of the implementations it tests at this time. The goals of the
OSLC Test Suite are to:

-   provide assessment tests for each of the OSLC domains
    -   priority is to cover MUST items first followed by SHOULD and
        then MAY items
    -   provide reporting to show provider implementations an assessment
        report of coverage and successful execution.
-   provide a tool to find bugs in OSLC providers and improve their
    quality

## OSLC Specifications Covered

-   Change Management (V1 and V2)
-   Asset Management (V1 and V2)
-   Quality Management (V2)
-   Requirements Management (V2)
-   Automation Management (V2)
-   Performance Monitoring (V2)

## Areas for Improvement

-   Increased coverage for currently supported specifications
-   Coverage for OSLC specifications not currently tested
-   Integrated reporting
-   Improved query tests
-   Improved OAuth tests

## Building and running the OSLC Test Suites and Reports

- [Detailed documentation](https://github.com/eclipse/lyo.testsuite/blob/master/org.eclipse.lyo.testsuite.server/assessment/documentation/HowToRunOSLCProviderTestsAndGenerateAssessmentReport.doc)
    for the OSLC Test Suite and Reports in MS Word format.
    * Overview of the test suites
    * How to run the tests
    * How to create reports
- [For Developers: Building and running the OSLC test suite](https://wiki.eclipse.org/Lyo/BuildTestSuite)
