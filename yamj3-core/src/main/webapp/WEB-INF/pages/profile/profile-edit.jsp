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
            <h2><spring:message code="page.artwork.profiles.title.edit"/></h2>
        </div>
        <p id="messageInfo"><spring:message code="page.artwork.profiles.info.edit"/></p>
        <form:form method="POST" commandName="profile" action="${pageContext.request.contextPath}/profile/edit.html">
            <form:hidden path="id"/>
            <form:hidden path="profileName"/>
            <form:hidden path="artworkType"/>
            
            <table id="headertable" class="hero-unit" style="width: 40%; margin: auto;">
            <tbody>
                <tr>
                    <td class="right"><spring:message code="label.name"/>:</td>
                    <td>&nbsp;</td>
                    <td>${profile.profileName}</td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="page.artwork.profiles.label.artworktype"/>:</td>
                    <td>&nbsp;</td>
                    <td>${profile.artworkType}</td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="page.artwork.profiles.label.width"/>:</td>
                    <td>&nbsp;</td>
                    <td><form:input path="width"/></td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="page.artwork.profiles.label.height"/>:</td>
                    <td>&nbsp;</td>
                    <td><form:input path="height"/></td>
                </tr>
                <c:if test="${profile.artworkType == 'POSTER' || profile.artworkType == 'FANART' || profile.artworkType == 'BANNER'}">
                    <tr>
                        <td class="right"><spring:message code="page.artwork.profiles.label.applyto"/>:</td>
                        <td>&nbsp;</td>
                        <td>
                            <table id="tablelist" style="margin:0px;width:10%;background-color:#222222">
                            <c:if test="${profile.artworkType != 'BANNER'}">
                                <tr style="background-color:#222222">
                                    <td style="border-bottom:0px"><spring:message code="metadata.movie"/>:</td>
                                    <td style="border-bottom:0px">&nbsp;</td>
                                    <td style="border-bottom:0px"><form:checkbox id="applyToMovie" path="applyToMovie"/></td>
                                </tr>
                            </c:if>
                            <tr style="background-color:#222222">
                                <td style="border-bottom:0px"><spring:message code="metadata.series"/>:</td>
                                <td style="border-bottom:0px">&nbsp;</td>
                                <td style="border-bottom:0px"><form:checkbox id="applyToSeries" path="applyToSeries"/></td>
                            </tr>
                            <tr style="background-color:#222222">
                                <td style="border-bottom:0px"><spring:message code="metadata.season"/>:</td>
                                <td style="border-bottom:0px">&nbsp;</td>
                                <td style="border-bottom:0px"><form:checkbox id="applyToSeason" path="applyToSeason"/></td>
                            </tr>
                            <tr style="background-color:#222222">
                                <td style="border-bottom:0px"><spring:message code="metadata.boxedset"/>:</td>
                                <td style="border-bottom:0px">&nbsp;</td>
                                <td style="border-bottom:0px"><form:checkbox id="applyToBoxedSet" path="applyToBoxedSet"/></td>
                            </tr>
                            </table>
                        </td>
                    </tr>
                </c:if>
                <tr>
                    <td class="right"><spring:message code="page.artwork.profiles.label.scaling"/>:</td>
                    <td>&nbsp;</td>
                    <td>
                        <form:select path="scalingType">
                            <form:option value="NORMALIZE"><spring:message code="page.artwork.profiles.scale.normalize"/></form:option>
                            <form:option value="STRETCH"><spring:message code="page.artwork.profiles.scale.stretch"/></form:option>
                            <form:option value="DEFAULT"><spring:message code="page.artwork.profiles.scale.default"/></form:option>
                        </form:select>
                    </td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="page.artwork.profiles.label.reflection"/>:</td>
                    <td>&nbsp;</td>
                    <td><form:checkbox path="reflection"/></td>
                </tr>
                <tr>
                    <td class="right"><spring:message code="page.artwork.profiles.label.roundedcorners"/>:</td>
                    <td>&nbsp;</td>
                    <td><form:checkbox path="roundedCorners"/></td>
                </tr>
                <tr>
                    <td colspan="2">&nbsp;</td>
                    <td class="left">
                        <input type="submit" name="update" class="btn info" value="<spring:message code="button.update"/>" >  
                        <a href="${pageContext.request.contextPath}/profile/list.html" class="btn info"><spring:message code="button.cancel"/></a>
                    </td>
                </tr>
            </tbody>
            <tfoot>
                <tr>
                    <td colspan="2"/>
                    <td class="left">
                        <c:if test="${errorMessage != null}">
                        <span id="messageError" style="align:right">${errorMessage}</span>
                        </c:if>
                        <c:if test="${successMessage != null}">
                        <span id="messageSuccess" style="align:right">${successMessage}</span>
                        </c:if>
                    </td>
                </tr>
            </tfoot>
            </table>
        </form:form>

        <!-- Import the footer -->
        <c:import url="../template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>

    </body>
</html>
