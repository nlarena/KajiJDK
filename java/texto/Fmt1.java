import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * El formato localizado de {@code java.time.format}, contra el JDK.
 *
 * <p>Lo que se verifica es la afirmacion sobre la que se apoya toda la tanda: que el patron que
 * {@code DateTimeFormatterBuilder.getLocalizedDateTimePattern} devuelve es <em>exactamente</em> el
 * que devuelve {@code java.text}. Se comparan los cuatro estilos, las tres combinaciones y los
 * locales para los que esta biblioteca trae datos.
 *
 * <p>Formatear con nombres solo funciona en ingles -- es la pared conocida de esta biblioteca --,
 * asi que los textos salen en locales ingleses y los demas locales entran solo por el patron.
 */
public class Fmt1 {

    static void linea(String s) {
        System.out.println("//" + esc(s));
    }

    /**
     * Todo lo que no sea ASCII imprimible sale escapado.
     *
     * <p>No es cosmetico. Varios de estos patrones traen caracteres que no se ven -- el ingles
     * separa el a.m./p.m. con un espacio angosto que no corta linea -- y ademas el JDK real, con su
     * salida redirigida a una tuberia, los convierte en signos de pregunta. Escapando, las dos
     * corridas comparan los mismos caracteres y no la codificacion de la consola.
     */
    static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 32 && c < 127) {
                b.append(c);
            } else {
                String h = Integer.toHexString(c);
                b.append("<u+");
                for (int k = h.length(); k < 4; k++) {
                    b.append('0');
                }
                b.append(h).append('>');
            }
        }
        return b.toString();
    }

    static final FormatStyle[] ESTILOS = {FormatStyle.FULL, FormatStyle.LONG, FormatStyle.MEDIUM,
        FormatStyle.SHORT};

    /**
     * Los locales que esta biblioteca trae, que son los que
     * {@link DecimalStyle#getAvailableLocales} enumera.
     *
     * <p>Un locale de afuera de esa lista cae en el mas cercano que haya -- {@code en_GB} termina
     * con los patrones de {@code en_US} --, asi que compararlo mediria el tamano de la tabla de
     * datos y no el codigo. La lista es el contrato; probar contra algo que no esta en ella no dice
     * nada sobre si el codigo esta bien.
     */
    static final Locale[] LOCALES = {Locale.ROOT, Locale.US, Locale.GERMANY,
        Locale.FRANCE, Locale.JAPAN};

    static void patrones() {
        linea("--- patrones por locale y estilo ---");
        for (int j = 0; j < LOCALES.length; j++) {
            Locale l = LOCALES[j];
            for (int i = 0; i < ESTILOS.length; i++) {
                linea(l + " fecha " + ESTILOS[i] + " = ["
                        + DateTimeFormatterBuilder.getLocalizedDateTimePattern(ESTILOS[i], null,
                                IsoChronology.INSTANCE, l) + "]");
            }
            for (int i = 0; i < ESTILOS.length; i++) {
                linea(l + " hora " + ESTILOS[i] + " = ["
                        + DateTimeFormatterBuilder.getLocalizedDateTimePattern(null, ESTILOS[i],
                                IsoChronology.INSTANCE, l) + "]");
            }
            for (int i = 0; i < ESTILOS.length; i++) {
                linea(l + " ambas " + ESTILOS[i] + " = ["
                        + DateTimeFormatterBuilder.getLocalizedDateTimePattern(ESTILOS[i],
                                ESTILOS[i], IsoChronology.INSTANCE, l) + "]");
            }
        }
        // Combinaciones cruzadas: fecha larga con hora corta y al reves.
        linea("US larga/corta = [" + DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                FormatStyle.LONG, FormatStyle.SHORT, IsoChronology.INSTANCE, Locale.US) + "]");
        linea("US corta/larga = [" + DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                FormatStyle.SHORT, FormatStyle.LONG, IsoChronology.INSTANCE, Locale.US) + "]");

        try {
            DateTimeFormatterBuilder.getLocalizedDateTimePattern(null, null,
                    IsoChronology.INSTANCE, Locale.US);
            linea("dos nulos aceptados");
        } catch (IllegalArgumentException e) {
            linea("dos nulos rechazados: " + e.getMessage());
        }
        try {
            DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.FULL, null, null,
                    Locale.US);
            linea("cronologia nula aceptada");
        } catch (NullPointerException e) {
            linea("cronologia nula rechazada");
        }
        try {
            DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.FULL, null,
                    IsoChronology.INSTANCE, null);
            linea("locale nulo aceptado");
        } catch (NullPointerException e) {
            linea("locale nulo rechazado");
        }
    }

    static void fabricas() {
        linea("--- las cuatro fabricas ---");
        LocalDate d = LocalDate.of(2024, 3, 15);
        LocalDateTime dt = LocalDateTime.of(2024, 3, 15, 14, 5, 9);
        for (int i = 0; i < ESTILOS.length; i++) {
            DateTimeFormatter f = DateTimeFormatter.ofLocalizedDate(ESTILOS[i])
                    .withLocale(Locale.US);
            linea("fecha " + ESTILOS[i] + " -> " + f.format(d)
                    + " cronologia=" + f.getChronology() + " resolutor=" + f.getResolverStyle());
        }
        for (int i = 0; i < ESTILOS.length; i++) {
            // FULL y LONG piden el nombre de la zona, que una hora local no tiene.
            if (ESTILOS[i] == FormatStyle.FULL || ESTILOS[i] == FormatStyle.LONG) {
                continue;
            }
            DateTimeFormatter f = DateTimeFormatter.ofLocalizedTime(ESTILOS[i])
                    .withLocale(Locale.US);
            linea("hora " + ESTILOS[i] + " -> " + f.format(dt));
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.US);
        linea("ambas MEDIUM -> " + dtf.format(dt));
        DateTimeFormatter mix = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG,
                FormatStyle.SHORT).withLocale(Locale.US);
        linea("larga/corta -> " + mix.format(dt));

        // El locale se resuelve al usar el formateador y no al armarlo: el mismo objeto da otro
        // texto con otro locale. Los nombres siguen siendo los ingleses -- es la pared conocida de
        // esta biblioteca -- asi que el otro locale es ROOT, que en CLDR es de donde el ingles
        // deriva y por eso comparte los nombres.
        DateTimeFormatter raiz = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.ROOT);
        linea("mismo estilo en ROOT -> " + raiz.format(d));
        linea("y de vuelta en US -> " + raiz.withLocale(Locale.US).format(d));

        try {
            DateTimeFormatter.ofLocalizedDate(null);
            linea("estilo nulo aceptado");
        } catch (NullPointerException e) {
            linea("estilo nulo rechazado");
        }
        try {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, null);
            linea("hora nula aceptada");
        } catch (NullPointerException e) {
            linea("hora nula rechazada");
        }
        try {
            new DateTimeFormatterBuilder().appendLocalized(null, null);
            linea("appendLocalized con dos nulos aceptado");
        } catch (IllegalArgumentException e) {
            linea("appendLocalized con dos nulos rechazado: " + e.getMessage());
        }
    }

    static void offsets() {
        linea("--- el offset con GMT ---");
        int[] segundos = {0, 3600, -3600, 8 * 3600, -(5 * 3600 + 30 * 60), 45 * 60,
            (5 * 3600 + 30 * 60 + 15), 10 * 3600, -(11 * 3600 + 45 * 60)};
        DateTimeFormatter full = new DateTimeFormatterBuilder()
                .appendLocalizedOffset(TextStyle.FULL).toFormatter(Locale.US);
        DateTimeFormatter corto = new DateTimeFormatterBuilder()
                .appendLocalizedOffset(TextStyle.SHORT).toFormatter(Locale.US);
        for (int i = 0; i < segundos.length; i++) {
            OffsetDateTime o = OffsetDateTime.of(LocalDateTime.of(2024, 3, 15, 12, 0),
                    ZoneOffset.ofTotalSeconds(segundos[i]));
            linea("offset " + segundos[i] + " full=[" + full.format(o) + "] corto=["
                    + corto.format(o) + "]");
        }
        // Y de vuelta: lo que se escribe se vuelve a leer.
        String[] textos = {"GMT", "GMT+8", "GMT+08:00", "GMT-5:30", "GMT+05:30:15"};
        for (int i = 0; i < textos.length; i++) {
            try {
                TemporalAccessor t = corto.parse(textos[i]);
                linea("leer [" + textos[i] + "] -> "
                        + t.get(java.time.temporal.ChronoField.OFFSET_SECONDS));
            } catch (RuntimeException e) {
                linea("leer [" + textos[i] + "] falla");
            }
        }
        try {
            new DateTimeFormatterBuilder().appendLocalizedOffset(TextStyle.NARROW);
            linea("estilo NARROW aceptado");
        } catch (IllegalArgumentException e) {
            linea("estilo NARROW rechazado: " + e.getMessage());
        }
        try {
            new DateTimeFormatterBuilder().appendLocalizedOffset(null);
            linea("estilo nulo aceptado");
        } catch (NullPointerException e) {
            linea("estilo nulo rechazado");
        }
    }

    static void calendario() {
        linea("--- el nombre del calendario ---");
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendChronologyText(TextStyle.FULL).toFormatter(Locale.US);
        linea("ISO full -> " + f.format(LocalDate.of(2024, 3, 15)));
        DateTimeFormatter c = new DateTimeFormatterBuilder()
                .appendChronologyText(TextStyle.SHORT).toFormatter(Locale.US);
        linea("ISO short -> " + c.format(LocalDate.of(2024, 3, 15)));
        linea("nombre directo=" + IsoChronology.INSTANCE.getDisplayName(TextStyle.FULL,
                Locale.US));
        try {
            new DateTimeFormatterBuilder().appendChronologyText(null);
            linea("estilo nulo aceptado");
        } catch (NullPointerException e) {
            linea("estilo nulo rechazado");
        }
    }

    static void simbolos() {
        linea("--- DecimalStyle ---");
        // La cantidad de locales no entra: esta biblioteca trae datos para seis y el JDK para
        // cientos. Lo que si tiene que coincidir es que los seis esten y que el conjunto no sea
        // modificable de rebote.
        java.util.Set<Locale> s = DecimalStyle.getAvailableLocales();
        linea("tiene ROOT=" + s.contains(Locale.ROOT) + " US=" + s.contains(Locale.US)
                + " DE=" + s.contains(Locale.GERMANY) + " FR=" + s.contains(Locale.FRANCE)
                + " JP=" + s.contains(Locale.JAPAN));
        linea("vacio=" + s.isEmpty());
        java.util.Set<Locale> otro = DecimalStyle.getAvailableLocales();
        otro.clear();
        linea("es copia=" + !DecimalStyle.getAvailableLocales().isEmpty());
    }

    public static int run() {
        patrones();
        fabricas();
        offsets();
        calendario();
        simbolos();
        return 0;
    }
}
