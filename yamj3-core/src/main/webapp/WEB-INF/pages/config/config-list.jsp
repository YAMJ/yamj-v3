<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
            <h2>Configuration Entries</h2>
            <p><a href="${pageContext.request.contextPath}/config/add.html" class="btn info">Add new configuration &raquo;</a></p>
        </div>

        <table id="headertable" class="hero-unit" style="width: 90%; margin: auto;">
            <tr>
                <th>Key</th>
                <th>Value</th>
                <th>Create Timestamp</th>
                <th>Update Timestamp</th>
                <th/>
            </tr>
            <tbody>
                <c:forEach items="${configlist}" var="entry" varStatus="row">
                    <tr>
                        <td>${entry.key}</td>
                        <td>${entry.value}</td>
                        <td>${entry.createTimestamp}</td>
                        <td>${entry.updateTimestamp}</td>
                        <td class="center" style="width:1%">
                           <span style="white-space:nowrap">
                            <a href="${pageContext.request.contextPath}/config/edit/${entry.key}.html" class="btn info">Edit</a>
                            <a href="${pageContext.request.contextPath}/config/delete/${entry.key}.html" class="btn info">Delete</a>
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
