<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
            <h2>Cast &amp; Crew Job List</h2>
        </div>
        <table id="tablelist" style="width: 20%; margin: auto;;">
            <c:forEach items="${countlist}" var="entry">
                <tr>
                    <th class="right" style="width: 10%">${entry.item}</th>
                    <td class="center" style="width: 10%">${entry.count}</td>
                </tr>
            </c:forEach>
        </table>

    <!-- Import the footer -->
    <c:import url="template.jsp">
        <c:param name="sectionName" value="FOOTER" />
    </c:import>

    </body>
</html>
