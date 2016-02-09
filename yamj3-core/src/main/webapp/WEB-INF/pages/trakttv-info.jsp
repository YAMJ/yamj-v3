<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
            <h2>Trakt.TV Settings</h2>
        </div>
        
        <c:if test="${!trakttv.synchronization}">
        <table id="tablelist" class="hero-unit" style="width: 40%; margin: auto;">
          	<tr>
               	<th class="left">Trakt.TV synchronization is not enabled.</th>
           	</tr>
        </table>
        </c:if>        
        <c:if test="${trakttv.synchronization}">
        <table id="headertable" class="hero-unit" style="width: 40%; margin: auto;">
            <tr>
                <th colspan="2">
                    <c:if test="${trakttv.push && trakttv.pull}">
                    Trakt.TV synchronization (Push &amp; Pull) is enabled.<br>
                   </c:if> 
                    <c:if test="${trakttv.push && !trakttv.pull}">
                    Trakt.TV synchronization (Push only) is enabled.
                   </c:if> 
                    <c:if test="${!trakttv.push && trakttv.pull}">
                    Trakt.TV synchronization (Pull only) is enabled.
                   </c:if>
                    <c:if test="${!trakttv.push && !trakttv.pull}">
                    Trakt.TV synchronization is enabled.
                   </c:if>
                </th>
            </tr>
            <tr>
                <td style="width:20%" class="left">Authorized:</td>
                <td style="width:80%" class="left">${trakttv.authorized}</td>
            </tr>
            <tr>
                <td style="width:20%" class="left">Expiration Date:</td>
                <td style="width:80%" class="left">${trakttv.expirationDate}</td>
            </tr>
        </table
        <p>&nbsp;</p>
        <form:form  method="POST" commandName="pin-entity" action="trakttv-pin.html">
            <table id="headertable" class="hero-unit" style="width:40%; margin: auto;">
                <tbody>
                    <tr>
                        <td colspan="2" class="center">
                            To request a Trakt.TV pin please follow this link:<br>
                            <a href="http://trakt.tv/pin/8032" target="_blank">http://trakt.tv/pin/8032</a>
                        </td>
                    </tr>
                    <tr>
                        <td class="right"><label for="pin"><b>Trakt.TV PIN:</b></label></td>
                        <td class="center"><input class="span4" id="pin" name="pin" type="text" value="" size="20"></td>
                    </tr>
                    <tr>
                        <td colspan="2" class="center">
                            <input type="submit" value="Authorize with PIN" class="btn info"><br>
                            <font color="red">${trakttv.message}</font>
                        </td>
                    </tr>
                </tbody>
            </table>
        </form:form>        
        </c:if>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
