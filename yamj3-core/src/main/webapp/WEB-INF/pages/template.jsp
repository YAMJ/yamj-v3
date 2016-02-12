<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<c:choose>
    <c:when test="${param.sectionName == 'HEAD'}">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/yamj-style.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/font-awesome.min.css">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
        <meta http-equiv="Pragma" content="no-cache">
        <meta http-equiv="Expires" content="0">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0"/>
        <meta charset="utf-8">
    </c:when>
    <c:when test="${param.sectionName == 'NAV'}">
        <!-- START header fixed navigation-->
        <div class="topbar">
            <div class="topbar-inner">
                <div class="container-fluid">
                    <a href="${pageContext.request.contextPath}/" class="brand"><img alt="YAMJ Logo" src="${pageContext.request.contextPath}/images/YAMJ-logo-120-white-reflectv3.png"></a>
                    <ul class="nav">
                        <li><a href="${pageContext.request.contextPath}/"><i class="fa fa-home fa-2x"></i><spring:message code="title.home"/></a></li>
                        <li><a href="${pageContext.request.contextPath}/skin-info.html"><i class="fa fa-ticket fa-2x"></i><spring:message code="title.skins"/></a></li>
                        <li><a href="${pageContext.request.contextPath}/config/list.html"><i class="fa fa-cogs fa-2x"></i><spring:message code="title.config"/></a></li>
                        <li><a href="http://www.networkedmediatank.com/forumdisplay.php?fid=139"><i class="fa fa-comments-o fa-2x"></i><spring:message code="title.forums"/></a></li>
                    </ul>
                </div>
            </div>
        </div>
        <br/>
        <br/>
        <br/>
        <br/>
        <!-- END header fixed navigation-->
    </c:when>
    <c:when test="${param.sectionName == 'FOOTER'}">
        <!-- START Footer -->
        <br/>
        <br/>
        <table class="footer" style="margin: auto;">
        <tbody>
            <tr>
                <th style="width:20%">Yet Another Movie Jukebox</th>
                <th style="width:10%">Revision</th>
                <th style="width:10%">Java</th>
                <th style="width:15%"><spring:message code="footer.builddate"/></th>
                <th style="width:15%"><spring:message code="footer.starttime"/></th>
                <th style="width:10%"><spring:message code="footer.uptime"/></th>
            </tr>
            <tr>
                <td>${yi.projectVersion}</td>
                <td>${yi.buildRevision}</td>
                <td>${yi.javaVersion}</td>
                <td>${yi.buildDate}</td>
                <td>${yi.startUpTime}</td>
                <td>${yi.uptime}</td>
            </tr>
        </tbody>
        </table>
        <!-- END Footer -->
    </c:when>
    <c:otherwise>
        <!--Display nothing-->
    </c:otherwise>
</c:choose>
