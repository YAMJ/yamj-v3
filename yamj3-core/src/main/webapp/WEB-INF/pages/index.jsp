<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>

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

        <table align="center" class="hero-unit">
            <tr>
                <td>
                    <h1><spring:message code="index.info.title"/></h1>
                    <p><spring:message code="index.info.line1"/></p>
                    <p><spring:message code="index.info.line2"/></p>
                </td>
            </tr>
        </table>
        <div id="logo">
            <h2><spring:message code="index.pages.title"/></h2>
        </div>
        <table id="headertable" class="hero-unit" style="width: 50%; margin: auto;">
            <tr>
                <th style="width: 10%"><i class="fa fa-info-circle fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/system-info.html"><spring:message code="index.pages.systeminfo.short"/></a></th>
                <td style="width: 30%"><spring:message code="index.pages.systeminfo.long"/></td>
            </tr>
            <tr>
                <th style="width: 10%"><i class="fa fa-ticket fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/skin-info.html"><spring:message code="index.pages.skins.short"/></a></th>
                <td style="width: 30%"><spring:message code="index.pages.skins.long"/></td>
            </tr>
            <tr>
                <th style="width: 10%"><i class="fa fa-cogs fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/config/list.html"><spring:message code="index.pages.config.short"/></a></th>
                <td style="width: 30%"><spring:message code="index.pages.config.long"/></td>
            </tr>
            <tr>
                <th style="width: 10%"><i class="fa fa-play-circle-o fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/player/list.html"><spring:message code="index.pages.players.short"/></a></th>
                <td style="width: 30%"><spring:message code="index.pages.players.long"/></td>
            </tr>
            <tr>
                <th style="width: 10%"><i class="fa fa-cogs fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/task/list.html"><spring:message code="index.pages.tasks.short"/></a></th>
                <td style="width: 30%"><spring:message code="index.pages.tasks.long"/></td>
            </tr>
            <tr>
                <th style="width: 10%"><i class="fa fa-cogs fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/profile/list.html"><spring:message code="index.pages.profiles.short"/></a></th>
                <td style="width: 30%"><spring:message code="index.pages.profiles.long"/></td>
            </tr>
            <tr>
                <th style="width: 10%"><i class="fa fa-cogs fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/trakttv-info.html"><spring:message code="index.pages.trakttv.short"/></a></th>
                <td style="width: 30%"><spring:message code="index.pages.trakttv.long"/></td>
            </tr>
        </table>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>

    </body>
</html>
