<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/yamj-style.css">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title id="title">YAMJ v3</title>
    </head>
    <body background="${pageContext.request.contextPath}/images/yamj-configbg.jpg">
        <h1>Skin Download Page</h1>

        <c:if test="${not empty message}">
            <p>Message: ${message}</p>
            <br/>
        </c:if>
        <br>
        <table style="width: 60%">
            <tbody>
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
                                <img src="${yi.skinDir}${skin.path}/${skin.image}" width="400"/>
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
            </tbody>
        </table>

        <p><a href="/yamj3/skin-info.html">Skins</a></p>
        <p><a href="/yamj3/">Home</a></p>

        <br>
        <br>
        <br>
        <table class="sysinfo">
            <tr>
                <th>Version</th>
                <th>Revision</th>
                <th>Java</th>
                <th>Build Date</th>
                <th>Start-up Time</th>
                <th>Uptime</th>
            </tr>
            <tr>
                <td>${yi.projectVersion}</td>
                <td>${yi.buildRevision}</td>
                <td>${yi.javaVersion}</td>
                <td>${yi.buildDate}</td>
                <td>${yi.startUpTime}</td>
                <td>${yi.uptime}</td>
            </tr>
        </table>

    </body>
</html>
