<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/yamj-style.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/font-awesome.min.css">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>YAMJ v3</title>
    </head>
    <body background="${pageContext.request.contextPath}/images/yamj-configbg.jpg">
        <div id="logo">
            <img alt="YAMJ Logo" src="${pageContext.request.contextPath}/images/yamj-logo.png">
            <h1>Yet Another Movie Jukebox</h1>
        </div>
        <br/>
        <c:if test="${not empty message}">
            <p>Message: ${message}</p>
            <br/>
        </c:if>

        <table id="headertable" style="width: 50%; margin: auto;">
            <tr>
                <th colspan="2">Index of Pages</th>
            </tr>
            <tr>
                <td style="width: 10%"><i class="fa fa-info-circle fa-2x"></i>&nbsp;
                    <a href="${pageContext.request.contextPath}/system-info.html">System information</a></td>
                <td style="width: 30%">Display information about the state of the core.</td>
            </tr>
            <tr>
                <td style="width: 10%"><i class="fa fa-ticket fa-2x"></i>&nbsp;
                    <a href="${pageContext.request.contextPath}/skin-info.html">Skins</a></td>
                <td style="width: 30%">Skins information.</td>
            </tr>
            <tr>
                <td style="width: 10%"><i class="fa fa-cogs fa-2x"></i>&nbsp;
                    <a href="${pageContext.request.contextPath}/config/list.html">Configuration</a></td>
                <td style="width: 30%">Display information about the configuration.</td>
            </tr>
            <tr>
                <td style="width: 10%"><i class="fa fa-play-circle-o fa-2x"></i>&nbsp;
                    <a href="${pageContext.request.contextPath}/player/list.html">Player Information</a></td>
                <td style="width: 30%">Display information about the player paths.</td>
            </tr>
            <tr>
                <td style="width: 10%"><i class="fa fa-list-ul fa-2x"></i>&nbsp;
                    <a href="${pageContext.request.contextPath}/count/job.html">Job List</a></td>
                <td style="width: 30%">List of jobs in the database.</td>
            </tr>
        </table>

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
