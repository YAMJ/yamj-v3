<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<!DOCTYPE html>
<html>
    <head>
        <title>YAMJ v3</title>
        <!--Import the header details-->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="HEAD" />
        </c:import>
    </head>
    <body>
        <!--Import the navigation header-->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="NAV" />
        </c:import>

        <div id="logo">
            <h2><spring:message code="title.trakttv"/></h2>
        </div>
        
        <c:if test="${!trakttv.synchronization}">
        <table id="tablelist" class="hero-unit" style="width: 40%; margin: auto;">
            <tr>
                <th class="left"><spring:message code="page.trakttv.sync.disabled"/></th>
            </tr>
        </table>
        </c:if>        
        <c:if test="${trakttv.synchronization}">
        <table id="headertable" class="hero-unit" style="width: 40%; margin: auto;">
            <tr>
                <th colspan="2">
                    <c:if test="${trakttv.push && trakttv.pull}">
                        <spring:message code="page.trakttv.sync.push.pull"/>
                       </c:if> 
                    <c:if test="${trakttv.push && !trakttv.pull}">
                        <spring:message code="page.trakttv.sync.push"/>
                    </c:if> 
                    <c:if test="${!trakttv.push && trakttv.pull}">
                        <spring:message code="page.trakttv.sync.pull"/>
                    </c:if>
                    <c:if test="${!trakttv.push && !trakttv.pull}">
                        <spring:message code="page.trakttv.sync.enabled"/>
                   </c:if>
                </th>
            </tr>
            <tr>
                <td style="width:20%" class="left"><spring:message code="page.trakttv.label.authorized"/>:</td>
                <td style="width:80%" class="left">${trakttv.authorized}</td>
            </tr>
            <tr>
                <td style="width:20%" class="left"><spring:message code="page.trakttv.label.expiration.date"/>:</td>
                <td style="width:80%" class="left">${trakttv.expirationDate}</td>
            </tr>
        </table>
        <p>&nbsp;</p>
        <form:form  method="POST" commandName="pin-entity" action="trakttv-pin.html">
            <table id="headertable" class="hero-unit" style="width:40%; margin: auto;">
                <tbody>
                    <tr>
                        <td colspan="2" class="center">
                            <spring:message code="page.trakttv.text.request.pin"/>:<br>
                            <a href="http://trakt.tv/pin/8032" target="_blank">http://trakt.tv/pin/8032</a>
                        </td>
                    </tr>
                    <tr>
                        <td class="right"><label for="pin"><b><spring:message code="page.trakttv.label.pin"/></b></label></td>
                        <td class="center"><input class="span4" id="pin" name="pin" type="text" value="" size="20"></td>
                    </tr>
                    <tr>
                        <td colspan="2" class="center">
                            <input type="submit" value="<spring:message code="page.trakttv.button.authorize.pin"/>" class="btn info"><br>
                        </td>
                    </tr>
                </tbody>
                <tfoot>
                    <tr>
                        <td colspan="2" class="center">
                            <c:if test="${errorMessage != null}">
                            <span id="messageError" style="align:right">${errorMessage}</span>
                            </c:if>
                            <c:if test="${successMessage != null}">
                            <span id="messageSuccess" style="align:right">${successMessage}</span>
                            </c:if>
                        </td>
                    </tr>
                </tfoot>
            </table>
        </form:form>        
        </c:if>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
