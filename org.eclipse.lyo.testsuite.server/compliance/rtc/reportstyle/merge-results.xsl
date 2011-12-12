<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:lxslt="http://xml.apache.org/xslt"
        xmlns:stringutils="xalan://org.apache.tools.ant.util.StringUtils"
        xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:file="xalan://java.io.File"
		extension-element-prefixes="file">

<!--  Java Function Reference article:  http://www.ibm.com/developerworks/xml/library/x-xalanextensions/index.html -->
<xsl:output method="xml" indent="yes" encoding="UTF-8"
  doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" />
<xsl:decimal-format decimal-separator="." grouping-separator="," />


<xsl:template match="/">
	<xsl:element name="testsuites">
		<xsl:apply-templates/>
	</xsl:element>
</xsl:template>

<xsl:template match="testclass">
	<xsl:variable name="classname" select="concat('.', @name)"/>
	<xsl:variable name="qname" select="concat(@package,$classname)"/>
	<xsl:variable name="filename" select="concat($qname,'.xml')"/>
	<xsl:variable name="resultname" select="concat('TEST-',$filename)"/>
	<xsl:variable name="pathname" select="concat('../junit/',$resultname)"/>

		<!--  Must test for file existence since not all tests run for all target providers and versions -->
		<xsl:if test="file:exists(file:new('junit',$resultname))">
			<xsl:copy-of select="document($pathname)"/>
		</xsl:if>
</xsl:template>

</xsl:stylesheet>
