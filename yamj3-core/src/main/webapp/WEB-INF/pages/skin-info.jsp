<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/yamj-style.css">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title id="title">YAMJ v3</title>
    </head>
    <body background="${pageContext.request.contextPath}/images/yamj-configbg.jpg">
        <div id="logo">
            <h1>${yi.projectName}</h1>
            <h2>Skin Listing</h2>
        </div>
        <table id="tablelist" style="width: 90%; margin: auto;">
            <tr>
                <th style="width:10%">Skin Name</th>
                <th style="width:10%">Version</th>
                <th style="width:20%">Image</th>
                <th style="width:30%">Skin Description</th>
                <th style="width:20%">Location</th>
            </tr>
            <c:forEach items="${skins}" var="skin">
                <tr>
                    <td>${skin.name}</td>
                    <td>${skin.version}</td>
                    <td class="center">
                        <c:choose>
                            <c:when test="${empty skin.image}">
                                No Image
                            </c:when>
                            <c:otherwise>
                                <a href="${yi.skinDir}${skin.path}/${skin.image}" target="_blank">
                                    <img src="${yi.skinDir}${skin.path}/${skin.image}" width="200"/>
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
                </tr>
            </c:forEach>
        </table>
        <br>
        <form:form  method="POST" commandName="skin-entity" action="skin-download.html">
            <table id="headertable" style="width:60%; margin: auto;">
                <tbody>
                    <tr>
                        <td class="right"><label for="sourceUrl">URL to download:</label></td>
                        <td class="center"><input id="sourceUrl" name="sourceUrl" type="text" value="" size="90"></td>
                    </tr>
                    <tr>
                        <td colspan="2" class="center"><input type="submit" value="Add skin"></td>
                    </tr>
                </tbody>
            </table>
        </form:form>

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
