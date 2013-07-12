<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title id="title">YAMJ v3</title>
        <link rel="stylesheet" type="text/css" href="yamj-style.css">
    </head>
    <body>
        <h1>${yi.projectName}</h1>

        <table id="main">
            <tr>
                <th colspan="2">System Information</th>
            </tr>
            <tr>
                <td class="right">Version</td>
                <td>${yi.projectVersion}</td>
            </tr>
            <tr>
                <td class="right">Revision</td>
                <td>${yi.buildRevision}</td>
            </tr>
            <tr>
                <td class="right">Java</td>
                <td>${yi.javaVersion}</td>
            </tr>
            <tr>
                <td class="right">Build Date</td>
                <td>${yi.buildDate}</td>
            </tr>
            <tr>
                <td class="right">Start-up Time</td>
                <td>${yi.startUpTime}</td>
            </tr>
            <tr>
                <td class="right">Uptime</td>
                <td>${yi.uptime}</td>
            </tr>
        </table>
        <br/>
        <table id="counts">
            <tr>
                <th colspan="2">Database Object Counts</th>
            </tr>
            <c:forEach items="${countlist}" var="entry">
                <tr>
                    <td class="right">${entry.key}</td>
                    <td>${entry.value}</td>
                </tr>
            </c:forEach>
        </table>
        <p><a href="/yamj3/">Home</a></p>
    </body>
</html>
