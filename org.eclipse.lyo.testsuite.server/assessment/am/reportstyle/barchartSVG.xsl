<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:lxslt="http://xml.apache.org/xslt"
        xmlns:stringutils="xalan://org.apache.tools.ant.util.StringUtils"
		xmlns:svg="http://www.w3.org/2000/svg"
		xmlns:xlink="http://www.w3.org/1999/xlink">

<xsl:output method="xml" indent="yes" encoding="UTF-8"
  doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" />
<xsl:decimal-format decimal-separator="." grouping-separator="," />
<!--
   License Text Here
 -->

<xsl:param name="TITLE">OSLC Unit Test Report.</xsl:param>
<xsl:variable name="spec" select="document('../oslc-spec-mapping/oslc-am-v2.xml')"/>

<!--

 Sample stylesheet to be used with Ant JUnitReport output.

 It creates a bar chart embeddable SVG file

-->

<xsl:template match="testsuites">
		<xsl:variable name="spec" select="document('../oslc-spec-mapping/oslc-am-v2.xml')"/>
		<xsl:variable name="mustCount" select="'107'"/>
		<xsl:variable name="junitMustCount" select="'58'"/>
		<xsl:variable name="testsuiteMustCount" select="count($spec//testcase[@level='MUST'])" />
        <xsl:variable name="passedMustCount" select="count(/testsuites/testsuite/testcase[@assessment='passedMust'])"/>
        <xsl:variable name="failedMustCount" select="count(/testsuites/testsuite/testcase[@assessment='failedMust'])"/>
        <xsl:variable name="errorMustCount" select="count(/testsuites/testsuite/testcase[@assessment='errorMust'])"/>
		<xsl:variable name="testsuiteAttemptedMustCount">
        	<xsl:value-of select="$passedMustCount + $failedMustCount + $errorMustCount"/>
        </xsl:variable>

        <xsl:variable name="shouldCount" select="count($spec//testcase[@level='SHOULD'])"/>
        <xsl:variable name="passedShouldCount" select="count(/testsuites/testsuite/testcase[@assessment='passedShould'])"/>
        <xsl:variable name="failedShouldCount" select="count(/testsuites/testsuite/testcase[@assessment='failedShould'])"/>
        <xsl:variable name="mayCount" select="count($spec//testcase[@level='MAY'])"/>
        <xsl:variable name="passedMayCount" select="count(/testsuites/testsuite/testcase[@assessment='passedMay'])"/>
        <xsl:variable name="failedMayCount" select="count(/testsuites/testsuite/testcase[@assessment='failedMay'])"/>

        <xsl:variable name="eval" select="($passedMustCount*5) + ($passedShouldCount*3) + ($passedMayCount*1)"/>
        <xsl:variable name="domain" select="$spec/provider-test/@domain"/>
        <xsl:variable name="version" select="$spec/provider-test/@version"/>


<svg:svg version="1.1" xmlns:svg="http://www.w3.org/2000/svg" width="1100" height="600" name="svgBOX" id="svgBOX" title="OSLC Assessment Chart" x="0.0000000" y="0.0000000" onload="RunScript(evt)">
<script type="text/ecmascript">
	<![CDATA[
		var SVGDoc;
		var SVGRoot;

		function RunScript(LoadEvent) {
			top.SVGsetDimension	= setDimension;
			top.SVGsetScale	= setScale;
			SVGDoc			= LoadEvent.target.ownerDocument;
			g_element		= SVGDoc.getElementById("layer1");
		}

		function setDimension(w,h) {
			SVGDoc.documentElement.setAttribute("width", w);
			SVGDoc.documentElement.setAttribute("height", h);
		}

		function setScale(sw, sh) {
			g_element.setAttribute("transform", "scale(" + sw + " " + sh +")");
		}
		]]></script>

				<svg:g transform="translate(0,50)" id="layer1">

    					<!-- Now Draw the main X and Y axis -->
							<svg:g style="stroke:black">

								<!-- Title -->
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:24px;" x="100" y="-20">OSLC TestSuite Execution Summary</svg:text>

								<!-- X Axis -->
								<svg:path d="M 100 200 L 500 200 Z" stroke-width="1"/>
								<!-- X Axis Labels -->
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="115" y="230">Attempted</svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="120" y="250">(MUSTS)</svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="230" y="230">Passed</svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="225" y="250">(MUSTS)</svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="330" y="230">Failed</svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="325" y="250">(MUSTS)</svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="430" y="230">Error</svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="425" y="250">(MUSTS)</svg:text>

								<!-- Y Axis -->
								<svg:path d="M 100 0 L 100 200 Z" stroke-width="1"/>
								<svg:path d="M 500 0 L 500 200 Z" stroke-width="1"/>
								<!-- Y Axis Labels -->
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="20"><xsl:value-of select="'170'"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="50"><xsl:value-of select="'150'"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="75"><xsl:value-of select="'125'"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="100"><xsl:value-of select="'100'"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="125"><xsl:value-of select="'75'"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="150"><xsl:value-of select="'50'"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="175"><xsl:value-of select="'25'"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="200">0</svg:text>

								<svg:g transform="rotate(-90)">
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="-150" y="20"># of Requirements</svg:text>
								</svg:g>

								<!-- Legend -->
								<svg:rect x="10" y="295" width="10" height="10" style="fill:rgb(0,0,255);"/>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:14px;" x="30" y="305">Attempted = Pass + Fail + Error.  # of Tests Executed for a Specification Type</svg:text>
								<svg:rect x="10" y="315" width="10" height="10" style="fill:rgb(0,255,0);"/>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:14px;" x="30" y="325">Pass: # of Test(s) achieving the respective test design's expected result</svg:text>
								<svg:rect x="10" y="335" width="10" height="10" style="fill:rgb(255,0,0);"/>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:14px;" x="30" y="345">Fail: # of Test(s) deviating from the respective test design's expected result.</svg:text>
								<svg:rect x="10" y="355" width="10" height="10" style="fill:rgb(160,32,240);"/>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:14px;" x="30" y="365">Error: # of Inconclusive Results. Test executions encountering error due to poor test design,</svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:14px;" x="70" y="380">faulty environment or invalid configuration. Test results could not be assessed.</svg:text>

								<!-- Domain Value Bars -->
								<svg:rect x="155" y="{200 - ($testsuiteAttemptedMustCount)}" width="10" height="{$testsuiteAttemptedMustCount}" style="fill:rgb(0,0,255);"/>
								<svg:rect x="255" y="{200 - ($passedMustCount)}" width="10" height="{$passedMustCount}" style="fill:rgb(0,255,0);"/>
								<svg:rect x="355" y="{200 - ($failedMustCount)}" width="10" height="{$failedMustCount}" style="fill:rgb(255,0,0);"/>
								<svg:rect x="455" y="{200 - ($errorMustCount)}" width="10" height="{$errorMustCount}" style="fill:rgb(160,32,240);"/>

								<!-- Lower Table Horizontal Lines -->
								<svg:path d="M 20 260 L 500 260 Z" stroke-width="1"/>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:10px;text-anchor: start;" x="30" y="275"><xsl:value-of select="$domain"/><xsl:text> </xsl:text><xsl:value-of select="$version"/></svg:text>
								<svg:path d="M 20 280 L 500 280 Z" stroke-width="1"/>

								<!-- Lower Table Lyo Values -->
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="145" y="275"><xsl:value-of select="$testsuiteAttemptedMustCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="245" y="275"><xsl:value-of select="$passedMustCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="345" y="275"><xsl:value-of select="$failedMustCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="445" y="275"><xsl:value-of select="$errorMustCount"/></svg:text>

								<!-- Lower Table Vertical Line -->
								<svg:path d="M 20 260 L 20 280 Z" stroke-width="1" stroke-dasharray="5"/>
							</svg:g>
							<svg:g  style="fill:none; stroke:#97694F; stroke-width:1; stroke-dasharray:2,4;text-anchor:end; font-size:30">

								<!-- Horizontal Gridlines -->
								<svg:path d="M 100 0 L 500 0 Z"/>
								<svg:path d="M 100 25 L 500 25 Z"/>
								<svg:path d="M 100 50 L 500 50 Z"/>
								<svg:path d="M 100 75 L 500 75 Z"/>
								<svg:path d="M 100 100 L 500 100 Z"/>
								<svg:path d="M 100 125 L 500 125 Z"/>
								<svg:path d="M 100 150 L 500 150 Z"/>
								<svg:path d="M 100 170 L 500 170 Z"/>

								<!-- Vertical Gridlines -->
								<svg:path d="M 100 280 L 100 200 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 200 280 L 200 200 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 300 280 L 300 200 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 400 280 L 400 200 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 500 280 L 500 200 Z" stroke-width="1" stroke-dasharray="5"/>

							</svg:g>
					</svg:g>
    			</svg:svg>
							<!-- Object Container to render the SVG User Load Preview Graph within the HTML
							<object type="image/svg+xml" name="SVGContainer" data="{concat(@name,'.svg')}" width="600" height="300">
 								<param name="src" value="{concat(@name,'.svg')}" />
								<param name="wmode" value="transparent"  />
								<embed src="{concat(@name,'.svg')}" type="image/svg+xml" width="450" height="300" wmode="transparent" />
							</object> -->
    </xsl:template>


</xsl:stylesheet>
