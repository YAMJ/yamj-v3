<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <title>YAMJ v3</title>
        <!--Import the header details-->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="HEAD" />
        </c:import>
    </head>
    <body background="${pageContext.request.contextPath}/images/yamj-configbg.jpg">
        <!--Import the navigation header-->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="NAV" />
        </c:import>

        <div id="logo">
            <h2>Player Entries</h2>
            <p><a href="${pageContext.request.contextPath}/player/add.html" class="btn info">Add new player &raquo;</a></p>
        </div>
        <c:if test="${not empty message}">
            <br/>
            <p class="message">Message: ${message}</p>
            <br/>
        </c:if>

        <table id="tablelist">
            <tr>
                <th>Player Name</th>
                <th>Device Type</th>
                <th>IP Address</th>
                <th>Actions</th>
            </tr>
            <tbody>
                <c:forEach items="${playerlist}" var="entry" varStatus="row">
                    <tr>
                        <td><a href="${pageContext.request.contextPath}/player/details/${entry.name}.html">${entry.name}</a></td>
                        <td>${entry.deviceType}</td>
                        <td>${entry.ipAddress}</td>
                        <td class="center">
                            <a href="${pageContext.request.contextPath}/player/edit/${entry.name}.html">Edit</a> or
                            <a href="${pageContext.request.contextPath}/player/delete/${entry.name}.html">Delete</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
