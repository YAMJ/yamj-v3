<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/yamj-style.css">
        <link rel="shortcut icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico" />
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>YAMJ v3</title>
    </head>
    <body>
        <div id="logo">
            <img src="${pageContext.request.contextPath}/images/yamj-logo.png">
            <h1>Yet Another Movie Jukebox</h1>
        </div>
        <br/>
        <c:if test="${not empty message}">
            <p>Message: ${message}</p>
            <br/>
        </c:if>
        <table id="headertable">
            <tr>
                <th colspan="2">Index of Pages</th>
            </tr>
            <tr>
                <!-- There is a bug in netbeans that displays this as an error. Ignore it -->
                <td><a href="${pageContext.request.contextPath}/system-info.html">System Information</a></td>
                <td>Display information about the state of the core.</td>
            </tr>
            <tr>
                <td><a href="${pageContext.request.contextPath}/config/list.html">Configuration</a></td>
                <td>Display information about the configuration.</td>
            </tr>
        </table>
    </body>
</html>
