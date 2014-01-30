<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
    <head>
        <title>YAMJ v3</title>
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
            <h2>Add Configuration Entry</h2>
        </div>
        <p id="message">Enter the property configuration key and value</p>
        <form:form method="POST" commandName="config" action="${pageContext.request.contextPath}/config/add/process.html">
            <table id="headertable">
                <tr>
                    <td class="right">Key:</td>
                    <td><form:input path="key"></form:input></td>
                    </tr>
                    <tr>
                        <td class="right">Value:</td>
                        <td><form:input path="value"></form:input></td>
                    </tr>
                    <tr>
                        <td colspan="2" class="center"><input value="Add Config" type="submit" class="btn default"></td>
                    </tr>
                </table>
        </form:form>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>

    </body>
</html>
