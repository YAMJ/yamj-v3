<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
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
            <h2><spring:message code="title.tasks"/></h2>
        </div>

        <table id="headertable" class="hero-unit" style="width: 90%; margin: auto;">
            <tr>
                <th><spring:message code="label.name"/></th>
                <th><spring:message code="page.task.label.task.name"/></th>
                <th><spring:message code="page.task.label.interval"/></th>
                <th><spring:message code="page.task.label.delay"/></th>
                <th><spring:message code="page.task.label.last.execution"/></th>
                <th><spring:message code="page.task.label.next.execution"/></th>
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
                            <a href="${pageContext.request.contextPath}/task/edit/${task.name}.html" class="btn info"><spring:message code="button.edit"/></a>
                            <a href="${pageContext.request.contextPath}/task/enqueue/${task.name}.html" class="btn info"><spring:message code="page.task.button.enqueue"/></a>
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
