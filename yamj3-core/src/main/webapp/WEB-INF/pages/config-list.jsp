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
                <th colspan="5">Configuration Settings</th>
            </tr>
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
                        <td>${entry.value}</td>
                        <td>${entry.createTimestamp}</td>
                        <td>${entry.updateTimestamp}</td>
                        <td>
                            <a href="${pageContext.request.contextPath}/config/edit/${entry.key}.html">Edit</a> or
                            <a href="${pageContext.request.contextPath}/config/delete/${entry.key}.html">Delete</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <p><a href="${pageContext.request.contextPath}/index.html">Home page</a></p></td>
    <table class="sysinfo">
        <br>
        <br>
        <br>
        <br>
        <tr>
            <th>Version: ${yi.projectVersion}</th>
            <th>Revision: ${yi.buildRevision}</th>
            <th>Java: ${yi.javaVersion}</th>
            <th>Build Date: ${yi.buildDate}</th>
            <th>Start-up Time: ${yi.startUpTime}</th>
            <th>Uptime: ${yi.uptime}</th>
        </tr>
    </table>
</td>
</body>
</html>
