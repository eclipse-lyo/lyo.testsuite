<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:lxslt="http://xml.apache.org/xslt"
        xmlns:stringutils="xalan://org.apache.tools.ant.util.StringUtils"
		xmlns:svg="http://www.w3.org/2000/svg"
		xmlns:exslt="http://exslt.org/common"
  		xmlns:msxsl="urn:schemas-microsoft-com:xslt"
  		xmlns:str="http://exslt.org/strings"
  		xmlns:date="xalan://java.util.Date"
  		xmlns:file="xalan://java.io.File"
  		xmlns:FR="xalan://java.io.FileReader"
		xmlns:scan="xalan://java.util.Scanner"
        extension-element-prefixes="str date FR scan">

<xsl:output method="html" indent="yes" encoding="US-ASCII"
  doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" />
<xsl:decimal-format decimal-separator="." grouping-separator="," />
<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 -->

<xsl:param name="TITLE">OSLC Unit Test Report</xsl:param>
<xsl:variable name="spec" select="document('../oslc-spec-mapping/oslc-cm-v2.xml')"/>
<xsl:variable name="troubleshoot" select="document('../support/troubleshooting.xml')"/>
<xsl:variable name="date" select="date:new()"/>
<xsl:variable name="propsFileName">
	<xsl:call-template name="substring-after-last">
		<xsl:with-param name="string" select="/testsuites/testsuite/properties/property[@name='props']/@value" />
		<xsl:with-param name="delimiter" select="'/'" />
	</xsl:call-template>
</xsl:variable>
<xsl:variable name="propsFileFolderPath" select="substring-before(/testsuites/testsuite/properties/property[@name='props']/@value,$propsFileName)" />
<xsl:variable name="propsFile" select="file:new(string($propsFileFolderPath),string($propsFileName))"/>
<xsl:variable name="file_reader" select="FR:new(string($propsFile))" />
<xsl:variable name="scanner" select="scan:new($file_reader)"/>

<!-- OSLC Config Value Pairs -->
<xsl:variable name="baseUri" select="scan:findWithinHorizon($scanner,'.*baseUri=.*',0)"/>
<xsl:variable name="useThisServiceProvider" select="scan:findWithinHorizon($scanner,'.*useThisServiceProvider=.*',0)"/>
<xsl:variable name="implName" select="scan:findWithinHorizon($scanner,'.*implName=.*',0)"/>
<xsl:variable name="authMethod" select="scan:findWithinHorizon($scanner,'.*authMethod=.*',0)"/>
<xsl:variable name="formUri" select="scan:findWithinHorizon($scanner,'.*formUri=.*',0)"/>
<xsl:variable name="userId" select="scan:findWithinHorizon($scanner,'.*userId=.*',0)"/>
<xsl:variable name="pw" select="scan:findWithinHorizon($scanner,'.*pw=.*',0)"/>
<xsl:variable name="testVersions" select="scan:findWithinHorizon($scanner,'.*testVersions=.*',0)"/>
<xsl:variable name="runOnlyOnce" select="scan:findWithinHorizon($scanner,'.*runOnlyOnce=.*',0)"/>
<xsl:variable name="queryEqualityProperty" select="scan:findWithinHorizon($scanner,'.*queryEqualityProperty=.*',0)"/>
<xsl:variable name="queryEqualityValue" select="scan:findWithinHorizon($scanner,'.*queryEqualityValue=.*',0)"/>
<xsl:variable name="queryComparisonProperty" select="scan:findWithinHorizon($scanner,'.*queryComparisonProperty=.*',0)"/>
<xsl:variable name="queryComparisonValue" select="scan:findWithinHorizon($scanner,'.*queryComparisonValue=.*',0)"/>
<xsl:variable name="queryAdditionalParameters" select="scan:findWithinHorizon($scanner,'.*queryAdditionalParameters=.*',0)"/>
<xsl:variable name="fullTextSearchTerm" select="scan:findWithinHorizon($scanner,'.*fullTextSearchTerm=.*',0)"/>
<xsl:variable name="createTemplateXmlFile" select="scan:findWithinHorizon($scanner,'.*createTemplateXmlFile=.*',0)"/>
<xsl:variable name="createTemplateRdfXmlFile" select="scan:findWithinHorizon($scanner,'.*createTemplateRdfXmlFile=.*',0)"/>
<xsl:variable name="createTemplateJsonFile" select="scan:findWithinHorizon($scanner,'.*createTemplateJsonFile=.*',0)"/>
<xsl:variable name="updateTemplateXmlFile" select="scan:findWithinHorizon($scanner,'.*updateTemplateXmlFile=.*',0)"/>
<xsl:variable name="updateTemplateRdfXmlFile" select="scan:findWithinHorizon($scanner,'.*updateTemplateRdfXmlFile=.*',0)"/>
<xsl:variable name="updateTemplateJsonFile" select="scan:findWithinHorizon($scanner,'.*updateTemplateJsonFile=.*',0)"/>
<xsl:variable name="updateParams" select="scan:findWithinHorizon($scanner,'.*updateParams=.*',0)"/>
<xsl:variable name="OAuthRequestTokenUrl" select="scan:findWithinHorizon($scanner,'.*OAuthRequestTokenUrl=.*',0)"/>
<xsl:variable name="OAuthAuthorizationUrl" select="scan:findWithinHorizon($scanner,'.*OAuthAuthorizationUrl=.*',0)"/>
<xsl:variable name="OAuthAuthorizationParameters" select="scan:findWithinHorizon($scanner,'.*OAuthAuthorizationParameters=.*',0)"/>
<xsl:variable name="OAuthAccessTokenUrl" select="scan:findWithinHorizon($scanner,'.*OAuthAccessTokenUrl=.*',0)"/>
<xsl:variable name="OAuthConsumerToken" select="scan:findWithinHorizon($scanner,'.*OAuthConsumerToken=.*',0)"/>
<xsl:variable name="OAuthConsumerSecret" select="scan:findWithinHorizon($scanner,'.*OAuthConsumerSecret=.*',0)"/>

<xsl:variable name="SystemErr"><b>System Err: </b></xsl:variable>
<!--

 Sample stylesheet to be used with Ant JUnitReport output.

 It creates a non-framed report that can be useful to send via
 e-mail or such.

-->
<xsl:template match="testsuites">
	<html>
        <head>
            <title><xsl:value-of select="$TITLE"/></title>
    <style type="text/css">
      body {
        font:normal 68% verdana,arial,helvetica;
        color:#000000;
      }
      table tr td, table tr th {
          font-size: 68%;
      }
      table.details tr th{
        font-weight: bold;
        text-align:left;
        background:#a6caf0;
      }
      table.details tr td{
        background:#eeeee0;
      }

      p {
        line-height:1.5em;
        margin-top:0.5em; margin-bottom:1.0em;
      }
      h1 {
        margin: 0px 0px 5px; font: 165% verdana,arial,helvetica
      }
      h2 {
        margin-top: 1em; margin-bottom: 0.5em; font: bold 125% verdana,arial,helvetica
      }
      h3 {
        margin-bottom: 0.5em; font: bold 115% verdana,arial,helvetica
      }
      h4 {
        margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
      }
      h5 {
        margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
      }
      h6 {
        margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
      }
      .Error {
        font-weight:bold; color:red;
      }
      .Failure {
        font-weight:bold; color:purple;
      }
      .Properties {
        text-align:right;
      }
      
      img.OSLCLogo {
      	width: 72px;
      	height:91px;
      	background:url(./oslc_sprite.png) 0px 0px;
      }
      img.troubleshoot {
      	width: 15px;
      	height:15px;
      	background:url(./oslc_sprite.png) -102px 0px;
      }
      
      </style>
      <script type="text/javascript" language="JavaScript">
        var TestCases = new Array();
        var SysOutMessages = new Array();
        var SysErrMessages = new Array();
        var cur;
        var curSysOut;
        var curSysErr;
        <xsl:for-each select="./testsuite">
            <xsl:apply-templates select="properties"/>
        </xsl:for-each>
		<!-- Populate OSLC Properties Javascript Datastructure -->
        <xsl:call-template name="OSLCProperties"/>
        <xsl:call-template name="systemOut"/>
        <xsl:call-template name="systemErr"/>
       </script>
       <script type="text/javascript" language="JavaScript"><![CDATA[
       var W3CDOM = (document.createElement && document.getElementsByTagName);
       window.onload 	= init;
       
        function displayProperties (name) {

          var win = window.open('','JUnitSystemProperties','scrollbars=1,resizable=1');
          var doc = win.document;
          doc.open();
          doc.write("<html><head><title>Properties of " + name + "</title>");
          doc.write("\x3Cscript src='http://platform.twitter.com/widgets.js' type='text/javascript'>\x3C/script>");
          doc.write("<style>")
          doc.write("body {font:normal 68% verdana,arial,helvetica; color:#000000; }");
          doc.write("table tr td, table tr th { font-size: 68%; }");
          doc.write("table.properties { border-collapse:collapse; border-left:solid 1 #cccccc; border-top:solid 1 #cccccc; padding:5px; }");
          doc.write("table.properties th { text-align:left; border-right:solid 1 #cccccc; border-bottom:solid 1 #cccccc; background-color:#eeeeee; }");
          doc.write("table.properties td { font:normal; text-align:left; border-right:solid 1 #cccccc; border-bottom:solid 1 #cccccc; background-color:#fffffff; }");
          doc.write("h3 { margin-bottom: 0.5em; font: bold 115% verdana,arial,helvetica }");
          doc.write("img.OSLCLogo {width: 72px;height:91px;background:url(./oslc_sprite.png) 0px 0px;}");
          doc.write("img.info {width: 9px; height:12px;background:url(./oslc_sprite.png) -74px 0px;}");
      	  doc.write("img.debug {width: 16px;height:15px;background:url(./oslc_sprite.png) -86px 0px;}");
          doc.write("</style>");
          doc.write("</head><body>");
          doc.write("<div width='100%' style='background-color:#687C97;'>");
    	  doc.write("<img class='OSLCLogo' valign='bottom' src='./1px.gif'/>");
    	  doc.write("<h1 style='display:inline;color:#FFFFFF;'>OSLC JUnit Properties</h1>");
    	  doc.write("<table width='100%'>");
    	  doc.write("<tr>");
          doc.write("<td align='left'></td>");
          doc.write("<td align='right'><a style='color:#FFFFFF' href='javascript:window.close();'>Close</a></td>");
    	  doc.write("</tr>");
    	  doc.write("</table>");
		  doc.write("</div>");  
          doc.write("<h3>Properties of " + name + "</h3>");
          doc.write("<table><tr><td>");
          doc.write("<table class='properties'>");
          doc.write("<tr><th>Name</th><th>Value</th></tr>");
          for (prop in TestCases[name]) {
            doc.write("<tr><th>" + prop + "</th><td>" + TestCases[name][prop] + "</td></tr>");
          }
          doc.write("</table></td>");
          doc.write("<td valign='top' align='right'><table width='70%' class='properties'>");
          doc.write("<tr><th width='50%'>Useful Resources</th><th>Description</th></tr>");
          doc.write("<tr><th><a title='OSLC Community Website' href='http://www.oslc.co/'>Open Services for Lifecycle Collaboration (OSLC)</a></th><td>An open community dedicated to making it easier to use lifecycle tools in combination</td></tr>");
		  doc.write("<tr><th><a title='Eclipse Lyo' href='http://eclipse.org/lyo/'>Eclipse Lyo</a></th><td>The Eclipse Lyo project focuses on providing an SDK to help the Eclipse community to adopt OSLC specifications and build OSLC-compliant tools. The source code is available <a href='http://git.eclipse.org/c/?q=lyo'>in a Git repository</a>.</td></tr>");
		  doc.write("<tr><th><a title='Eclipse Lyo Wiki' href='http://wiki.eclipse.org/Lyo'>Eclipse Lyo Wiki</a></th><td>The Eclipse Lyo Wiki focuses on providing documentation and plans that compliment the use of the Eclipse Lyo SDK</td></tr>");
		  doc.write("<tr><th><a title='OSLC Primer' href='http://open-services.net/primer/#primer_main'>OSLC Primer</a></th><td>A primer for technical leaders who want to understand the concepts and goals of OSLC and its relationship to other standards for evaluation, as well as potential OSLC implementers who want a general overview of the OSLC concepts and an understanding of the thinking and use-cases that led to their definition.</td></tr>");
		  doc.write("<tr><th><a title='OSLC Tutorial' href='http://open-services.net/tutorial/#tutorial_main'>OSLC Tutorial</a></th><td>This tutorial explains how to integrate tools with OSLC. The tutorial uses examples, starting with simple ones and building to more advanced topics like implementing an OSLC Provider.</td></tr>");
          doc.write("<tr><th>OSLC Social Media</th><td><a href='http://twitter.com/#!/oslcnews' class='twitter-follow-button'>Follow @oslcNews</a>\x3Cbr/>\x3Cbr/><a href='http://www.linkedin.com/groups/OSLC-Open-Services-Lifecycle-Collaboration-3957829'><img style='vertical-align:bottom;' src='http://open-services.net/images/LinkedIn_IN_Icon_35px.png' alt='OSLC on LinkedIn' height='16' width='16'>&#160;LinkedIn OSLC Discussion</a></td></tr>");
          doc.write("</table></td></tr></table>");
          doc.write("<br/>");
          doc.write("<h3>System Out Trace</h3>");
          doc.write("<table style='border: thin black solid;'>");
          for (prop in SysOutMessages['SystemOut']) {
            doc.write("<tr>" + SysOutMessages['SystemOut'][prop] + "</tr>");
          }
          doc.write("</table>");
          doc.write("<br/>");
          doc.write("<h3>System Err Trace</h3>");
          doc.write("<table style='border: thin black solid;'>");
          for (prop in SysErrMessages['SystemErr']) {
            doc.write("<tr>" + SysErrMessages['SystemErr'][prop] + "</tr>");
          }
          doc.write("</table>");
          doc.write("</body></html>");
          doc.close();
          win.focus();
        }
        
		function init(evt) {
			SVGscale(0.5);
		}
        
        function SVGscale(scale) {
			window.SVGsetDimension(1600*scale,1200*scale);
			window.SVGsetScale(scale,scale);	
       		if (!W3CDOM) return;
			var box 	= document.getElementById('svgid');
			box.width  	= 1600*scale;
			box.height 	= 1200*scale;
		}
      ]]></script> 
        </head>
        <body>

            <a name="top"></a>
            <xsl:call-template name="pageHeader"/>
			
			<!-- Compliance part -->
            <xsl:call-template name="compliancepart"/>
            <hr size="1" width="95%" align="left"/>

            <!-- Summary part -->
            <xsl:call-template name="summary"/>
            <hr size="1" width="95%" align="left"/>

            <!-- Package List part -->
            <xsl:call-template name="packagelist"/>
            <hr size="1" width="95%" align="left"/>

            <!-- For each package create its part -->
            <xsl:call-template name="packages"/>
            <hr size="1" width="95%" align="left"/>

            <!-- For each class create the  part -->
            <xsl:call-template name="classes"/>

        </body>
    </html>
</xsl:template>

    <!-- ================================================================== -->
    <!-- Write a list of all packages with an hyperlink to the anchor of    -->
    <!-- of the package name.                                               -->
    <!-- ================================================================== -->
    <xsl:template name="packagelist">
        <h2>Packages</h2>
        Note: package statistics are not computed recursively, they only sum up all of its testsuites numbers.
        <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
            <xsl:call-template name="testsuite.test.header"/>
            <!-- list all packages recursively -->
            <xsl:for-each select="./testsuite[not(./@package = preceding-sibling::testsuite/@package)]">
                <xsl:sort select="@package"/>
                <xsl:variable name="testsuites-in-package" select="/testsuites/testsuite[./@package = current()/@package]"/>
                <xsl:variable name="testCount" select="sum($testsuites-in-package/@tests)"/>
                <xsl:variable name="errorCount" select="sum($testsuites-in-package/@errors)"/>
                <xsl:variable name="failureCount" select="sum($testsuites-in-package/@failures)"/>
                <xsl:variable name="timeCount" select="sum($testsuites-in-package/@time)"/>

                <!-- write a summary for the package -->
                <tr valign="top">
                    <!-- set a nice color depending if there is an error/failure -->
                    <xsl:attribute name="class">
                        <xsl:choose>
                            <xsl:when test="$failureCount &gt; 0">Failure</xsl:when>
                            <xsl:when test="$errorCount &gt; 0">Error</xsl:when>
                        </xsl:choose>
                    </xsl:attribute>
                    <td><a href="#">OSLC JUnit TestSuite</a></td>
                    <!-- <td><a href="#{@package}"><xsl:value-of select="@package"/></a></td>  -->
                    <td><xsl:value-of select="$testCount"/></td>
                    <td><xsl:value-of select="$errorCount"/></td>
                    <td><xsl:value-of select="$failureCount"/></td>
                    <td>
                    <xsl:call-template name="display-time">
                        <xsl:with-param name="value" select="$timeCount"/>
                    </xsl:call-template>
                    </td>
                    <td><xsl:value-of select="$testsuites-in-package/@timestamp"/></td>
                    <td><xsl:value-of select="$testsuites-in-package/@hostname"/></td>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>


    <!-- ================================================================== -->
    <!-- Write a package level report                                       -->
    <!-- It creates a table with values from the document:                  -->
    <!-- Name | Tests | Errors | Failures | Time                            -->
    <!-- ================================================================== -->
    <xsl:template name="packages">
        <!-- create an anchor to this package name -->
        <xsl:for-each select="/testsuites/testsuite[not(./@package = preceding-sibling::testsuite/@package)]">
            <xsl:sort select="@package"/>
                <a name="{@package}"></a>
                <h3>Package <xsl:value-of select="@package"/></h3>

                <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
                    <xsl:call-template name="testsuite.test.header"/>

                    <!-- match the testsuites of this package -->
                    <xsl:apply-templates select="/testsuites/testsuite[./@package = current()/@package]" mode="print.test"/>
                </table>
                <a href="#top">Back to top</a>
                <p/>
                <p/>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="classes">
        <xsl:for-each select="testsuite">
            <xsl:sort select="@name"/>
            <!-- create an anchor to this class name -->
            <a name="{@name}"></a>
            <h3>TestCase <xsl:value-of select="@name"/></h3>

            <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
              <xsl:call-template name="testcase.test.header"/>
              <!--
              test can even not be started at all (failure to load the class)
              so report the error directly
              -->
                <xsl:if test="./error">
                    <tr class="Error">
                        <td colspan="4"><xsl:apply-templates select="./error"/></td>
                    </tr>
                </xsl:if>
                <xsl:apply-templates select="./testcase" mode="print.test"/>
            </table>
            <div class="Properties">
                <a>
                    <xsl:attribute name="href">javascript:displayProperties('<xsl:value-of select="@package"/>.<xsl:value-of select="@name"/>');</xsl:attribute>
                    Properties &#187;
                </a>
            </div>
            <p/>

            <a href="#top">Back to top</a>
        </xsl:for-each>
    </xsl:template>

	<xsl:template name="compliancepart">
	
	
			<!-- Object Container to render the SVG User Load Preview Graph within the HTML -->
							<span style="text-align:center;display:block;margin: 0 auto;"><object id="svgid" type="image/svg+xml" name="SVGContainer" data="barchartSVG.svg" codebase="http://www.adobe.com/svg/viewer/install/" width="1100" height="350">
 								<param name="src" value="barchartSVG.svg" />
								<param name="wmode" value="transparent"  />
								<embed id="svgid" src="barchartSVG.svg" type="image/svg+xml" width="1100" height="350" wmode="transparent" pluginspage="http://www.adobe.com/svg/viewer/install/" />
							</object></span>
							
						<!--	<a href="#" onclick="SVGscale(0.1);">x-small</a>&#160;&#160;<a href="#" onclick="SVGscale(0.25);">small</a>&#160;&#160;<a href="#" onclick="SVGscale(0.5);">medium</a>&#160;&#160;
							<a href="#" onclick="SVGscale(0.65);">1024x768</a>&#160;&#160;<a href="#" onclick="SVGscale(0.8);">1280x1024</a>&#160;&#160;
        					<a href="#" onclick="SVGscale(1);">1600x1200</a>&#160;&#160;<a href="#" onclick="SVGscale(1.5);">x-large</a> -->
		
        <h2>OSLC compliance</h2>

		<xsl:variable name="mustCount" select="'107'"/>
		<xsl:variable name="junitMustCount" select="'58'"/>
		<xsl:variable name="junitUniqueReqMustCount" select="'51'"/>
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

        <xsl:variable name="domain" select="$spec/provider-test/@domain"/>
        <xsl:variable name="version" select="$spec/provider-test/@version"/>

        <table border="0" cellpadding="5" cellspacing="2" width="40%">
        <tr bgcolor="a6caf0" valign="top">
        	<th>Report Date</th>
        	<th>OSLC Domain</th>
        	<th>Version</th>
			<th>OSLC Service Provider</th>
			<th>Compliance Level</th>
        	<th>Test Coverage Statement</th>
        	<th>Test Development Statement</th>
        </tr>
        <tr valign="top" style="text-align:center;">
        	<td bgcolor="eeeee0"><xsl:value-of select="$date"/></td>
        	<td bgcolor="eeeee0"><xsl:value-of select="$spec/provider-test/@domain"/></td>
        	<td bgcolor="eeeee0"><xsl:value-of select="$spec/provider-test/@version"/></td>
        	<td bgcolor="eeeee0"><xsl:value-of select="substring-after($implName,'=')"/></td>

        	<xsl:choose>
                <xsl:when test="($mustCount=$passedMustCount) and ($shouldCount=$passedShouldCount) and ($mayCount=$passedMayCount)">
					<td bgcolor="#0000ff" style="color:#ffff00;" title="All Must, Should and May Test(s) Passing">Level 3 Compliance</td>
				</xsl:when>
                <xsl:when test="($mustCount=$passedMustCount) and ($shouldCount=$passedShouldCount)">
                	<td bgcolor="#00ff00" title="All Must and Should Test(s) Passing">Level 2 Compliance</td>
                </xsl:when>
                <xsl:when test="($testsuiteMustCount=$passedMustCount)">
                	<td bgcolor="#ffff00" title="All Must Test(s) Passing">Level 1 Compliance</td>
                </xsl:when>
                <xsl:otherwise>
                	<td bgcolor="#ff0000">Non-Compliant</td>
                </xsl:otherwise>
            </xsl:choose>
            <td><div style="color:blue;"><b><xsl:value-of select="format-number($junitMustCount div $mustCount,'#.#%')"/></b> (<xsl:value-of select="$junitMustCount"/>/<xsl:value-of select="$mustCount"/>)</div><xsl:value-of select="$junitMustCount"/> of the <xsl:value-of select="$mustCount"/><xsl:text> </xsl:text><xsl:value-of select="$domain"/><xsl:text> </xsl:text><xsl:value-of select="$version"/> MUST requirements are currently testable via the Lyo OSLC testsuite.</td>
       		<td><div style="color:blue;"><b><xsl:value-of select="format-number($junitUniqueReqMustCount div $junitMustCount,'#.#%')"/></b> (<xsl:value-of select="$junitUniqueReqMustCount"/>/<xsl:value-of select="$junitMustCount"/>)</div><xsl:value-of select="$junitUniqueReqMustCount"/> of the <xsl:value-of select="$junitMustCount"/> testable MUST requirements currently have JUnit test case coverage within the Lyo OSLC testsuite</td>
        </tr>
        </table>
        <table border="0" width="95%">
        <tr>
        <td style="text-align: justify;">
        <span style="background-color:#ff0000;">Non-Compliant</span>: One or more attempted tests covering a MUST requirement has encountered a failure or error<br/>
        <span style="background-color:#ffff00;">Level 1 Compliance</span>: All Attempted Tests covering a MUST requirement are Passing and free of failure or error<br/>
        <span style="background-color:#00ff00;">Level 2 Compliance</span>: All Attempted Tests covering a MUST and SHOULD requirement are Passing and free of failure or error<br/>
        <span style="background-color:#0000ff;color:#ffff00;">Level 3 Compliance</span>: All Attempted Tests covering a MUST, SHOULD and MAY requirement are Passing and free of failure or error<br/>
        Note: This testsuite will continue to evolve and expand.  Requirements may have one or more associated test(s) for coverage to address positive and negative input behaviors.
        </td>
        </tr>
        </table>
		<br/>
		<h2>OSLC TestSuite Execution Summary</h2>
        <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
        <tr valign="top">
            <th>Spec Type</th>
            <th>Attempted</th>
            <th>Passed</th>
            <th>Failed</th>
            <th>Error</th>
        </tr>
        <tr valign="top" style="align:center;">
            <td>MUSTs</td>
            <td><xsl:value-of select="$testsuiteMustCount"/></td>
            <td><xsl:value-of select="$passedMustCount"/></td>
            <td><xsl:value-of select="$failedMustCount"/></td>
            <td><xsl:value-of select="$errorMustCount"/></td>
        </tr>
        </table>
        
        <table border="0" width="95%">
        <tr>
        <td style="text-align: justify;">
        Attempted = Pass + Fail + Error.  # of Tests Executed for a Specification Type<br/>
        Pass: # of Test(s) achieving the respective test design's expected result<br/>
        Fail: # of Test(s) deviating from the respective test design's expected result.<br/>
        Error: # of Inconclusive Results.  Test executions encountering error due to poor test design, faulty environment or invalid configuration.  Test results could not be assessed.
        </td>
        </tr>
        </table>
        
        <div class="Properties">
                <a>
                    <xsl:attribute name="href">javascript:displayProperties('<xsl:value-of select="/testsuites/testsuite/properties/property[@name='props']/@value"/>');</xsl:attribute>
                    OSLC Configuration Properties &#187;
                </a>
        </div>

    </xsl:template>


    <xsl:template name="summary">
        <h2>Unit Test Summary</h2>
        <xsl:variable name="testCount" select="sum(testsuite/@tests)"/>
        <xsl:variable name="errorCount" select="sum(testsuite/@errors)"/>
        <xsl:variable name="failureCount" select="sum(testsuite/@failures)"/>
        <xsl:variable name="timeCount" select="sum(testsuite/@time)"/>
        <xsl:variable name="successRate" select="($testCount - $failureCount - $errorCount) div $testCount"/>
        <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
        <tr valign="top">
            <th>Tests</th>
            <th>Failures</th>
            <th>Errors</th>
            <th>Success rate</th>
            <th>Time</th>
        </tr>
        <tr valign="top">
            <xsl:attribute name="class">
                <xsl:choose>
                    <xsl:when test="$failureCount &gt; 0">Failure</xsl:when>
                    <xsl:when test="$errorCount &gt; 0">Error</xsl:when>
                </xsl:choose>
            </xsl:attribute>
            <td><xsl:value-of select="$testCount"/></td>
            <td><xsl:value-of select="$failureCount"/></td>
            <td><xsl:value-of select="$errorCount"/></td>
            <td>
                <xsl:call-template name="display-percent">
                    <xsl:with-param name="value" select="$successRate"/>
                </xsl:call-template>
            </td>
            <td>
                <xsl:call-template name="display-time">
                    <xsl:with-param name="value" select="$timeCount"/>
                </xsl:call-template>
            </td>

        </tr>
        </table>
        <table border="0" width="95%">
        <tr>
        <td style="text-align: justify;">
        Note: <i>failures</i> are anticipated and checked for with assertions while <i>errors</i> are unanticipated.
        </td>
        </tr>
        </table>
    </xsl:template>

  <!--
   Write properties into a JavaScript data structure.
   This is based on the original idea by Erik Hatcher (ehatcher@apache.org)
   -->
  <xsl:template match="properties">
    cur = TestCases['<xsl:value-of select="../@package"/>.<xsl:value-of select="../@name"/>'] = new Array();
    <xsl:for-each select="property">
    <xsl:sort select="@name"/>
        cur['<xsl:value-of select="@name"/>'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="@value"/></xsl:call-template>';
    </xsl:for-each>
  </xsl:template>

<!-- Page HEADER -->
<xsl:template name="pageHeader">
<div width="100%" style="background-color:#687C97;">
    <img class="OSLCLogo" valign="bottom" src="./1px.gif"/>
    <h1 style="display:inline;color:#FFFFFF;"><xsl:value-of select="$TITLE"/></h1>
    <table width="100%">
    <tr>
        <td align="left"></td>
        <td align="right" style="color:#FFFFFF;">Designed for use with <a style="color:#FFFFFF" href='http://www.junit.org'>JUnit</a> and <a style="color:#FFFFFF" href='http://ant.apache.org/ant'>Ant</a>.</td>
    </tr>
    </table>
</div>
    <hr size="1"/> 
</xsl:template>

<xsl:template match="testsuite" mode="header">
    <tr valign="top">
        <th width="80%">Name</th>
        <th>Tests</th>
        <th>Errors</th>
        <th>Failures</th>
        <th nowrap="nowrap">Time(s)</th>
    </tr>
</xsl:template>

<!-- class header -->
<xsl:template name="testsuite.test.header">
    <tr valign="top">
        <th width="80%">Name</th>
        <th>Tests</th>
        <th>Errors</th>
        <th>Failures</th>
        <th nowrap="nowrap">Time(s)</th>
        <th nowrap="nowrap">Time Stamp</th>
        <th>Host</th>
    </tr>
</xsl:template>

<!-- method header -->
<xsl:template name="testcase.test.header">
    <tr valign="top">
        <th>Name</th>
        <th>Status</th>
        <th>Spec</th>
        <th width="80%">Type</th>
        <th nowrap="nowrap">Time(s)</th>
    </tr>
</xsl:template>


<!-- class information -->
<xsl:template match="testsuite" mode="print.test">
    <tr valign="top">
        <!-- set a nice color depending if there is an error/failure -->
        <xsl:attribute name="class">
            <xsl:choose>
                <xsl:when test="@failures[.&gt; 0]">Failure</xsl:when>
                <xsl:when test="@errors[.&gt; 0]">Error</xsl:when>
            </xsl:choose>
        </xsl:attribute>

        <!-- print testsuite information -->
        <td><a href="#{@name}"><xsl:value-of select="@name"/></a></td>
        <td><xsl:value-of select="@tests"/></td>
        <td><xsl:value-of select="@errors"/></td>
        <td><xsl:value-of select="@failures"/></td>
        <td>
            <xsl:call-template name="display-time">
                <xsl:with-param name="value" select="@time"/>
            </xsl:call-template>
        </td>
        <td><xsl:apply-templates select="@timestamp"/></td>
        <td><xsl:apply-templates select="@hostname"/></td>
    </tr>
</xsl:template>

<xsl:template match="testcase" mode="print.test">
	<xsl:variable name="length" select="string-length(@name)"/>
	<xsl:variable name="thiscase" select="substring(@name, 0, $length - 2)"/>
    <tr valign="top">
        <xsl:attribute name="class">
            <xsl:choose>
                <xsl:when test="failure | error">Error</xsl:when>
            </xsl:choose>
        </xsl:attribute>
        <td><xsl:value-of select="@name"/></td>
        <xsl:choose>
            <xsl:when test="failure">
                <td>Failure</td>
                <td><xsl:value-of select="$spec//testcase[.=$thiscase]/@level"/></td>
                <td><xsl:apply-templates select="failure"/></td>
            </xsl:when>
            <xsl:when test="error">
                <td>Error</td>
                <td><xsl:value-of select="$spec//testcase[.=$thiscase]/@level"/></td>
                <td><xsl:apply-templates select="error"/></td>
            </xsl:when>
            <xsl:otherwise>
                <td>Success</td>
                <td><xsl:value-of select="$spec//testcase[.=$thiscase]/@level"/></td>
                <td></td>
            </xsl:otherwise>
        </xsl:choose>
        <td>
            <xsl:call-template name="display-time">
                <xsl:with-param name="value" select="@time"/>
            </xsl:call-template>
        </td>
    </tr>
</xsl:template>


<xsl:template match="failure">
    <xsl:call-template name="display-failures"/>
</xsl:template>

<xsl:template match="error">
    <xsl:call-template name="display-failures"/>
</xsl:template>

<!-- Style for the error and failure in the tescase template -->
<xsl:template name="display-failures">
    <xsl:choose>
        <xsl:when test="not(@message)">N/A</xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="@message"/>
        </xsl:otherwise>
    </xsl:choose>
  
    <!-- display the stacktrace -->
    <code>
        <br/><br/>
        <xsl:call-template name="br-replace">
            <xsl:with-param name="word" select="."/>
        </xsl:call-template>
    </code>
    
    <!-- the later is better but might be problematic for non-21" monitors... -->
    <!--pre><xsl:value-of select="."/></pre-->
    <xsl:variable name="currentMessage" select="@message"/>
    <xsl:variable name="currentStackTrace">
    	<xsl:call-template name="br-replace">
            <xsl:with-param name="word" select="."/>
        </xsl:call-template>
    </xsl:variable>
	<xsl:for-each select="$troubleshoot/provider-test/spec[translate(@domain,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')=translate($spec/provider-test/@domain,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')]/error">
	<xsl:variable name="scannedMessage" select="scan:findWithinHorizon(scan:new(normalize-space($currentMessage)),string(./@regexp),0)"/>
	<xsl:variable name="scannedStackTrace" select="scan:findWithinHorizon(scan:new(normalize-space($currentStackTrace)),string(./@regexp),0)"/>
	<xsl:choose>
     	<!--  Checking for failure match-->
       <xsl:when test="(string-length($scannedMessage) > 0) or (string-length($scannedStackTrace) > 0)">
       <br/>
	    <span style="color:#0066CC;"><img class="troubleshoot" title="OSLC Smart Tip" alt="OSLC Troubleshooting" src='./1px.gif' style='vertical-align:bottom;'/><xsl:text> </xsl:text><u>Root Cause:</u><xsl:text>  </xsl:text><xsl:value-of select="./rootcause"/><br/><br/>
	    <img class="troubleshoot" title="OSLC Smart Tip" alt="OSLC Troubleshooting" src='./1px.gif' style='vertical-align:bottom;'/><xsl:text> </xsl:text><u>Reaction Plan:</u>
	    <UL>
	    	<xsl:for-each select="./action">
	    		<xsl:variable name="lowercaseAction" select="translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')"/>
	    		<xsl:choose>
	    			<xsl:when test="contains($lowercaseAction,'baseuri')">
	    				<LI>
	    					<xsl:value-of select="substring-before(.,'baseUri')"/>
	    					<a target="_new">
	    						<xsl:attribute name="href"><xsl:value-of select="substring-after($baseUri,'=')"/></xsl:attribute>baseUri
	    					</a><xsl:text> </xsl:text>(<xsl:value-of select="substring-after($baseUri,'=')"/>)
	    					<xsl:value-of select="substring-after(.,'baseUri')"/>
	    				</LI>
	    			</xsl:when>
	    			<xsl:otherwise>
	    				<LI><xsl:value-of select="."/></LI>
	    			</xsl:otherwise>
	    		</xsl:choose>

	    	</xsl:for-each>
	    </UL>
	    </span>
     </xsl:when>
   </xsl:choose>
   </xsl:for-each>
    
</xsl:template>

<xsl:template name="JS-escape">
    <xsl:param name="string"/>
    <xsl:param name="tmp1" select="stringutils:replace(string($string),'\','\\')"/>
    <xsl:param name="tmp2" select="stringutils:replace(string($tmp1),&quot;'&quot;,&quot;\&apos;&quot;)"/>
    <xsl:param name="tmp3" select="stringutils:replace(string($tmp2),&quot;&#10;&quot;,'\n')"/>
 	<xsl:param name="tmp4" select="stringutils:replace(string($tmp3),&quot;&#13;&quot;,'\r')"/>
 	<xsl:param name="tmp5" select="stringutils:replace(string($tmp4),&quot;&#xa;&quot;,'\n')"/>
 	
 	<xsl:choose>
 	<xsl:when test="starts-with($tmp5,'#')">
 		 	<span style="color:gray;"><xsl:value-of select="concat('# ',substring-after($tmp5,'='))"/></span>
 	</xsl:when>
 	<xsl:otherwise>
 		<xsl:choose>
 			<xsl:when test="contains($tmp5,'System Out')">
 				<xsl:value-of select="$tmp5"/>
 			</xsl:when>
 			 <xsl:when test="contains($tmp5,'System Err')">
 				<xsl:value-of select="$tmp5"/>
 			</xsl:when>
 			<xsl:when test="contains($tmp5,'=')">
 				<xsl:value-of select="substring-after($tmp5,'=')"/>
 			</xsl:when>
 			<xsl:otherwise>
 				<xsl:value-of select="$tmp5"/>
 			</xsl:otherwise>	
 		</xsl:choose>
 	</xsl:otherwise>
 	</xsl:choose>

</xsl:template>


<!--
    template that will convert a carriage return into a br tag
    @param word the text from which to convert CR to BR tag
-->
<xsl:template name="br-replace">
    <xsl:param name="word"/>
    <xsl:value-of disable-output-escaping="yes" select='stringutils:replace(string($word),"&#xA;","&lt;br/>")'/>
</xsl:template>

<xsl:template name="display-time">
    <xsl:param name="value"/>
    <xsl:value-of select="format-number($value,'0.000')"/>
</xsl:template>

<xsl:template name="display-percent">
    <xsl:param name="value"/>
    <xsl:value-of select="format-number($value,'0.00%')"/>
</xsl:template>

<xsl:template name="substring-after-last">
  <xsl:param name="string" />
  <xsl:param name="delimiter" />
  <xsl:choose>
    <xsl:when test="contains($string, $delimiter)">
      	<xsl:call-template name="substring-after-last">
       		<xsl:with-param name="string" select="substring-after($string, $delimiter)" />
        		<xsl:with-param name="delimiter" select="$delimiter" />
      	</xsl:call-template>
    </xsl:when>
    <xsl:otherwise><xsl:value-of select="$string"/></xsl:otherwise>
  </xsl:choose>
</xsl:template>

  <xsl:template name="OSLCProperties">
  
  	<!-- OSLC Config Value Pairs -->
	cur = TestCases['<xsl:value-of select="/testsuites/testsuite/properties/property[@name='props']/@value"/>'] = new Array();
	cur['baseUri'] = '<a href="{substring-after($baseUri,'=')}" target="_new"><xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$baseUri"/></xsl:call-template></a>';
	cur['useThisServiceProvider'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$useThisServiceProvider"/></xsl:call-template>';
	cur['implName'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$implName"/></xsl:call-template>';
	cur['authMethod'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$authMethod"/></xsl:call-template>';
	cur['formUri'] = '<a href="{substring-after($formUri,'=')}" target="_new"><xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$formUri"/></xsl:call-template></a>';
	cur['userId'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$userId"/></xsl:call-template>';
	cur['pw'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$pw"/></xsl:call-template>';
	cur['testVersions'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$testVersions"/></xsl:call-template>';
	cur['runOnlyOnce'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$runOnlyOnce"/></xsl:call-template>';
	cur['queryEqualityProperty'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$queryEqualityProperty"/></xsl:call-template>';
	cur['queryEqualityValue'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$queryEqualityValue"/></xsl:call-template>';
	cur['queryComparisonProperty'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$queryComparisonProperty"/></xsl:call-template>';
	cur['queryComparisonValue'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$queryComparisonValue"/></xsl:call-template>';
	cur['queryAdditionalParameters'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$queryAdditionalParameters"/></xsl:call-template>';
	cur['fullTextSearchTerm'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$fullTextSearchTerm"/></xsl:call-template>';
	cur['createTemplateXmlFile'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$createTemplateXmlFile"/></xsl:call-template>';
	cur['createTemplateRdfXmlFile'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$createTemplateRdfXmlFile"/></xsl:call-template>';
	cur['createTemplateJsonFile'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$createTemplateJsonFile"/></xsl:call-template>';
	cur['updateTemplateXmlFile'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$updateTemplateXmlFile"/></xsl:call-template>';
	cur['updateTemplateRdfXmlFile'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$updateTemplateRdfXmlFile"/></xsl:call-template>';
	cur['updateTemplateJsonFile'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$updateTemplateJsonFile"/></xsl:call-template>';
	cur['updateParams'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$updateParams"/></xsl:call-template>';
	cur['OAuthRequestTokenUrl'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$OAuthRequestTokenUrl"/></xsl:call-template>';
	cur['OAuthAuthorizationUrl'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$OAuthAuthorizationUrl"/></xsl:call-template>';
	cur['OAuthAuthorizationParameters'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$OAuthAuthorizationParameters"/></xsl:call-template>';
	cur['OAuthAccessTokenUrl'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$OAuthAccessTokenUrl"/></xsl:call-template>';
	cur['OAuthConsumerToken'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$OAuthConsumerToken"/></xsl:call-template>';
	cur['OAuthConsumerSecret'] = '<xsl:call-template name="JS-escape"><xsl:with-param name="string" select="$OAuthConsumerSecret"/></xsl:call-template>';
  </xsl:template>
  
  <xsl:template name="systemOut">
  	curSysOut = SysOutMessages['SystemOut'] = new Array();
   		<xsl:for-each select="//system-out[string-length(normalize-space(.)) > 0]">
   	curSysOut['<xsl:value-of select="position()"/>'] = '<td><img class='info' style='vertical-align:bottom;' src='./1px.gif'/><xsl:text> </xsl:text><xsl:call-template name="JS-escape"><xsl:with-param name="string" select="concat('System Out: ',.)"/></xsl:call-template></td>';
   		</xsl:for-each>
  </xsl:template>
  
    <xsl:template name="systemErr">
  	curSysErr = SysErrMessages['SystemErr'] = new Array();
   		<xsl:for-each select="//system-err[string-length(normalize-space(.)) > 0]">
   				<xsl:choose>
   					<xsl:when test="position() mod 2 = 1">
   	curSysErr['<xsl:value-of select="position()"/>'] = '<td style="background-color:#F8E8E7;"><img class='debug' style='vertical-align:bottom;' src='./1px.gif'/><xsl:text> </xsl:text><xsl:call-template name="JS-escape"><xsl:with-param name="string" select="concat('System Err: ',.)"/></xsl:call-template></td>';				
   					</xsl:when>
   					<xsl:otherwise>
   	curSysErr['<xsl:value-of select="position()"/>'] = '<td><img class='debug' style='vertical-align:bottom;' src='./1px.gif'/><xsl:text> </xsl:text><xsl:call-template name="JS-escape"><xsl:with-param name="string" select="concat('System Err: ',.)"/></xsl:call-template></td>';
   					</xsl:otherwise>
   				</xsl:choose>
   		</xsl:for-each>
  </xsl:template>



</xsl:stylesheet>
