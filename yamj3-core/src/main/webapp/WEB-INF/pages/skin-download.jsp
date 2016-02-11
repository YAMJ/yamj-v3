<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
            <h2><spring:message code="page.skin.title.download"/></h2>
        </div>
        <table class="headertable" style="width: 90%; margin:auto;">
            <tr>
                <th style="width: 30%"><spring:message code="page.skin.label.skin.name"/></th>
                <th style="width: 10%"><spring:message code="page.skin.label.skin.version"/></th>
                <th style="width: 10%"><spring:message code="page.skin.label.skin.date"/></th>
            </tr>
            <tr>
                <td>${skin.name}</td>
                <td>${skin.version}</td>
                <td>${skin.skinDate}</td>
            </tr>
            <tr>
                <td colspan="3">
                    <c:choose>
                        <c:when test="${empty skin.image}">
                            <spring:message code="page.skin.no.image"/>
                        </c:when>
                        <c:otherwise>
                            <a href="${yi.skinDir}${skin.path}/${skin.image}" target="_blank">
                                <img src="${yi.skinDir}${skin.path}/${skin.image}" width="400"/>
                            </a>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
            <tr>
                <td colspan="3">
                    <c:forEach items="${skin.description}" var="line">
                        ${line}<br>
                    </c:forEach>
                </td>
            </tr>
            <tr>
                <th style="width: 10%"><spring:message code="page.skin.label.skin.location"/></th>
                <td style="width: 40%" colspan="2">${skin.path}</td>
            </tr>
            <tr>
                <th style="width: 10%"><spring:message code="page.skin.label.skin.source"/></th>
                <td style="width: 40%" colspan="2">${skin.sourceUrl}</td>
            </tr>
            <tr>
                <th style="width: 10%"><spring:message code="page.skin.label.skin.support"/></th>
                <td style="width: 40%" colspan="2">${skin.supportUrl}</td>
            </tr>
        </table>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
