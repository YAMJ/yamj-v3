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
    <body background="${pageContext.request.contextPath}/images/yamj-configbg.jpg">
        <!--Import the navigation header-->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="NAV" />
        </c:import>

        <div id="logo">
            <h2>Edit Player</h2>
        </div>
        <p id="message">Enter the new value required</p>
        <form:form method="POST" commandName="player" action="${pageContext.request.contextPath}/player/edit/${player.name}.html">
            <table id="headertable" style="width:75%;">
                <tr>
                    <td class="right">Player Name:</td>
                    <td colspan="2" class="center">${player.name}</td>
                </tr>
                <tr>
                    <td class="center">Player Data</td>
                    <td class="center">Current</td>
                    <td class="center">New</td>
                </tr>
                <tr>
                    <td class="right">Device Type:</td>
                    <td>${player.deviceType}</td>
                    <td><form:input path="deviceType" size="100"></form:input></td>
                </tr>
                <tr>
                    <td class="right">IP Address:</td>
                    <td>${player.ipAddress}</td>
                    <td><form:input path="ipAddress" size="50"></form:input></td>
                </tr>
                <tr>
                    <td colspan="3" class="center"><input value="Update" type="submit"></td>
                </tr>
                </table>
        </form:form>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
