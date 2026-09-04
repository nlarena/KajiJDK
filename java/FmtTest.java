// El parseo de `DateTimeFormatter` y los nueve `parse(texto, formateador)` de `java.time`.
//
// **Se comprueba contra `java` real corriendo lo mismo**, que es lo unico que distingue "parsea" de
// "parsea igual que el JDK". La mitad de las comprobaciones son de ida y vuelta --formatear y volver
// a leer-- porque esa es la propiedad que el formateador promete: lo que escribe, lo relee.
//
// La otra mitad son los casos que **no** tienen que andar: un texto que sobra, un campo que falta, un
// patron de solo hora al que le piden una fecha. Un parser que acepta de mas es peor que uno que no
// parsea, porque devuelve una fecha equivocada en vez de un error.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.text.ParsePosition;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.DecimalStyle;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.Locale;

public class FmtTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    // Las consultas van como campos y no en linea: `parseBest` toma un `TemporalQuery...`, y una
    // referencia a metodo en posicion de varargs no la soporta el generador de bytecode de este
    // compilador todavia.
    static final TemporalQuery<Instant> INSTANTE = Instant::from;
    static final TemporalQuery<OffsetDateTime> CON_OFFSET = OffsetDateTime::from;
    static final TemporalQuery<LocalDateTime> SIN_OFFSET = LocalDateTime::from;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    // Que `f.parse(texto, X::from)` tire, y no que devuelva cualquier cosa.
    static void tira(DateTimeFormatter f, String texto, int cual) {
        boolean tiro = false;
        try {
            if (cual == 0) {
                LocalDate.parse(texto, f);
            } else if (cual == 1) {
                LocalTime.parse(texto, f);
            } else {
                LocalDateTime.parse(texto, f);
            }
        } catch (DateTimeParseException e) {
            tiro = true;
        }
        ok(tiro);
    }

    // Que algo tire, sin decir que. Para lo que falla al **escribir** el patron y no al usarlo.
    static void tiraAlgo(Runnable r) {
        boolean tiro = false;
        try {
            r.run();
        } catch (RuntimeException e) {
            tiro = true;
        }
        ok(tiro);
    }

    static void fechas() {
        DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ok(LocalDate.parse("2024-02-29", iso).equals(LocalDate.of(2024, 2, 29)));
        ok(LocalDate.parse("0001-01-01", iso).equals(LocalDate.of(1, 1, 1)));

        // Otro orden, otro separador: el patron manda.
        DateTimeFormatter barra = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        ok(LocalDate.parse("09/03/1998", barra).equals(LocalDate.of(1998, 3, 9)));

        // Sin relleno: `d` y `M` leen uno o dos digitos.
        DateTimeFormatter corto = DateTimeFormatter.ofPattern("d/M/yyyy");
        ok(LocalDate.parse("9/3/1998", corto).equals(LocalDate.of(1998, 3, 9)));
        ok(LocalDate.parse("19/12/1998", corto).equals(LocalDate.of(1998, 12, 19)));

        // Nombre de mes, corto y largo. **Solo ida y vuelta**, sin el texto escrito a mano: los
        // nombres dependen del locale --el `java` real con locale espaniol escribe `jul`, esta
        // biblioteca escribe siempre en ingles-- asi que comparar la cadena compararia el locale y
        // no el parser. Lo que sí es independiente del locale es que **lo que se escribe se relee**,
        // que es justo la propiedad que interesa.
        DateTimeFormatter mes3 = DateTimeFormatter.ofPattern("dd MMM yyyy");
        LocalDate julio = LocalDate.of(2021, 7, 5);
        ok(LocalDate.parse(mes3.format(julio), mes3).equals(julio));
        DateTimeFormatter mesL = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        LocalDate septiembre = LocalDate.of(2021, 9, 5);
        ok(LocalDate.parse(mesL.format(septiembre), mesL).equals(septiembre));

        // Dia del anio.
        DateTimeFormatter doy = DateTimeFormatter.ofPattern("yyyy-DDD");
        ok(LocalDate.parse("2021-032", doy).equals(LocalDate.of(2021, 2, 1)));

        // Anio de dos digitos: ventana 2000-2099.
        DateTimeFormatter dos = DateTimeFormatter.ofPattern("dd/MM/yy");
        ok(LocalDate.parse("01/02/24", dos).equals(LocalDate.of(2024, 2, 1)));

        // Ida y vuelta sobre un anio bisiesto y un fin de mes.
        LocalDate[] varias = {
            LocalDate.of(2000, 2, 29), LocalDate.of(1999, 12, 31), LocalDate.of(2024, 1, 1),
        };
        int i = 0;
        while (i < varias.length) {
            ok(LocalDate.parse(iso.format(varias[i]), iso).equals(varias[i]));
            ok(LocalDate.parse(mesL.format(varias[i]), mesL).equals(varias[i]));
            i = i + 1;
        }
    }

    static void horas() {
        DateTimeFormatter hms = DateTimeFormatter.ofPattern("HH:mm:ss");
        ok(LocalTime.parse("00:00:00", hms).equals(LocalTime.of(0, 0, 0)));
        ok(LocalTime.parse("23:59:59", hms).equals(LocalTime.of(23, 59, 59)));

        // Sin segundos: los que faltan son cero.
        DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
        ok(LocalTime.parse("07:05", hm).equals(LocalTime.of(7, 5)));

        // Fraccion: `S` son los primeros digitos del nano, y hay que devolverlos a su escala.
        DateTimeFormatter frac = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        ok(LocalTime.parse("01:02:03.400", frac).equals(LocalTime.of(1, 2, 3, 400000000)));
        DateTimeFormatter frac1 = DateTimeFormatter.ofPattern("HH:mm:ss.S");
        ok(LocalTime.parse("01:02:03.5", frac1).equals(LocalTime.of(1, 2, 3, 500000000)));

        // Reloj de 12 horas con AM/PM. Otra vez ida y vuelta y no el texto a mano: el marcador
        // depende del locale --el `java` real en espaniol escribe `a. m.`, esta biblioteca `AM`--.
        //
        // Lo que la ida y vuelta comprueba es exactamente lo que importa: que **las cuatro horas se
        // distingan entre si**. Medianoche y mediodia son las dos que confunden, porque las dos se
        // escriben `12:00` y solo el marcador las separa; si el parser lo ignorara, una de las dos
        // volveria como la otra y esto lo veria.
        DateTimeFormatter doce = DateTimeFormatter.ofPattern("hh:mm a");
        LocalTime[] docenas = {
            LocalTime.of(0, 0), LocalTime.of(12, 0), LocalTime.of(13, 30), LocalTime.of(11, 45),
        };
        int k = 0;
        while (k < docenas.length) {
            ok(LocalTime.parse(doce.format(docenas[k]), doce).equals(docenas[k]));
            k = k + 1;
        }

        // Ida y vuelta.
        LocalTime[] varias = { LocalTime.of(0, 0), LocalTime.of(12, 0), LocalTime.of(23, 59, 59) };
        int i = 0;
        while (i < varias.length) {
            ok(LocalTime.parse(hms.format(varias[i]), hms).equals(varias[i]));
            i = i + 1;
        }
    }

    static void fechaYHora() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime esperado = LocalDateTime.of(2021, 7, 5, 14, 30, 15);
        ok(LocalDateTime.parse("2021-07-05 14:30:15", f).equals(esperado));
        ok(LocalDateTime.parse(f.format(esperado), f).equals(esperado));

        // Literal entre comillas, y la comilla escapada.
        DateTimeFormatter lit = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        ok(LocalDateTime.parse("2021-07-05T14:30:15", lit).equals(esperado));
    }

    static void anioYMes() {
        DateTimeFormatter soloAnio = DateTimeFormatter.ofPattern("yyyy");
        ok(Year.parse("2021", soloAnio).equals(Year.of(2021)));

        DateTimeFormatter am = DateTimeFormatter.ofPattern("yyyy-MM");
        ok(YearMonth.parse("2021-07", am).equals(YearMonth.of(2021, 7)));

        DateTimeFormatter md = DateTimeFormatter.ofPattern("MM-dd");
        ok(MonthDay.parse("07-05", md).equals(MonthDay.of(7, 5)));

        // Un patron completo tambien sirve para pedir solo una parte: los campos crudos siguen ahi.
        DateTimeFormatter completo = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ok(Year.parse("2021-07-05", completo).equals(Year.of(2021)));
        ok(YearMonth.parse("2021-07-05", completo).equals(YearMonth.of(2021, 7)));
    }

    static void conDesplazamiento() {
        DateTimeFormatter ot = DateTimeFormatter.ofPattern("HH:mm:ssXXX");
        ok(OffsetTime.parse("10:15:30+01:00", ot)
                .equals(OffsetTime.of(LocalTime.of(10, 15, 30), ZoneOffset.ofHours(1))));
        // `X` escribe y lee `Z` para el cero.
        ok(OffsetTime.parse("10:15:30Z", ot)
                .equals(OffsetTime.of(LocalTime.of(10, 15, 30), ZoneOffset.UTC)));
        // Negativo, y con minutos.
        ok(OffsetTime.parse("10:15:30-03:30", ot)
                .equals(OffsetTime.of(LocalTime.of(10, 15, 30), ZoneOffset.ofHoursMinutes(-3, -30))));

        DateTimeFormatter odt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        OffsetDateTime esperado = OffsetDateTime.of(LocalDateTime.of(2021, 7, 5, 14, 30, 15),
                ZoneOffset.ofHours(-3));
        ok(OffsetDateTime.parse("2021-07-05T14:30:15-03:00", odt).equals(esperado));
        ok(OffsetDateTime.parse(odt.format(esperado), odt).equals(esperado));

        ZonedDateTime zesperado = ZonedDateTime.of(LocalDateTime.of(2021, 7, 5, 14, 30, 15),
                ZoneOffset.ofHours(2));
        ok(ZonedDateTime.parse("2021-07-05T14:30:15+02:00", odt).equals(zesperado));
        ok(ZonedDateTime.parse(odt.format(zesperado), odt).equals(zesperado));
    }

    // Lo que NO tiene que andar. Un parser que acepta de mas devuelve una fecha equivocada, que es
    // mucho peor que un error.
    static void loQueFalla() {
        DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        tira(iso, "2024-02-29 sobra", 0);   // texto de mas al final
        tira(iso, "2024-02", 0);            // se corta
        tira(iso, "", 0);                   // vacio
        tira(iso, "abcd-ef-gh", 0);         // no son numeros
        tira(iso, "2024/02/29", 0);         // separador que no es

        // Un patron de solo hora no da una fecha, y uno de solo fecha no da una hora.
        DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
        tira(hm, "10:15", 0);
        tira(iso, "2024-02-29", 1);

        // Un patron de fecha y hora al que le falta la hora tampoco da un LocalDateTime.
        tira(iso, "2024-02-29", 2);
    }

    // ------------------------------------------------------------------ los predefinidos
    //
    // Aca **si** se compara el texto escrito a mano, y no rompe la regla del locale: ISO-8601 fija el
    // texto entero --digitos, guiones, la `T`, la `Z`-- y no tiene un solo nombre adentro. Es
    // exactamente el caso opuesto al de `MMM`: el locale no puede cambiar el resultado.
    static void predefinidos() {
        LocalDate d = LocalDate.of(2024, 2, 29);
        ok(DateTimeFormatter.ISO_LOCAL_DATE.format(d).equals("2024-02-29"));
        ok(LocalDate.parse("2024-02-29", DateTimeFormatter.ISO_LOCAL_DATE).equals(d));
        ok(DateTimeFormatter.BASIC_ISO_DATE.format(d).equals("20240229"));
        ok(LocalDate.parse("20240229", DateTimeFormatter.BASIC_ISO_DATE).equals(d));
        ok(DateTimeFormatter.ISO_ORDINAL_DATE.format(d).equals("2024-060"));
        ok(LocalDate.parse("2024-060", DateTimeFormatter.ISO_ORDINAL_DATE).equals(d));
        // El calendario de semanas ISO: el 29 de febrero de 2024 es el jueves de la semana 9.
        ok(DateTimeFormatter.ISO_WEEK_DATE.format(d).equals("2024-W09-4"));
        ok(LocalDate.parse("2024-W09-4", DateTimeFormatter.ISO_WEEK_DATE).equals(d));
        ok(DateTimeFormatter.ISO_DATE.format(d).equals("2024-02-29"));
        ok(LocalDate.parse("2024-02-29", DateTimeFormatter.ISO_DATE).equals(d));

        // La hora, con sus dos secciones opcionales. `10:15` se escribe `10:15:00` --un `LocalTime`
        // siempre tiene segundos-- y la fraccion no sale cuando es cero.
        LocalTime conFraccion = LocalTime.of(10, 15, 30, 500000000);
        ok(DateTimeFormatter.ISO_LOCAL_TIME.format(conFraccion).equals("10:15:30.5"));
        ok(DateTimeFormatter.ISO_LOCAL_TIME.format(LocalTime.of(10, 15, 30)).equals("10:15:30"));
        ok(DateTimeFormatter.ISO_LOCAL_TIME.format(LocalTime.of(10, 15)).equals("10:15:00"));
        ok(LocalTime.parse("10:15:30.5", DateTimeFormatter.ISO_LOCAL_TIME).equals(conFraccion));
        // Y al leer, la seccion que no esta simplemente no esta: `10:15` es una hora valida.
        ok(LocalTime.parse("10:15", DateTimeFormatter.ISO_LOCAL_TIME).equals(LocalTime.of(10, 15)));
        ok(DateTimeFormatter.ISO_TIME.format(conFraccion).equals("10:15:30.5"));

        LocalDateTime ldt = LocalDateTime.of(2021, 7, 5, 14, 30, 15);
        ok(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ldt).equals("2021-07-05T14:30:15"));
        ok(LocalDateTime.parse("2021-07-05T14:30:15", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .equals(ldt));
        ok(LocalDateTime.parse("2021-07-05T14:30:15.123456789",
                DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .equals(LocalDateTime.of(2021, 7, 5, 14, 30, 15, 123456789)));
        ok(DateTimeFormatter.ISO_DATE_TIME.format(ldt).equals("2021-07-05T14:30:15"));

        OffsetDateTime odt = OffsetDateTime.of(ldt, ZoneOffset.ofHours(2));
        ok(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(odt).equals("2021-07-05T14:30:15+02:00"));
        ok(OffsetDateTime.parse("2021-07-05T14:30:15+02:00",
                DateTimeFormatter.ISO_OFFSET_DATE_TIME).equals(odt));
        ok(DateTimeFormatter.ISO_OFFSET_DATE.format(odt).equals("2021-07-05+02:00"));
        ok(DateTimeFormatter.ISO_DATE.format(odt).equals("2021-07-05+02:00"));
        ok(DateTimeFormatter.BASIC_ISO_DATE.format(odt).equals("20210705+0200"));

        OffsetTime ot = OffsetTime.of(LocalTime.of(10, 15, 30), ZoneOffset.ofHours(1));
        ok(DateTimeFormatter.ISO_OFFSET_TIME.format(ot).equals("10:15:30+01:00"));
        ok(OffsetTime.parse("10:15:30+01:00", DateTimeFormatter.ISO_OFFSET_TIME).equals(ot));
        ok(DateTimeFormatter.ISO_TIME.format(ot).equals("10:15:30+01:00"));

        // Con una zona que es un desplazamiento, la seccion del `[...]` **no sale**: `[+02:00]`
        // repetiria lo que el desplazamiento ya dijo. (Una region de verdad no se prueba aca porque
        // esta biblioteca no trae la base de datos de zonas horarias.)
        ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneOffset.ofHours(2));
        ok(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(zdt).equals("2021-07-05T14:30:15+02:00"));
        ok(ZonedDateTime.parse("2021-07-05T14:30:15+02:00", DateTimeFormatter.ISO_ZONED_DATE_TIME)
                .equals(zdt));
        ok(DateTimeFormatter.ISO_DATE_TIME.format(zdt).equals("2021-07-05T14:30:15+02:00"));

        // Ida y vuelta por los `ISO_*`, que es la propiedad que de verdad importa.
        LocalDate[] varias = {
            LocalDate.of(2000, 2, 29), LocalDate.of(1999, 12, 31), LocalDate.of(1, 1, 1),
        };
        int i = 0;
        while (i < varias.length) {
            ok(LocalDate.parse(DateTimeFormatter.ISO_LOCAL_DATE.format(varias[i]),
                    DateTimeFormatter.ISO_LOCAL_DATE).equals(varias[i]));
            ok(LocalDate.parse(DateTimeFormatter.BASIC_ISO_DATE.format(varias[i]),
                    DateTimeFormatter.BASIC_ISO_DATE).equals(varias[i]));
            ok(LocalDate.parse(DateTimeFormatter.ISO_ORDINAL_DATE.format(varias[i]),
                    DateTimeFormatter.ISO_ORDINAL_DATE).equals(varias[i]));
            ok(LocalDate.parse(DateTimeFormatter.ISO_WEEK_DATE.format(varias[i]),
                    DateTimeFormatter.ISO_WEEK_DATE).equals(varias[i]));
            i = i + 1;
        }
    }

    // ------------------------------------------------------------------ el instante
    //
    // `ISO_INSTANT` es el unico que escribe **siempre en UTC**, y el unico que lee dos cosas que
    // ningun reloj admite: `24:00` --la medianoche del dia siguiente-- y `:60`, el segundo
    // intercalar. Que las lea y las cuente bien es lo que se comprueba aca.
    static void instantes() {
        ok(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(0L))
                .equals("1970-01-01T00:00:00Z"));
        ok(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(1609459200L))
                .equals("2021-01-01T00:00:00Z"));
        // La fraccion sale en grupos de tres, que es lo que ISO-8601 pide.
        ok(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(1609459200L, 123000000L))
                .equals("2021-01-01T00:00:00.123Z"));
        ok(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(1609459200L, 123456000L))
                .equals("2021-01-01T00:00:00.123456Z"));
        ok(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(1609459200L, 123456789L))
                .equals("2021-01-01T00:00:00.123456789Z"));
        ok(new DateTimeFormatterBuilder().appendInstant(3).toFormatter()
                .format(Instant.ofEpochSecond(1609459200L)).equals("2021-01-01T00:00:00.000Z"));
        ok(new DateTimeFormatterBuilder().appendInstant(0).toFormatter()
                .format(Instant.ofEpochSecond(1609459200L, 500000000L))
                .equals("2021-01-01T00:00:00Z"));

        Instant esperado = Instant.ofEpochSecond(1625495415L, 500000000L);
        ok(DateTimeFormatter.ISO_INSTANT.parse("2021-07-05T14:30:15.5Z", INSTANTE)
                .equals(esperado));
        // El mismo instante escrito con otro desplazamiento es el mismo instante.
        ok(DateTimeFormatter.ISO_INSTANT.parse("2021-07-05T16:30:15.5+02:00", INSTANTE)
                .equals(esperado));
        // `24:00` es la medianoche del dia siguiente, no la hora 24 del mismo.
        ok(DateTimeFormatter.ISO_INSTANT.parse("2021-07-05T24:00:00Z", INSTANTE)
                .equals(DateTimeFormatter.ISO_INSTANT.parse("2021-07-06T00:00:00Z", INSTANTE)));
        // El segundo intercalar se lee como `:59` y se anota.
        ok(DateTimeFormatter.ISO_INSTANT.parse("2021-07-05T23:59:60Z")
                .query(DateTimeFormatter.parsedLeapSecond()).booleanValue());
        ok(!DateTimeFormatter.ISO_INSTANT.parse("2021-07-05T23:59:59Z")
                .query(DateTimeFormatter.parsedLeapSecond()).booleanValue());

        // Ida y vuelta.
        Instant[] instantes = {
            Instant.ofEpochSecond(0L), Instant.ofEpochSecond(-1L),
            Instant.ofEpochSecond(1625495415L, 1L), Instant.ofEpochSecond(1625495415L, 999000000L),
        };
        int i = 0;
        while (i < instantes.length) {
            ok(DateTimeFormatter.ISO_INSTANT.parse(
                    DateTimeFormatter.ISO_INSTANT.format(instantes[i]), INSTANTE)
                    .equals(instantes[i]));
            i = i + 1;
        }
    }

    // ------------------------------------------------------------------ el armador
    //
    // Lo que una cadena de patron **no puede decir**: relleno, secciones opcionales, un anio
    // recortado, un valor por defecto, nombres puestos a mano.
    static void armador() {
        // Relleno: se aplica a la pieza siguiente y a ninguna mas.
        ok(new DateTimeFormatterBuilder().padNext(5, '0')
                .appendValue(ChronoField.DAY_OF_MONTH).toFormatter()
                .format(LocalDate.of(2024, 2, 29)).equals("00029"));

        // Seccion opcional escrita en el patron con corchetes.
        DateTimeFormatter conCola = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd[ HH:mm]").toFormatter();
        ok(conCola.format(LocalDateTime.of(2021, 7, 5, 14, 30)).equals("2021-07-05 14:30"));
        ok(LocalDate.parse("2021-07-05", conCola).equals(LocalDate.of(2021, 7, 5)));
        ok(LocalDateTime.parse("2021-07-05 14:30", conCola)
                .equals(LocalDateTime.of(2021, 7, 5, 14, 30)));

        // Anio recortado a dos digitos con ventana explicita.
        DateTimeFormatter dosDigitos = new DateTimeFormatterBuilder()
                .appendValueReduced(ChronoField.YEAR, 2, 2, 2000).toFormatter();
        ok(dosDigitos.format(LocalDate.of(2024, 1, 1)).equals("24"));
        ok(Year.parse("24", dosDigitos).equals(Year.of(2024)));
        // Con el maximo en dos, un anio de fuera de la ventana se recorta igual.
        ok(dosDigitos.format(LocalDate.of(1875, 1, 1)).equals("75"));
        // Con el maximo en cuatro, sale entero: `75` se releeria como 2075, `1875` no.
        DateTimeFormatter hastaCuatro = new DateTimeFormatterBuilder()
                .appendValueReduced(ChronoField.YEAR, 2, 4, 2000).toFormatter();
        ok(hastaCuatro.format(LocalDate.of(1875, 1, 1)).equals("1875"));
        ok(hastaCuatro.format(LocalDate.of(2024, 1, 1)).equals("24"));
        ok(Year.parse("1875", hastaCuatro).equals(Year.of(1875)));
        ok(Year.parse("24", hastaCuatro).equals(Year.of(2024)));

        // Un valor por defecto completa lo que el texto no trajo, sin que el patron mienta.
        DateTimeFormatter conDia = new DateTimeFormatterBuilder().appendPattern("yyyy-MM")
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1L).toFormatter();
        ok(LocalDate.parse("2021-07", conDia).equals(LocalDate.of(2021, 7, 1)));

        // La fraccion, con y sin punto.
        ok(new DateTimeFormatterBuilder().appendFraction(ChronoField.NANO_OF_SECOND, 3, 9, true)
                .toFormatter().format(LocalTime.of(1, 2, 3, 400000000)).equals(".400"));
        ok(new DateTimeFormatterBuilder().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, false)
                .toFormatter().format(LocalTime.of(1, 2, 3, 123456000)).equals("123456"));

        // Las formas del desplazamiento. La minuscula sale solo cuando hay algo que decir.
        OffsetDateTime dosHoras = OffsetDateTime.of(LocalDateTime.of(2021, 7, 5, 14, 30, 15),
                ZoneOffset.ofHours(2));
        ok(new DateTimeFormatterBuilder().appendOffset("+HH", "Z").toFormatter()
                .format(dosHoras).equals("+02"));
        ok(new DateTimeFormatterBuilder().appendOffset("+HHmm", "Z").toFormatter()
                .format(OffsetDateTime.of(LocalDateTime.of(2021, 7, 5, 14, 30, 15),
                        ZoneOffset.ofHoursMinutes(-3, -30))).equals("-0330"));
        ok(new DateTimeFormatterBuilder().appendOffset("+HH:MM:ss", "Z").toFormatter()
                .format(OffsetDateTime.of(LocalDateTime.of(2021, 7, 5, 14, 30, 15),
                        ZoneOffset.ofHoursMinutesSeconds(5, 45, 0))).equals("+05:45"));
        ok(new DateTimeFormatterBuilder().appendOffset("+HHMM", "GMT").toFormatter()
                .format(OffsetDateTime.of(LocalDateTime.of(2021, 7, 5, 14, 30, 15),
                        ZoneOffset.UTC)).equals("GMT"));

        // El signo que siempre sale.
        ok(new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4, 10, SignStyle.ALWAYS).toFormatter()
                .format(LocalDate.of(2024, 1, 1)).equals("+2024"));

        // El id del calendario --que no es texto traducible, es la clave--.
        ok(new DateTimeFormatterBuilder().appendChronologyId().toFormatter()
                .format(LocalDate.of(2024, 1, 1)).equals("ISO"));

        // La comilla escapada.
        ok(DateTimeFormatter.ofPattern("'a''b'").format(LocalDate.of(2024, 1, 1)).equals("a'b"));
    }

    // ------------------------------------------------------------------ los nombres
    //
    // Con `Locale.ENGLISH` explicito los dos lados escriben lo mismo, y por eso aca **si** se puede
    // comparar el texto. Sin el locale explicito no: el `java` real toma el de la maquina.
    static void nombres() {
        Locale en = Locale.ENGLISH;
        ok(DateTimeFormatter.ofPattern("dd MMM yyyy", en)
                .format(LocalDate.of(2021, 7, 5)).equals("05 Jul 2021"));
        ok(DateTimeFormatter.ofPattern("dd MMMM yyyy", en)
                .format(LocalDate.of(2021, 9, 5)).equals("05 September 2021"));
        ok(DateTimeFormatter.ofPattern("EEEE", en)
                .format(LocalDate.of(2021, 7, 5)).equals("Monday"));
        // La inicial: dato del ingles, no una invencion.
        ok(new DateTimeFormatterBuilder().appendText(ChronoField.MONTH_OF_YEAR, TextStyle.NARROW)
                .toFormatter(en).format(LocalDate.of(2024, 7, 1)).equals("J"));
        ok(new DateTimeFormatterBuilder().appendText(ChronoField.ERA, TextStyle.SHORT)
                .toFormatter(en).format(LocalDate.of(2024, 7, 1)).equals("AD"));
        ok(DateTimeFormatter.ofPattern("GGGG yyyy", en)
                .format(LocalDate.of(-5, 1, 1)).equals("Before Christ 0006"));
        // `u` es el anio proleptico y `y` el de la era: para un anio antes de Cristo no son el mismo.
        ok(DateTimeFormatter.ofPattern("uuuu-MM-dd").format(LocalDate.of(-5, 1, 1))
                .equals("-0005-01-01"));
        ok(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.of(-5, 1, 1))
                .equals("0006-01-01"));
        // Sin distinguir mayusculas de minusculas, cuando se pide.
        ok(new DateTimeFormatterBuilder().parseCaseInsensitive()
                .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT).toFormatter(en)
                .parse("jUl").getLong(ChronoField.MONTH_OF_YEAR) == 7L);

        // Las horas del reloj de doce, y las del de veinticuatro que se le parecen.
        ok(DateTimeFormatter.ofPattern("hh:mm a", en).format(LocalTime.of(0, 0)).equals("12:00 AM"));
        ok(DateTimeFormatter.ofPattern("hh:mm a", en).format(LocalTime.of(12, 0))
                .equals("12:00 PM"));
        ok(DateTimeFormatter.ofPattern("KK:mm", en).format(LocalTime.of(13, 5)).equals("01:05"));
        ok(DateTimeFormatter.ofPattern("kk:mm", en).format(LocalTime.of(0, 5)).equals("24:05"));
        ok(DateTimeFormatter.ofPattern("QQ", en).format(LocalDate.of(2024, 7, 1)).equals("03"));

        // El RFC 1123 escribe nombres en ingles **porque el formato los fija**, no porque el locale
        // los pida: con otro locale escribe los mismos.
        OffsetDateTime odt = OffsetDateTime.of(LocalDateTime.of(2021, 7, 5, 14, 30, 15),
                ZoneOffset.ofHours(-3));
        ok(DateTimeFormatter.RFC_1123_DATE_TIME.format(odt)
                .equals("Mon, 5 Jul 2021 14:30:15 -0300"));
        ok(DateTimeFormatter.RFC_1123_DATE_TIME.format(
                OffsetDateTime.of(LocalDateTime.of(2021, 7, 5, 14, 30, 15), ZoneOffset.UTC))
                .equals("Mon, 5 Jul 2021 14:30:15 GMT"));
        ok(LocalDate.parse("Mon, 05 Jul 2021 14:30:15 GMT", DateTimeFormatter.RFC_1123_DATE_TIME)
                .equals(LocalDate.of(2021, 7, 5)));
        ok(OffsetDateTime.parse("Mon, 5 Jul 2021 14:30:15 -0300",
                DateTimeFormatter.RFC_1123_DATE_TIME).equals(odt));
    }

    // ------------------------------------------------------------------ los ajustes
    static void ajustes() {
        // Los tres modos de resolver, sobre el mismo texto imposible.
        DateTimeFormatter p = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ok(LocalDate.parse("2023-02-30", p.withResolverStyle(ResolverStyle.SMART))
                .equals(LocalDate.of(2023, 2, 28)));
        ok(LocalDate.parse("2023-02-30", p.withResolverStyle(ResolverStyle.LENIENT))
                .equals(LocalDate.of(2023, 3, 2)));
        tiraAlgo(new Runnable() {
            public void run() {
                LocalDate.parse("2023-02-30", DateTimeFormatter.ISO_LOCAL_DATE);
            }
        });

        // Los getters devuelven lo que se puso.
        ok(DateTimeFormatter.ISO_LOCAL_DATE.getResolverStyle() == ResolverStyle.STRICT);
        ok(DateTimeFormatter.ISO_LOCAL_DATE.getZone() == null);
        ok(DateTimeFormatter.ISO_LOCAL_DATE.getChronology()
                .equals(java.time.chrono.IsoChronology.INSTANCE));
        ok(DateTimeFormatter.ISO_LOCAL_DATE.getDecimalStyle().equals(DecimalStyle.STANDARD));
        ok(p.withZone(ZoneOffset.UTC).getZone().equals(ZoneOffset.UTC));
        ok(p.withResolverStyle(ResolverStyle.LENIENT).getResolverStyle()
                == ResolverStyle.LENIENT);
        ok(p.withResolverFields(ChronoField.YEAR, ChronoField.MONTH_OF_YEAR,
                ChronoField.DAY_OF_MONTH).getResolverFields().size() == 3);
        ok(p.getResolverFields() == null);

        // Los simbolos: otro separador decimal, y otro cero.
        ok(DateTimeFormatter.ISO_LOCAL_TIME
                .withDecimalStyle(DecimalStyle.STANDARD.withDecimalSeparator(','))
                .format(LocalTime.of(10, 15, 30, 500000000)).equals("10:15:30,5"));
        ok(DateTimeFormatter.ofPattern("HH:mm")
                .withDecimalStyle(DecimalStyle.STANDARD.withZeroDigit('A'))
                .format(LocalTime.of(10, 15)).equals("BA:BF"));

        // Con zona: un instante se convierte de verdad --la fecha que sale es la de ese lugar--.
        ok(p.withZone(ZoneOffset.UTC).format(Instant.ofEpochSecond(1609459200L))
                .equals("2021-01-01"));
        ok(p.withZone(ZoneOffset.ofHours(-5)).format(Instant.ofEpochSecond(1609459200L))
                .equals("2020-12-31"));

        // Leer desde una posicion, dejando el resto.
        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor leido = DateTimeFormatter.ISO_LOCAL_DATE.parse("2024-02-29 y algo mas", pos);
        ok(pos.getIndex() == 10);
        ok(LocalDate.from(leido).equals(LocalDate.of(2024, 2, 29)));

        // Sin resolver: los campos crudos, y **ningun** dia epoch, porque nadie lo calculo.
        ParsePosition pos2 = new ParsePosition(0);
        TemporalAccessor crudo = DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved("2024-02-29", pos2);
        ok(pos2.getIndex() == 10);
        ok(crudo.getLong(ChronoField.YEAR) == 2024L);
        ok(crudo.getLong(ChronoField.MONTH_OF_YEAR) == 2L);
        ok(!crudo.isSupported(ChronoField.EPOCH_DAY));
        // Un texto que no encaja no tira aca: se anota en la posicion.
        ParsePosition pos3 = new ParsePosition(0);
        ok(DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved("no", pos3) == null);
        ok(pos3.getErrorIndex() >= 0);

        // La mejor de varias: el mismo formateador da un `OffsetDateTime` o un `LocalDateTime`
        // segun lo que el texto de verdad traiga.
        TemporalAccessor conOffset = DateTimeFormatter.ISO_DATE_TIME
                .parseBest("2021-07-05T14:30:15+02:00", CON_OFFSET, SIN_OFFSET);
        ok(conOffset instanceof OffsetDateTime);
        TemporalAccessor sinOffset = DateTimeFormatter.ISO_DATE_TIME
                .parseBest("2021-07-05T14:30:15", CON_OFFSET, SIN_OFFSET);
        ok(sinOffset instanceof LocalDateTime);

        // El dia que sobra de un `24:00`.
        TemporalAccessor medianoche = DateTimeFormatter.ISO_LOCAL_TIME
                .withResolverStyle(ResolverStyle.SMART).parse("24:00");
        ok(medianoche.query(DateTimeFormatter.parsedExcessDays()).equals(Period.ofDays(1)));
        ok(LocalTime.from(medianoche).equals(LocalTime.of(0, 0)));
        ok(DateTimeFormatter.ISO_LOCAL_TIME.parse("10:15")
                .query(DateTimeFormatter.parsedExcessDays()).equals(Period.ZERO));

        // El puente con `java.text`.
        ok(DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").toFormat()
                .format(LocalDateTime.of(2021, 7, 5, 14, 30, 15)).equals("2021-07-05T14:30:15"));
    }

    // Lo que NO tiene que andar, segunda tanda: lo que las piezas nuevas tienen que rechazar.
    static void loQueFallaMas() {
        // (Las letras que necesitarian datos del CLDR --`z`, `w`, `QQQ`-- las rechaza esta biblioteca
        // en `ofPattern` con el motivo escrito, y el `java` real las acepta porque tiene los datos.
        // Esa diferencia **no se comprueba aca**: esta prueba compara las dos maquinas, y una
        // divergencia deliberada haria fallar a una de las dos por definicion. Queda documentada en
        // `DateTimeFormatterBuilder`.)
        //
        // Una seccion opcional que no se abrio.
        tiraAlgo(new Runnable() {
            public void run() {
                new DateTimeFormatterBuilder().optionalEnd();
            }
        });
        // Una comilla sin cerrar.
        tiraAlgo(new Runnable() {
            public void run() {
                DateTimeFormatter.ofPattern("yyyy'sin cerrar");
            }
        });
        // Un formateador de fecha no escribe un valor que no tiene fecha.
        tiraAlgo(new Runnable() {
            public void run() {
                DateTimeFormatter.ISO_LOCAL_DATE.format(LocalTime.of(10, 15));
            }
        });
        // Textos que no encajan con los predefinidos.
        tira(DateTimeFormatter.ISO_LOCAL_DATE, "2024-2-29", 0);
        tira(DateTimeFormatter.ISO_LOCAL_DATE, "20240229", 0);
        tira(DateTimeFormatter.BASIC_ISO_DATE, "2024-02-29", 0);
        tira(DateTimeFormatter.ISO_OFFSET_DATE_TIME, "2021-07-05T14:30:15", 2);
        tira(DateTimeFormatter.ISO_WEEK_DATE, "2024-W09", 0);
    }

    public static int run() {
        fechas();
        horas();
        fechaYHora();
        anioYMes();
        conDesplazamiento();
        loQueFalla();
        predefinidos();
        instantes();
        armador();
        nombres();
        ajustes();
        loQueFallaMas();
        return primerFallo;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
