<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
            <h2>Edit Player</h2>
        </div>
        <p id="message">Enter the new value required</p>
        <form:form method="POST" commandName="player" action="${pageContext.request.contextPath}/player/edit/${player.name}.html">
            <table id="headertable" style="width:30%;">
                <tr>
                    <td class="right">Player Name:</td>
                    <td>${player.name}</td>
                </tr>
                <tr>
                    <td class="right">Path Prefix:</td>
                    <td>Current: ${player.pathPrefix}</td>
                    <td><form:input path="pathPrefix"></form:input></td>
                </tr>
                <tr>
                        <td class="right">Path Suffix:</td>
                        <td>Current: ${player.pathSuffix}</td>
                    <td><form:input path="pathSuffix"></form:input></td>
                </tr>
                <tr>
                    <td colspan="2" class="center"><input value="Update" type="submit"></td>
                </tr>
            </table>
        </form:form>

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
