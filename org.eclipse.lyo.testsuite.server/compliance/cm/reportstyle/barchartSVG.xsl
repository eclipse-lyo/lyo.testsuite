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
<xsl:variable name="spec" select="document('../oslc-spec-mapping/oslc-cm-v2.xml')"/>

<!--

 Sample stylesheet to be used with Ant JUnitReport output.

 It creates a bar chart embeddable SVG file

-->

<xsl:template match="testsuites">
		<xsl:variable name="spec" select="document('../oslc-spec-mapping/oslc-cm-v2.xml')"/>
		<xsl:variable name="mustCount" select="'107'"/>
		<xsl:variable name="junitMustCount" select="'58'"/>
		<xsl:variable name="testsuiteMustCount" select="count($spec//testcase[@level='MUST'])" />       
        <xsl:variable name="passedMustCount" select="count(/testsuites/testsuite/testcase[@compliance='passedMust'])"/>
        <xsl:variable name="failedMustCount" select="count(/testsuites/testsuite/testcase[@compliance='failedMust'])"/>
        <xsl:variable name="errorMustCount">
        	<xsl:choose>
        		<xsl:when test="/testsuites/testsuite/testcase[@compliance='passedMust' or @compliance='failedMust']//error">
        			<xsl:value-of select="count(/testsuites/testsuite/testcase[@compliance='passedMust' or @compliance='failedMust']//error)"/>
        		</xsl:when>
        		<xsl:otherwise>
        			<xsl:value-of select="$testsuiteMustCount - ($passedMustCount + $failedMustCount)"/>
        		</xsl:otherwise>
        	</xsl:choose>
        </xsl:variable>
        <xsl:variable name="shouldCount" select="count($spec//testcase[@level='SHOULD'])"/>
        <xsl:variable name="passedShouldCount" select="count(/testsuites/testsuite/testcase[@compliance='passedShould'])"/>
        <xsl:variable name="failedShouldCount" select="count(/testsuites/testsuite/testcase[@compliance='failedShould'])"/>
        <xsl:variable name="mayCount" select="count($spec//testcase[@level='MAY'])"/>
        <xsl:variable name="passedMayCount" select="count(/testsuites/testsuite/testcase[@compliance='passedMay'])"/>
        <xsl:variable name="failedMayCount" select="count(/testsuites/testsuite/testcase[@compliance='failedMay'])"/>
        
        <xsl:variable name="eval" select="($passedMustCount*5) + ($passedShouldCount*3) + ($passedMayCount*1)"/>
        <xsl:variable name="domain" select="$spec/provider-test/@domain"/>
        <xsl:variable name="version" select="$spec/provider-test/@version"/>


        
        
<svg:svg version="1.1" xmlns:svg="http://www.w3.org/2000/svg" width="1100" height="350px" name="svgBOX" id="svgBOX" title="OSLC Compliance Chart" x="0.0000000" y="0.0000000" onload="RunScript(evt)">
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
							<!-- <rect x="0" y="0" width="450" height="200" style="fill:#FAF0E6;"/> -->
    							<!-- Now Draw the main X and Y axis -->
							<svg:g style="stroke:black">
							
								<!-- Title -->
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:24px;" x="100" y="-20">OSLC TestSuite Execution Summary</svg:text>
								<!-- X Axis -->
								<svg:path d="M 100 100 L 500 100 Z" stroke-width="1"/>
								<!-- X Axis Labels -->
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="115" y="130">Attempted</svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="120" y="150">(MUSTS)</svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="230" y="130">Passed</svg:text>
								<!--  <svg:image x="280" y="105" width="16" height="16" alt="Junit" title="# of Tests Capable of Being Tested via JUnit" xlink:href="./junit.png" /> -->
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="225" y="150">(MUSTS)</svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="330" y="130">Failed</svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="325" y="150">(MUSTS)</svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="430" y="130">Error</svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="425" y="150">(MUSTS)</svg:text>
								<!-- <svg:text style="fill:black;stroke:none;font-family:tahoma;" x="520" y="120"></svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="530" y="137"></svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="515" y="154"></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="620" y="120"></svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="630" y="137"></svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="615" y="154"></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="730" y="130">MAYs</svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="820" y="130">Testable</svg:text>
								<svg:image x="880" y="105" width="16" height="16" alt="Junit" title="# of Tests Capable of Being Tested via JUnit" xlink:href="./junit.png" />
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="830" y="150">MAYs</svg:text>								
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="925" y="130">Passed</svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="930" y="150">MAYs</svg:text> -->

								<!-- Y Axis -->
								<svg:path d="M 100 0 L 100 100 Z" stroke-width="1"/>
								<svg:path d="M 500 0 L 500 100 Z" stroke-width="1"/>
								<!-- Y Axis Labels -->
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="10"><xsl:value-of select="'100'"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="30"><xsl:value-of select="'75'"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="55"><xsl:value-of select="'50'"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="80"><xsl:value-of select="'25'"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="50" y="105">0</svg:text>

								<svg:g transform="rotate(-90)">
									<svg:text style="fill:black;stroke:none;font-family:tahoma;" x="-110" y="20"># of Requirements</svg:text>
								</svg:g>
								
								<!-- Legend -->
								<svg:rect x="10" y="195" width="10" height="10" style="fill:rgb(0,0,255);"/>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:14px;" x="30" y="205">Attempted = Pass + Fail + Error.  # of Tests Executed for a Specification Type</svg:text>
								<svg:rect x="10" y="215" width="10" height="10" style="fill:rgb(0,255,0);"/>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:14px;" x="30" y="225">Pass: # of Test(s) achieving the respective test design's expected result</svg:text>
								<svg:rect x="10" y="235" width="10" height="10" style="fill:rgb(255,0,0);"/>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:14px;" x="30" y="245">Fail: # of Test(s) deviating from the respective test design's expected result.</svg:text>
								<svg:rect x="10" y="255" width="10" height="10" style="fill:rgb(160,32,240);"/>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:14px;" x="30" y="265">Error: # of Inconclusive Results. Test executions encountering error due to poor test design,</svg:text>
									<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:14px;" x="70" y="280">faulty environment or invalid configuration. Test results could not be assessed.</svg:text>
								
								<!-- Lyo Value Bars
								<svg:rect x="135" y="{100 - ($mustCount div 2)}" width="10" height="{$mustCount div 2}" style="fill:rgb(255,0,0);"/>
								<svg:rect x="235" y="{100 - ($junitMustCount div 2)}" width="10" height="{$junitMustCount div 2}" style="fill:rgb(255,0,0);"/>
								<svg:rect x="335" y="{100 - ($passedMustCount div 2)}" width="10" height="{$passedMustCount div 2}" style="fill:rgb(255,0,0);"/>
								<svg:rect x="435" y="{100 - ($shouldCount div 2)}" width="10" height="{$shouldCount div 2}" style="fill:rgb(255,0,0);"/>
								<svg:rect x="535" y="{100 - ($shouldCount div 2)}" width="10" height="{$shouldCount div 2}" style="fill:rgb(255,0,0);"/>
								<svg:rect x="635" y="{100 - ($passedShouldCount div 2)}" width="10" height="{$passedShouldCount div 2}" style="fill:rgb(255,0,0);"/>
								<svg:rect x="735" y="{100 - ($mayCount div 2)}" width="10" height="{$mayCount div 2}" style="fill:rgb(255,0,0);"/>
								<svg:rect x="835" y="{100 - ($mayCount div 2)}" width="10" height="{$mayCount div 2}" style="fill:rgb(255,0,0);"/>
								<svg:rect x="935" y="{100 - ($passedMayCount div 2)}" width="10" height="{$passedMayCount div 2}" style="fill:rgb(255,0,0);"/>
								-->
								
								<!-- Domain Value Bars -->
								<svg:rect x="155" y="{100 - ($testsuiteMustCount)}" width="10" height="{$testsuiteMustCount}" style="fill:rgb(0,0,255);"/>
								<svg:rect x="255" y="{100 - ($passedMustCount)}" width="10" height="{$passedMustCount}" style="fill:rgb(0,255,0);"/>
								<svg:rect x="355" y="{100 - ($failedMustCount)}" width="10" height="{$failedMustCount}" style="fill:rgb(255,0,0);"/>
								<svg:rect x="455" y="{100 - ($errorMustCount)}" width="10" height="{$errorMustCount}" style="fill:rgb(160,32,240);"/>
								<!--  <svg:rect x="555" y="{100 - ($unknownMustCount div 2)}" width="10" height="{$unknownMustCount div 2}" style="fill:rgb(0,0,255);"/>
								<svg:rect x="655" y="{100 - ($unknownMustCount div 2)}" width="10" height="{$unknownMustCount div 2}" style="fill:rgb(0,0,255);"/>
								<svg:rect x="755" y="{100 - ($mayCount div 2)}" width="10" height="{$mayCount div 2}" style="fill:rgb(0,0,255);"/>
								<svg:rect x="855" y="{100 - ($mayCount div 2)}" width="10" height="{$mayCount div 2}" style="fill:rgb(0,0,255);"/>
								<svg:rect x="955" y="{100 - ($passedMayCount div 2)}" width="10" height="{$passedMayCount div 2}" style="fill:rgb(0,0,255);"/>
								 -->
								
								<!-- Lower Table Horizontal Lines -->
								<svg:path d="M 20 160 L 500 160 Z" stroke-width="1"/>
								<!-- <svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="38" y="175">Lyo</svg:text>
								<svg:rect x="25" y="165" width="10" height="10" style="fill:rgb(255,0,0);"/>
									<svg:path d="M 20 180 L 1000 180 Z" stroke-width="1"/> -->
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:10px;text-anchor: start;" x="30" y="175"><xsl:value-of select="$domain"/><xsl:text> </xsl:text><xsl:value-of select="$version"/></svg:text>
								<!-- <svg:rect x="25" y="165" width="10" height="10" style="fill:rgb(0,0,255);"/> -->
									<svg:path d="M 20 180 L 500 180 Z" stroke-width="1"/>
									
								<!-- Lower Table Lyo Values -->
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="145" y="175"><xsl:value-of select="$testsuiteMustCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="245" y="175"><xsl:value-of select="$passedMustCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="345" y="175"><xsl:value-of select="$failedMustCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="445" y="175"><xsl:value-of select="$errorMustCount"/></svg:text>
								<!-- <svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="545" y="175"><xsl:value-of select="$failedMustCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="645" y="175"><xsl:value-of select="$unknownMustCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="745" y="175"><xsl:value-of select="$mayCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="845" y="175"><xsl:value-of select="$mayCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="945" y="175"><xsl:value-of select="$passedMayCount"/></svg:text>
								 -->
								
								<!-- Lower Table Domain Values
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="145" y="195"><xsl:value-of select="$mustCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="245" y="195"><xsl:value-of select="$mustCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="345" y="195"><xsl:value-of select="$passedMustCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="445" y="195"><xsl:value-of select="$shouldCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="545" y="195"><xsl:value-of select="$shouldCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="645" y="195"><xsl:value-of select="$passedShouldCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="745" y="195"><xsl:value-of select="$mayCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="845" y="195"><xsl:value-of select="$mayCount"/></svg:text>
								<svg:text style="fill:black;stroke:none;font-family:tahoma;font-size:12px;text-anchor: start;" x="945" y="195"><xsl:value-of select="$passedMayCount"/></svg:text>
								-->	
									
								<!-- Lower Table Vertical Line -->	
								<svg:path d="M 20 160 L 20 180 Z" stroke-width="1" stroke-dasharray="5"/>
							</svg:g>							
							<svg:g  style="fill:none; stroke:#97694F; stroke-width:1; stroke-dasharray:2,4;text-anchor:end; font-size:30">
								<!-- Horizontal Gridlines -->
								<svg:path d="M 100 0 L 500 0 Z"/>
								<svg:path d="M 100 25 L 500 25 Z"/>
								<svg:path d="M 100 50 L 500 50 Z"/>
								<svg:path d="M 100 75 L 500 75 Z"/>
		
								<!-- Vertical Gridlines -->
								<svg:path d="M 100 100 L 100 180 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 200 100 L 200 180 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 300 100 L 300 180 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 400 100 L 400 180 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 500 100 L 500 180 Z" stroke-width="1" stroke-dasharray="5"/>
								<!-- <svg:path d="M 600 100 L 600 180 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 700 100 L 700 180 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 800 100 L 800 180 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 900 100 L 900 180 Z" stroke-width="1" stroke-dasharray="5"/>
								<svg:path d="M 1000 100 L 1000 180 Z" stroke-width="1" stroke-dasharray="5"/> -->
							</svg:g>
					</svg:g>
    			</svg:svg>
							<!-- Object Container to render the SVG User Load Preview Graph within the HTML
							<object type="image/svg+xml" name="SVGContainer" data="{concat(@name,'.svg')}" codebase="http://www.adobe.com/svg/viewer/install/" width="450" height="200">
 								<param name="src" value="{concat(@name,'.svg')}" />
								<param name="wmode" value="transparent"  />
								<embed src="{concat(@name,'.svg')}" type="image/svg+xml" width="450" height="200" wmode="transparent" pluginspage="http://www.adobe.com/svg/viewer/install/" />
							</object> -->
    </xsl:template>


</xsl:stylesheet>
