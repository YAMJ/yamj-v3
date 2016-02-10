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
            <h2>Execution Tasks</h2>
        </div>

        <table id="headertable" class="hero-unit" style="width: 90%; margin: auto;">
            <tr>
                <th>Name</th>
                <th>Task Name</th>
                <th>Interval</th>
                <th>Delay</th>
                <th>Last Execution</th>
                <th>Next Execution</th>
                <th/>
            </tr>
            <tbody>
                <c:forEach items="${tasklist}" var="task" varStatus="row">
                    <tr>
                        <td>${task.name}</td>
                        <td>${task.taskName}</td>
                        <td>${task.intervalType}</td>
                        <td>${task.delay}</td>
                        <td>${task.lastExecution}</td>
                        <td>${task.nextExecution}</td>
                        <td class="center" style="width:1%">
                           <span style="white-space:nowrap">
                            <a href="${pageContext.request.contextPath}/task/edit/${task.name}.html" class="btn info">Edit</a>
                            <a href="${pageContext.request.contextPath}/task/enqueue/${task.name}.html" class="btn info">Enqueue</a>
                            </span>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
            <tfoot>
                <tr>
                    <td colspan="7" class="right">
                        <c:if test="${errorMessage != null}">
                        <span id="messageError" style="align:right">${errorMessage}</span>
                        </c:if>
                        <c:if test="${successMessage != null}">
                        <span id="messageSuccess" style="align:right">${successMessage}</span>
                        </c:if>
                    </td>
                </tr>
            </tfoot>
        </table>

        <!-- Import the footer -->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>

    </body>
</html>
