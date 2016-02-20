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
            <h2><spring:message code="page.skin.title.info"/></h2>
        </div>
        <table id="tablelist" class="hero-unit" style="width: 90%; margin: auto;">
        <tbody>
            <tr>
                <th style="width:10%"><spring:message code="page.skin.label.skin.name"/></th>
                <th style="width:5%" ><spring:message code="page.skin.label.skin.version"/></th>
                <th style="width:15%"><spring:message code="page.skin.label.skin.image"/></th>
                <th style="width:30%"><spring:message code="page.skin.label.skin.description"/></th>
                <th style="width:20%"><spring:message code="page.skin.label.skin.location"/></th>
                <th style="width:10%"><spring:message code="page.skin.label.skin.support"/></th>
            </tr>
            <c:forEach items="${skins}" var="skin">
                <tr>
                    <td>${skin.name}</td>
                    <td>${skin.version}</td>
                    <td class="center">
                        <c:choose>
                            <c:when test="${empty skin.image}">
                                <spring:message code="page.skin.no.image"/>
                            </c:when>
                            <c:otherwise>
                                <a href="${yi.skinDir}${skin.path}/${skin.image}" target="_blank">
                                    <img alt="Skin image" src="${yi.skinDir}${skin.path}/${skin.image}" width="200"/>
                                </a>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:forEach items="${skin.description}" var="line">
                            ${line}<br>
                        </c:forEach>
                    </td>
                    <td><a href="${yi.skinDir}${skin.path}/" target="_blank">${skin.path}</a></td>
                    <td class="center">
                        <c:choose>
                            <c:when test="${empty skin.supportUrl}">
                                <spring:message code="page.skin.no.url"/>
                            </c:when>
                            <c:otherwise>
                                <a href="${skin.supportUrl}" target="_blank"><spring:message code="button.click.here"/></a>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
        </table>
        <br>
        <form:form  method="POST" commandName="skin-entity" action="skin-download.html">
            <table id="headertable" class="hero-unit" style="width:40%; margin: auto;">
                <tbody>
                    <tr>
                        <td class="right"><label for="sourceUrl"><b><spring:message code="page.skin.label.download.url"/>:</b></label></td>
                        <td class="center"><input class="span4" id="sourceUrl" name="sourceUrl" type="text" value="" size="90"></td>
                    </tr>
                    <tr>
                        <td colspan="2" class="center"><input type="submit" value="<spring:message code="page.skin.button.add.skin"/>" class="btn info"></td>
                    </tr>
                </tbody>
            </table>
        </form:form>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
