<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SP Lite &ndash; Login</title>
    <link rel="stylesheet" href="css/uikit.min.css" />
    <script src="js/uikit.min.js"></script>
    <script src="js/uikit-icons.min.js"></script>
</head>
<body>

<div class="uk-flex uk-flex-center uk-flex-middle uk-height-viewport">
    <div class="uk-card uk-card-default uk-card-body uk-width-1-3@m uk-box-shadow-large"
         style="border-radius: 8px;">

        <h2 class="uk-card-title uk-text-center">
            <span uk-icon="icon: lock; ratio: 1.5"></span> SP Lite
        </h2>
        <c:if test="${not empty error}">
            <div class="uk-alert-danger" uk-alert>
                <a class="uk-alert-close" uk-close></a>
                <p><c:out value="${error}"/></p>
            </div>
        </c:if>
        <form action="tperApp" method="post" class="uk-margin-top">
            <input type="hidden" name="action" value="login" />

            <div class="uk-margin uk-text-center">
                <button type="submit" class="uk-button uk-button-primary uk-button-large uk-width-1-1">
                    <span uk-icon="icon: sign-in; ratio: 1.2"></span>
                    Sign In with Genesys
                </button>
            </div>
        </form>
    </div>
</div>

</body>
</html>