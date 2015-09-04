<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
            <h2>System Information</h2>
        </div>
        <table id="headertable" class="hero-unit" style="width: 50%; margin: auto;">
            <tr>
                <th class="left" style="width: 20%">Core IP/Port</th>
                <td class="left" style="width: 20%">${yi.coreIp}:${yi.corePort}</td>
            </tr>
            <tr>
                <th class="left" style="width: 20%">Database IP</th>
                <td class="left" style="width: 20%">${yi.databaseIp}</td>
            </tr>
            <tr>
                <th class="left" style="width: 20%">Database Name</th>
                <td class="left" style="width: 20%">${yi.databaseName}</td>
            </tr>
            <tr>
                <th class="left" style="width: 20%">Artwork Location URL</th>
                <td class="left" style="width: 20%"><a href="${yi.baseArtworkUrl}">${yi.baseArtworkUrl}</a></td>
            </tr>
            <tr>
                <th class="left" style="width: 20%">Photo Location URL</th>
                <td class="left" style="width: 20%"><a href="${yi.basePhotoUrl}">${yi.basePhotoUrl}</a></td>
            </tr>
            <tr>
                <th class="left" style="width: 20%">Trailer Location URL</th>
                <td class="left" style="width: 20%"><a href="${yi.baseTrailerUrl}">${yi.baseTrailerUrl}</a></td>
            </tr>
            <tr>
                <th class="left" style="width: 20%">MediaInfo Location URL</th>
                <td class="left" style="width: 20%"><a href="${yi.baseMediainfoUrl}">${yi.baseMediainfoUrl}</a></td>
            </tr>
            <tr>
                <th class="left" style="width: 20%">Skins Directory</th>
                <td class="left" style="width: 20%"><a href="${yi.skinDir}">${yi.skinDir}</a></td>
            </tr>
        </table>
        <br/>
        <div id="logo">
            <h2>Database Object Counts</h2>
        </div>
        <table id="headertable" class="hero-unit" style="width: 50%; margin: auto;">
            <c:forEach items="${countlist}" var="entry">
                <tr>
                    <th class="left" style="width: 20%">${entry.key}</th>
                    <td class="center" style="width: 20%">${entry.value}</td>
                </tr>
            </c:forEach>
        </table>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
