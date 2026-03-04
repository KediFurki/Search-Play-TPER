<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>SP Lite &ndash; Search Calls</title>
    <link rel="stylesheet" href="css/uikit.min.css" />
    <script src="js/uikit.min.js"></script>
    <script src="js/uikit-icons.min.js"></script>
    <style>
        .tableView {
            width: 100%;
            border-collapse: collapse;
            font-family: Arial, sans-serif;
        }
        thead th {
            top: 0;
            position: sticky;
            z-index: 2;
            background-color: #004080;
            color: white;
            text-align: center;
            vertical-align: middle;
            padding: 8px 10px;
            border: 1px solid #ddd;
        }
        tbody tr { border-bottom: 1px solid #ddd; }
        tbody td {
            padding: 8px 10px;
            border: 1px solid #ddd;
            text-align: center;
            color: #333;
        }
        .bottom {
            width: 100%;
            height: 70px;
            border-top: 1px #CCC solid;
            background: white;
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-top: 20px;
            padding: 0 10px;
        }
        .highlight { background-color: #83AF9B; }
    </style>
</head>
<body>
<nav class="uk-navbar-container" uk-navbar style="background-color: #004080;">
    <div class="uk-navbar-left">
        <a class="uk-navbar-item uk-logo" href="#" style="color: white; font-weight: bold;">
            TPER Search &amp; Play
        </a>
    </div>
    <div class="uk-navbar-right">
        <div class="uk-navbar-item">
            <a href="tperApp?action=logout" class="uk-button uk-button-danger uk-button-small">
                <span uk-icon="icon: sign-out"></span> Logout
            </a>
        </div>
    </div>
</nav>
<c:if test="${not empty retentionMsg}">
    <div class="uk-alert-primary uk-margin-small-top uk-margin-small-left uk-margin-small-right" uk-alert>
        <a class="uk-alert-close" uk-close></a>
        <p><c:out value="${retentionMsg}"/></p>
    </div>
</c:if>

<c:if test="${not empty error}">
    <div class="uk-alert-danger uk-margin-small-top uk-margin-small-left uk-margin-small-right" uk-alert>
        <a class="uk-alert-close" uk-close></a>
        <p><c:out value="${error}"/></p>
    </div>
</c:if>
<div class="uk-container uk-container-expand uk-margin-small-top">
    <form id="searchForm" action="tperApp" method="post" class="uk-grid-small uk-flex-middle" uk-grid>
        <input type="hidden" name="action" value="searchCall" />
        <input type="hidden" id="currentpage" name="currentpage"
               value="${not empty currentpage ? currentpage : 1}" />
        <input type="hidden" id="order" name="order"
               value="${not empty order ? order : 'desc'}" />

        <div class="uk-width-1-6@m">
            <label class="uk-form-label">From</label>
            <input class="uk-input uk-form-small" type="datetime-local"
                   name="from" value="<c:out value='${from}'/>" />
        </div>
        <div class="uk-width-1-6@m">
            <label class="uk-form-label">To</label>
            <input class="uk-input uk-form-small" type="datetime-local"
                   name="to" value="<c:out value='${to}'/>" />
        </div>
        <div class="uk-width-1-6@m">
            <label class="uk-form-label">ANI</label>
            <input class="uk-input uk-form-small" type="text" name="ani"
                   value="<c:out value='${ani}'/>" placeholder="Caller number" />
        </div>
        <div class="uk-width-1-6@m">
            <label class="uk-form-label">DNIS</label>
            <input class="uk-input uk-form-small" type="text" name="dnis"
                   value="<c:out value='${dnis}'/>" placeholder="Called number" />
        </div>
        <div class="uk-width-auto@m" style="padding-top:20px;">
            <button type="submit" class="uk-button uk-button-primary uk-button-small">
                <span uk-icon="icon: search"></span> Search
            </button>
        </div>
    </form>
</div>
<div class="uk-container uk-container-expand uk-margin-small-top">
    <table class="tableView">
        <thead>
            <tr>
                <th>Conversation ID</th>
                <th style="cursor:pointer;" onclick="toggleOrder()">
                    Start Date
                    <c:choose>
                        <c:when test="${order == 'desc'}">
                            <span uk-icon="icon: arrow-down; ratio: 1.3"></span>
                        </c:when>
                        <c:otherwise>
                            <span uk-icon="icon: arrow-up; ratio: 1.3"></span>
                        </c:otherwise>
                    </c:choose>
                </th>
                <th>Duration</th>
                <th>ANI</th>
                <th>DNIS</th>
                <th>Division ID</th>
                <th>Direction</th>
                <th>Play</th>
                <th>Retention</th>
            </tr>
        </thead>
        <tbody>
            <c:choose>
                <c:when test="${not empty conversations and totalHits > 0}">
                    <c:forEach var="call" items="${conversations}" varStatus="st">
                        <tr id="row-${st.index}">
                            <td><c:out value="${call.conversationId}"/></td>
                            <td><c:out value="${call.formattedStart}"/></td>
                            <td><c:out value="${call.duration}"/></td>
                            <td><c:out value="${call.ani}"/></td>
                            <td><c:out value="${call.dnis}"/></td>
                            <td><c:out value="${call.divisionIds}"/></td>
                            <td><c:out value="${call.originatingDirection}"/></td>
                            <td>
                                <button type="button"
                                        class="uk-button uk-button-default uk-button-small"
                                        onclick="playAudio('row-${st.index}', '${call.conversationId}')">
                                    <span uk-icon="icon: play-circle"></span> Listen
                                </button>
                            </td>
                            <td>
                                <form action="tperApp" method="post" style="display:inline;">
                                    <input type="hidden" name="action" value="extendRetention" />
                                    <input type="hidden" name="convId"
                                           value="${call.conversationId}" />
                                    <button type="submit"
                                            class="uk-button uk-button-primary uk-button-small"
                                            onclick="return confirm('Extend retention beyond 90 days for this call?');">
                                        <span uk-icon="icon: lock"></span> Extend 90 Days
                                    </button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <tr>
                        <td colspan="9" class="uk-text-center uk-text-muted" style="padding: 30px;">
                            No records found. Please perform a search.
                        </td>
                    </tr>
                </c:otherwise>
            </c:choose>
        </tbody>
    </table>
</div>
<div id="audioPlayerModal" uk-modal="bg-close: true">
    <div class="uk-modal-dialog uk-modal-body uk-text-center"
         style="border-radius: 6px;">
        <button class="uk-modal-close-default" type="button" uk-close></button>
        <h3 class="uk-modal-title">Audio Player</h3>
        <audio id="audioPlayer" controls controlsList="nodownload"
               style="width:100%;">
            Your browser does not support the audio element.
        </audio>
    </div>
</div>
<div id="loadingModal" uk-modal="bg-close: false">
    <div class="uk-modal-dialog uk-modal-body uk-text-center"
         style="width: fit-content; border-radius: 4px;">
        <div uk-spinner></div>
        Loading page&hellip;
    </div>
</div>
<c:if test="${not empty conversations and totalHits > 0}">
    <div class="bottom">
        <div>
            <c:choose>
                <c:when test="${currentpage * pageSize > totalHits}">
                    ${totalHits}
                </c:when>
                <c:otherwise>
                    ${currentpage * pageSize}
                </c:otherwise>
            </c:choose>
            / ${totalHits}
        </div>
        <div></div>
        <div>
            <button class="uk-button uk-button-default uk-button-small"
                    onclick="goToPage(1)"
                    ${currentpage == 1 ? 'disabled' : ''}>First</button>
            <button class="uk-button uk-button-default uk-button-small"
                    onclick="goToPage(${currentpage - 1})"
                    ${currentpage == 1 ? 'disabled' : ''}>Previous</button>

            <span id="pageButtons"></span>

            <button class="uk-button uk-button-default uk-button-small"
                    onclick="goToPage(${currentpage + 1})"
                    ${currentpage >= totalPages ? 'disabled' : ''}>Next</button>
            <button class="uk-button uk-button-default uk-button-small"
                    onclick="goToPage(${totalPages})"
                    ${currentpage >= totalPages ? 'disabled' : ''}>Last</button>
        </div>
    </div>
</c:if>
<script>
    function playAudio(rowId, conversationId) {
        document.querySelectorAll("tr").forEach(function(r) {
            r.classList.remove("highlight");
        });
        var row = document.getElementById(rowId);
        if (row) row.classList.add("highlight");

        var player = document.getElementById("audioPlayer");
        player.src = "tperApp?action=playAudio&convId=" +
                     encodeURIComponent(conversationId);
        player.load();

        UIkit.modal("#audioPlayerModal").show();
    }
    function goToPage(page) {
        document.getElementById("currentpage").value = page;
        UIkit.modal("#loadingModal").show();
        document.getElementById("searchForm").submit();
    }
    function toggleOrder() {
        var orderField = document.getElementById("order");
        orderField.value = (orderField.value === "desc") ? "asc" : "desc";
        document.getElementById("currentpage").value = "1";
        UIkit.modal("#loadingModal").show();
        document.getElementById("searchForm").submit();
    }
    (function() {
        var current    = parseInt("${not empty currentpage ? currentpage : 1}", 10);
        var totalPages = parseInt("${not empty totalPages ? totalPages : 0}", 10);
        if (totalPages <= 0) return;

        var container = document.getElementById("pageButtons");
        if (!container) return;

        var startPage = Math.max(1, current - 2);
        var endPage   = Math.min(totalPages, startPage + 6);
        if (totalPages > 5) {
            if (current > 3) startPage = current - 2;
            if (totalPages - startPage < 5) startPage = totalPages - 5;
            startPage = Math.max(1, startPage);
            endPage   = Math.min(totalPages, startPage + 6);
        }

        for (var i = startPage; i <= endPage; i++) {
            var btn = document.createElement("button");
            btn.className = "uk-button uk-button-default uk-button-small";
            btn.textContent = i;
            if (i === current) {
                btn.classList.add("uk-button-primary");
                btn.disabled = true;
            } else {
                btn.onclick = (function(p) {
                    return function() { goToPage(p); };
                })(i);
            }
            container.appendChild(btn);
        }

        if (totalPages > endPage) {
            container.appendChild(document.createTextNode(" ... "));
            var lastBtn = document.createElement("button");
            lastBtn.className = "uk-button uk-button-default uk-button-small";
            lastBtn.textContent = totalPages;
            lastBtn.onclick = function() { goToPage(totalPages); };
            container.appendChild(lastBtn);
        }
    })();
</script>
</body>
</html>