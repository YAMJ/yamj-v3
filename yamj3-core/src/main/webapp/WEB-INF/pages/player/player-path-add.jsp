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
            <h2>Add Player Path</h2>
        </div>

        <table id="headertable" style="width:75%;">
            <tr>
                <td class="right">Player Name:</td>
                <td>${player.name}</td>
            </tr>
            <tr>
                <td class="right">Device Type:</td>
                <td>${player.deviceType}</td>
            </tr>
            <tr>
                <td class="right">IP Address:</td>
                <td>${player.ipAddress}</td>
            </tr>
        </table>

        <p id="message" class="center">Enter the path information</p>
        <form:form method="POST" commandName="playerPath" action="${pageContext.request.contextPath}/player/add-path/process/${player.id}.html">
            <table id="headertable" class="hero-unit" style="width:95%; margin:auto;">
                <tr>
                    <td style="width:25%" class="right">Source Path:</td>
                    <td style="width:75%"><form:input size="100" path="sourcePath"></form:input></td>
                    </tr>
                    <tr>
                        <td style="width:25%" class="right">Target Path:</td>
                        <td style="width:75%"><form:input size="50" path="targetPath"></form:input></td>
                    </tr>
                    <tr>
                        <td colspan="2" class="center">
                            <input value="Add Path" type="submit" class="btn info">
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
