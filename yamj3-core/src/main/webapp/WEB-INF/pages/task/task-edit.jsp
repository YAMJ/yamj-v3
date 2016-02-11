<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<!DOCTYPE html>
<html>
    <head>
        <title>YAMJ v3</title>
        <script src="${pageContext.request.contextPath}/scripts/jquery-1.12.0.min.js"></script>
        <script src="${pageContext.request.contextPath}/scripts/jquery.datetimepicker.full.min.js"></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/jquery.datetimepicker.css"/>
            
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
            <h2><spring:message code="page.task.title.edit"/></h2>
        </div>
        <p id="messageInfo"><spring:message code="page.task.info.edit"/></p>
        <form:form method="POST" commandName="task" action="${pageContext.request.contextPath}/task/edit/${task.name}.html">
            <table id="headertable" class="hero-unit" style="width: 40%; margin: auto;">
                <tr>
                    <td class="right"><spring:message code="label.name"/>:</td>
                    <td>&nbsp;</td>
                    <td>${task.name}</td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="page.task.label.task.name"/>:</td>
                    <td>&nbsp;</td>
                    <td>${task.taskName}</td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="page.task.label.interval"/>:</td>
                    <td>&nbsp;</td>
                    <td>
                        <form:select path="interval">
                            <form:option value="DAILY"><spring:message code="page.task.interval.daily"/></form:option>
                            <form:option value="MONTHLY"><spring:message code="page.task.interval.monthly"/></form:option>
                            <form:option value="DAYS"><spring:message code="page.task.interval.days"/></form:option>
                            <form:option value="HOURS"><spring:message code="page.task.interval.hours"/></form:option>
                            <form:option value="MINUTES"><spring:message code="page.task.interval.minutes"/></form:option>
                        </form:select>
                    </td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="page.task.label.delay"/>:</td>
                    <td>&nbsp;</td>
                    <td><form:input path="delay"/></td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="page.task.label.next.execution"/>:</td>
                    <td>&nbsp;</td>
                    <td>
                      <input name="nextExecDate" style="display: none;" id="nextExecDate" type="text"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">&nbsp;</td>
                    <td class="left">
                        <input type="submit" name="update" class="btn info" value="<spring:message code="button.update"/>" >  
                        <a href="${pageContext.request.contextPath}/task/list.html" class="btn info"><spring:message code="button.cancel"/></a><br>
                    </td>
                </tr>
            </table>
            <tfoot>
                <tr>
                    <td colspan="3" class="right">
                        <c:if test="${errorMessage != null}">
                        <span id="messageError" style="align:right">${errorMessage}</span>
                        </c:if>
                        <c:if test="${successMessage != null}">
                        <span id="messageSuccess" style="align:right">${successMessage}</span>
                        </c:if>
                    </td>
                </tr>
            </tfoot>
        </form:form>

        <!-- Import the footer -->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>

    </body>
    <script type="text/javascript">
    var givenDate = '${task.nextExecDate}';
    $('#nextExecDate').datetimepicker({
        dayOfWeekStart: 1,
        step: 5,
        startDate: new Date(givenDate),
        inline :true,
        theme: 'dark',
        yearStart: 2015,
        yearEnd: 2050
    });
    </script>
</html>
