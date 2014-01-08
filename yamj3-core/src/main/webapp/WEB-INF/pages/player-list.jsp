<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/yamj-style.css">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>YAMJ v3</title>
    </head>
    <body background="${pageContext.request.contextPath}/images/yamj-configbg.jpg">
        <div id="logo">
            <h1>Yet Another Movie Jukebox</h1>
            <h2>Player Entries</h2>
            <p><a href="${pageContext.request.contextPath}/player/add.html">Add new player</a></p>
        </div>
        <c:if test="${not empty message}">
            <br/>
            <p class="message">Message: ${message}</p>
            <br/>
        </c:if>

        <table id="tablelist">
            <tr>
                <th>Player Name</th>
                <th>Path Prefix</th>
                <th>Path Suffix</th>
                <th>Actions</th>
            </tr>
            <tbody>
                <c:forEach items="${playerlist}" var="entry" varStatus="row">
                    <tr>
                        <td>${entry.name}</td>
                        <td>${entry.pathPrefix}</td>
                        <td>${entry.pathSuffix}</td>
                        <td class="center">
                            <a href="${pageContext.request.contextPath}/player/edit/${entry.name}.html">Edit</a> or
                            <a href="${pageContext.request.contextPath}/player/delete/${entry.name}.html">Delete</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <p><a href="${pageContext.request.contextPath}/index.html">Home page</a></p>
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
