<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
    <head>
        <title>YAMJ v3</title>
        <!--Import the header details-->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="HEAD" />
        </c:import>
    </head>
    <body>
        <!--Import the navigation header-->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="NAV" />
        </c:import>

        <div id="logo">
            <h2>Player Entries</h2>
            <p><a href="${pageContext.request.contextPath}/player/add.html" class="btn info">Add new player &raquo;</a></p>
        </div>

        <table id="tablelist" style="width:90%; margin:auto;">
            <tr>
                <th>Player Name</th>
                <th>Device Type</th>
                <th>IP Address</th>
                <th># Paths</th>
                <th style="width:15%">&nbsp;</th>
            </tr>
            <tbody>
                <c:forEach items="${playerlist}" var="entry" varStatus="row">
                    <tr>
                        <td><a href="${pageContext.request.contextPath}/player/details/${entry.id}.html">${entry.name}</a></td>
                        <td>${entry.deviceType}</td>
                        <td>${entry.ipAddress}</td>
                        <td>${fn:length(entry.paths)}</td>
                        <td class="center" style="width:1%">
                           <span style="white-space:nowrap">
                            <a href="${pageContext.request.contextPath}/player/edit/${entry.id}.html" class="btn info">Edit</a>
                            <a href="${pageContext.request.contextPath}/player/delete/${entry.id}.html" class="btn info">Delete</a>
                            <a href="${pageContext.request.contextPath}/player/add-path/${entry.id}.html" class="btn info">Add Path</a>
                            </span>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <!-- Import the footer -->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
