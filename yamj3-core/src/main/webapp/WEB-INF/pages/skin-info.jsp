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

            <table id="tablelist" style="width: 90%; align:center;">
                <tr>
                    <th colspan="4">Skin listing</th>
                </tr>
                <tr>
                    <th style="width:20%">Skin Name</th>
                    <th style="width:10%">Image</th>
                    <th style="width:30%">Skin Description</th>
                    <th style="width:20%">Location</th>
                </tr>
                <c:forEach items="${skins}" var="skin">
                    <tr>
                        <td>${skin.name}</td>
                        <td>
                            <c:choose>
                                <c:when test="${empty skin.image}">
                                    No Image
                                </c:when>
                                <c:otherwise>
                                    <img src="${yi.skinDir}${skin.path}/${skin.image}" width="100" height="100"/>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:forEach items="${skin.description}" var="line">
                                ${line}<br>
                            </c:forEach>
                        </td>
                        <td>${skin.path}</td>
                    </tr>
                </c:forEach>
            </table>
            <br>
            <form:form  method="POST" commandName="skin-entity" action="skin-download.html">
                <table id="headertable" style="width:50%;">
                    <tbody>
                        <tr>
                            <td style="width:20%"><form:label path="sourceUrl">URL for new skin:</form:label></td>
                            <td style="width:30%"><form:input path="sourceUrl"></form:input></td>
                            </tr>
                            <tr>
                                <td colspan="2" class="center"><input type="submit" value="Add skin"></td>
                            </tr>
                        </tbody>
                    </table>
            </form:form>
        </div>

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

        <p><a href="/yamj3/">Home</a></p>
    </body>
</html>
