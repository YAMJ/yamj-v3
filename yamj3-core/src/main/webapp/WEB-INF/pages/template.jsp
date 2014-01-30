<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <c:choose>
        <c:when test="${param.sectionName == 'HEAD'}">
            <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/yamj-style.css">
            <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap.css">
            <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/font-awesome.min.css">
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0"/>
            <title>YAMJ v3</title>
        </c:when>
        <c:when test="${param.sectionName == 'NAV'}">
            <!-- START header fixed navigation-->
            <div class="topbar">
                <div class="topbar-inner">
                    <div class="container-fluid">
                        <a class="brand" href="index.jsp"><img alt="" src="${pageContext.request.contextPath}/images/YAMJ-logo-120-white-reflectv3.png"></a>
                        <ul class="nav">
                            <li><a href="/"><i class="fa fa-home fa-2x"></i> Home</a></li>
                            <li><a href="${pageContext.request.contextPath}/skin-info.html"><i class="fa fa-ticket fa-2x"></i> Skins</a></li>
                            <li><a href="${pageContext.request.contextPath}/config/list.html"><i class="fa fa-cogs fa-2x"></i> Config</a></li>
                            <li><a href="http://www.networkedmediatank.com/forumdisplay.php?fid=139"><i class="fa fa-comments-o fa-2x"></i> Forums</a></li>
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
            <table class="footer">
                <tr>
                    <td>Yet Another Movie Jukebox</td>
                    <th>Revision</th>
                    <th>Java</th>
                    <th>Build Date</th>
                    <th>Start-up Time</th>
                    <th>Uptime</th>
                </tr>
                <tr>
                    <td>${yi.projectVersion}</td>
                    <td>${yi.buildRevision}</td>
                    <td>${yi.javaVersion}</td>
                    <td>${yi.buildDate}</td>
                    <td>${yi.startUpTime}</td>
                    <td>${yi.uptime}</td>
                </tr>
            </table>
            <!-- END Footer -->
        </c:when>
        <c:otherwise>
            <!--Display nothing-->
        </c:otherwise>
    </c:choose>
</html>
