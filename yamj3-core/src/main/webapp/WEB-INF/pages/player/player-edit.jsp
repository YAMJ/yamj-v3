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
            <h2><spring:message code="page.player.title.edit"/></h2>
        </div>
        <p id="messageInfo"><spring:message code="page.player.info.edit"/></p>
        <form:form method="POST" commandName="player" action="${pageContext.request.contextPath}/player/edit/${player.id}.html">
            <table id="headertable" class="hero-unit" style="width: 50%; margin: auto;">
            <tbody>
                <tr>
                    <td colspan="2">&nbsp;</td>
                    <td><spring:message code="label.value.old"/></td>
                    <td><spring:message code="label.value.new"/></td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="page.player.label.playername"/>:</td>
                    <td>&nbsp;</td>
                    <td colspan="2" class="left">${player.name}</td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="page.player.label.devicetype"/>:</td>
                    <td>&nbsp;</td>
                    <td>${player.deviceType}</td>
                    <td><form:input path="deviceType" size="100"></form:input></td>
                 </tr>
                 <tr>
                     <td class="right"><spring:message code="page.player.label.ipaddress"/>:</td>
                    <td>&nbsp;</td>
                    <td>${player.ipAddress}</td>
                     <td><form:input path="ipAddress" size="50"></form:input></td>
                 </tr>
                 <tr>
                    <td colspan="3">&nbsp;</td>
                     <td class="left">
                        <input type="submit" name="update" class="btn info" value="<spring:message code="button.update"/>" >  
                        <a href="${pageContext.request.contextPath}/player/list.html" class="btn info"><spring:message code="button.cancel"/></a>
                    </td>
                </tr>
            </tbody>
            </table>
        </form:form>

        <!-- Import the footer -->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
