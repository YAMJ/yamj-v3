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
            <h2><spring:message code="page.config.title.add"/></h2>
        </div>
        <p id="messageInfo"><spring:message code="page.config.info.add"/></p>
        <form:form method="POST" commandName="config" action="${pageContext.request.contextPath}/config/add/process.html">
            <table id="headertable" class="hero-unit" style="width: 40%; margin: auto;">
            <tbody>
                <tr>
                    <td class="right"><spring:message code="label.key"/>:</td>
                    <td>&nbsp;</td>
                    <td><form:input path="key"/></td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="label.value"/>:</td>
                    <td>&nbsp;</td>
                    <td><form:input path="value"/></td>
                </tr>
                <tr>
                 <td colspan="2">&nbsp;</td>
                    <td class="left"">
                        <input type="submit" name="add" class="btn info" value="<spring:message code="button.add"/>">  
                        <a href="${pageContext.request.contextPath}/config/list.html" class="btn info"><spring:message code="button.cancel"/></a>
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
