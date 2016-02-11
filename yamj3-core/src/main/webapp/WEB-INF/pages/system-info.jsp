<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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

        <div id="logo">
            <h2><spring:message code="title.systeminfo"/></h2>
        </div>
        <table id="headertable" class="hero-unit" style="width:50%; margin:auto;">
            <tr>
                <th class="left" style="width: 20%"><spring:message code="page.systeminfo.core.ip"/></th>
                <td class="left" style="width: 20%">${yi.coreIp}:${yi.corePort}</td>
            </tr>
            <tr>
                <th class="left" style="width: 20%"><spring:message code="page.systeminfo.database.ip"/></th>
                <td class="left" style="width: 20%">${yi.databaseIp}</td>
            </tr>
            <tr>
                <th class="left" style="width: 20%"><spring:message code="page.systeminfo.database.name"/></th>
                <td class="left" style="width: 20%">${yi.databaseName}</td>
            </tr>
            <tr>
                <th class="left" style="width: 20%"><spring:message code="page.systeminfo.artwork.location.url"/></th>
                <td class="left" style="width: 20%"><a href="${yi.baseArtworkUrl}">${yi.baseArtworkUrl}</a></td>
            </tr>
            <tr>
                <th class="left" style="width: 20%"><spring:message code="page.systeminfo.photo.location.url"/></th>
                <td class="left" style="width: 20%"><a href="${yi.basePhotoUrl}">${yi.basePhotoUrl}</a></td>
            </tr>
            <tr>
                <th class="left" style="width: 20%"><spring:message code="page.systeminfo.trailer.location.url"/>L</th>
                <td class="left" style="width: 20%"><a href="${yi.baseTrailerUrl}">${yi.baseTrailerUrl}</a></td>
            </tr>
            <tr>
                <th class="left" style="width: 20%"><spring:message code="page.systeminfo.mediainfo.location.url"/></th>
                <td class="left" style="width: 20%"><a href="${yi.baseMediainfoUrl}">${yi.baseMediainfoUrl}</a></td>
            </tr>
            <tr>
                <th class="left" style="width: 20%"><spring:message code="page.systeminfo.skins.directory"/></th>
                <td class="left" style="width: 20%"><a href="${yi.skinDir}">${yi.skinDir}</a></td>
            </tr>
        </table>
        <br/>
        <div id="logo">
            <h2><spring:message code="page.systeminfo.database.object.counts"/></h2>
        </div>
        
        <table style="width:50%; margin:auto; align:center">
            <tr>
                <td class="right" style="width:50%">
                    <table id="headertable" class="hero-unit" style="width:100%;margin:auto;align:right">
                        <c:forEach items="${countlist}" var="entry">
                            <tr>
                                <th class="right" style="width:60%">${entry.key}</th>
                                <td class="center" style="width:40%">${entry.value}</td>
                            </tr>
                        </c:forEach>
                    </table>
                </td>
                <td class="left" style="width:50%">
                    <table id="headertable" class="hero-unit" style="width:100%;margin:auto;align:left">
                        <c:forEach items="${joblist}" var="job">
                            <tr>
                                <th class="right" style="width:60%">${job.item}</th>
                                <td class="center" style="width:40%">${job.counter}</td>
                            </tr>
                        </c:forEach>
                    </table>
                </td>
            </tr>
        </table>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
