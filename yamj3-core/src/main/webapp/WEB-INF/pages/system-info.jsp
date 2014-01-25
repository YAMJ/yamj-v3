<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
            <h2>System Information</h2>
        </div>
        <table id="headertable" style="width: 40%; margin: auto;">
            <tr>
                <th class="right" style="width: 10%">Version</th>
                <td>${yi.projectVersion}</td>
            </tr>
            <tr>
                <th class="right">Revision</th>
                <td>${yi.buildRevision}</td>
            </tr>
            <tr>
                <th class="right">Java</th>
                <td>${yi.javaVersion}</td>
            </tr>
            <tr>
                <th class="right">Build Date</th>
                <td>${yi.buildDate}</td>
            </tr>
            <tr>
                <th class="right">Start-up Time</th>
                <td>${yi.startUpTime}</td>
            </tr>
            <tr>
                <th class="right">Uptime</th>
                <td>${yi.uptime}</td>
            </tr>
        </table>
        <br/>
        <div id="logo">
            <h2>Server Information</h2>
        </div>
        <table id="headertable" style="width: 50%; margin: auto;">
            <tr>
                <th class="right" style="width: 10%">Core IP/Port</th>
                <td>${yi.coreIp}:${yi.corePort}</td>
            </tr>
            <tr>
                <th class="right">Database IP</th>
                <td>${yi.databaseIp}</td>
            </tr>
            <tr>
                <th class="right">Database Name</th>
                <td>${yi.databaseName}</td>
            </tr>
            <tr>
                <th class="right">Artwork Location URL</th>
                <td><a href="${yi.baseArtworkUrl}">${yi.baseArtworkUrl}</a></td>
            </tr>
            <tr>
                <th class="right">MediaInfo Location URL</th>
                <td><a href="${yi.baseMediainfoUrl}">${yi.baseMediainfoUrl}</a></td>
            </tr>
            <tr>
                <th class="right">Skins Directory</th>
                <td><a href="${yi.skinDir}">${yi.skinDir}</a></td>
            </tr>
        </table>
        <br/>
        <div id="logo">
            <h2>Database Object Counts</h2>
        </div>
        <table id="tablelist" style="width: 20%; margin: auto;;">
            <c:forEach items="${countlist}" var="entry">
                <tr>
                    <th class="right" style="width: 10%">${entry.key}</th>
                    <td class="center" style="width: 10%">${entry.value}</td>
                </tr>
            </c:forEach>
        </table>
        <p><a href="/yamj3/">Home</a></p>
    </body>
</html>
