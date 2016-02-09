<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
            <h2>Edit Configuration</h2>
        </div>
        <p id="message">Enter the new value required</p>
        <form:form method="POST" commandName="config" action="${pageContext.request.contextPath}/config/edit/${config.key}.html">
            <table id="headertable" style="width:30%;">
                <tr>
                    <td class="right">Key:</td>
                    <td>${config.key}</td>
                </tr>
                <tr>
                    <td class="right">Old Value:</td>
                    <td>${config.value}</td>
                </tr>
                <tr>
                    <td class="right">New Value:</td>
                    <td><form:input path="value"></form:input></td>
                    </tr>
                    <tr>
                        <td colspan="2" class="center"><input value="Update" type="submit"></td>
                    </tr>
                </table>
        </form:form>

        <!-- Import the footer -->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>

    </body>
</html>
