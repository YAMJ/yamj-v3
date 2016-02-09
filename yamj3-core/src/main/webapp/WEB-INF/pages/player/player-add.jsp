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
            <h2>Add Player Entry</h2>
        </div>
        <p id="message" class="center">Enter the player information</p>
        <form:form method="POST" commandName="player" action="${pageContext.request.contextPath}/player/add/process.html">
            <table id="headertable" class="hero-unit" style="width:95%; margin:auto;">
                <tr>
                    <td style="width:25%" class="right">Player Name:</td>
                    <td style="width:75%" class="left"><form:input size="200" path="name"></form:input></td>
                    </tr>
                    <tr>
                        <td style="width:25%" class="right">Device Type:</td>
                        <td style="width:75%"><form:input size="200" path="deviceType"></form:input></td>
                    </tr>
                    <tr>
                        <td style="width:25%" class="right">IP Address:</td>
                        <td style="width:75%"><form:input size="100" path="ipAddress"></form:input></td>
                    </tr>
                    <tr>
                        <td colspan="2" class="center">
                            <input value="Add Player" type="submit" class="btn info">
                            <a href="${pageContext.request.contextPath}/player/list.html" class="btn info">Cancel</a>
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
