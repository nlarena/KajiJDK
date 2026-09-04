import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.SequencedMap;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;

// Auditoria de comportamiento de java.util: los puntos donde una reimplementacion tiene mas
// chance de diferir del JDK real. No mide superficie -- eso ya lo hace apidiff -- sino que las
// firmas que existen hagan lo que el contrato promete.
//
// El contrato de esta clase es el de la casa: run() devuelve -1 si todo pasa, o el indice de la
// primera comprobacion que falla. El indice es estable porque chk() se llama siempre en el mismo
// orden; por eso ninguna comprobacion esta dentro de un if.
public class UtilAuditTest {

    static int n;
    static int fail;

    static void chk(boolean ok) {
        if (!ok && fail < 0) {
            fail = n;
        }
        n = n + 1;
    }

    // Une lo que salga de un iterable con comas. Se usa para comparar ORDEN, que es la mitad de
    // lo que esta prueba mira: un conjunto igual en contenido y distinto en orden es un bug.
    static String join(java.util.Collection<?> c) {
        StringBuilder sb = new StringBuilder();
        Iterator<?> it = c.iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (!first) {
                sb.append(",");
            }
            sb.append(String.valueOf(it.next()));
            first = false;
        }
        return sb.toString();
    }

    enum Color { RED, GREEN, BLUE, WHITE }

    public static int run() {
        n = 0;
        fail = -1;

        // ---- SequencedCollection / SequencedMap: los defaults de Java 21 ----------------------
        //
        // El JDK los implementa sobre iterator()/reversed()/entrySet(); solo addFirst/addLast y
        // putFirst/putLast se niegan. Un default que lanza donde el JDK devuelve es una firma que
        // miente: existe, compila, y el llamador se come una UnsupportedOperationException.

        // 0: getFirst/getLast heredados por una lista.
        List<String> l0 = new ArrayList<String>();
        l0.add("a");
        l0.add("b");
        l0.add("c");
        chk(l0.getFirst().equals("a") && l0.getLast().equals("c"));

        // 1: las tres vistas secuenciadas de un mapa ordenado.
        TreeMap<String, Integer> tm1 = new TreeMap<String, Integer>();
        tm1.put("b", 2);
        tm1.put("a", 1);
        tm1.put("c", 3);
        chk(join(tm1.sequencedKeySet()).equals("a,b,c"));

        // 2: sequencedValues sigue el mismo orden que las claves.
        chk(join(tm1.sequencedValues()).equals("1,2,3"));

        // 3: sequencedEntrySet, y que sus extremos se puedan pedir.
        chk(tm1.sequencedEntrySet().getFirst().getKey().equals("a"));

        // 4: en un LinkedHashMap las vistas van por orden de insercion, no natural.
        LinkedHashMap<String, Integer> lhm4 = new LinkedHashMap<String, Integer>();
        lhm4.put("z", 1);
        lhm4.put("y", 2);
        lhm4.put("x", 3);
        chk(lhm4.sequencedKeySet().getFirst().equals("z")
                && lhm4.sequencedKeySet().getLast().equals("x"));

        // 5: firstEntry/lastEntry sobre la interfaz, no sobre la clase concreta.
        SequencedMap<String, Integer> sm5 = lhm4;
        chk(sm5.firstEntry().getKey().equals("z") && sm5.lastEntry().getKey().equals("x"));

        // 6: putFirst/putLast SI se niegan en un mapa ordenado -- la posicion la decide el orden.
        boolean uoe6 = false;
        try {
            tm1.putFirst("q", 9);
        } catch (UnsupportedOperationException e) {
            uoe6 = true;
        }
        chk(uoe6);

        // ---- orden de iteracion ----------------------------------------------------------------

        // 7: reinsertar una clave que ya estaba NO la mueve al final en un LinkedHashMap normal.
        LinkedHashMap<String, Integer> lhm7 = new LinkedHashMap<String, Integer>();
        lhm7.put("a", 1);
        lhm7.put("b", 2);
        lhm7.put("c", 3);
        lhm7.put("a", 99);
        chk(join(lhm7.keySet()).equals("a,b,c"));

        // 8: en access-order si la mueve, y un get cuenta como acceso.
        LinkedHashMap<String, Integer> lhm8 = new LinkedHashMap<String, Integer>(16, 0.75f, true);
        lhm8.put("a", 1);
        lhm8.put("b", 2);
        lhm8.put("c", 3);
        lhm8.get("a");
        chk(join(lhm8.keySet()).equals("b,c,a"));

        // 9: re-add sobre un LinkedHashSet tampoco mueve.
        LinkedHashSet<String> lhs9 = new LinkedHashSet<String>();
        lhs9.add("a");
        lhs9.add("b");
        lhs9.add("a");
        chk(join(lhs9).equals("a,b"));

        // 10: un TreeSet con comparador invertido ordena al reves, y descendingSet lo desanda.
        TreeSet<Integer> ts10 = new TreeSet<Integer>(Collections.reverseOrder());
        ts10.add(1);
        ts10.add(3);
        ts10.add(2);
        chk(join(ts10).equals("3,2,1") && join(ts10.descendingSet()).equals("1,2,3"));

        // 11: ArrayDeque -- push mete adelante, add atras.
        ArrayDeque<String> dq11 = new ArrayDeque<String>();
        dq11.add("b");
        dq11.add("c");
        dq11.push("a");
        chk(join(dq11).equals("a,b,c"));

        // ---- comparadores ----------------------------------------------------------------------

        // 12: comparing + thenComparing, con el desempate haciendo trabajo de verdad.
        List<String> l12 = new ArrayList<String>();
        l12.add("bb");
        l12.add("a");
        l12.add("cc");
        l12.add("dd");
        Comparator<String> c12 = Comparator
                .<String, Integer>comparing(new java.util.function.Function<String, Integer>() {
                    public Integer apply(String s) {
                        return Integer.valueOf(s.length());
                    }
                })
                .thenComparing(Comparator.<String>reverseOrder());
        Collections.sort(l12, c12);
        chk(join(l12).equals("a,dd,cc,bb"));

        // 13: nullsFirst delega en el comparador de atras para los no-nulos.
        List<String> l13 = new ArrayList<String>();
        l13.add("b");
        l13.add(null);
        l13.add("a");
        Collections.sort(l13, Comparator.nullsFirst(Comparator.<String>naturalOrder()));
        chk(join(l13).equals("null,a,b"));

        // 14: reversed() de un comparador natural.
        chk(Comparator.<Integer>naturalOrder().reversed()
                .compare(Integer.valueOf(1), Integer.valueOf(2)) > 0);

        // ---- subList y vistas ------------------------------------------------------------------

        // 15: subList es una VISTA: escribir en ella escribe en la base.
        List<String> base15 = new ArrayList<String>();
        base15.add("a");
        base15.add("b");
        base15.add("c");
        base15.add("d");
        List<String> sub15 = base15.subList(1, 3);
        sub15.set(0, "B");
        chk(join(base15).equals("a,B,c,d") && join(sub15).equals("B,c"));

        // 16: y clear() sobre la vista borra el rango de la base.
        List<String> base16 = new ArrayList<String>();
        base16.add("a");
        base16.add("b");
        base16.add("c");
        base16.add("d");
        base16.subList(1, 3).clear();
        chk(join(base16).equals("a,d"));

        // 17: keySet() de un HashMap es vista: remove sobre ella saca del mapa.
        HashMap<String, Integer> hm17 = new HashMap<String, Integer>();
        hm17.put("a", 1);
        hm17.put("b", 2);
        hm17.keySet().remove("a");
        chk(hm17.size() == 1 && !hm17.containsKey("a"));

        // 18: subMap de un TreeMap tambien es vista, en los dos sentidos.
        TreeMap<Integer, String> tm18 = new TreeMap<Integer, String>();
        tm18.put(1, "a");
        tm18.put(3, "c");
        tm18.put(5, "e");
        java.util.SortedMap<Integer, String> v18 = tm18.subMap(Integer.valueOf(2),
                Integer.valueOf(6));
        tm18.put(4, "d");
        chk(join(v18.keySet()).equals("3,4,5"));

        // 19: values() es vista y admite duplicados -- no es un conjunto.
        LinkedHashMap<String, Integer> lhm19 = new LinkedHashMap<String, Integer>();
        lhm19.put("a", 7);
        lhm19.put("b", 7);
        chk(lhm19.values().size() == 2 && join(lhm19.values()).equals("7,7"));

        // ---- Collections.unmodifiable* ---------------------------------------------------------

        // 20: el envoltorio se niega a escribir.
        List<String> ro20 = Collections.unmodifiableList(base16);
        boolean uoe20 = false;
        try {
            ro20.add("x");
        } catch (UnsupportedOperationException e) {
            uoe20 = true;
        }
        chk(uoe20);

        // 21: pero es una VISTA, no una copia: un cambio en la base se ve desde el envoltorio.
        base16.add("z");
        chk(join(ro20).equals("a,d,z"));

        // 22: y su equals sigue siendo el de List, o sea por contenido.
        chk(ro20.equals(base16) && ro20.hashCode() == base16.hashCode());

        // 23: el iterador del envoltorio tampoco deja borrar.
        boolean uoe23 = false;
        try {
            Iterator<String> it23 = ro20.iterator();
            it23.next();
            it23.remove();
        } catch (UnsupportedOperationException e) {
            uoe23 = true;
        }
        chk(uoe23);

        // 24: en un mapa de solo lectura, setValue sobre una entrada del entrySet tampoco.
        HashMap<String, Integer> hm24 = new HashMap<String, Integer>();
        hm24.put("a", 1);
        Map<String, Integer> ro24 = Collections.unmodifiableMap(hm24);
        boolean uoe24 = false;
        try {
            ro24.entrySet().iterator().next().setValue(Integer.valueOf(2));
        } catch (UnsupportedOperationException e) {
            uoe24 = true;
        }
        chk(uoe24);

        // 25: la vista de solo lectura hereda los extremos, no los pierde.
        chk(ro20.getFirst().equals("a") && ro20.getLast().equals("z"));

        // ---- EnumSet / EnumMap -----------------------------------------------------------------

        // 26: un EnumSet itera por ORDINAL, no por orden de insercion.
        EnumSet<Color> es26 = EnumSet.of(Color.WHITE, Color.RED, Color.GREEN);
        chk(join(es26).equals("RED,GREEN,WHITE"));

        // 27: range es inclusivo en los dos extremos.
        chk(join(EnumSet.range(Color.GREEN, Color.WHITE)).equals("GREEN,BLUE,WHITE"));

        // 28: complementOf sobre el mismo tipo de enum.
        chk(join(EnumSet.complementOf(EnumSet.of(Color.RED, Color.BLUE))).equals("GREEN,WHITE"));

        // 29: un EnumMap tambien va por ordinal.
        EnumMap<Color, Integer> em29 = new EnumMap<Color, Integer>(Color.class);
        em29.put(Color.BLUE, 3);
        em29.put(Color.RED, 1);
        chk(join(em29.keySet()).equals("RED,BLUE"));

        // 30: una clave null en un EnumMap es NullPointerException, no una entrada mas.
        boolean npe30 = false;
        try {
            em29.put(null, Integer.valueOf(0));
        } catch (NullPointerException e) {
            npe30 = true;
        }
        chk(npe30);

        // ---- BitSet ------------------------------------------------------------------------------

        // 31: toString con el formato de conjunto, que es parte del contrato.
        BitSet bs31 = new BitSet();
        bs31.set(1);
        bs31.set(3);
        bs31.set(64);
        chk(bs31.toString().equals("{1, 3, 64}") && bs31.cardinality() == 3);

        // 32: length es el indice del bit mas alto MAS UNO; size es capacidad, y son cosas
        // distintas -- confundirlas es el error clasico.
        chk(bs31.length() == 65);

        // 33: nextSetBit / nextClearBit, incluyendo el cruce de palabra.
        chk(bs31.nextSetBit(4) == 64 && bs31.nextClearBit(1) == 2 && bs31.nextSetBit(65) == -1);

        // 34: las operaciones logicas mutan el receptor.
        BitSet a34 = new BitSet();
        a34.set(0);
        a34.set(1);
        BitSet b34 = new BitSet();
        b34.set(1);
        b34.set(2);
        BitSet x34 = (BitSet) a34.clone();
        x34.andNot(b34);
        BitSet y34 = (BitSet) a34.clone();
        y34.xor(b34);
        chk(x34.toString().equals("{0}") && y34.toString().equals("{0, 2}"));

        // 35: set(from, to) es semiabierto.
        BitSet bs35 = new BitSet();
        bs35.set(2, 5);
        chk(bs35.toString().equals("{2, 3, 4}"));

        // ---- StringJoiner ------------------------------------------------------------------------

        // 36: sin elementos sale prefijo+sufijo, no la cadena vacia.
        chk(new StringJoiner(",", "[", "]").toString().equals("[]"));

        // 37: setEmptyValue reemplaza TODO -- prefijo y sufijo incluidos -- y solo si esta vacio.
        StringJoiner sj37 = new StringJoiner(",", "[", "]");
        sj37.setEmptyValue("VACIO");
        chk(sj37.toString().equals("VACIO"));

        // 38: en cuanto entra un elemento, el emptyValue deja de aplicar.
        sj37.add("a");
        chk(sj37.toString().equals("[a]"));

        // 39: merge inserta el CONTENIDO del otro como un elemento, sin sus delimitadores de borde.
        StringJoiner sj39 = new StringJoiner(",", "[", "]");
        sj39.add("a");
        StringJoiner otro39 = new StringJoiner("-", "<", ">");
        otro39.add("x");
        otro39.add("y");
        sj39.merge(otro39);
        chk(sj39.toString().equals("[a,x-y]"));

        // 40: length() cuenta prefijo y sufijo.
        chk(sj39.length() == sj39.toString().length());

        // ---- Random --------------------------------------------------------------------------

        // 41: el LCG esta especificado, asi que la secuencia de una semilla fija es un numero
        // concreto y comparable entre implementaciones.
        Random r41 = new Random(42L);
        chk(r41.nextInt() == -1170105035 && r41.nextInt() == 234785527);

        // 42: nextInt(bound) con bound potencia de dos toma otro camino que el general.
        Random r42 = new Random(42L);
        chk(r42.nextInt(100) == 30 && r42.nextInt(64) == 3);

        // 43: nextLong consume DOS next(32), en ese orden.
        chk(new Random(42L).nextLong() == -5025562857975149833L);

        // 44: setSeed reinicia el generador al mismo estado que el constructor.
        Random r44 = new Random(1L);
        r44.nextInt();
        r44.setSeed(42L);
        chk(r44.nextInt() == -1170105035);

        // 45: nextBoolean y nextDouble salen del mismo flujo.
        Random r45 = new Random(42L);
        chk(r45.nextBoolean() && r45.nextDouble() == 0.05466526274716077d);

        // ---- Formatter -------------------------------------------------------------------------

        // 46: ancho, precision, justificacion y relleno con ceros.
        chk(fmt("[%5.2f][%-6s][%05d]", Double.valueOf(3.14159), "ab", Integer.valueOf(42))
                .equals("[ 3.14][ab    ][00042]"));

        // 47: las bases, con y sin el flag de alternativa.
        chk(fmt("%x|%X|%o|%#x", Integer.valueOf(255), Integer.valueOf(255), Integer.valueOf(8),
                Integer.valueOf(255)).equals("ff|FF|10|0xff"));

        // 48: notacion cientifica -- dos digitos de exponente como minimo.
        chk(fmt("%e|%.2e", Double.valueOf(12345.678), Double.valueOf(0.00012))
                .equals("1.234568e+04|1.20e-04"));

        // 49: null se formatea como "null", no explota.
        chk(fmt("%s|%S|%b", null, "ab", null).equals("null|AB|false"));

        // 50: el indice de argumento reusa sin volver a pasarlo.
        chk(fmt("%1$s-%1$s-%2$d", "a", Integer.valueOf(7)).equals("a-a-7"));

        // ---- manejo de null --------------------------------------------------------------------

        // 51: un HashMap acepta clave y valor null; distinguir "no esta" de "vale null" es lo que
        // separa get() de containsKey().
        HashMap<String, String> hm51 = new HashMap<String, String>();
        hm51.put(null, null);
        chk(hm51.containsKey(null) && hm51.get(null) == null && hm51.size() == 1);

        // 52: getOrDefault NO sustituye un null que esta guardado de verdad.
        chk(hm51.getOrDefault(null, "def") == null
                && hm51.getOrDefault("ausente", "def").equals("def"));

        // 53: un TreeMap sin comparador no puede comparar null.
        TreeMap<String, String> tm53 = new TreeMap<String, String>();
        boolean npe53 = false;
        try {
            tm53.put(null, "x");
        } catch (NullPointerException e) {
            npe53 = true;
        }
        chk(npe53);

        // 54: las factorias inmutables rechazan null en el acto.
        boolean npe54 = false;
        try {
            List.of("a", null);
        } catch (NullPointerException e) {
            npe54 = true;
        }
        chk(npe54);

        // 55: Objects es null-safe por diseno.
        chk(Objects.equals(null, null) && !Objects.equals(null, "a") && Objects.hashCode(null) == 0
                && Objects.toString(null, "d").equals("d"));

        // 56: Arrays.asList es una vista de tamano fijo: set anda, add no.
        String[] arr56 = new String[] { "a", "b" };
        List<String> al56 = Arrays.asList(arr56);
        al56.set(0, "z");
        boolean uoe56 = false;
        try {
            al56.add("c");
        } catch (UnsupportedOperationException e) {
            uoe56 = true;
        }
        chk(arr56[0].equals("z") && uoe56);

        // 57: un Set.of con duplicados es un error de programa, no un conjunto de uno.
        boolean iae57 = false;
        try {
            Set.of("a", "a");
        } catch (IllegalArgumentException e) {
            iae57 = true;
        }
        chk(iae57);

        // 58: Collections.emptyList() es inmutable, no una lista vacia cualquiera.
        boolean uoe58 = false;
        try {
            Collections.<String>emptyList().add("x");
        } catch (UnsupportedOperationException e) {
            uoe58 = true;
        }
        chk(uoe58);

        // 59: LinkedList como Deque -- los extremos y el orden que dejan.
        LinkedList<String> ll59 = new LinkedList<String>();
        ll59.addLast("b");
        ll59.addFirst("a");
        ll59.addLast("c");
        chk(join(ll59).equals("a,b,c") && ll59.removeFirst().equals("a")
                && ll59.removeLast().equals("c"));

        // 60: reversed() es una vista viva sobre la lista, no una copia.
        List<String> l60 = new ArrayList<String>();
        l60.add("a");
        l60.add("b");
        List<String> rev60 = l60.reversed();
        l60.add("c");
        chk(join(rev60).equals("c,b,a"));

        // 61: Collections.sort es estable -- los iguales conservan su orden relativo.
        List<String> l61 = new ArrayList<String>();
        l61.add("b1");
        l61.add("a1");
        l61.add("b2");
        l61.add("a2");
        Collections.sort(l61, new Comparator<String>() {
            public int compare(String x, String y) {
                return x.charAt(0) - y.charAt(0);
            }
        });
        chk(join(l61).equals("a1,a2,b1,b2"));

        return fail;
    }

    // El Formatter se usa por objeto y no via String.format, que vive en java.lang: lo que esta
    // prueba audita es java.util.
    static String fmt(String pattern, Object... args) {
        Formatter f = new Formatter();
        f.format(pattern, args);
        String s = f.toString();
        f.close();
        return s;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
