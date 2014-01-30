<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <!--Import the header details-->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="HEAD" />
        </c:import>
    </head>
    <body>
        <!--Import the navigation header-->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="NAV" />
        </c:import>

        <div id="logo">
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
                        <td class="center">${entry.value}</td>
                        <td class="center">${entry.createTimestamp}</td>
                        <td class="center">${entry.updateTimestamp}</td>
                        <td class="center">
                            <a href="${pageContext.request.contextPath}/config/edit/${entry.key}.html">Edit</a> or
                            <a href="${pageContext.request.contextPath}/config/delete/${entry.key}.html">Delete</a>
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
