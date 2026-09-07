import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Random;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Que sea {@code Serializable} lo que en el JDK es {@code Serializable}.
 *
 * <h2>Por que hace falta una prueba para esto</h2>
 *
 * <p>Una clausula {@code implements} no es un miembro, asi que el censo del API no la mira: una
 * clase puede tener sus doscientos metodos completos y no ser serializable, y el censo la cuenta
 * igual. Y sin embargo se nota, y de la peor manera: {@code "hola" instanceof Serializable} dando
 * falso no rompe nada en el acto, hace que cualquier cosa que filtre por serializabilidad tire lo
 * que no tenia que tirar, en silencio.
 *
 * <p>Asi se encontro: {@code RMIConnectorServer.getAttributes()} descarta lo que no puede viajar, y
 * descartaba todo. El error no estaba ahi sino en {@code java.lang.String}.
 *
 * <p>Las tres raices explicaban casi todo: {@code Throwable} --y con el todas las excepciones--,
 * {@code String} y {@code Class}. El resto son las colecciones y los tipos de fecha, que lo declaran
 * cada uno por su cuenta.
 *
 * <h2>Los arreglos</h2>
 *
 * <p>Un arreglo es {@code Serializable} y {@code Cloneable} por definicion del lenguaje, no porque
 * ninguna clase lo declare: eso lo tiene que saber la VM. Se comprueba aca igual, porque es la
 * mitad de la pregunta y se comprueba en el mismo lugar.
 */
public class SER1 {

    /** Lo que en el JDK es serializable; cada uno vale un bit en el resultado. */
    static Class<?>[] tipos() {
        return new Class<?>[] {
            String.class, Throwable.class, RuntimeException.class, Class.class,
            Integer.class, Double.class, Boolean.class, Character.class,
            ArrayList.class, LinkedList.class, HashMap.class, LinkedHashMap.class,
            HashSet.class, LinkedHashSet.class, TreeMap.class, TreeSet.class,
            Hashtable.class, Properties.class, Vector.class, Stack.class,
            ArrayDeque.class, PriorityQueue.class, IdentityHashMap.class, BitSet.class,
            java.util.EnumMap.class, java.util.EnumSet.class,
            Date.class, GregorianCalendar.class, Locale.class, Random.class,
            java.math.BigInteger.class, java.math.BigDecimal.class, java.math.MathContext.class,
            java.io.File.class, java.net.URI.class, java.net.URL.class,
            java.time.Duration.class, java.time.Instant.class, java.time.LocalDate.class,
            java.time.LocalDateTime.class, java.time.LocalTime.class, java.time.Period.class,
            java.time.Year.class, java.time.YearMonth.class, java.time.ZoneId.class,
            java.time.ZoneOffset.class, java.time.ZonedDateTime.class,
            java.text.DecimalFormatSymbols.class,
            int[].class, String[].class, Object[].class, int[][].class,
        };
    }

    /**
     * Cuantas de las de {@link #tipos()} no son serializables.
     *
     * <p>Se cuentan en vez de devolver la primera para que el resultado diga de una cuanto falta:
     * arreglar de a una y volver a correr treinta veces no aporta nada.
     *
     * @return cuantas fallan; 0 es lo correcto
     */
    public static int cuantasFaltan() {
        final Class<?>[] t = tipos();
        int n = 0;
        for (int i = 0; i < t.length; i++) {
            if (!Serializable.class.isAssignableFrom(t[i])) {
                n++;
            }
        }
        return n;
    }

    /**
     * El indice de la primera que falla, o -1 si no falla ninguna.
     *
     * @return el indice en {@link #tipos()}, o -1
     */
    public static int primera() {
        final Class<?>[] t = tipos();
        for (int i = 0; i < t.length; i++) {
            if (!Serializable.class.isAssignableFrom(t[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Lo mismo pero con instancias, que es como se ve en el codigo de verdad.
     *
     * <p>{@code isAssignableFrom} pregunta por la clase y {@code instanceof} por el objeto. Los dos
     * caminos tienen que dar lo mismo, y son dos caminos distintos adentro de la VM.
     *
     * @return cuantas fallan; 0 es lo correcto
     */
    public static int porInstancia() {
        final Object[] o = {
            "hola", new RuntimeException(), Integer.valueOf(1), Double.valueOf(1),
            new ArrayList<String>(), new HashMap<String, String>(), new TreeSet<String>(),
            new Date(), new int[0], new String[0], String.class,
        };
        int n = 0;
        for (int i = 0; i < o.length; i++) {
            if (!(o[i] instanceof Serializable)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Que los arreglos sean {@code Cloneable}, que es la otra mitad de lo que define el lenguaje.
     *
     * @return cuantas fallan; 0 es lo correcto
     */
    public static int arreglosClonables() {
        final Class<?>[] t = {int[].class, String[].class, Object[].class, int[][].class};
        int n = 0;
        for (int i = 0; i < t.length; i++) {
            if (!Cloneable.class.isAssignableFrom(t[i])) {
                n++;
            }
        }
        return n;
    }

    public static void main(String[] args) {
        System.out.println("faltan=" + cuantasFaltan() + " primera=" + primera()
                + " porInstancia=" + porInstancia() + " clonables=" + arreglosClonables());
        if (primera() >= 0) {
            System.out.println("  la primera es " + tipos()[primera()].getName());
        }
    }
}
