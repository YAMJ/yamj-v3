<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
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
            <h2>Skin Download Page</h2>
        </div>
        <table class="headertable" style="width: 90%; margin:auto;">
            <tr>
                <th style="width: 30%">Skin Name</th>
                <th style="width: 10%">Skin Version</th>
                <th style="width: 10%">Skin Date</th>
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
                            No Image
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
                <th style="width: 10%">Path</th>
                <td style="width: 40%" colspan="2">${skin.path}</td>
            </tr>
            <tr>
                <th style="width: 10%">Source Url</th>
                <td style="width: 40%" colspan="2">${skin.sourceUrl}</td>
            </tr>
        </table>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
