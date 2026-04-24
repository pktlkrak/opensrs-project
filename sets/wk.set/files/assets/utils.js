"use strict";
(() => {
    const InfoTip = {
        enlargeCharacters: () => {
            $(".radical-icon, .kanji-icon, .vocabulary-icon").mouseenter(function () {
                return $(this).find("img").length > 0 || $(this).find("i[class^=radical-]").length > 0 ? void $(this).prepend('<span id="enlarge-hover" class="' + $(this).attr("class") + ' enlarge-hover">' + $(this).html() + "</span>") : void $(this).prepend('<span id="enlarge-hover" class="' + $(this).attr("class") + ' enlarge-hover">' + $(this).text() + "</span>")
            }), $(".radical-icon, .kanji-icon, .vocabulary-icon").mouseleave(function () {
                $("#enlarge-hover").remove()
            })
        },
        popover: () => {
            $("[rel=popover]").popover({
                trigger: "hover",
                template: '<div class="popover default"><div class="arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>'
            })
        },
        popoverSrs: () => {
            var e;
            e = function (e, t) {
                var n, i;
                return i = window.innerWidth, n = $(t).offset().left, i < 500 ? "bottom" : i - n > 460 ? "right" : "left"
            };
            $("[rel=popover-srs]").popover({
                html: !0,
                trigger: "hover",
                animation: !1,
                placement: e,
                container: "body",
                template: '<div class="popover srs"><div class="arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>'
            })
        },
        popoverLattice: () => {
            var e;
            e = function (e, t) {
                var n, i;
                return i = window.innerWidth, n = $(t).offset().left, i < 500 ? "bottom" : i - n > 400 ? "right" : "left"
            }, $("[rel=auto-popover]").popover({
                html: !0,
                animation: !1,
                trigger: "hover",
                container: "body",
                placement: e,
                template: '<div class="popover lattice"><div class="arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>'
            })
        }
    }
    const DateTime = {
        tooltip: () => {
            $("[rel=tooltip]").tooltip({
                container: "body"
            })
        },

        epochToFormatted: (e) => {
            var t, n, i, r, o;
            return r = new Array("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"), t = (o = new Date(1e3 * e)).getDate(), n = o.getMonth(), i = o.getFullYear(), r[n] + " " + t + ", " + i
        },

        dailyTimeStamp: () => {
            var e, t, n;
            return e = (n = new Date).getDate(), t = n.getMonth(), n.getFullYear() + "" + t + e
        },

        activateRelativeDate: () => {
            var e;
            (e = function () {
                $("time.relative_date").each(function () {
                    var e;
                    (e = $(this)).text(DateTime.formatRelativeDate(e.attr("datetime")))
                }), setTimeout(e, 6e4)
            })()
        },

        formatRelativeDate: (e) => {
            var t, n, i;
            if (i = new Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"), e = isNaN(e) ? e : 1e3 * parseInt(e), n = new Date(e), null !== e) return t = n.getDate(), i[n.getMonth()] + " " + t
        },

        activateFuzzyDate: () => {
            var e;
            (e = function () {
                $("time.timeago").each(function () {
                    var e, t, n;
                    t = (e = $(this)).attr("datetime"), n = e.data("show-past"), e.text(DateTime.formatTimeAgo(t, n))
                }), setTimeout(e, 6e4)
            })()
        },

        insertFuzzyDate: (e, t) => {
            var n;
            (n = $(e + " time.timeago")).attr("datetime", t).text(DateTime.formatTimeAgo(t, n.data("show-past")))
        },

        formatTimeAgo: (e, t) => {
            var n, i;
            return null == t && (t = !0), t = !0 === t || "true" === t, e = isNaN(e) ? e : 1e3 * parseInt(e), i = new Date(e), n = new Date, null === e ? "N/A" : i > n ? $.timeago(i) : i <= n ? t ? $.timeago(i) : "Available Now" : void 0
        }
    }

    const DashBoard = {
        messageStatus: () => {
            var e, t, n, i, r, o;
            i = $(".hide-toggle"), o = i.closest("section"), r = o.children("div"), e = $.cookie(o.attr("class") + "_message_status"), t = function (e) {
                r.hide(), $(e).children("i").removeClass("icon-chevron-up").addClass("icon-chevron-down"), $(e).removeClass("hide-toggle").addClass("show-toggle"), $.cookie(o.attr("class") + "_message_status", "false", {
                    expires: 365
                })
            }, n = function (e) {
                r.show(), $(e).children("i").removeClass("icon-chevron-down").addClass("icon-chevron-up"), $(e).removeClass("show-toggle").addClass("hide-toggle"), $.cookie(o.attr("class") + "_message_status", "true", {
                    expires: 365
                })
            }, "true" !== e && t(i), $(i).click(function () {
                $(r).is(":visible") ? t(this) : n(this)
            })
        }
    }

    const Counts = {
        fillInSrsCount: (e) => {
            document.querySelector("#apprentice span").innerText = e.apprentice.total;
            document.querySelector("#guru span").innerText = e.guru.total;
            document.querySelector("#master span").innerText = e.master.total;
            document.querySelector("#enlightened span").innerText = e.enlighten.total;
            document.querySelector("#burned span").innerText = e.burned.total;
            document.querySelector("#apprentice").setAttribute("data-content", "<ul><li>Radicals<span>" + e.apprentice.radicals + "</span></li><li>Kanji<span>" + e.apprentice.kanji + "</span></li><li>Vocabulary<span>" + e.apprentice.vocabulary + "</span></li></ul>");
            document.querySelector("#guru").setAttribute("data-content", "<ul><li>Radicals<span>" + e.guru.radicals + "</span></li><li>Kanji<span>" + e.guru.kanji + "</span></li><li>Vocabulary<span>" + e.guru.vocabulary + "</span></li></ul>");
            document.querySelector("#master").setAttribute("data-content", "<ul><li>Radicals<span>" + e.master.radicals + "</span></li><li>Kanji<span>" + e.master.kanji + "</span></li><li>Vocabulary<span>" + e.master.vocabulary + "</span></li></ul>");
            document.querySelector("#enlightened").setAttribute("data-content", "<ul><li>Radicals<span>" + e.enlighten.radicals + "</span></li><li>Kanji<span>" + e.enlighten.kanji + "</span></li><li>Vocabulary<span>" + e.enlighten.vocabulary + "</span></li></ul>");
            document.querySelector("#burned").setAttribute("data-content", "<ul><li>Radicals<span>" + e.burned.radicals + "</span></li><li>Kanji<span>" + e.burned.kanji + "</span></li><li>Vocabulary<span>" + e.burned.vocabulary + "</span></li></ul>");
        }
    }

    Object.defineProperty(window, "DashBoard", {
        value: DashBoard,
        writable: false
    });
    Object.defineProperty(window, "InfoTip", {
        value: InfoTip,
        writable: false
    });
    Object.defineProperty(window, "DateTime", {
        value: DateTime,
        writable: false
    });
    Object.defineProperty(window, "Count", {
        value: Counts,
        writable: false
    })
})();