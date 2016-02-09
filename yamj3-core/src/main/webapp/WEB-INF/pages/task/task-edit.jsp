<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
            <h2>Edit Execution Task</h2>
        </div>
        <p id="message">Enter the new values required</p>
        <form:form method="POST" commandName="task" action="${pageContext.request.contextPath}/task/edit/${task.name}.html">
        	<table id="headertable" class="hero-unit" style="width: 40%; margin: auto;">
                <tr>
                    <td class="right">Name:</td>
                    <td>&nbsp;</td>
                    <td>${task.name}</td>
                </tr>
                <tr>
                    <td class="right">Task Name:</td>
                    <td>&nbsp;</td>
                    <td><form:input path="taskName"/></td>
                </tr>
                <tr>
                    <td class="right">Interval:</td>
                    <td>&nbsp;</td>
                    <td>
                        <form:select path="interval">
                            <form:option value="DAILY" label="Daily"/>
                            <form:option value="MONTHLY" label="Monthly"/>
                            <form:option value="DAYS" label="Days"/>
                            <form:option value="HOURS" label="Hours"/>
                            <form:option value="MINUTES" label="Minutes"/>
                        </form:select>
                    </td>
                </tr>
                <tr>
                    <td class="right">Delay:</td>
                    <td>&nbsp;</td>
                    <td><form:input path="delay"/></td>
                </tr>
                <tr>
                    <td class="right">Next Execution:</td>
                    <td>&nbsp;</td>
                    <td>
                      <input name="nextExecDate" style="display: none;" id="nextExecDate" type="text"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">&nbsp;</td>
                    <td class="left">
                        <input type="submit" name="update" class="btn info" value="Update" >  
                        <a href="${pageContext.request.contextPath}/task/list.html" class="btn info">Cancel</a><br>
                        <font color="red">${task.errorMessage}</font>
                    </td>
                </tr>
            </table>
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
        inline:true,
        theme:'dark',
        yearStart: 2015,
    });
    </script>
</html>
