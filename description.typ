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
  },
  footer: context {
    if counter(page).get().first() > 1 [
        #align(center, [
            #(counter(page).get().first())
        ])
    ]
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
Takie założenia zostały ustalone, aby lepiej zrozumieć jak gotowe narzędzia, takie jak ORM Hibernate lub serwer Spring działają od podszewki - poprzez implementację ich uproszczonych wersji.

Każdy z podsystemów projektu opisanych poniżej jest jeszcze dokładniej opisany przy użyciu JavaDoc'ów, które można znaleźć w folderze `target/reports/apidocs` po zbudowaniu projektu. 

== Czym jest SRS

SRS w tym kontekście to skrót od Spaced Repetition System. #link("https://en.wikipedia.org/wiki/Spaced_repetition")[Spaced Repetition] to technka nauki opierająca się na powtarzaniu fiszek, w której te które są określone jako lepiej znane przez powtarzającego pojawiają się coraz rzadziej i rzadziej.

Idea skupia się na najpierw wprowadzeniu fiszek w postaci lekcji w której użytkownik poznaje jej zawartość. Po poznaniu jej trafia do póli powtórek. Użytkowni w dowolnym czasie może przystąpić do powtórek, jeśli ma jakiekolwiek fiszki do powtórzenia.
Po powtórzeniu danej fiszki dostępna będzie ona jako powtórka dopiero po jakimś czasie. Clue systemu leży w tym, że im częściej użytkownik odpowiada dobrze na daną fiszkę zyskuje w niej poziom, a odpowiadając źle traci.

Każdy zestaw fiszek definiuje widełki faz w zależności od poziomu użytkownika w danej fiszce. Faza definiuje jakie odstępy czasowe będą występowały między powtózeniami fiszki oraz ile poziomów użytkownik traci za złą odpowiedź (dobra odpowiedź daje zawsze tylko jeden poziom).
Istnieje też mechanizm Coerce - gdy użytkownik odpowie na "bliską" odpowiedź zdefiniowaną dla danej fiszki, nie zostanie od razu ukarany a otrzyma związaną z jego odpowiedzią podpowiedź.

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

== Archiktura

W projekcie można wyodrębnić 4 główne częsci projektu:
- Serwer Autumn - Opisany wyżej serwer, działający za pomocą refleksji.
Odopowiada za serwowanie dostępu do określonych przes OpenSRS endpointów.
Jest on niezależny od reszty projektu więc może zostać wykorzystany do innych projektów.
- OpenSRS - Sam system SRS definiujący logikę interaktywnego systemu fiszek.
Definiuje zarówno zachowania samych endpointów jak i komunikację aplikacji z bazą danych.
- Baza danych SQLite - Baza danych przechowująca informajce o postępnie użytkowników, ich sesjach lekcji i powtórek.
- Sety - zapisane w odpowiednim folderze, definiują fiszki dla danego zestawu a także różne parametry takie jak grupy fiszek, fazy znajomości każdej z nich.
Sam OpenSRS też nie ma zdefiniowanego front endu. Każdy set mająć dostęp do endpointów wymaganych do obsługi aplikacji definiuje własne UI do obsługi danego setu.

== Autentykacja

Rejestracja i logowanie użytkowników odbywa się przez endpointy opisane w pliku `Login.java`.

Serwis OpenSRS wykorzystuje JWT do zapisania informacji o obecnie zalogowanym użytkowniku. Jest to wygodne, ponieważ serwer nie musi utrzymywać w pamięci informacji o zalogowanych użytkownikach.

Istnieją trzy endpointy związane z logowaniem:

- `/login` - Przymuje login i hasło. Jeżeli dane są poprawne, tworzy i podpisuje JWT.
- `/logout` - Usuwa cookie sesji
- `/register` - Tworzy konto użytkownika jeżeli takowe jeszcze nie istnieje.

Z uwagi na założenia projektu, według których OpenSRS był zaprojektowany aby działać w Home Labach a nie na serwerach dużej firmy, aplikacja nie ma możliwości resetu hasła.

== Lekcje
Dany Set ma w sobie wiele fiszek. 
Najpierw użytkownik musi przerobić daną fiszkę w ramach lekcji - nauczyć się jej aby móc zacząc ją powtarzać jako review.
Niektóre fiszki mogą wymagać wiedzy z poprzednich fiszek np. kanji może składać z kilku radykałów (składowych, prostrzych znaków).
Dlatego system wspiera zablokowanie lekcji za innymi lekcjami (użytkownik nie ma dostępu do lekcji o danym kanji dopóki nie przerobi jego wszsytkich radykałów).
Po przerobieniu danej fiszki jako lekcja staje się ona dostępna dla użytkownika jako powrtórka, która będzie omówiona w następnej sekcji.

OpenSRS definiuje różne endpointy do obsługi lekcji zdefiniowne w pliku `Lesson.java`:
- '/api/{set}/lesson/start' - Ustawia nowe cookie sesji lekcji, które będzie użyte do grupowania elementów poznanych w tej sesji (jeśli użytkownik pozna więcej niż jedną fiszkę podczas jednego posiedzenia będą pogrupowane tym cookie).
- '/api/{set}/lesson/queue' - Daje dostęp do kolejki dostępnych dla użytkownika lekcji. Same UI setu definiuje jak zaprezentować użytkownikowi tą kolejkę.
- '/api/{set}/lesson/completed' - Pozwala przesłać informację o zakończeniu jednej lub wielu lekcji z informacją podczas jakiej sesji dana lekcja została zakończona (można przesyłać tą informajcę np. po każdej lekcji albo po wszystkich - zależy to od samego UI setu).
- '/api/{set}/lesson/lastSession' - Daje informacje zwrotne o ostatnio zakończonej sesji - może zostać wykorzystny do wyświetlenia np. ekranu podsumowania co dany użytkownik nauczył się podczas danej sesji.

== Powtórki
Po tym jak użytkownik przerobi daną fiszkę jako lekcja staje się ona dostępna jako powtórka.
Każdy set definiuje fazy znajomości danej fiszki - najpierw użytkownik zaczyna od najniższej fazy (np. begginer).
Każde udane powtórzenie danej fiszki zwiększa poziom o 1 (numeryczna wartość reprezentująca znajomośc danej fiszki).
Każda faza ma swoje zdefiniowane widełki (np. set może zdefiniować drugą fazę, intermidiate od poziomu > 3 - użytkownik osiągnie fazę intermidiate po 3 udanych powtórzeniach fiszki).
Każde nieudane powtórzenie fiszki obniża poziom. Każdy set definuje dla każdej fazy, jaka jest w niej utrata poziomu (np. złe powtórzenie fiszki, w której użytkownik ma fazę Begginer może obniżyc poziom o 1, natomiast złe powtórzenie fiszki na fazie Guru może obniżyć poziom o 3)
Fiszek nie powtarza się też kiedy chcę - każda powtórka ustawia czas, za który najwcześniej użytkownik powinien powtórzyć daną fiszkę.
Im wyższa faza danego użytkownika, tym dłuższe okresy między kiedy należy zrobić następną powtórkę (np. powtórzenie fiszki na poziomie Begginer może ustawić powtórkę na dostepną za godzinę a powótrzenie fiszki na poziomie Guru na za dwa tygodnie).   

Powtórki mogą zawierać też funkcjonalność incentyw. Każda fiszka może mieć jakieś incentywy - odpowiedzi bliskie lub związane z poprawną, które nie są jednak wymaganą odpowiedzią.
Wtedy zamiast liczyć się jako nieudana próba odpowiedzenia użytkownik otrzyma podpowiedź związaną z daną incentywą.   

OpenSRS definiuje różne endpointy do obsługi powtórek zdefiniowne w pliku `Lesson.java`:
- '/api/{set}/review/start' - Ustawia nowe cookie sesji powtórek, które będzie użyte do grupowania elementów powtórzonych w tej sesji (jeśli użytkownik powtórzy więcej niż jedną fiszkę podczas jednego posiedzenia będą pogrupowane tym cookie).
- '/api/{set}/review/queue' - Daje dostęp do kolejki dostępnych dla użytkownika powtórek (tych, których czas między powtórkami już upłynoł). Same UI setu definiuje jak zaprezentować użytkownikowi tą kolejkę.
- '/api/{set}/review/lastSession' - Daje informacje zwrotne o ostatnio zakończonej sesji - może zostać wykorzystny do wyświetlenia np. ekranu podsumowania co dany użytkownik powtórzył podczas danej sesji.
- '/api/{set}/review/progress' - 

= Definiowanie nowych zestawów

Tworzenie własnych zestawów ("set'ów"), zgodnie z założeniami projektu jest bardzo proste. Dla OpenSRS'a, zestawem fiszek jest każdy folder wewnątrz folderu `sets`, którego nazwa kończy się sufixem `.set`. Wewnątrz tego folderu powinny znajdować się pliki `config.XML`, `content.XML` oraz folder `files`.

Aby stworzyć nowy set, należy więc zacząć od stworzenia nowego folderu w folderze `sets`.

== Pisanie `config.XML`

Następnym etapem jest utworzenie pliku `config.XML`, w którym zdefiniowane są metadata o zestawie. Każdy plik `config.XML` składa się z pojedyńczego objektu `SRSConfig`, w którym następnie zdefiniowane są:

- nazwy oraz granice faz poznawania fiszek
- informacje o karach za błędne odpowiedzi
- informacje o przerwach pomiędzy powtórzeniami
- aliasy (przyjazne nazwy) plików frontendu

Przykładowy plik `config.XML` wygląda następująco:

=== Fazy

W systemie OpenSRS, faza ("phase") to konkretny poziom poznania danej fiszki. Pojedyńcza powtórka nosi nazwę etapu ("stage"). OpenSRS pozwala nadać fazom przyjazne dla użytkownika nazwy - do tego właśnie służy objekt `<phases>`.
Wewnątrz niego powinny znaleźć się jedynie objekty `<phase>`, zawierające atrybuty `number` oraz `fromStage`. Atrybut `number` określa numer danej fazy, `fromStage` informuje od którego etapu dana faza się rozpoczyna. Wewnątrz tego objektu zapisana powinna być nazwa fazy.

=== Kary

Sekcja kar zawiera informacje o ile etapów użytkownik "spada" po podaniu złej odpowiedzi, w zależności od obecnego etapu. Sekcja ta składa się z objektu `<penalties>` zawierającego subobjekty `<stage>` z atrybutami `number`.
Wewnątrz objektów stage zawarta jest informacja ile etapów należy odjąć dla danego przedmiotu po błędnej odpowiedzi na pytanie, zakładając, że użytkownik zna dany objekt na etapie odczytanym z atrybutu `number.`

=== Przerwy

Analogicznie do sekcji kar, informacje o przerwach zakodowane są przy pomocy objektu `<timings>`, wewnątrz którego znajdują się objekty `<stage>` o formacie identycznym do tych wewnątrz sekcji kar. Wewnątrz każdego objektu `<stage>` zawarta jest liczba godzin opóźnienia.

=== Aliasy

Aliasy pozwalają na przekierowanie poszczególnych URI na konkretne pliki z folderu `files` danego zestawu. Zapisane są jako objekt `<bindings>` zawierający objekty `<bind>`.

Każdy typ `<bind>` składa się z atrybutów `source`, `target` oraz `type`.

- source wskazuje na źródłowe URI, które ma być przemapowane
- target zawiera nazwę pliku docelowego
- type informuje system OpenSRS czy `source` jest wyrażeniem regularnym, czy zwykłą regułą. (Możliwe wartości: `literal`, `regex`. Domyślna: `literal`)


=== Przykładowy plik

```xml
<?xml version="1.0"?>
<SRSConfig>
    <phases>
        <phase number="1" fromStage="1">Apprentice</phase>
        <phase number="2" fromStage="5">Guru</phase> <!--Odblokowanie podległych fiszek następuje po przejściu na drugą fazę-->
    </phases>
    <penalties>
        <stage number="1">1</stage> <!--Kara za błąd na pierwszym etapie wynosi spadek o 1-->
        <stage number="2">2</stage> <!--...analogicznie - dla etapu 2, spadek wynosi 2-->
    </penalties>
    <timings>
        <stage number="1">4</stage> <!--Po wejściu na etap 1, opóźnienie 4 godziny-->
        <stage number="2">8</stage> <!--Etap 2 - 8 godzin-->
        <!--Po etapie 2 system uznaje objekt za "dobrze znany" przez użytkownika - nie będzie już o niego pytał.-->
    </timings>
    <bindings>
        <bind source="summary.html" target="NONE"/>     <!--Zablokuj możliwość odczytu pliku summary.html bezpośrednio-->
        <bind source="dashboard" target="index.html"/>  <!--Przekieruj /wk.set/dashboard na plik index.html-->
        <bind source="lesson" target="summary.html"/>   <!--Przekieruj /wk.set/lesson na summary.html - w ten sposób można uzyskać do niego dostęp w kontrolowany sposób-->
    </bindings>
</SRSConfig>
```

== Pisanie `content.XML`

Plik `content.XML` zawiera informacje o fiszkach. Jest to plik o znacznie prostrzym formacie, składający się jedynie z root-objektu `<SRSData>`, zawierającego wiele `<SRSObject>`.
Każdy objekt `<SRSObject>` reprezentuje jedną fiszkę.

Każdy `<SRSObject>` zawiera następujące wartości:

- Atrybut `id` - numeryczny identyfikator danej fiszki
- Objekt `<family>` - rodzina, do której fiszka należy - służy do późniejszego grupowania po stronie frontendu
- Objekt `<query>` - jest to "przednia strona" fiszki - frontend powinien wyświetlić to zapytanie użytkownikowi
- Objekt `<dependencies>` - zawiera subobjekty `<dependsOn>`. W każdym z nich powinno znaleźć się ID fiszki, która musi zostać odblokowana, aby można było nauczyć się fiszki reprezentowanej przez obecny `<SRSObject>`
- Objekt `<level>` - zawiera numer "poziomu", do którego fiszka należy. Poziomy pozwalają na łatwe sprawdzenie jak wiele fiszek dany użytkownik zna. Pozwala też w łatwy sposób grupować materiał do poznania (fiszki z kolejnego poziomu odblokowywują się tylko po poznaniu wszystkich z poziomu obecnego)
- Objekt `<reviewable>` - zawiera objekty `<param>` oraz `<coerce>`. Objekty `<param>` opisują konkretną cechę danej fiszki, która ma być sprawdzona - zawierają atrybut `name`, oraz żądaną odpowiedź, jako zawartość. Wiele objektów `<param>` z taką samą wartością `name` traktowane jest jako alternatywa. Objekty `<coerce>` to podpowiedzi. Zawierają atrybut `for`, w którym zapisana jest błędna odpowiedź. W zawartości natomiast zapisana jest treść podpowiedzi.
- Objekt `<data>` - JSON, który zostanie przekazany frontend'owi bezpośrednio. Sam system OpenSRS nie sprawdza go.

=== Przykładowy plik

```xml
<?xml version="1.0" ?>
<SRSData>
    <SRSObject id="0">
        <family>radical</family>
        <query>亅</query>
        <dependencies/>
        <level>1</level>
        <reviewable>
            <param name="name">Barb</param>
        </reviewable>
        <data>{}</data>
    </SRSObject>
    <SRSObject id="125">
        <family>kanji</family>
        <query>了</query>
        <dependencies>
            <dependsOn>0</dependsOn>
        </dependencies>
        <level>2</level>
        <reviewable>
            <param name="reading">ryou</param>
            <param name="meaning">Finish</param>
            <param name="meaning">Complete</param>
            <param name="meaning">End</param>
        </reviewable>
        <data>{}</data>
    </SRSObject>
</SRSData>
```

== Tworzenie frontendu

Każdy zestaw powinien sam zdefiniować frontend. W tym celu użyć musi on endpointów, które dokładnie opisane były we wcześniejszej części dokumentacji, oraz pliku `osrs.js`, który stanowi prostą bibliotekę dla kodu frontend'ów.

Zawartość folderu `files` dostępna jest dla klientów pod adresem `http://<adres>/<nazwa zbioru>/ ... pliki`. Jeżeli plik nie istnieje w folderze `files` danego zbioru, zostanie od odczytany z folderu `common` w CWD serwera.

= Uruchamianie projektu

Projekt OpenSRS został napisany w IDE NetBeans, jednak może być uruchomiony za pomocą systemu Maven.
Aby projekt uruchomił się poprawnie, w folderze z którego zostanie uruchomiony musi znajdować się folder `sets` z poprawną strukturą zbiorów OpenSRS.

```sh
$ mvn clean dependency:copy-dependencies javadoc:javadoc package
$ java -cp "target/OpenSRSMVN-1.0-SNAPSHOT.jar:$(printf %s: target/dependency/*.jar)" opensrs.OpenSRS
```

Po wykonaniu powyższych komend, uruchomiony zostanie serwer lokalny na `http://0.0.0.0:9998`, a pliki JavaDoc wygenerowane zostaną w folderze `target/reports/apidocs`.


= Podsumowanie
OpenSRS to interaktywny system fiszek wspierający adaptywne czasy powtórzeń na podstawie znajomości użytkownika, definiowanie fiszek które wymagają znajomości innych fiszek, grupowanie ich po typach oraz dodawanie incentyw dla bliskich odpowiedzi.
Cechuje się uniwersalnością - może wspierać wielu użytkowników i wiele zestawów na raz. Każdy zestaw definiuje nie tylko własne fiszki ale ich grupy, fazy i interfejs graficzny.
Cały projekt działa na serwerze Autumn który też jest niezależny od reszty projektu i może być wykorzystany do innych celów.


= Możliwe sposoby rozwoju aplikacji

- Łatwiejszy system edycji treści - obecnie można to robić jedynie przez edycję plików XML set'ów.
- Możliwość resetu haseł, a co za tym idzie wysyłanie e-maili.
- Przepisanie na system ORM, np. Hibernate.
