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

        <table>
            <tbody>
                <tr>
                    <th>Skin Name</th>
                    <td>${skin.name}</td>
                </tr>
                <tr>
                    <th>Image</th>
                    <td>${skin.image}</td>
                </tr>
                <tr>
                    <th>Skin Description</th>
                    <td>
                        <c:forEach items="${skin.description}" var="line">
                            ${line}<br>
                        </c:forEach>
                    </td>
                </tr>
                <tr>
                    <th>Path</th>
                    <td>${skin.path}</td>
                </tr>
                <tr>
                    <th>Source Url</th>
                    <td>${skin.sourceUrl}</td>
                </tr>
            </tbody>
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
