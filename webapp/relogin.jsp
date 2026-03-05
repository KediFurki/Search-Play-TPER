<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Search And Play</title>
    <link rel="icon" type="image/x-icon" href="img/favicon.ico">
    <link rel="stylesheet" href="css/uikit.min.css" />
    <script src="js/uikit.min.js"></script>
    <script src="js/uikit-icons.min.js"></script>
    <style>
        html, body {
            font: 12px "Roboto", Cairo, Sans-serif;
            line-height: 20px;
            color: #555;
            margin: 0;
            height: 100%;
        }
    </style>
</head>
<body>

<div class="uk-flex uk-flex-center uk-flex-middle uk-height-viewport"
     style="background: linear-gradient(135deg, #004080 0%, #44444E 100%);">
    <div class="uk-card uk-card-default uk-card-body uk-width-1-3@m uk-box-shadow-large uk-text-center"
         style="border-radius: 8px; padding: 40px 30px;">

        <span uk-icon="icon: lock; ratio: 3" style="color: #44444E;"></span>

        <h2 class="uk-card-title uk-margin-small-top" style="color: #44444E; font-weight: bold;">
            Search And Play
        </h2>

        <hr class="uk-divider-small uk-text-center">

        <c:choose>
            <c:when test="${not empty message}">
                <p class="uk-text-muted" style="font-size: 14px;"><c:out value="${message}"/></p>
            </c:when>
            <c:otherwise>
                <p class="uk-text-muted" style="font-size: 14px;">
                    Your session has ended or has expired due to inactivity.<br/>
                    Would you like to log in again?
                </p>
            </c:otherwise>
        </c:choose>

        <a href="<c:url value='tperApp?action=login'/>"
           class="uk-button uk-button-large uk-width-1-1 uk-margin-top"
           style="background-color: #44444E; color: #fff; border-radius: 4px; text-transform: none;">
            <span uk-icon="icon: sign-in; ratio: 1.2"></span>
            Log In Again
        </a>
    </div>
</div>

</body>
</html>
