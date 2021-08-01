# 2021 intro to building OSLC TestSuite reports

> **WIP**

Force download of the `ant-contrib` via Maven (needed by Ant build config we use):

    mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=ant-contrib:ant-contrib:1.0b3:jar

Edit the values in `assessment.props`. As of 2021, only `provider` and `top` are required.

Build a report from the module root

    cd org.eclipse.lyo.testsuite.server/
    ant -f build-cm.xml junitreport

In theory, this should be enough and you should see a report under `assessment/report/%PROVIDER%`.


## Equivalent Saxon commands to avoid Ant just for XSLT

Assuming you just ran the test suite via Maven from the command line:

    cd org.eclipse.lyo.testsuite.server/
    saxon -s:target/surefire-reports/TEST-org.eclipse.lyo.testsuite.DynamicSuiteBuilder.xml -xsl:assessment/cm/reportstyle/append-oslc-spec-info.xsl -o:target/oslc-reports/TEST-org.eclipse.lyo.testsuite.DynamicSuiteBuilderOSLC.xml
    saxon -s:target/oslc-reports/TEST-org.eclipse.lyo.testsuite.DynamicSuiteBuilderOSLC.xml -xsl:assessment/cm/reportstyle/barchartSVG.xsl -o:target/oslc-reports/barchartSVG.svg
    saxon -s:target/oslc-reports/TEST-org.eclipse.lyo.testsuite.DynamicSuiteBuilderOSLC.xml -xsl:assessment/cm/reportstyle/junit-noframes.xsl -o:target/oslc-reports/OSLCAssessmentReport.html
