<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:lxslt="http://xml.apache.org/xslt"
        xmlns:stringutils="xalan://org.apache.tools.ant.util.StringUtils">
<xsl:output method="xml" indent="yes" encoding="UTF-8"
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
<xsl:variable name="spec" select="document('../oslc-spec-mapping/oslc-cm-v2.xml')"/>

<xsl:template match="testsuites">
	<xsl:copy>
		<xsl:apply-templates/>
	</xsl:copy>
</xsl:template>

<xsl:template match="testsuite">
	<xsl:copy>
		<xsl:attribute name="errors"><xsl:value-of select="@errors"/></xsl:attribute>
		<xsl:attribute name="failures"><xsl:value-of select="@failures"/></xsl:attribute>
		<xsl:attribute name="hostname"><xsl:value-of select="@hostname"/></xsl:attribute>
		<xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
		<xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
		<xsl:attribute name="package"><xsl:value-of select="@package"/></xsl:attribute>
		<xsl:attribute name="tests"><xsl:value-of select="@tests"/></xsl:attribute>
		<xsl:attribute name="time"><xsl:value-of select="@time"/></xsl:attribute>
		<xsl:attribute name="timestamp"><xsl:value-of select="@timestamp"/></xsl:attribute>
		<xsl:apply-templates/>
	</xsl:copy>
</xsl:template>

<xsl:template match="properties">
	<xsl:copy-of select="."></xsl:copy-of>
</xsl:template>

<xsl:template match="testcase">
	<xsl:copy>
		<xsl:variable name="length" select="string-length(@name)"/>
		<xsl:variable name="thiscase" select="substring(@name, 0, $length - 2)"/>
		<xsl:variable name="thiscaseFull" select="@name"/>
		<xsl:variable name="thisPackage" select="@classname"/>
		<xsl:variable name="level" select="$spec//testclass[contains($thisPackage,./@name)]/testcase[.=$thiscase]/@level"/>
				
		<xsl:attribute name="classname"><xsl:value-of select="@classname"/></xsl:attribute>
		<xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
		<xsl:attribute name="time"><xsl:value-of select="@time"/></xsl:attribute>

		<xsl:choose>
			<xsl:when test="error">
		    	<xsl:choose>
				    <xsl:when test="$level='MUST'">
						<xsl:attribute name="compliance">failedMust</xsl:attribute>
					</xsl:when>
				    <xsl:when test="$level='SHOULD'">
				    	<xsl:attribute name="compliance">failedShould</xsl:attribute>
				    </xsl:when>
				    <xsl:otherwise>
						<xsl:attribute name="compliance">failedMay</xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
		    <xsl:when test="failure">
		    	<xsl:choose>
				    <xsl:when test="$level='MUST'">
						<xsl:attribute name="compliance">failedMust</xsl:attribute>
					</xsl:when>
				    <xsl:when test="$level='SHOULD'">
				    	<xsl:attribute name="compliance">failedShould</xsl:attribute>
				    </xsl:when>
				    <xsl:otherwise>
						<xsl:attribute name="compliance">failedMay</xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>		    
		    </xsl:when>
		    <xsl:otherwise>
		    	<xsl:choose>
				    <xsl:when test="$level='MUST'">
					    <xsl:if test = "contains($thiscaseFull,'[0]')">
							<xsl:attribute name="compliance">passedMust</xsl:attribute>
						</xsl:if>
					</xsl:when>
				    <xsl:when test="$level='SHOULD'">
				    	<xsl:attribute name="compliance">passedShould</xsl:attribute>
				    </xsl:when>
				    <xsl:otherwise>
						<xsl:attribute name="compliance">passedMay</xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
		    </xsl:otherwise>
		  </xsl:choose>
	  
		<xsl:apply-templates/>
	</xsl:copy>
</xsl:template>

<xsl:template match="failure">
	<xsl:copy-of select="."></xsl:copy-of>
</xsl:template>

<xsl:template match="error">
	<xsl:copy-of select="."></xsl:copy-of>
</xsl:template>

<xsl:template match="system-out">
	<xsl:copy-of select="."></xsl:copy-of>
</xsl:template>

<xsl:template match="system-err">
	<xsl:copy-of select="."></xsl:copy-of>
</xsl:template>

</xsl:stylesheet>
