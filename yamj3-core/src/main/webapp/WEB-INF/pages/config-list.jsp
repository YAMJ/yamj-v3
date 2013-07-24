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
            <h2>Configuration Entries</h2>
            <p><a href="${pageContext.request.contextPath}/config/add.html">Add new configuration</a></p>
        </div>
        <c:if test="${not empty message}">
            <br/>
            <p class="message">Message: ${message}</p>
            <br/>
        </c:if>

        <table id="tablelist">
            <tr>
                <th>Key</th>
                <th>Value</th>
                <th>Create Timestamp</th>
                <th>Update Timestamp</th>
                <th>Actions</th>
            </tr>
            <tbody>
                <c:forEach items="${configlist}" var="entry" varStatus="row">
                    <tr>
                        <td>${entry.key}</td>
                        <td class="center">${entry.value}</td>
                        <td class="center">${entry.createTimestamp}</td>
                        <td class="center">${entry.updateTimestamp}</td>
                        <td class="center">
                            <a href="${pageContext.request.contextPath}/config/edit/${entry.key}.html">Edit</a> or
                            <a href="${pageContext.request.contextPath}/config/delete/${entry.key}.html">Delete</a>
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
