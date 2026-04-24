function Navigation() {
    function t() {
        e(), n(), r(), o(), i(), a()
    }

    function e() {
        document.addEventListener("click", function (t) {
            L(t.target) || O(t.target) || (S() ? A() : (b(j.toggle, !1), b(j.content, !1), w(!1), E(!1)))
        })
    }

    function b(){}

    function n() {
        v(j.toggle, S())
    }

    function r() {
        var t = B(function () {
            if (S()) return A(), v(j.toggle, !0), void w(!1);
            w(T(j.content)), v(j.toggle, T(j.content))
        }, 100);
        window.addEventListener("resize", t)
    }

    function o() {
        s(j.toggle, d), U.toggles.forEach(function (t) {
            s(t, l)
        }), c()
    }

    function i() {
        u(R.triggerDesktop), f(R.triggerDesktop), u(R.triggerMobile), f(R.triggerMobile)
    }

    function a() {
        function t() {
            r(), o()
        }

        function e() {
            return window.innerWidth < a
        }

        function n() {
            return !e()
        }

        function r() {
            if (!e()) {
                var t = document.querySelector(".global-header");
                i(c.getBoundingClientRect().bottom - t.offsetHeight > 0 ? "add" : "remove")
            }
        }

        function o() {
            n() || i("add")
        }

        function i(t) {
            document.querySelector(".navigation-shortcuts").classList[t]("hidden")
        }
        var a = 1024,
            s = document.querySelector(".dashboard");
        if (s) {
            var c = s.querySelector(".lessons-and-reviews");
            c && (window.addEventListener("scroll", t), window.addEventListener("resize", t))
        }
    }

    function s(t, e) {
        t.addEventListener("click", function (n) {
            e(n, t)
        })
    }

    function c() {
        window.addEventListener("keydown", function (t) {
            if ("Escape" === t.key) {
                var e = document.querySelector('button.sitemap__section-header[data-expanded="true"]');
                return e && (e.focus(), l(t, e)), !1
            }
        })
    }

    function u(t) {
        t.addEventListener("click", function (e) {
            h(e, t)
        })
    }

    function f(t) {
        window.addEventListener("keydown", function (e) {
            P(e, t) && h(e, t), R.input === document.activeElement && "Escape" === e.key && h(e, t)
        })
    }

    function l(t, e) {
        t.preventDefault(), S() && A(e), m(e, !T(e)), _(e)
    }

    function d(t, e) {
        t.preventDefault(), m(e, !T(e)), w(), E(), D(e, "navigationContent").forEach(function (t) {
            b(t, !T(t))
        })
    }

    function h(t, e) {
        t.preventDefault(), p(e), m(e, !T(e)), _(e)
    }

    function p(t) {
        R.triggerDesktop.classList.toggle("hover:border-transparent-1"), R.triggerDesktop.classList.toggle("border-transparent-3"), R.triggerMobile.classList.toggle("border-transparent-3"), R.triggerDesktop.classList.toggle("hover:border-transparent-3"), R.content.classList.toggle("hidden"), R.content.classList.contains("hidden") ? t.focus() : ($(window).scrollTop(0), R.input.focus(), R.input.select())
    }

    function y(t) {
        var e = t.style.transition;
        t.style.transition = "", requestAnimationFrame(function () {
            g(t), t.style.transition = e, requestAnimationFrame(function () {
                x(t)
            })
        })
    }

    function g(t) {
        t.style.height = t.scrollHeight + "px"
    }

    function m(t, e) {
        v(t, e), b(t, e)
    }

    function b(t, e) {
        t.setAttribute("data-expanded", e.toString())
    }

    function v(t, e) {
        t.setAttribute("aria-expanded", e.toString())
    }

    function w(t) {
        document.body.classList.toggle("fixed", t)
    }

    function E(t) {
        j.element.classList.toggle("navigation--focused", t)
    }

    function A(t) {
        U.elements.forEach(function (e) {
            t && D(t, "navigationSectionContent").indexOf(e) >= 0 || (b(e, !1), x(e))
        }), U.toggles.forEach(function (e) {
            e !== t && m(e, !1)
        })
    }

    function _(t) {
        D(t, "navigationSectionContent").forEach(function (t) {
            S() || (T(t) ? y(t) : g(t)), b(t, !T(t))
        })
    }

    function x(t) {
        t.style.height = ""
    }

    function B(t, e, n) {
        var r;
        return function () {
            var o = this,
                i = arguments,
                a = function () {
                    r = null, n || t.apply(o, i)
                },
                s = n && !r;
            clearTimeout(r), r = setTimeout(a, e), s && t.apply(o, i)
        }
    }

    function D(t, e) {
        for (var n = t.parentNode.children, r = [], o = 0; o < n.length; o += 1) {
            var i = n[o];
            i.dataset[e] !== undefined && r.push(i)
        }
        return r
    }

    function T(t) {
        return "true" === t.dataset.expanded
    }

    function S() {
        return document.body.clientWidth >= k.medium
    }

    function O(t) {
        return t.closest("[data-navigation-content]") === j.content
    }

    function L(t) {
        return t === j.toggle || t.closest("[data-navigation-toggle]")
    }

    function P(t, e) {
        return e === document.activeElement && ("Enter" === t.key || " " === t.key)
    }
    var k = {
            small: 480,
            medium: 1024,
            large: 1280
        },
        U = {
            elements: document.querySelectorAll("[data-navigation-section-content]"),
            toggles: document.querySelectorAll("[data-navigation-section-toggle]")
        },
        j = {
            content: document.querySelector("[data-navigation-content]"),
            element: document.querySelector(".navigation"),
            toggle: document.querySelector("[data-navigation-toggle]")
        },
        R = {
            triggerDesktop: document.getElementById("search__trigger"),
            triggerMobile: document.getElementById("search__trigger__mobile"),
            content: document.getElementById("search-bar"),
            input: document.getElementsByClassName("search-query")[0]
        };
    return {
        init: t
    }
}

function removeGravatarBackgroundImage() {
    document.querySelectorAll(".user-avatar-default").forEach(function (t) {
        t.setAttribute("style", "")
    })
}

! function () {
    "use strict";
    if (self.document) {
        var t = KeyboardEvent.prototype,
            e = Object.getOwnPropertyDescriptor(t, "key");
        if (e) {
            var n = {
                Win: "Meta",
                Scroll: "ScrollLock",
                Spacebar: " ",
                Down: "ArrowDown",
                Left: "ArrowLeft",
                Right: "ArrowRight",
                Up: "ArrowUp",
                Del: "Delete",
                Apps: "ContextMenu",
                Esc: "Escape",
                Multiply: "*",
                Add: "+",
                Subtract: "-",
                Decimal: ".",
                Divide: "/"
            };
            Object.defineProperty(t, "key", {
                get: function () {
                    var t = e.get.call(this);
                    return n.hasOwnProperty(t) ? n[t] : t
                }
            })
        }
    }
}(),
function (t, e) {
    "object" == typeof exports && "undefined" != typeof module ? e(exports) : "function" == typeof define && define.amd ? define(["exports"], e) : e(t.WHATWGFetch = {})
}(this, function (t) {
    "use strict";

    function e(t) {
        return t && DataView.prototype.isPrototypeOf(t)
    }

    function n(t) {
        if ("string" != typeof t && (t = String(t)), /[^a-z0-9\-#$%&'*+.^_`|~]/i.test(t)) throw new TypeError("Invalid character in header field name");
        return t.toLowerCase()
    }

    function r(t) {
        return "string" != typeof t && (t = String(t)), t
    }

    function o(t) {
        var e = {
            next: function () {
                var e = t.shift();
                return {
                    done: e === undefined,
                    value: e
                }
            }
        };
        return v.iterable && (e[Symbol.iterator] = function () {
            return e
        }), e
    }

    function i(t) {
        this.map = {}, t instanceof i ? t.forEach(function (t, e) {
            this.append(e, t)
        }, this) : Array.isArray(t) ? t.forEach(function (t) {
            this.append(t[0], t[1])
        }, this) : t && Object.getOwnPropertyNames(t).forEach(function (e) {
            this.append(e, t[e])
        }, this)
    }

    function a(t) {
        if (t.bodyUsed) return Promise.reject(new TypeError("Already read"));
        t.bodyUsed = !0
    }

    function s(t) {
        return new Promise(function (e, n) {
            t.onload = function () {
                e(t.result)
            }, t.onerror = function () {
                n(t.error)
            }
        })
    }

    function c(t) {
        var e = new FileReader,
            n = s(e);
        return e.readAsArrayBuffer(t), n
    }

    function u(t) {
        var e = new FileReader,
            n = s(e);
        return e.readAsText(t), n
    }

    function f(t) {
        for (var e = new Uint8Array(t), n = new Array(e.length), r = 0; r < e.length; r++) n[r] = String.fromCharCode(e[r]);
        return n.join("")
    }

    function l(t) {
        if (t.slice) return t.slice(0);
        var e = new Uint8Array(t.byteLength);
        return e.set(new Uint8Array(t)), e.buffer
    }

    function d() {
        return this.bodyUsed = !1, this._initBody = function (t) {
            this._bodyInit = t, t ? "string" == typeof t ? this._bodyText = t : v.blob && Blob.prototype.isPrototypeOf(t) ? this._bodyBlob = t : v.formData && FormData.prototype.isPrototypeOf(t) ? this._bodyFormData = t : v.searchParams && URLSearchParams.prototype.isPrototypeOf(t) ? this._bodyText = t.toString() : v.arrayBuffer && v.blob && e(t) ? (this._bodyArrayBuffer = l(t.buffer), this._bodyInit = new Blob([this._bodyArrayBuffer])) : v.arrayBuffer && (ArrayBuffer.prototype.isPrototypeOf(t) || E(t)) ? this._bodyArrayBuffer = l(t) : this._bodyText = t = Object.prototype.toString.call(t) : this._bodyText = "", this.headers.get("content-type") || ("string" == typeof t ? this.headers.set("content-type", "text/plain;charset=UTF-8") : this._bodyBlob && this._bodyBlob.type ? this.headers.set("content-type", this._bodyBlob.type) : v.searchParams && URLSearchParams.prototype.isPrototypeOf(t) && this.headers.set("content-type", "application/x-www-form-urlencoded;charset=UTF-8"))
        }, v.blob && (this.blob = function () {
            var t = a(this);
            if (t) return t;
            if (this._bodyBlob) return Promise.resolve(this._bodyBlob);
            if (this._bodyArrayBuffer) return Promise.resolve(new Blob([this._bodyArrayBuffer]));
            if (this._bodyFormData) throw new Error("could not read FormData body as blob");
            return Promise.resolve(new Blob([this._bodyText]))
        }, this.arrayBuffer = function () {
            return this._bodyArrayBuffer ? a(this) || Promise.resolve(this._bodyArrayBuffer) : this.blob().then(c)
        }), this.text = function () {
            var t = a(this);
            if (t) return t;
            if (this._bodyBlob) return u(this._bodyBlob);
            if (this._bodyArrayBuffer) return Promise.resolve(f(this._bodyArrayBuffer));
            if (this._bodyFormData) throw new Error("could not read FormData body as text");
            return Promise.resolve(this._bodyText)
        }, v.formData && (this.formData = function () {
            return this.text().then(y)
        }), this.json = function () {
            return this.text().then(JSON.parse)
        }, this
    }

    function h(t) {
        var e = t.toUpperCase();
        return A.indexOf(e) > -1 ? e : t
    }

    function p(t, e) {
        var n = (e = e || {}).body;
        if (t instanceof p) {
            if (t.bodyUsed) throw new TypeError("Already read");
            this.url = t.url, this.credentials = t.credentials, e.headers || (this.headers = new i(t.headers)), this.method = t.method, this.mode = t.mode, this.signal = t.signal, n || null == t._bodyInit || (n = t._bodyInit, t.bodyUsed = !0)
        } else this.url = String(t);
        if (this.credentials = e.credentials || this.credentials || "same-origin", !e.headers && this.headers || (this.headers = new i(e.headers)), this.method = h(e.method || this.method || "GET"), this.mode = e.mode || this.mode || null, this.signal = e.signal || this.signal, this.referrer = null, ("GET" === this.method || "HEAD" === this.method) && n) throw new TypeError("Body not allowed for GET or HEAD requests");
        this._initBody(n)
    }

    function y(t) {
        var e = new FormData;
        return t.trim().split("&").forEach(function (t) {
            if (t) {
                var n = t.split("="),
                    r = n.shift().replace(/\+/g, " "),
                    o = n.join("=").replace(/\+/g, " ");
                e.append(decodeURIComponent(r), decodeURIComponent(o))
            }
        }), e
    }

    function g(t) {
        var e = new i;
        return t.replace(/\r?\n[\t ]+/g, " ").split(/\r?\n/).forEach(function (t) {
            var n = t.split(":"),
                r = n.shift().trim();
            if (r) {
                var o = n.join(":").trim();
                e.append(r, o)
            }
        }), e
    }

    function m(t, e) {
        e || (e = {}), this.type = "default", this.status = e.status === undefined ? 200 : e.status, this.ok = this.status >= 200 && this.status < 300, this.statusText = "statusText" in e ? e.statusText : "OK", this.headers = new i(e.headers), this.url = e.url || "", this._initBody(t)
    }

    var v = {
        searchParams: "URLSearchParams" in self,
        iterable: "Symbol" in self && "iterator" in Symbol,
        blob: "FileReader" in self && "Blob" in self && function () {
            try {
                return new Blob, !0
            } catch (t) {
                return !1
            }
        }(),
        formData: "FormData" in self,
        arrayBuffer: "ArrayBuffer" in self
    };
    if (v.arrayBuffer) var w = ["[object Int8Array]", "[object Uint8Array]", "[object Uint8ClampedArray]", "[object Int16Array]", "[object Uint16Array]", "[object Int32Array]", "[object Uint32Array]", "[object Float32Array]", "[object Float64Array]"],
        E = ArrayBuffer.isView || function (t) {
            return t && w.indexOf(Object.prototype.toString.call(t)) > -1
        };
    i.prototype.append = function (t, e) {
        t = n(t), e = r(e);
        var o = this.map[t];
        this.map[t] = o ? o + ", " + e : e
    }, i.prototype["delete"] = function (t) {
        delete this.map[n(t)]
    }, i.prototype.get = function (t) {
        return t = n(t), this.has(t) ? this.map[t] : null
    }, i.prototype.has = function (t) {
        return this.map.hasOwnProperty(n(t))
    }, i.prototype.set = function (t, e) {
        this.map[n(t)] = r(e)
    }, i.prototype.forEach = function (t, e) {
        for (var n in this.map) this.map.hasOwnProperty(n) && t.call(e, this.map[n], n, this)
    }, i.prototype.keys = function () {
        var t = [];
        return this.forEach(function (e, n) {
            t.push(n)
        }), o(t)
    }, i.prototype.values = function () {
        var t = [];
        return this.forEach(function (e) {
            t.push(e)
        }), o(t)
    }, i.prototype.entries = function () {
        var t = [];
        return this.forEach(function (e, n) {
            t.push([n, e])
        }), o(t)
    }, v.iterable && (i.prototype[Symbol.iterator] = i.prototype.entries);
    var A = ["DELETE", "GET", "HEAD", "OPTIONS", "POST", "PUT"];
    p.prototype.clone = function () {
        return new p(this, {
            body: this._bodyInit
        })
    }, d.call(p.prototype), d.call(m.prototype), m.prototype.clone = function () {
        return new m(this._bodyInit, {
            status: this.status,
            statusText: this.statusText,
            headers: new i(this.headers),
            url: this.url
        })
    }, m.error = function () {
        var t = new m(null, {
            status: 0,
            statusText: ""
        });
        return t.type = "error", t
    };
    var _ = [301, 302, 303, 307, 308];
    m.redirect = function (t, e) {
        if (-1 === _.indexOf(e)) throw new RangeError("Invalid status code");
        return new m(null, {
            status: e,
            headers: {
                location: t
            }
        })
    }, t.DOMException = self.DOMException;
    try {
        new t.DOMException
    } catch (x) {
        t.DOMException = function (t, e) {
            this.message = t, this.name = e;
            var n = Error(t);
            this.stack = n.stack
        }, t.DOMException.prototype = Object.create(Error.prototype), t.DOMException.prototype.constructor = t.DOMException
    }
}), Element.prototype.matches || (Element.prototype.matches = Element.prototype.msMatchesSelector || Element.prototype.webkitMatchesSelector), Element.prototype.closest || (Element.prototype.closest = function (t) {
    var e = this;
    do {
        if (e.matches(t)) return e;
        e = e.parentElement || e.parentNode
    } while (null !== e && 1 === e.nodeType);
    return null
}), Array.prototype.forEach || (Array.prototype.forEach = function (t) {
    var e, n;
    if (null == this) throw new TypeError("this is null or not defined");
    var r = Object(this),
        o = r.length >>> 0;
    if ("function" != typeof t) throw new TypeError(t + " is not a function");
    for (arguments.length > 1 && (e = arguments[1]), n = 0; n < o;) {
        var i;
        n in r && (i = r[n], t.call(e, i, n, r)), n++
    }
}), window.NodeList && !NodeList.prototype.forEach && (NodeList.prototype.forEach = function (t, e) {
    e = e || window;
    for (var n = 0; n < this.length; n++) t.call(e, this[n], n, this)
});
var navigation = Navigation();
navigation.init();