package java.time.format;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// El formateador de `java.time`: escribe un valor temporal como texto y vuelve a leerlo.
//
// **Formatea y parsea exactamente el mismo lenguaje**, y esa simetria es a proposito: lo que este
// formateador escribe, lo vuelve a leer. No hay ninguna pieza que sepa solo una de las dos cosas.
//
// Adentro no hay una cadena de patron: hay una lista de piezas (`Pieza`), y `ofPattern` es un
// compilador a esa lista. El porque esta escrito en `Pieza` y en `DateTimeFormatterBuilder`; lo que
// importa desde afuera es la consecuencia, que es que los `ISO_*` de aca son los de verdad --con sus
// secciones opcionales-- y no aproximaciones.
//
// **LOS SEIS MIEMBROS QUE NO ESTAN, Y EL CRITERIO.**
//
// `ofLocalizedDate`, `ofLocalizedTime`, los dos `ofLocalizedDateTime` y `ofLocalizedPattern` piden lo
// mismo: **el patron que un locale usa** para una fecha corta, media, larga o completa. Ese patron no
// se deduce de nada, es un dato: `M/d/yy` en los Estados Unidos, `d/M/yy` en la Argentina,
// `yyyy/MM/dd` en el Japon, y para `FULL` ademas con el nombre del dia adelante. Esta biblioteca no
// trae los datos del CLDR, y **no hay un patron por defecto que sea correcto**: cualquiera que se
// elija acierta para un locale y miente para los otros ciento y pico. Un `ofLocalizedDate(SHORT)` que
// devolviera `dd/MM/yyyy` no seria una version incompleta de la respuesta correcta, seria una
// respuesta equivocada con la forma de la correcta, y el que la llame se entera en produccion.
//
// La distincion, que es la que gobierna todo el paquete: **no es que falte el dato, es que inventarlo
// seria mentir**. Donde el dato falta pero la respuesta no depende de el, el miembro esta: los
// `ISO_*` son fijos por norma y no miran el locale; `withLocale` guarda el locale y lo devuelve
// igual; `appendText(field, Map)` deja poner los nombres propios. Lo unico que se queda afuera es lo
// que **es** una consulta al CLDR.
//
// `localizedBy(Locale)` tambien queda afuera, por una razon distinta y mas fina: no es "el
// formateador con otro locale" --eso es `withLocale`-- sino "el formateador con las extensiones
// Unicode del locale aplicadas": `u-ca` el calendario, `u-nu` el sistema de numeracion, `u-rg` la
// region de formato, `u-tz` la zona. De las cuatro, aca se podrian honrar dos. Un `localizedBy` que
// aplica la mitad de lo que promete y calla la otra mitad es peor que no tenerlo, porque el que lo
// llama cree que aplico las cuatro.
//
// **Lo que si depende del locale es una sola pieza**: la de los nombres (`MMM`, `EEEE`, `a`, `G`).
// Con `Locale.ROOT` o ingles escribe los nombres ingleses, que es lo correcto; con cualquier otro
// **tira** un `DateTimeException` que dice que falta el dato, en vez de escribir el ingles bajo otra
// bandera. El razonamiento completo esta en `PiezaTexto`.
public final class DateTimeFormatter {

    // ---- los predefinidos ----------------------------------------------------------------------
    //
    // Los quince del JDK, armados con el mismo armador que cualquiera puede usar. Ninguno mira el
    // locale: ISO-8601 y el RFC 1123 fijan el texto, y ahi no hay nada que traducir. `RFC_1123` usa
    // `appendText(field, Map)` con los nombres del RFC precisamente por eso: no son "los nombres en
    // ingles", son los nombres que el formato exige, y con un mapa explicito lo dicen.

    public static final DateTimeFormatter ISO_LOCAL_DATE = isoFechaLocal();

    public static final DateTimeFormatter ISO_OFFSET_DATE = isoFechaConOffset();

    public static final DateTimeFormatter ISO_DATE = isoFecha();

    public static final DateTimeFormatter ISO_LOCAL_TIME = isoHoraLocal();

    public static final DateTimeFormatter ISO_OFFSET_TIME = isoHoraConOffset();

    public static final DateTimeFormatter ISO_TIME = isoHora();

    public static final DateTimeFormatter ISO_LOCAL_DATE_TIME = isoFechaHoraLocal();

    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME = isoFechaHoraConOffset();

    public static final DateTimeFormatter ISO_ZONED_DATE_TIME = isoFechaHoraConZona();

    public static final DateTimeFormatter ISO_DATE_TIME = isoFechaHora();

    public static final DateTimeFormatter ISO_ORDINAL_DATE = isoOrdinal();

    public static final DateTimeFormatter ISO_WEEK_DATE = isoSemana();

    public static final DateTimeFormatter ISO_INSTANT = isoInstante();

    public static final DateTimeFormatter BASIC_ISO_DATE = isoBasico();

    public static final DateTimeFormatter RFC_1123_DATE_TIME = rfc1123();

    private static final TemporalQuery<Period> EXCESO = new ConsultaExceso();

    private static final TemporalQuery<Boolean> BISIESTO = new ConsultaBisiesto();

    private final PiezaCompuesta piezas;
    private final Locale locale;
    private final DecimalStyle simbolos;
    private final ResolverStyle resolutor;
    private final Set<TemporalField> camposResolutor;
    private final Chronology cronologia;
    private final ZoneId zona;

    DateTimeFormatter(PiezaCompuesta piezas, Locale locale, DecimalStyle simbolos,
            ResolverStyle resolutor, Set<TemporalField> camposResolutor, Chronology cronologia,
            ZoneId zona) {
        this.piezas = piezas;
        this.locale = locale;
        this.simbolos = simbolos;
        this.resolutor = resolutor;
        this.camposResolutor = camposResolutor;
        this.cronologia = cronologia;
        this.zona = zona;
    }

    PiezaCompuesta piezas() {
        return this.piezas;
    }

    // ---- fabricas ------------------------------------------------------------------------------

    public static DateTimeFormatter ofPattern(String pattern) {
        return new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter();
    }

    // El locale se guarda y se devuelve tal cual; lo unico que cambia con el son los nombres, y esos
    // solo existen en ingles. Un patron sin nombres da el mismo texto en cualquier locale.
    public static DateTimeFormatter ofPattern(String pattern, Locale locale) {
        return new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter(locale);
    }

    // Las dos consultas que solo tienen sentido sobre el resultado de un parseo. Devuelven **la misma
    // instancia siempre**: se comparan por identidad, y una nueva en cada llamada no coincidiria
    // nunca con la que el resultado reconoce (fue un bug real en `TemporalQueries`).
    public static final TemporalQuery<Period> parsedExcessDays() {
        return EXCESO;
    }

    public static final TemporalQuery<Boolean> parsedLeapSecond() {
        return BISIESTO;
    }

    // ---- copias con un ajuste cambiado ---------------------------------------------------------

    public Locale getLocale() {
        return this.locale;
    }

    public DateTimeFormatter withLocale(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        if (locale.equals(this.locale)) {
            return this;
        }
        return new DateTimeFormatter(this.piezas, locale, this.simbolos, this.resolutor,
                this.camposResolutor, this.cronologia, this.zona);
    }

    /**
     * Una copia con el locale puesto **y con la cronologia y los simbolos deducidos de el**.
     *
     * <p>Es lo que la distingue de {@link #withLocale}, y la diferencia no es de matiz: `withLocale`
     * cambia el locale y **conserva** lo que le hayan puesto a mano, mientras que esta lo
     * **sobreescribe** con lo que el locale dice. Un formateador con cronologia tailandesa puesta a
     * mano sigue siendo tailandes despues de `withLocale(FRANCE)`, y pasa a ISO despues de
     * `localizedBy(FRANCE)`.
     *
     * <p>De donde sale cada cosa, que se verifico contra el JDK real y no se supuso:
     *
     * <ul>
     * <li><strong>Cronologia</strong>: de la extension Unicode `u-ca` del locale si la trae, y si no
     *     del calendario que ese locale usa por omision --que es lo que hace
     *     {@link Chronology#ofLocale}--. Nunca queda en `null`: `localizedBy` de un locale comun deja
     *     ISO, no "sin cronologia".</li>
     * <li><strong>Simbolos</strong>: {@link DecimalStyle#of}, que lee la extension `u-nu`.</li>
     * <li><strong>Zona</strong>: **se conserva la que habia**, salvo que el locale traiga `u-tz`. No
     *     se limpia como la cronologia, y eso sorprende hasta que se lo mide.</li>
     * </ul>
     *
     * <p><strong>La unica aproximacion, dicha:</strong> la extension `u-tz` se ignora. Sus valores
     * son identificadores cortos de CLDR --`uslax` por `America/Los_Angeles`-- y la tabla que los
     * traduce son unas cuatrocientas cincuenta filas de datos opacos, sin regla que los derive.
     * Inventarla de memoria daria justo lo que este paquete evita en todos lados: nombres de zona
     * plausibles y equivocados. Un locale con `u-tz` conserva la zona que tenia, que es la misma
     * respuesta que da un locale sin extension.
     *
     * @throws NullPointerException si `locale` es nulo
     */
    public DateTimeFormatter localizedBy(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        Chronology cron = Chronology.ofLocale(locale);
        DecimalStyle simbolos = DecimalStyle.of(locale);
        return new DateTimeFormatter(this.piezas, locale, simbolos, this.resolutor,
                this.camposResolutor, cron, this.zona);
    }

    public DecimalStyle getDecimalStyle() {
        return this.simbolos;
    }

    public DateTimeFormatter withDecimalStyle(DecimalStyle decimalStyle) {
        if (decimalStyle == null) {
            throw new NullPointerException("decimalStyle");
        }
        if (decimalStyle.equals(this.simbolos)) {
            return this;
        }
        return new DateTimeFormatter(this.piezas, this.locale, decimalStyle, this.resolutor,
                this.camposResolutor, this.cronologia, this.zona);
    }

    public Chronology getChronology() {
        return this.cronologia;
    }

    public DateTimeFormatter withChronology(Chronology chrono) {
        if (chrono == null ? this.cronologia == null : chrono.equals(this.cronologia)) {
            return this;
        }
        return new DateTimeFormatter(this.piezas, this.locale, this.simbolos, this.resolutor,
                this.camposResolutor, chrono, this.zona);
    }

    public ZoneId getZone() {
        return this.zona;
    }

    // Al escribir, convierte el valor a esta zona --si trae un instante-- y si no, se la presta: un
    // `LocalDateTime` no sabe donde esta, y con `withZone` pasa a saberlo. Al leer, es la zona que
    // vale cuando el texto no trajo ninguna, que es lo que permite sacar un `ZonedDateTime` de un
    // texto sin zona.
    public DateTimeFormatter withZone(ZoneId zone) {
        if (zone == null ? this.zona == null : zone.equals(this.zona)) {
            return this;
        }
        return new DateTimeFormatter(this.piezas, this.locale, this.simbolos, this.resolutor,
                this.camposResolutor, this.cronologia, zone);
    }

    public ResolverStyle getResolverStyle() {
        return this.resolutor;
    }

    public DateTimeFormatter withResolverStyle(ResolverStyle resolverStyle) {
        if (resolverStyle == null) {
            throw new NullPointerException("resolverStyle");
        }
        if (resolverStyle.equals(this.resolutor)) {
            return this;
        }
        return new DateTimeFormatter(this.piezas, this.locale, this.simbolos, resolverStyle,
                this.camposResolutor, this.cronologia, this.zona);
    }

    public Set<TemporalField> getResolverFields() {
        return this.camposResolutor;
    }

    public DateTimeFormatter withResolverFields(TemporalField... resolverFields) {
        Set<TemporalField> juego = null;
        if (resolverFields != null) {
            juego = new HashSet<TemporalField>();
            int i = 0;
            while (i < resolverFields.length) {
                juego.add(resolverFields[i]);
                i = i + 1;
            }
        }
        return new DateTimeFormatter(this.piezas, this.locale, this.simbolos, this.resolutor,
                juego, this.cronologia, this.zona);
    }

    public DateTimeFormatter withResolverFields(Set<TemporalField> resolverFields) {
        Set<TemporalField> juego = null;
        if (resolverFields != null) {
            juego = new HashSet<TemporalField>(resolverFields);
        }
        return new DateTimeFormatter(this.piezas, this.locale, this.simbolos, this.resolutor,
                juego, this.cronologia, this.zona);
    }

    // ---- escribir ------------------------------------------------------------------------------

    public String format(TemporalAccessor temporal) {
        StringBuilder salida = new StringBuilder(32);
        this.escribir(temporal, salida);
        return salida.toString();
    }

    public void formatTo(TemporalAccessor temporal, Appendable appendable) {
        if (appendable == null) {
            throw new NullPointerException("appendable");
        }
        // El JDK no declara `throws IOException` aca y envuelve: quien pide formatear a un
        // `Appendable` no tiene por que atrapar E/S, y `DateTimeException` es la excepcion de este
        // paquete. El error no se pierde, cambia de forma.
        try {
            appendable.append(this.format(temporal));
        } catch (java.io.IOException e) {
            throw new java.time.DateTimeException("fallo al escribir en el destino", e);
        }
    }

    private void escribir(TemporalAccessor temporal, StringBuilder salida) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        CtxImprimir ctx = new CtxImprimir(this.ajustar(temporal), this.locale, this.simbolos);
        this.piezas.imprimir(ctx, salida);
    }

    // El valor tal como las piezas lo ven: con los campos deducidos, y con los reemplazos de
    // `withZone` y `withChronology` ya aplicados.
    private TemporalAccessor ajustar(TemporalAccessor temporal) {
        // Se envuelve **primero** para deducir, y recien despues se mira si hay un instante: un
        // `Instant` de esta biblioteca no contesta `INSTANT_SECONDS` por si mismo, y sin la
        // envoltura `withZone` no lo reconoceria como convertible.
        TemporalAccessor base = new TemporalDerivado(temporal, null, null);
        if (this.zona != null && !this.zona.equals(base.query(TemporalQueries.zoneId()))
                && base.isSupported(ChronoField.INSTANT_SECONDS)) {
            // Con un instante a mano el cambio de zona es una conversion de verdad: la fecha y la
            // hora que salen son las de **ese** lugar. Sin instante lo unico honesto es prestarle la
            // zona al valor, sin mover la hora --que es lo que hace `TemporalDerivado`--.
            base = ZonedDateTime.ofInstant(Instant.from(base), this.zona);
        }
        return new TemporalDerivado(base, this.cronologia, this.zona);
    }

    // ---- leer ----------------------------------------------------------------------------------

    /**
     * Lee `text` con este formateador y devuelve los campos que encontro.
     *
     * <p>Lo que vuelve **no es una fecha**: es el conjunto de campos que el texto traia, ya
     * resueltos --anio+mes+dia se convierten en el dia epoch, hora+minuto en el nano del dia-- pero
     * sin decidir en que clase entran. Esa decision es del que llama, y por eso existe la otra
     * version: `parse(texto, LocalDate::from)`.
     *
     * @throws java.time.format.DateTimeParseException si el texto no encaja
     */
    public TemporalAccessor parse(CharSequence text) {
        return this.parseTodo(text);
    }

    /**
     * Lee `text` y arma con el lo que `query` pida.
     *
     * <p>Es la forma que usan los `parse(texto, formateador)` de `LocalDate`, `LocalTime` y las
     * demas: cada una pasa su propio `from`.
     *
     * @throws java.time.format.DateTimeParseException si el texto no encaja, o si lo que encaja no
     *     alcanza para lo que `query` pide
     */
    public <T> T parse(CharSequence text, TemporalQuery<T> query) {
        if (query == null) {
            throw new NullPointerException("query");
        }
        Parsed parsed = this.parseTodo(text);
        try {
            return query.queryFrom(parsed);
        } catch (DateTimeException e) {
            // El texto encajo pero no traia lo que hacia falta --un patron de solo hora al que le
            // piden una fecha--. Se reetiqueta como error de parseo porque desde afuera es lo mismo:
            // el texto no dio lo que se pedia. El mensaje original va adentro.
            throw new DateTimeParseException(
                    "Text '" + text + "' could not be parsed: " + e.getMessage(), text, 0);
        }
    }

    /**
     * Lee desde `position` y **deja el resto**, moviendo `position` hasta donde llego.
     *
     * <p>A diferencia de los otros `parse`, no exige haber consumido el texto entero: es la forma
     * que sirve para leer una fecha que esta adentro de un texto mas largo. Un error no tira: se
     * anota en `position.getErrorIndex()` y se devuelve `null`.
     */
    public TemporalAccessor parse(CharSequence text, ParsePosition position) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        if (position == null) {
            throw new NullPointerException("position");
        }
        String t = text.toString();
        CtxParseo ctx = this.leer(t, position);
        // **Tira, no devuelve `null`.** Es lo que hace el JDK, aunque la posicion tenga un
        // `errorIndex` donde anotarlo: la version que no tira es `parseUnresolved`. Que las dos
        // tomen un `ParsePosition` no las hace la misma operacion --esta resuelve, y resolver puede
        // fallar por una razon que un indice no sabe contar--.
        if (ctx == null) {
            throw new DateTimeParseException("Text '" + t + "' could not be parsed at index "
                    + position.getErrorIndex(), text, position.getErrorIndex());
        }
        try {
            return this.resolver(ctx);
        } catch (DateTimeParseException e) {
            throw e;
        } catch (RuntimeException e) {
            position.setErrorIndex(position.getIndex());
            throw new DateTimeParseException(
                    "Text '" + t + "' could not be parsed: " + e.getMessage(), text, 0, e);
        }
    }

    /**
     * Lo mismo que `parse(text, position)` pero **sin resolver**: los campos crudos, tal como el
     * texto los trajo.
     *
     * <p>Sirve para mirar que decia el texto antes de que la cronologia decida que significa. Un
     * error no tira: se anota en `position`.
     */
    public TemporalAccessor parseUnresolved(CharSequence text, ParsePosition position) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        if (position == null) {
            throw new NullPointerException("position");
        }
        CtxParseo ctx = this.leer(text.toString(), position);
        if (ctx == null) {
            return null;
        }
        return new Crudo(ctx);
    }

    /**
     * Lee el texto y devuelve **la primera** de `queries` que se pueda armar con lo que trajo.
     *
     * <p>Para eso estan las secciones opcionales: `ISO_DATE_TIME` lee un texto con zona o sin ella, y
     * `parseBest(t, ZonedDateTime::from, LocalDateTime::from)` devuelve lo que el texto de verdad
     * decia en vez de forzar el mas rico y fallar. El orden manda: se prueban de mas especifico a
     * menos.
     *
     * @throws java.time.format.DateTimeParseException si el texto no encaja, o si ninguna de las
     *     consultas se puede armar
     */
    public TemporalAccessor parseBest(CharSequence text, TemporalQuery<?>... queries) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        if (queries == null || queries.length < 2) {
            throw new IllegalArgumentException(
                    "At least two queries must be specified");
        }
        Parsed parsed = this.parseTodo(text);
        int i = 0;
        while (i < queries.length) {
            try {
                return (TemporalAccessor) queries[i].queryFrom(parsed);
            } catch (RuntimeException e) {
                i = i + 1;
            }
        }
        throw new DateTimeParseException("Text '" + text
                + "' could not be parsed: unable to obtain any of the requested types", text, 0);
    }

    private Parsed parseTodo(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        String t = text.toString();
        ParsePosition pos = new ParsePosition(0);
        CtxParseo ctx = this.leer(t, pos);
        if (ctx == null) {
            int i = pos.getErrorIndex();
            throw new DateTimeParseException(
                    "Text '" + t + "' could not be parsed at index " + i, text, i);
        }
        if (pos.getIndex() != t.length()) {
            throw new DateTimeParseException("Text '" + t
                    + "' could not be parsed, unparsed text found at index " + pos.getIndex(),
                    text, pos.getIndex());
        }
        try {
            return this.resolver(ctx);
        } catch (DateTimeParseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DateTimeParseException(
                    "Text '" + t + "' could not be parsed: " + e.getMessage(), text, 0, e);
        }
    }

    private CtxParseo leer(String t, ParsePosition pos) {
        CtxParseo ctx = new CtxParseo(this.locale, this.simbolos,
                this.resolutor != ResolverStyle.LENIENT, this.cronologia, null);
        int r = this.piezas.parsear(ctx, t, pos.getIndex());
        if (r < 0) {
            pos.setErrorIndex(~r);
            return null;
        }
        pos.setIndex(r);
        return ctx;
    }

    private Parsed resolver(CtxParseo ctx) {
        return new Parsed(ctx, this.resolutor, this.camposResolutor, this.zona, this.cronologia);
    }

    // ---- puente con java.text ------------------------------------------------------------------

    /**
     * Este formateador visto como un `java.text.Format`.
     *
     * <p>**Solo escribe.** El `java.text.Format` de esta biblioteca es la mitad de formateo de la
     * jerarquia --`parseObject` no esta declarado ahi-- asi que el objeto que vuelve cumple entero el
     * contrato que su tipo declara. La mitad de lectura se hace por `parse(texto, ParsePosition)`,
     * que es la misma operacion sin el intermediario.
     */
    public Format toFormat() {
        return new FormatoDeFecha(this, null);
    }

    /**
     * Como `toFormat()`, pero el resultado que se le pide al parseo ya viene fijado.
     */
    public Format toFormat(TemporalQuery<?> parseQuery) {
        if (parseQuery == null) {
            throw new NullPointerException("parseQuery");
        }
        return new FormatoDeFecha(this, parseQuery);
    }

    public String toString() {
        return this.piezas.toString();
    }

    // ---- los predefinidos, armados -------------------------------------------------------------

    private static DateTimeFormatter estricto(DateTimeFormatterBuilder b) {
        // Los `ISO_*` resuelven en `STRICT` --como en el JDK-- porque ISO-8601 no admite un 31 de
        // febrero ni redondearlo al 28: un texto que no es una fecha tiene que fallar.
        return b.toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
                .withChronology(IsoChronology.INSTANCE);
    }

    private static DateTimeFormatterBuilder fechaLocal() {
        return new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH, 2);
    }

    private static DateTimeFormatterBuilder horaLocal() {
        return new DateTimeFormatterBuilder()
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .optionalEnd()
                .optionalEnd();
    }

    private static DateTimeFormatterBuilder fechaHoraLocal() {
        return fechaLocal().appendLiteral('T').append(ISO_LOCAL_TIME);
    }

    private static DateTimeFormatter isoFechaLocal() {
        return estricto(fechaLocal());
    }

    private static DateTimeFormatter isoFechaConOffset() {
        return estricto(new DateTimeFormatterBuilder().append(ISO_LOCAL_DATE).appendOffsetId());
    }

    private static DateTimeFormatter isoFecha() {
        return estricto(new DateTimeFormatterBuilder().append(ISO_LOCAL_DATE)
                .optionalStart().appendOffsetId().optionalEnd());
    }

    private static DateTimeFormatter isoHoraLocal() {
        return estricto(horaLocal());
    }

    private static DateTimeFormatter isoHoraConOffset() {
        return estricto(new DateTimeFormatterBuilder().append(ISO_LOCAL_TIME).appendOffsetId());
    }

    private static DateTimeFormatter isoHora() {
        return estricto(new DateTimeFormatterBuilder().append(ISO_LOCAL_TIME)
                .optionalStart().appendOffsetId().optionalEnd());
    }

    private static DateTimeFormatter isoFechaHoraLocal() {
        return estricto(fechaHoraLocal());
    }

    private static DateTimeFormatter isoFechaHoraConOffset() {
        return estricto(new DateTimeFormatterBuilder().append(ISO_LOCAL_DATE_TIME).appendOffsetId());
    }

    // El `[Europe/Paris]` va **despues** del desplazamiento y adentro de una seccion opcional: un
    // `OffsetDateTime` sale sin corchetes y un `ZonedDateTime` con ellos, del mismo formateador.
    private static DateTimeFormatter isoFechaHoraConZona() {
        return estricto(new DateTimeFormatterBuilder().append(ISO_OFFSET_DATE_TIME)
                .optionalStart()
                .appendLiteral('[')
                .parseCaseSensitive()
                .appendZoneRegionId()
                .appendLiteral(']')
                .optionalEnd());
    }

    private static DateTimeFormatter isoFechaHora() {
        return estricto(new DateTimeFormatterBuilder().append(ISO_LOCAL_DATE_TIME)
                .optionalStart()
                .appendOffsetId()
                .optionalStart()
                .appendLiteral('[')
                .parseCaseSensitive()
                .appendZoneRegionId()
                .appendLiteral(']')
                .optionalEnd()
                .optionalEnd());
    }

    private static DateTimeFormatter isoOrdinal() {
        return estricto(new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_YEAR, 3)
                .optionalStart()
                .appendOffsetId()
                .optionalEnd());
    }

    private static DateTimeFormatter isoSemana() {
        return estricto(new DateTimeFormatterBuilder()
                .appendValue(IsoFields.WEEK_BASED_YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral("-W")
                .appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_WEEK, 1)
                .optionalStart()
                .appendOffsetId()
                .optionalEnd());
    }

    private static DateTimeFormatter isoInstante() {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendInstant()
                .toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    }

    // Sin separadores: los tres campos son de ancho fijo y por eso se pueden pegar. El offset va en
    // modo laxo porque `+0000` y `Z` son los dos legales y el estricto rechazaria uno.
    private static DateTimeFormatter isoBasico() {
        return estricto(new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .optionalStart()
                .parseLenient()
                .appendOffset("+HHMMss", "Z")
                .parseStrict()
                .optionalEnd());
    }

    // Los nombres del RFC 1123 van por `appendText(field, Map)` y no por el juego ingles: el RFC los
    // fija, no los toma del locale. Con un mapa explicito el formateador dice exactamente eso, y
    // sigue funcionando con cualquier `withLocale`.
    private static DateTimeFormatter rfc1123() {
        Map<Long, String> dias = new HashMap<Long, String>();
        dias.put(Long.valueOf(1L), "Mon");
        dias.put(Long.valueOf(2L), "Tue");
        dias.put(Long.valueOf(3L), "Wed");
        dias.put(Long.valueOf(4L), "Thu");
        dias.put(Long.valueOf(5L), "Fri");
        dias.put(Long.valueOf(6L), "Sat");
        dias.put(Long.valueOf(7L), "Sun");
        Map<Long, String> meses = new HashMap<Long, String>();
        meses.put(Long.valueOf(1L), "Jan");
        meses.put(Long.valueOf(2L), "Feb");
        meses.put(Long.valueOf(3L), "Mar");
        meses.put(Long.valueOf(4L), "Apr");
        meses.put(Long.valueOf(5L), "May");
        meses.put(Long.valueOf(6L), "Jun");
        meses.put(Long.valueOf(7L), "Jul");
        meses.put(Long.valueOf(8L), "Aug");
        meses.put(Long.valueOf(9L), "Sep");
        meses.put(Long.valueOf(10L), "Oct");
        meses.put(Long.valueOf(11L), "Nov");
        meses.put(Long.valueOf(12L), "Dec");
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .optionalStart()
                .appendText(ChronoField.DAY_OF_WEEK, dias)
                .appendLiteral(", ")
                .optionalEnd()
                .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                .appendLiteral(' ')
                .appendText(ChronoField.MONTH_OF_YEAR, meses)
                .appendLiteral(' ')
                .appendValue(ChronoField.YEAR, 4)
                .appendLiteral(' ')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .optionalEnd()
                .appendLiteral(' ')
                .appendOffset("+HHMM", "GMT")
                .toFormatter(Locale.ROOT)
                .withResolverStyle(ResolverStyle.SMART)
                .withChronology(IsoChronology.INSTANCE);
    }
}

// Los campos tal como el texto los trajo, sin resolver. Es lo que devuelve `parseUnresolved`.
final class Crudo implements TemporalAccessor {

    private final Map<TemporalField, Long> campos;
    private final ZoneId zona;
    private final Chronology cronologia;

    Crudo(CtxParseo ctx) {
        this.campos = ctx.campos;
        this.zona = ctx.zona != null ? ctx.zona : ctx.offset;
        this.cronologia = ctx.cronologia;
    }

    public boolean isSupported(TemporalField campo) {
        return campo != null && this.campos.containsKey(campo);
    }

    public long getLong(TemporalField campo) {
        Long v = this.campos.get(campo);
        if (v == null) {
            throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: "
                    + campo);
        }
        return v.longValue();
    }

    public int get(TemporalField campo) {
        return campo.range().checkValidIntValue(this.getLong(campo), campo);
    }

    public <R> R query(TemporalQuery<R> consulta) {
        if (consulta == TemporalQueries.zoneId() || consulta == TemporalQueries.zone()) {
            return (R) this.zona;
        }
        if (consulta == TemporalQueries.chronology()) {
            return (R) this.cronologia;
        }
        return consulta.queryFrom(this);
    }

    public String toString() {
        return this.campos.toString();
    }
}

// `DateTimeFormatter.toFormat()`.
//
// Solo escribe, porque `java.text.Format` de esta biblioteca solo declara la mitad de escritura. El
// `parseQuery` se guarda para que el objeto sea el que el llamador pidio --dos `toFormat` con
// consultas distintas no son iguales-- aunque hoy no haya un `parseObject` que lo use.
final class FormatoDeFecha extends Format {

    private final DateTimeFormatter formateador;
    private final TemporalQuery<?> consulta;

    FormatoDeFecha(DateTimeFormatter formateador, TemporalQuery<?> consulta) {
        this.formateador = formateador;
        this.consulta = consulta;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (obj == null) {
            throw new NullPointerException("obj");
        }
        if (toAppendTo == null) {
            throw new NullPointerException("toAppendTo");
        }
        if (!(obj instanceof TemporalAccessor)) {
            throw new IllegalArgumentException("Format target must implement TemporalAccessor");
        }
        toAppendTo.append(this.formateador.format((TemporalAccessor) obj));
        return toAppendTo;
    }
}
