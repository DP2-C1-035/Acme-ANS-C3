<%--
- form.jsp
-
- Copyright (C) 2012-2025 Rafael Corchuelo.
-
- In keeping with the traditional purpose of furthering education and research, it is
- the policy of the copyright owner to permit non-commercial use and redistribution of
- this software. It has been tested carefully, but it is not guaranteed for any particular
- purposes.  The copyright owner does not offer any warranties or representations, nor do
- they accept any liabilities with respect to them.
--%>

<%@page%>

<%@taglib prefix="jstl" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="acme" uri="http://acme-framework.org/"%>

<acme:list>
	<acme:list-column code="assistanceAgent.trackingLog.list.label.lastUpdateMoment" path="lastUpdateMoment"/>
	<acme:list-column code="assistanceAgent.trackingLog.list.label.resolutionPercentage" path="resolutionPercentage"/>
	<acme:list-column code="assistanceAgent.trackingLog.list.label.indicator" path="indicator"/>
	<acme:list-column code="assistanceAgent.trackingLog.list.label.published" path="published"/>		
</acme:list>

<jstl:choose>
		<jstl:when test="${noMore}">
			<acme:button code="assistanceAgent.trackingLog.list.button.create" action="/assistance-agent/tracking-log/create?masterId=${masterId}"/>
		</jstl:when>	
		<jstl:when test="${exceptionalCase}">
			<acme:button code="assistanceAgent.trackingLog.list.button.create-exceptional-case" action="/assistance-agent/tracking-log/exceptional-case?masterId=${masterId}"/>
		</jstl:when>		
</jstl:choose>