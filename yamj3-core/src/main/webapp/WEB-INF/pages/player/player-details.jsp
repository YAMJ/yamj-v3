<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
            <h2>Player Path Entries</h2>
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

     	<table id="headertable" class="hero-unit" style="width: 60%; margin: auto;">
        <tr>
           	<th colspan="3" class="center">
           		<a href="${pageContext.request.contextPath}/player/add-path/${player.id}.html" class="btn info">Add new path &raquo;</a>
			</th>
		</tr>       	
        <tr>
            <th>Source Path</th>
            <th>Target Path</th>
            <th>&nbsp;</th>
        </tr>
        <tbody>
            <c:forEach items="${pathlist}" var="entry" varStatus="row">
                <tr>
                    <td>${entry.sourcePath}</td>
                    <td>${entry.targetPath}</td>
                    <td class="center">
                        <a href="${pageContext.request.contextPath}/player/edit-path/${player.id}/${entry.id}.html" class="btn info">Edit</a>
                        <a href="${pageContext.request.contextPath}/player/delete-path/${player.id}/${entry.id}.html" class="btn info">Delete</a>
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
