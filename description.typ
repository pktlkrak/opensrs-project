#import "@preview/cetz:0.4.2"
#import "@preview/cetz-plot:0.1.3": chart
#import "@preview/fletcher:0.5.8" as fletcher: diagram, node, edge
#set text(lang: "pl")
#set par(justify: true)
#set heading(numbering: "1.")
#let title = [OpenSRS - Prosty system do cyklicznego powtarzania fiszek]

#set document(title: title)
#set text(14pt)

#show link: underline

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
Postanowiłem ustanowić takie założenia, aby lepiej zrozumieć jak gotowe narzędzia, takie jak ORM Hibernate, lub serwer Spring działają od podszewki, poprzez implementację ich uproszczonych wersji.


= Serwer HTTP - "Autumn"

== Podsumowanie

Autumn zbudowany jest w dużej mierze na bibliotece #link("https://mvnrepository.com/artifact/org.reflections/reflections")[Reflections],
która wyszukuje metody z określonymi adnotacjami oraz na zestawie adnotacji, które dyktują w jaki sposób każdy z endpointów działa.
Definiują one, między innymi:

- Metody, które dane endpointy mogą obsłużyć
- Adres URI endpointu
- To, czy body ma być parsowane przez serwer, czy sam endpoint ma się tym zająć

Podczas uruchamiania aplikacji, Autumn buduje listę endpointów z narzuconymi priorytetami. W ten sposób, w momencie przyjścia nowego żądania, serwer jest w stanie szybko stwierdzić, który endpoint powinien być wykorzystany.

== Definiowanie endpointów

Podstawowym konceptem serwera Autumn jest "endpoint". Jest to metoda oznaczona adnotacją `@Endpoint`, która przyjmuje conajmniej jeden argument - `State` - Stan serwera, który reprezentuje zarówno żądanie przychodzące, jak i odpowiedź serwera.

```java
@Endpoint("/hello") // Standardowo, handler definiowany jest dla metody GET
public void sayHello(State state) throws IOException {
    state.sendString("Hi!", "text/plain");
}
```

Dodatkowo, jeżeli endpoint należy do konkretnej grupy zdefiniowanej wewnątrz jednej klasy, klasa ta może otrzymać adnotację `@Route("/.../")`. String zawarty wewnątrz adnotacji będzie traktowany przez Autumn jako prefiks każdego z endpointów wewnątrz niej:

```java
@Route("/api/")
public class TestAPI {
    @Endpoint(value = "hello", allowedMethods = { "POST" })
    public void sayHelloPost(State state) throws IOException {
        state.sendString("Hello from the POST office!", "text/plain");
    }
    @Endpoint("hello")
    public void sayHelloGet(State state) throws IOException {
        state.sendString("Hello! (GET REQUEST)", "text/plain");
    }
}
```

== Wypakowywanie danych z żądań

Autumn do pobierania danych wykorzystuje adnotacje argumentów funkcji, przykładowo
aby otrzymać wartość z parametru query, argument w funkcji należy oznaczyć adnotacją `@FromQuery("nazwa")`:

#pagebreak()

```java
@Endpoint("hello")
public void sayHelloGet(State state, @FromQuery("test") String test) throws IOException {
    state.sendString("Hello! (GET REQUEST): " + test, "text/plain");
}
```

Podobna reguła występuje w przypadku wypakowywania danych z:
- cookie (`@FromCookie("...")`) 
- body (o ile parsowanie ciała żądania jest włączone - jest to parametr `allowParsingBody` w adnotacji `@Endpoint`) (`@FromBody`)
- ścieżki dostępu (`@FromPath("nazwa")`) - Uwaga: W takiej sytuacji, `@Route` lub `@Endpoint` muszą zawierać odpowiednią informację o ścieżce - przykładowo `@Endpoint("/fields/{field}")` oraz `@FromPath("field")`.
- parametrów "extra" narzuconych przez middleware (`@FromExtra("...")`)

== Middleware

Middleware to metody podobne do endpointów, jednak zamiast adnotacji `@Endpoint`, wykorzystywana jest adnotacja `@Middleware`.
Mogą one posługiwać się tymi samymi metodami ekstrakcji danych, co endpointy.

Middleware dzielą się na lokalne (takie, które działają tylko wewnątrz klasy, w której są definiowane), oraz globalne (działające na wszystkie żądania).

Klasa, w której definiowany jest middleware musi mieć adnotację `@Route(...)`

Przykład:

```java
@Route("/api/")
public class TestAPI {
    @Middleware(scope = Middleware.Scope.CLASS)
    public void middleware(State state) {
        state.setExtra("extra", "Hello!!");
    }
    @Endpoint("hello")
    public void sayHelloGet(State state, @FromExtra("extra") String extra)
        throws IOException
    {
        state.sendString("Hello! (GET REQUEST): " + extra, "text/plain");
    }
}
```

== Objekt `State`

Objekt state, dla serwera Autumn stanowi zarówno abstrakcję żądania, jak i odpowiedzi serwera. Ma funkcje pozwalające na przykład na odczytanie zawartości strumienia
przychodzącego żądania, oraz funkjce służące do ustawiania nagłówków odpowiedzi i odczytywania nagłówków przychodzących. Wszystkie metody opisane są w Javadoc'u.

= Prosta implementacja systemu ORM

== Podsumowanie

Połączenie z bazą danych reprezentuje abstrakcyjna klasa `Database`.
Sam system ORM zaimplementowany został wewnątrz `SQLDatabaseImplementation` oraz objektów należących do pakietu `opensrs.dataobjects`.

System ten polega na założeniach, które spełniać musi każdy objekt, który zapisany zostanie wewnątrz bazy danych.

== Definiowanie objektów

Objekty danych dziedziczą po `DataObject`, oraz implementują własną wersję enumeratora `GenericParameter`, który za zadanie ma informować jakiego typu jest dana kolumna, oraz czy dane pole powinno zostać wprowadzone do bazy danych w przypadku operacji insert lub update.
Enumerator ten stanowi także prosty w obsłudze interface, który mogą wykorzystywać implementacje bazy danych aby ustawiać i odczytywać konkretne wartości z objektów, przy pomocy funkcji `getGetter` oraz `getSetter`.

Najprostrzy typ danych (samo ID) mogłoby zostać zapisane w poniższy sposób:

```java
public class SimpleDataType extends DataObject<SimpleDataType.Parameter> {
    private int id;
    public SimpleDataType() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public enum Parameter extends GenericParameter<SimpleDataType> {
        ID((BiConsumer<SimpleDataType, Integer>) Review::setId, Review::getId, ParameterType.EQ_ONLY, false),

        private final BiConsumer<Review, ?> setter;
        private final Function<Review, ?> getter;
        private final ParameterType type;
        private final boolean insert;

        private Parameter(BiConsumer<Review, ?> setter, Function<Review, Object> getter, ParameterType type, boolean insert) {
            this.setter = setter;
            this.getter = getter;
            this.type = type;
            this.insert = insert;
        }

        @Override
        public ParameterType getType() {
            return type;
        }

        @Override
        public BiConsumer<Review, ?> getSetter() {
            return setter;
        }

        @Override
        public Function<Review, ?> getGetter() {
            return getter;
        }

        @Override
        public boolean insert(){
            return insert;
        }
    }
}
```

== API bazy danych

Implementacja klasy `Database` dla każdego typu danych musi zawierać funkcje:

```java
public List<Datatype> fetchDatatypes(Filter<Datatype.Parameter> filter, Sorter<Datatype.Parameter> sorter, int limit);
public List<Datatype> updateDatatype(Filter<Datatype.Parameter> filter, Update<Datatype.Parameter>... updates);
```

Gdzie `DataType` to objekt, który powinien zostać zapisany w bazie.

Ponieważ system jest skonstruowany na zasanie niemutowalnego ledger'a, funkcja służąca do kasowania objektów nie powinna być zaimplementowana.

== Klasy pomocnicze

=== Filtry

Filtrowanie objektów w bazie danych odbywa się przy pomocy klasy `Filter`. Klasa ta symbolizuje pojedyńcze kryterum, które każdy z objektów zwróconych przez implementację bazy danych ma spełniać. Aby wykonywać bardziej skomplikowane filtrowanie, należy wykorzystać opisaną niżej klasę `FilterChain`.

Przykład filtrowania przy pomocy pojedyńczego filtru:

```java
Review r = database.fetchReview(new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, userId), null);
```

Takie zapytanie znajdzie pierwszy `Review`, którego `UID` ustawiony jest na wartość `userId`. Identyfikatory objektów mają zdefiniowany typ `EqOnly` pomimo tego, że są liczbami, z uwagi na to, iż filtrowanie po ID większych lub mniejszych niż konkretna wartość nie ma logicznego sensu.

==== Łączenie filtrów

Aby filtrorwać przy użyciu wielu kryterów, należy wykorzystać klasę `FilterChain`, która symbolizuje logiczną koniunkcję lub alternatywę z kilku parametrów. Warto zauważyć, że `FilterChain` implementuje `Filter`, więc może być on wykorzystywany rekurencyjnie.

Przykładowe zapytanie:

```java
Review rev = database.fetchReview(new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
    new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, uid),
    new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, set.hashCode()))
), null);
```

=== Sortowanie

Sortowanie zaimplementowane jest w podobny sposób do filtrów. Jedyną różnicą jest to, że w obecnej wersji tego systemu nie da się reprezentować sortowania po kilku parametrach.

Przykładowe zapytanie:

```java
Review last = database.fetchReview(
    new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
        new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, uid),
        new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, set.hashCode()),
        new Filter(Review.Parameter.UNLOCK_DATE, Filters.Number.EGREATER, 0),
        new Filter(Review.Parameter.TYPE, Filters.EqOnly.EQUAL, Review.TYPE_REVIEW))),
    new Sorter(Review.Parameter.UNLOCK_DATE, Sorters.NumberSorter.DESCENDING));
```

= Logika i endpointy systemu OpenSRS

= Podsumowanie

= Możliwe sposoby rozwoju aplikacji

