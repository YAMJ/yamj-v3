<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
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
            <h2><spring:message code="page.player.title.scan"/></h2>
        </div>

        <c:if test="${empty players}">
            <form action="PlayerServlet" method="get">
                <input type="hidden" name="listallplayers" value="1"/><br/>
                <input type="submit" value="<spring:message code="page.player.show.all"/>"/>
            </form>
        </c:if>
        <c:if test="${!empty players}">
            <table id="tablelist">
                <tr>
                    <th><spring:message code="page.player.label.playername"/></th>
                    <th><spring:message code="page.player.label.ipaddress"/></th>
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
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
