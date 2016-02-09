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
            <h2>Edit Player Path</h2>
        </div>

        <table id="headertable" style="width:50%;" class="center">
            <tr>
                <td class="right">Player Name:</td>
				<td>&nbsp;</td>
                <td>${player.name}</td>
            </tr>
            <tr>
                <td class="right">Device Type:</td>
				<td>&nbsp;</td>
                <td>${player.deviceType}</td>
            </tr>
            <tr>
                <td class="right">IP Address:</td>
				<td>&nbsp;</td>
                <td>${player.ipAddress}</td>
            </tr>
        </table>

        <p id="message">Edit the values required</p>
        <form:form method="POST" commandName="path" action="${pageContext.request.contextPath}/player/edit-path/${player.id}/${path.id}.html">
          	<table id="headertable" class="hero-unit" style="width: 50%; margin: auto;">
                <tr>
                    <td colspan="2">&nbsp;</td>
                    <td>Current</td>
                    <td>New</td>
                </tr>
                <tr>
                    <td class="right">Source Path:</td>
					<td>&nbsp;</td>
                    <td>${path.sourcePath}</td>
                    <td><form:input path="sourcePath" size="100"/></td>
                </tr>
                <tr>
                    <td class="right">Target Path:</td>
					<td>&nbsp;</td>
                    <td>${path.targetPath}</td>
                    <td><form:input path="targetPath" size="50"/></td>
                </tr>
                <tr>
					<td colspan="3">&nbsp;</td>
                    <td class="left">
                        <input type="submit" name="update" class="btn info" value="Update" >  
                        <a href="${pageContext.request.contextPath}/player/details/${player.id}.html" class="btn info">Cancel</a>
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
