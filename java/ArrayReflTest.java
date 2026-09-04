import java.lang.reflect.Array;

/**
 * `java.lang.reflect.Array`: crear, medir, leer y escribir un arreglo cuyo tipo se conoce recien
 * corriendo.
 *
 * <p>Es la unica puerta que tiene la reflexion a un arreglo, porque un arreglo es el unico objeto
 * que el lenguaje sabe crear solo con sintaxis --`new int[n]`, `a[i]`-- y nunca con una llamada.
 *
 * <p>Lo que esta prueba fija, y es lo que se equivoca facil, son las **conversiones**: cuales
 * ensanchan y cuales no. Un `byte` entra en un `short[]`, un `char` no; un `int` entra en un
 * `long[]`, un `long` no entra en un `int[]`. La regla no es simetrica entre leer y escribir --se
 * puede leer un `byte[]` como `int` y no se puede escribir un `int` en un `byte[]`-- y cada
 * combinacion que no vale tiene que salir como `IllegalArgumentException` y no como un valor
 * truncado en silencio.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25.
 */
public class ArrayReflTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    /** Corre `r` y dice si tiro una {@code IllegalArgumentException}. */
    static boolean tiraIAE(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- crear y medir
        Object ints = Array.newInstance(Integer.TYPE, 3);
        ok("crea un int[]", ints instanceof int[]);
        ok("con el largo pedido", Array.getLength(ints) == 3);
        Object cadenas = Array.newInstance(String.class, 2);
        ok("crea un String[]", cadenas instanceof String[]);
        ok("y lo mide", Array.getLength(cadenas) == 2);

        Object matriz = Array.newInstance(Integer.TYPE, 2, 3);
        ok("crea un int[][]", matriz instanceof int[][]);
        ok("con la primera dimension", Array.getLength(matriz) == 2);
        ok("y la segunda", Array.getLength(Array.get(matriz, 0)) == 3);

        // ---- escribir y leer cada primitivo, sin conversion
        Object zs = Array.newInstance(Boolean.TYPE, 1);
        Array.setBoolean(zs, 0, true);
        ok("boolean va y vuelve", Array.getBoolean(zs, 0));
        ok("y boxeado tambien", Boolean.TRUE.equals(Array.get(zs, 0)));

        Object bs = Array.newInstance(Byte.TYPE, 1);
        Array.setByte(bs, 0, (byte) -5);
        ok("byte va y vuelve", Array.getByte(bs, 0) == (byte) -5);

        Object cs = Array.newInstance(Character.TYPE, 1);
        Array.setChar(cs, 0, 'k');
        ok("char va y vuelve", Array.getChar(cs, 0) == 'k');

        Object ss = Array.newInstance(Short.TYPE, 1);
        Array.setShort(ss, 0, (short) -300);
        ok("short va y vuelve", Array.getShort(ss, 0) == (short) -300);

        Array.setInt(ints, 1, 77);
        ok("int va y vuelve", Array.getInt(ints, 1) == 77);

        Object ls = Array.newInstance(Long.TYPE, 1);
        Array.setLong(ls, 0, 9000000000L);
        ok("long va y vuelve", Array.getLong(ls, 0) == 9000000000L);

        Object fs = Array.newInstance(Float.TYPE, 1);
        Array.setFloat(fs, 0, 1.5f);
        ok("float va y vuelve", Array.getFloat(fs, 0) == 1.5f);

        Object ds = Array.newInstance(Double.TYPE, 1);
        Array.setDouble(ds, 0, 2.25);
        ok("double va y vuelve", Array.getDouble(ds, 0) == 2.25);

        // ---- referencias
        Array.set(cadenas, 0, "kaji");
        ok("una referencia va y vuelve", "kaji".equals(Array.get(cadenas, 0)));
        Array.set(cadenas, 1, null);
        ok("un null es un elemento legal", Array.get(cadenas, 1) == null);

        // ---- leer ensanchando: byte y short se leen como int, int como long, float como double
        ok("un byte[] se lee como int", Array.getInt(bs, 0) == -5);
        ok("y como long", Array.getLong(bs, 0) == -5L);
        ok("y como double", Array.getDouble(bs, 0) == -5.0);
        ok("un char[] se lee como int", Array.getInt(cs, 0) == 'k');
        ok("un short[] se lee como int", Array.getInt(ss, 0) == -300);
        ok("un int[] se lee como long", Array.getLong(ints, 1) == 77L);
        ok("y como float", Array.getFloat(ints, 1) == 77.0f);
        ok("un float[] se lee como double", Array.getDouble(fs, 0) == 1.5);

        // ---- leer angostando: no
        final Object bsF = bs;
        final Object intsF = ints;
        final Object lsF = ls;
        final Object dsF = ds;
        final Object csF = cs;
        ok("un int[] no se lee como byte", tiraIAE(() -> Array.getByte(intsF, 1)));
        ok("un long[] no se lee como int", tiraIAE(() -> Array.getInt(lsF, 0)));
        ok("un double[] no se lee como float", tiraIAE(() -> Array.getFloat(dsF, 0)));
        ok("un byte[] no se lee como char", tiraIAE(() -> Array.getChar(bsF, 0)));
        ok("un char[] no se lee como short", tiraIAE(() -> Array.getShort(csF, 0)));

        // ---- escribir ensanchando
        Array.setByte(ss, 0, (byte) 7);
        ok("un byte entra en un short[]", Array.getShort(ss, 0) == (short) 7);
        Array.setInt(ls, 0, 42);
        ok("un int entra en un long[]", Array.getLong(ls, 0) == 42L);
        Array.setLong(ds, 0, 8L);
        ok("un long entra en un double[]", Array.getDouble(ds, 0) == 8.0);
        Array.setChar(ints, 2, 'z');
        ok("un char entra en un int[]", Array.getInt(ints, 2) == 'z');

        // ---- escribir angostando: no
        final Object ssF = ss;
        final Object fsF = fs;
        ok("un int no entra en un byte[]", tiraIAE(() -> Array.setInt(bsF, 0, 1)));
        ok("un long no entra en un int[]", tiraIAE(() -> Array.setLong(intsF, 0, 1L)));
        ok("un char no entra en un short[]", tiraIAE(() -> Array.setChar(ssF, 0, 'a')));
        ok("un double no entra en un float[]", tiraIAE(() -> Array.setDouble(fsF, 0, 1.0)));

        // ---- set(Object) desenvuelve y ensancha
        Array.set(ls, 0, Integer.valueOf(11));
        ok("set con un Integer en un long[]", Array.getLong(ls, 0) == 11L);
        final Object lsG = ls;
        ok("set con un tipo que no entra tira",
                tiraIAE(() -> Array.set(lsG, 0, Double.valueOf(1.0))));
        ok("set con null en un primitivo tira", tiraIAE(() -> Array.set(lsG, 0, null)));
        final Object cadenasF = cadenas;
        ok("set con un tipo ajeno en un String[] tira",
                tiraIAE(() -> Array.set(cadenasF, 0, Integer.valueOf(1))));

        // ---- lo que no es un arreglo, y los indices
        final Object noEsArreglo = "no soy un arreglo";
        ok("getLength de algo que no es arreglo tira",
                tiraIAE(() -> Array.getLength(noEsArreglo)));
        ok("get de algo que no es arreglo tira", tiraIAE(() -> Array.get(noEsArreglo, 0)));

        boolean tiroRango = false;
        try {
            Array.getInt(ints, 3);
        } catch (ArrayIndexOutOfBoundsException e) {
            tiroRango = true;
        }
        ok("un indice fuera de rango tira", tiroRango);

        boolean tiroRangoNeg = false;
        try {
            Array.set(cadenas, -1, "x");
        } catch (ArrayIndexOutOfBoundsException e) {
            tiroRangoNeg = true;
        }
        ok("un indice negativo tambien", tiroRangoNeg);

        boolean tiroNulo = false;
        try {
            Array.getLength(null);
        } catch (NullPointerException e) {
            tiroNulo = true;
        }
        ok("getLength(null) tira NullPointerException", tiroNulo);

        boolean tiroNegativo = false;
        try {
            Array.newInstance(Integer.TYPE, -1);
        } catch (NegativeArraySizeException e) {
            tiroNegativo = true;
        }
        ok("un largo negativo tira", tiroNegativo);

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("ArrayReflTest " + ArrayReflTest.run());
    }
}
