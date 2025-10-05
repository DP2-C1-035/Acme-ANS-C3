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

<acme:form> 
	<acme:input-select code="flight-crew-member.flight-assignment.form.label.flight-number" path="leg" choices="${legs}"/>
	<acme:input-textbox code="flight-crew-member.flight-assignment.form.label.member" path="member" readonly = "true"/>
	<acme:input-select code="flight-crew-member.flight-assignment.form.label.flight-crew-duty" path="flightCrewDuty" choices="${duties}"/>
	
	<jstl:if test="${_command != 'create'}">
		<acme:input-moment code="flight-crew-member.flight-assignment.form.label.last-update" path="lastUpdate" readonly="true"/>
		<acme:input-select code="flight-crew-member.flight-assignment.form.label.assignment-status" path="assignmentStatus" choices="${assignmentStatus}"/>
	</jstl:if>

	<acme:input-textarea code="flight-crew-member.flight-assignment.form.label.remarks" path="remarks"/>

	
	<hr />
		<h4>Tripulantes asignados a este vuelo:</h4>
		<ul>
			<jstl:choose>
				<jstl:when test="${not empty crewMembers}">
					<jstl:forEach var="cm" items="${crewMembers}">
						<li><jstl:out value="${cm}" /></li>
					</jstl:forEach>
				</jstl:when>
				<jstl:otherwise>
					<li><em>No hay tripulantes asignados todavía.</em></li>
				</jstl:otherwise>
			</jstl:choose>
		</ul>
		<h4>Otras legs asociadas al mismo vuelo:</h4>
		<jstl:choose>
			<jstl:when test="${not empty associatedLegs}">
				<ul>
					<jstl:forEach var="leg" items="${associatedLegs}">
						<li><jstl:out value="${leg}" /></li>
					</jstl:forEach>
				</ul>
			</jstl:when>
			<jstl:otherwise>
				<p><em>No hay otras legs asociadas a este vuelo.</em></p>
			</jstl:otherwise>
		</jstl:choose>
	<hr />
	
	

	<jstl:choose>
		<jstl:when test="${_command == 'show' && draftMode == false && legHasArrive == true}">
			<acme:button code="flight-crew-member.activity-log.form.button.list" action="/flight-crew-member/activity-log/list?masterId=${id}"/>
		</jstl:when>
		<jstl:when test="${acme:anyOf(_command, 'show|update|delete|publish') && draftMode == true}">
			<acme:submit code="flight-crew-member.flight-assignment.form.button.update" action="/flight-crew-member/flight-assignment/update"/>
			<acme:submit code="flight-crew-member.flight-assignment.form.button.delete" action="/flight-crew-member/flight-assignment/delete"/>
			<acme:submit code="flight-crew-member.flight-assignment.form.button.publish" action="/flight-crew-member/flight-assignment/publish"/>
		</jstl:when>
		<jstl:when test="${_command == 'create'}">
			<acme:submit code="flight-crew-member.flight-assignment.form.button.create" action="/flight-crew-member/flight-assignment/create"/>
		</jstl:when>
	</jstl:choose>
</acme:form>
