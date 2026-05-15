#import "@preview/cetz:0.4.2"
#import "@preview/cetz-plot:0.1.3": chart

#set text(lang: "pl")
#set par(justify: true)
#set heading(numbering: "1.")
#let title = [OpenSRS - Prosty system do cyklicznego powtarzania fiszek]

#set document(title: title)
#set text(14pt)

#set page(
  margin: 5em,
  numbering: "1",

  header: context {
    if counter(page).get().first() > 1 [
        #smallcaps({
            grid(columns: (1fr, 1fr, 1fr), align: (left, center, right),
            "Politechnika Krakowska - Java",
            "Projekt Końcowy",
            "PK, WN"
            )
            v(-.7em)
            line(start: (0%, 0pt), end: (100%, 0pt))
        })
    ] else []
  }
)

#par([
  \
  \
  \
  \
  \
  \
  \
  \
  \
  \
  \
  \
  \
  \
  \
])


#set text(20pt)

#align(center, [* #title *])
#set text(14pt)

#align(center, [
    * PK, WN *
])

#set text(11pt)

#pagebreak()
#outline(title: [Spis Treści])
#pagebreak()


#set page(
  margin: 5em,
  // columns: 2
)

= Wstęp

Projekt OpenSRS ma na celu umożliwić jego użytkownikom swobodną naukę dowolnego zestawu fiszek przy użyciu aplikacji webowej hostowanej przez wbudowany serwer HTTP.

Projekt składa się z kilku warstw, z czego każda z nich zaprojektowana była ręcznie i nie jest zaimplementowana przy użyciu bibliotek zewnętrznych.

