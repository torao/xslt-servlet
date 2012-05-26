# XSL Servlet

## Introduction

The servlet implementation to transform static xml or xhtml files with XSL in server-side and send result to browser.
This make you easy to maintainance your design. 

* Environment: Java SE 6 or later, Servlet API 2.5 (Java EE 5) or later; such as Tomcat 6.0 (SuSE Linux 11, MacOS X 10.5)
* Libraries: No additional library dependencies, using JDK-Standard Java Logging API

## Feature

* Server-side XSL transform for static xml files. You can easy to maintainance your site design.
* Cache transform result for performance. You will get same performance as static file access after second.
* Support GZIP compression, If-Modified-Since
* Additionan custom handler for complex transformation such as syntax-hilight.

# Getting Started
You may add this XSLT Servlet on your Web Application as follows:

    <!-- web.xml -->
    <servlet>
      <servlet-name>XSLTServlet</servlet-name>
      <servlet-class>org.koiroha.kwt.xsl.XSLTServlet</servlet-class>
      <init-param>
        <param-name>default-xsl-uri</param-name>
        <param-value>/style/default.xsl</param-value>
      </init-param>
    </servlet>
    <servlet-mapping>
      <servlet-name>XSLTServlet</servlet-name>
      <url-pattern>*.xhtml</url-pattern>
    </servlet-mapping>

http://www.koiroha.org/kwt/xsl/index.xhtml

# Reference

* [API Reference](http://www.koiroha.org/kwt/api/)

# License
This module, contains source code, binary and documentation, is in the BSD License, and comes with NO WARRANTY
