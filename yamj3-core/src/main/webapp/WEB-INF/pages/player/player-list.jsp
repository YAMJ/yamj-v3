<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
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
            <h2><spring:message code="page.player.title.list"/></h2>
            <p><a href="${pageContext.request.contextPath}/player/add.html" class="btn info"><spring:message code="page.player.button.add.player"/> &raquo;</a></p>
        </div>

        <table id="tablelist" style="width:90%; margin:auto;">
            <thead>
                <tr>
	                <th><spring:message code="page.player.label.playername"/></th>
	                <th><spring:message code="page.player.label.devicetype"/></th>
	                <th><spring:message code="page.player.label.ipaddress"/></th>
	                <th><spring:message code="page.player.label.countpaths"/></th>
	                <th/>
                </tr>
            <thead>
            <tbody>
                <c:forEach items="${playerlist}" var="entry" varStatus="row">
                    <tr>
                        <td><a href="${pageContext.request.contextPath}/player/details/${entry.id}.html">${entry.name}</a></td>
                        <td>${entry.deviceType}</td>
                        <td>${entry.ipAddress}</td>
                        <td>${fn:length(entry.paths)}</td>
                        <td class="center" style="width:1%">
                           <span class="nobr">
                            <a href="${pageContext.request.contextPath}/player/edit/${entry.id}.html" class="btn info"><spring:message code="button.edit"/></a>
                            <a href="${pageContext.request.contextPath}/player/delete/${entry.id}.html" class="btn info"><spring:message code="button.delete"/></a>
                            <a href="${pageContext.request.contextPath}/player/add-path/${entry.id}.html" class="btn info"><spring:message code="page.player.button.add.path"/></a>
                            </span>
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
