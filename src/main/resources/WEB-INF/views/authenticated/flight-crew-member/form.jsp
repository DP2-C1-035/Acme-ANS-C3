<%@page%>
<%@taglib prefix="jstl" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="acme" uri="http://acme-framework.org/"%>

<acme:form>

    <acme:input-textbox 
        code="authenticated.flight-crew-member.form.label.employeeCode" 
        path="employeeCode" 
    />

    <acme:input-textbox 
        code="authenticated.flight-crew-member.form.label.phoneNumber" 
        path="phoneNumber" 
    />

    <acme:input-textbox 
        code="authenticated.flight-crew-member.form.label.languageSkills" 
        path="languageSkills" 
    />

    <acme:input-money 
        code="authenticated.flight-crew-member.form.label.salary" 
        path="salary" 
    />

    <acme:input-integer 
        code="authenticated.flight-crew-member.form.label.yearsOfExperience" 
        path="yearsOfExperience" 
    />

    <acme:input-select 
        code="authenticated.flight-crew-member.form.label.availabilityStatus" 
        path="availabilityStatus" 
        choices="${availabilityStatuses}" 
    />

    <acme:input-select 
        code="authenticated.flight-crew-member.form.label.airline" 
        path="airlineId" 
        choices="${airlines}" 
    />

    <jstl:choose>
        <jstl:when test="${_command == 'update'}">
            <acme:submit 
                code="authenticated.flight-crew-member.form.button.update" 
                action="/authenticated/flight-crew-member/update"
            />
        </jstl:when>
        <jstl:when test="${_command == 'create'}">
            <acme:submit 
                code="authenticated.flight-crew-member.form.button.create" 
                action="/authenticated/flight-crew-member/create"
            />
        </jstl:when>
    </jstl:choose>

</acme:form>
