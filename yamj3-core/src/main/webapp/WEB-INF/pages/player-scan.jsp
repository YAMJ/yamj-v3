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
            <h2>Scan For Players</h2>
        </div>

        <c:if test="${empty players}">
            <form action="PlayerServlet" method="get">
                <input type="hidden" name="listallplayers" value="1"/><br/>
                <input type="submit" value="Show all players"/>
            </form>
        </c:if>
        <c:if test="${!empty players}">
            <table id="tablelist">
                <tr>
                    <th>Player Name</th>
                    <th>IP Address</th>
                </tr>
                <tbody>
                    <c:forEach items="${playerlist}" var="entry" varStatus="row">
                        <tr>
                            <td>${entry.name}</td>
                            <td>${entry.ipAddress}</td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:if>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
