<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
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
        }

        .uk-tile {
            padding: 10px;
        }

        .uk-button-group {
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 10px;
            width: 100%;
        }

        .uk-button-small {
            padding: 0;
            padding-left: 7px;
            padding-right: 7px;
            cursor: pointer;
            border: 0px;
            text-transform: none;
        }

        .uk-button:hover:enabled {
            color: #fff !important;
            background-color: #585858;
            background: linear-gradient(to bottom, #585858 0%, #111 100%);
        }

        .uk-button:disabled {
            color: grey;
        }

        .highlight {
            background-color: #83AF9B;
        }

        .rowselected {
            background-color: rgb(176, 255, 194);
        }

        td {
            padding: 5px;
            border-bottom: 1px #CCC solid;
            text-overflow: ellipsis;
            white-space: nowrap;
            max-width: 2px;
            overflow: hidden;
            text-align: left;
            cursor: pointer;
        }

        .uk-table th {
            vertical-align: middle;
            padding-left: 0px;
            padding-right: 0px;
            text-align: center;
        }

        .uk-table td {
            padding-top: 5px;
            padding-bottom: 5px;
        }

        .bottom {
            position: fixed;
            bottom: 0;
            width: 100%;
            height: 90px;
            border-top: 1px #CCC solid;
            background: white;
            z-index: 10;
        }

        .select {
            border: 1px #ccc solid !important;
            color: #CCC;
        }

        .clear-all {
            background-color: white;
            color: red;
        }

        .search-panel {
            background-color: #f8f8f8;
            border: 1px solid #e5e5e5;
            border-radius: 4px;
            padding: 15px;
        }

        /* Tablo responsive - dar ekranlarda yatay scroll */
        .table-wrapper {
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
        }

        /* Genesys Cloud Apps embed uyumu */
        @media (max-width: 900px) {
            .uk-button-small {
                font-size: 10px;
                padding-left: 5px;
                padding-right: 5px;
            }
            .bottom {
                height: 80px;
            }
        }
    </style>
</head>
<body>

<!-- ======== NAVBAR (Referans: main.jsp üst bar yapısı) ======== -->
<nav class="uk-navbar-container" uk-navbar style="background-color: #44444E; padding-left: 10px; padding-right: 10px;">
    <div class="uk-navbar-left">
        <a class="uk-navbar-item uk-logo" href="#" style="color: white; font-weight: bold; text-decoration: none; font-size: 14px;">
            <span uk-icon="icon: receiver; ratio: 1.2" style="margin-right: 5px;"></span>
            Search And Play
        </a>
    </div>
    <div class="uk-navbar-center">
        <span class="uk-navbar-item" style="color: rgba(255,255,255,0.5); font-size: 10px;">
            &nbsp;
        </span>
    </div>
    <div class="uk-navbar-right">
        <div class="uk-navbar-item">
            <c:if test="${not empty sessionScope.gui_user}">
                <span style="color: rgba(255,255,255,0.7); margin-right: 10px; font-size: 12px;">
                    <span uk-icon="icon: user; ratio: 0.8"></span>
                    <c:out value="${sessionScope.gui_user}"/>
                </span>
            </c:if>
            <a href="<c:url value='tperApp?action=logout'/>"
               style="text-decoration: none; color: white;" title="Logout">
                <span uk-icon="icon: sign-out; ratio: 1.1" style="color: white;"></span>
            </a>
        </div>
    </div>
</nav>

<!-- ======== ALERTS ======== -->
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

<!-- ======== SEARCH FORM (Referans: main.jsp + CallList.jsp arama yapısı) ======== -->
<div style="margin: 10px 15px 0 15px;">
    <form id="searchForm" action="<c:url value='tperApp'/>" method="post">
        <input type="hidden" name="action" value="searchCall" />
        <input type="hidden" id="currentpage" name="currentpage"
               value="${not empty currentpage ? currentpage : 1}" />
        <input type="hidden" id="order" name="order"
               value="${not empty order ? order : 'desc'}" />

        <div class="search-panel">
            <div class="uk-tile uk-tile-muted" style="background-color: #44444E; color: white; border-radius: 4px 4px 0 0; margin: -15px -15px 15px -15px; padding: 8px 15px; user-select: none; font-weight: bold;">
                <span uk-icon="icon: search; ratio: 0.9" style="margin-right: 5px;"></span> SEARCH FILTERS
            </div>
            <div class="uk-grid-small" uk-grid>
                <div class="uk-width-1-4@m">
                    <label class="uk-form-label" style="font-size: 11px; color: #44444E; font-weight: bold;">FROM:</label>
                    <input class="uk-input uk-form-small" type="datetime-local"
                           name="from" value="<c:out value='${from}'/>" />
                </div>
                <div class="uk-width-1-4@m">
                    <label class="uk-form-label" style="font-size: 11px; color: #44444E; font-weight: bold;">TO:</label>
                    <input class="uk-input uk-form-small" type="datetime-local"
                           name="to" value="<c:out value='${to}'/>" />
                </div>
                <div class="uk-width-1-4@m">
                    <label class="uk-form-label" style="font-size: 11px; color: #44444E; font-weight: bold;">CONVERSATION ID:</label>
                    <input class="uk-input uk-form-small" type="text" name="conversationId"
                           value="<c:out value='${conversationId}'/>" placeholder="Conversation Id" />
                </div>
                <div class="uk-width-1-4@m">
                    <label class="uk-form-label" style="font-size: 11px; color: #44444E; font-weight: bold;">OPERATOR:</label>
                    <input class="uk-input uk-form-small" type="text" name="operator"
                           value="<c:out value='${operator}'/>" placeholder="Operator name" />
                </div>
                <div class="uk-width-1-4@m">
                    <label class="uk-form-label" style="font-size: 11px; color: #44444E; font-weight: bold;">ANI (CALLER):</label>
                    <input class="uk-input uk-form-small" type="text" name="ani"
                           value="<c:out value='${ani}'/>" placeholder="Caller number" />
                </div>
                <div class="uk-width-1-4@m">
                    <label class="uk-form-label" style="font-size: 11px; color: #44444E; font-weight: bold;">DNIS (CALLED):</label>
                    <input class="uk-input uk-form-small" type="text" name="dnis"
                           value="<c:out value='${dnis}'/>" placeholder="Called number" />
                </div>
                <div class="uk-width-1-4@m">
                    <label class="uk-form-label" style="font-size: 11px; color: #44444E; font-weight: bold;">QUEUE:</label>
                    <input class="uk-input uk-form-small" type="text" name="queue"
                           value="<c:out value='${queue}'/>" placeholder="Queue name" />
                </div>
                <div class="uk-width-1-4@m uk-flex uk-flex-bottom" style="gap: 10px;">
                    <button type="submit" class="uk-button uk-button-small"
                            style="background-color: #44444E; color: white;">
                        <span uk-icon="icon: search; ratio: 0.8"></span> Search
                    </button>
                    <button type="button" id="clear-all-btn" class="clear-all uk-button uk-button-small"
                            style="border: 1px solid #ccc;">
                        <span uk-icon="icon: close; ratio: 0.8"></span> Clear
                    </button>
                </div>
            </div>
        </div>
    </form>
</div>

<!-- ======== RESULTS TABLE (Referans: CallList.jsp tablo yapısı) ======== -->
<div style="height: 100%; padding-left: 10px; padding-right: 10px;">
    <div class="table-wrapper">
    <table id="resizeMe" class="uk-table uk-table-hover uk-table-divider" style="width: 100%; margin-bottom: 95px; margin-top: 10px;">
        <thead style="width: 100%; table-layout: fixed; position: sticky; top: 0; padding-bottom: 0px;">
            <tr>
                <th class="resizerTarget" style="padding-bottom: 2px; padding-top: 4px; cursor: pointer; min-width: 70px; text-align: center;" onclick="toggleOrder()">
                    <nobr style="padding-right: 5px;">DATE
                    <c:choose>
                        <c:when test="${order == 'desc'}">
                            <span class="uk-margin-xsmall-right" uk-icon="icon: arrow-down; ratio: 1"></span>
                        </c:when>
                        <c:otherwise>
                            <span class="uk-margin-xsmall-right" uk-icon="icon: arrow-up; ratio: 1"></span>
                        </c:otherwise>
                    </c:choose>
                    </nobr>
                </th>
                <th class="resizerTarget" style="padding-bottom: 2px; padding-top: 4px; min-width: 70px; text-align: center;"><nobr>CONVERSATION ID</nobr></th>
                <th class="resizerTarget" style="padding-bottom: 2px; padding-top: 4px; min-width: 70px; text-align: center;"><nobr>ANI</nobr></th>
                <th class="resizerTarget" style="padding-bottom: 2px; padding-top: 4px; min-width: 70px; text-align: center;"><nobr>DNIS</nobr></th>
                <th class="resizerTarget" style="padding-bottom: 2px; padding-top: 4px; min-width: 70px; text-align: center;"><nobr>QUEUE</nobr></th>
                <th class="resizerTarget" style="padding-bottom: 2px; padding-top: 4px; min-width: 70px; text-align: center;"><nobr>OPERATOR</nobr></th>
                <th class="resizerTarget" style="padding-bottom: 2px; padding-top: 4px; min-width: 70px; text-align: center;"><nobr>ACTIONS</nobr></th>
            </tr>
        </thead>
        <tbody>
            <c:choose>
                <c:when test="${not empty conversations and totalHits > 0}">
                    <c:forEach var="call" items="${conversations}" varStatus="st">
                        <tr id="row-${st.index}">
                            <td style="text-align: center;"><c:out value="${call.conversationstart}"/></td>
                            <td style="text-align: center;"><c:out value="${call.conversationid}"/></td>
                            <td style="text-align: center;"><c:out value="${call.ani}"/></td>
                            <td style="text-align: center;"><c:out value="${call.dnis}"/></td>
                            <td style="text-align: center;"><c:out value="${call.queue_name}"/></td>
                            <td style="text-align: center;"><c:out value="${call.operator_name}"/></td>
                            <td style="text-align: center;">
                                <span style="width:30px; cursor:pointer;" uk-icon="icon: play-circle;"
                                      onclick="playAudio('row-${st.index}', '${call.conversationid}')"></span>
                                <form action="<c:url value='tperApp'/>" method="post" style="display:inline;">
                                    <input type="hidden" name="action" value="extendRetention" />
                                    <input type="hidden" name="convId" value="${call.conversationid}" />
                                    <button type="submit" class="uk-button uk-button-default uk-button-small"
                                            onclick="return confirm('Extend retention for this call?');">
                                        <span uk-icon="icon: lock"></span> Extend
                                    </button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <tr>
                        <td colspan="7" class="uk-text-center uk-text-muted" style="padding: 30px; cursor: default;">
                            No records found. Please perform a search.
                        </td>
                    </tr>
                </c:otherwise>
            </c:choose>
        </tbody>
    </table>
    </div>
</div>
<div id="audioPlayerModal" uk-modal="bg-close: true">
    <div class="uk-modal-dialog uk-modal-body uk-text-center" style="border-radius: 6px;">
        <button class="uk-modal-close-default" type="button" uk-close></button>
        <h3 class="uk-modal-title" style="color: #44444E;">Audio Player</h3>
        <audio id="audioPlayer" controls controlsList="nodownload" style="width:100%;">
            Your browser does not support the audio element.
        </audio>
    </div>
</div>

<!-- ======== LOADING MODAL (Referans: CallList.jsp ricerca in corso) ======== -->
<div id="loadingModal" uk-modal="bg-close: false; esc-close: false;">
    <div class="uk-modal-dialog uk-modal-body uk-text-center"
         style="width: fit-content; border-radius: 4px;">
        <div uk-spinner></div>
        <div class="uk-margin-small-top">Loading...</div>
    </div>
</div>

<!-- ======== AUDIO LOADING MODAL ======== -->
<div id="modal-attesa-audio" uk-modal="bg-close: false; esc-close: false;">
    <div class="uk-modal-dialog uk-modal-body uk-text-center">
        <h2 class="uk-modal-title" style="color: #44444E;">Please wait...</h2>
        <p>The audio file is being prepared...</p>
        <div uk-spinner="ratio: 2"></div>
    </div>
</div>

<!-- ======== BOTTOM PAGINATION BAR (Referans: CallList.jsp sayfalama) ======== -->
<c:if test="${not empty conversations and totalHits > 0}">
    <div class="bottom">
        <table style="width: 100%;" class="uk-margin-small-top">
            <tr>
                <td style="border: 0px; text-align: left;">
                    <button class="uk-button uk-button-link uk-button-small">
                        <c:choose>
                            <c:when test="${currentpage * pageSize > totalHits}">
                                ${totalHits}
                            </c:when>
                            <c:otherwise>
                                ${currentpage * pageSize}
                            </c:otherwise>
                        </c:choose>
                        / ${totalHits}
                    </button>
                </td>
                <td style="border: 0px; text-align: center;">
                    <audio controls preload="none" id="control" style="width: 100%;">
                        <source id="_player" src="" type="audio/mpeg">
                    </audio>
                </td>
                <td style="border: 0px; text-align: right;">
                    <span>
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
                    </span>
                </td>
            </tr>
        </table>
    </div>
</c:if>

<script>
    /* ===== Clear All Button (Referans: main.jsp) ===== */
    document.getElementById('clear-all-btn').addEventListener('click', function() {
        var inputs = document.querySelectorAll('.search-panel .uk-input');
        inputs.forEach(function(input) {
            input.value = '';
        });
    });

    /* ===== Play Audio ===== */
    function playAudio(rowId, conversationId) {
        document.querySelectorAll("tr").forEach(function(r) {
            r.classList.remove("highlight");
        });
        var row = document.getElementById(rowId);
        if (row) row.classList.add("highlight");

        var player = document.getElementById("audioPlayer");
        UIkit.modal('#modal-attesa-audio').show();

        player.src = "<c:url value='tperApp'/>" + "?action=playAudio&convId=" +
                     encodeURIComponent(conversationId);
        player.load();

        player.oncanplay = function() {
            UIkit.modal('#modal-attesa-audio').hide();
            UIkit.modal("#audioPlayerModal").show();
        };

        player.onerror = function() {
            UIkit.modal('#modal-attesa-audio').hide();
            UIkit.modal.alert('Error loading or processing audio.');
        };
    }

    /* ===== Pagination ===== */
    function goToPage(page) {
        document.getElementById("currentpage").value = page;
        UIkit.modal("#loadingModal").show();
        document.getElementById("searchForm").submit();
    }

    /* ===== Order Toggle ===== */
    function toggleOrder() {
        var orderField = document.getElementById("order");
        orderField.value = (orderField.value === "desc") ? "asc" : "desc";
        document.getElementById("currentpage").value = "1";
        UIkit.modal("#loadingModal").show();
        document.getElementById("searchForm").submit();
    }

    /* ===== Page Buttons (Referans: CallList.jsp sayfalama butonları) ===== */
    (function() {
        var current    = parseInt("${not empty currentpage ? currentpage : 1}", 10);
        var totalPages = parseInt("${not empty totalPages ? totalPages : 0}", 10);
        if (totalPages <= 0) return;

        var container = document.getElementById("pageButtons");
        if (!container) return;

        var startPage = Math.max(1, current - 2);
        var endPage   = Math.min(totalPages, startPage + 5);
        if (totalPages > 5) {
            if (current > 3) startPage = current - 2;
            if (totalPages - startPage < 5) startPage = totalPages - 5;
            startPage = Math.max(1, startPage);
            endPage   = Math.min(totalPages, startPage + 5);
        }

        for (var i = startPage; i <= endPage; i++) {
            var btn = document.createElement("button");
            btn.className = "uk-button uk-button-default uk-button-small";
            btn.textContent = i;
            if (i === current) {
                btn.style.border = "1px #ccc solid";
                btn.style.color = "#CCC";
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

    /* ===== Session Timeout (Referans: main.jsp + CallList.jsp scadenzaSessione) ===== */
    function scadenzaSessione() {
        localStorage.clear();
        sessionStorage.clear();

        UIkit.modal.alert('Session expired due to inactivity. You will be redirected to the login page.')
            .then(function() {
                window.location.href = "<c:url value='tperApp?action=logout'/>";
            });
    }

    var sessionTimeoutMs = 30 * 60 * 1000; // 30 minutes
    var timeoutTimer;

    function resetTimer() {
        clearTimeout(timeoutTimer);
        timeoutTimer = setTimeout(scadenzaSessione, sessionTimeoutMs);
    }

    document.onmousemove = resetTimer;
    document.onkeypress  = resetTimer;
    document.onclick     = resetTimer;

    resetTimer();
</script>
</body>
</html>
