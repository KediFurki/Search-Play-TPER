<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%
    // Auto-detect session: if guser exists, go to search; otherwise trigger login/OAuth
    Object guser = (session != null) ? session.getAttribute("guser") : null;
    if (guser != null) {
        response.sendRedirect(response.encodeRedirectURL("tperApp?action=searchCall"));
        return;
    } else {
        response.sendRedirect(response.encodeRedirectURL("tperApp?action=login"));
        return;
    }
%>