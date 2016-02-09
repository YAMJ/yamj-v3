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
            <h2>Add Configuration Entry</h2>
        </div>
        <p id="message">Enter the property configuration key and value</p>
        <form:form method="POST" commandName="config" action="${pageContext.request.contextPath}/config/add/process.html">
        	<table id="headertable" class="hero-unit" style="width: 40%; margin: auto;">
                <tr>
                    <td class="right">Key:</td>
                    <td>&nbsp;</td>
                    <td><form:input path="key"/></td>
                </tr>
                <tr>
                    <td class="right">Value:</td>
                	<td>&nbsp;</td>
                    <td><form:input path="value"/></td>
                </tr>
                <tr>
                 <td colspan="2">&nbsp;</td>
                    <td class="left"">
                 		<input type="submit" name="add" class="btn info" value="Add Config" >  
	                    <a href="${pageContext.request.contextPath}/config/list.html" class="btn info">Cancel</a>
                   	</td>
                </tr>
            </table>
        </form:form>

        <!-- Import the footer -->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>

    </body>
</html>
