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

        <table align="center" class="hero-unit">
            <tr>
                <td>
                    <h1>Hello, YAMJ User</h1>
                    <p>The place to configure your YAMJ V3 jukebox.</p>
                    <p>For help and support visit the NMT forums.</p>
                    <!--<a class="btn info small">YAMJ help &raquo;</a>-->
                </td>
            </tr>
        </table>
        <div id="logo">
            <h2>Index of Pages</h2>
        </div>
        <table id="headertable" class="hero-unit" style="width: 50%; margin: auto;">
            <tr>
                <th style="width: 10%"><i class="fa fa-info-circle fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/system-info.html">System information</a></th>
                <td style="width: 30%">Display information about the state of the core.</td>
            </tr>
            <tr>
                <th style="width: 10%"><i class="fa fa-ticket fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/skin-info.html">Skins</a></th>
                <td style="width: 30%">Skin Information.</td>
            </tr>
            <tr>
                <th style="width: 10%"><i class="fa fa-cogs fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/config/list.html">Configuration</a></th>
                <td style="width: 30%">Display information about the configuration.</td>
            </tr>
            <tr>
                <th style="width: 10%"><i class="fa fa-play-circle-o fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/player/list.html">Player Information</a></th>
                <td style="width: 30%">Display information about the players and their paths.</td>
            </tr>
            <tr>
                <th style="width: 10%"><i class="fa fa-list-ul fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/count/job.html">Job List</a></th>
                <td style="width: 30%">List of jobs in the database.</td>
            </tr>
            <tr>
                <th style="width: 10%"><i class="fa fa-info-circle fa-lg"></i>&nbsp;<a href="${pageContext.request.contextPath}/trakttv/info.html">Trakt.TV Settings</a></th>
                <td style="width: 30%">Trakt.TV authorization.</td>
            </tr>
        </table>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>

    </body>
</html>
