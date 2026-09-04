import java.util.HashMap;
import java.util.Map;

/**
 * El cast de una referencia a un primitivo, que es un desempaquetado (§5.5).
 *
 * <p>Existe por un bug que no daba error de compilacion: `(int) o` emitia `aload; istore` --la
 * referencia guardada tal cual en un local `int`--, sin `checkcast` ni `intValue()`. Nuestra VM
 * entraba en panic y la JVM real rechazaba la clase al verificarla. Le pegaba a cualquier
 * `(int) mapa.get(k)`, que es de lo mas comun.
 *
 * <p>Lo que se comprueba no es solo que ande, sino las **dos formas** que el javac real distingue: si
 * el operando ya es el wrapper se desempaqueta directo, y si es `Object` o `Number` va primero un
 * `checkcast` al wrapper **del destino**. Por eso hay casos con destino mas ancho que el operando
 * (`(long) unInteger`) y casos con operando opaco (`(long) unObject`, que exige un `Long`).
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25.
 */
public class CastUnboxTest {

    static int fallas = 0;

    static void chequear(String que, long esperado, long dio) {
        if (esperado != dio) {
            System.out.println("FALLA " + que + ": esperaba " + esperado + " y dio " + dio);
            fallas = fallas + 1;
        }
    }

    static Object comoObject(Object o) { return o; }
    static Number comoNumber(Number n) { return n; }

    public static int run() {
        fallas = 0;

        // Operando opaco: `checkcast` al wrapper del destino y despues desempaquetar.
        chequear("(int) Object", 42L, (long) (int) CastUnboxTest.comoObject(Integer.valueOf(42)));
        chequear("(long) Object", 7L, (long) CastUnboxTest.comoObject(Long.valueOf(7L)));
        chequear("(char) Object", 65L, (long) (char) CastUnboxTest.comoObject(Character.valueOf('A')));
        chequear("(byte) Object", -3L, (long) (byte) CastUnboxTest.comoObject(Byte.valueOf((byte) -3)));
        chequear("(short) Object", 300L, (long) (short) CastUnboxTest.comoObject(Short.valueOf((short) 300)));

        // Operando `Number`: tampoco se conoce el wrapper, mismo camino.
        chequear("(int) Number", 5L, (long) (int) CastUnboxTest.comoNumber(Integer.valueOf(5)));
        chequear("(long) Number", 9L, (long) CastUnboxTest.comoNumber(Long.valueOf(9L)));

        // Operando que YA es el wrapper: desempaquetado directo, sin checkcast.
        Integer i = Integer.valueOf(11);
        chequear("(int) Integer", 11L, (long) (int) i);
        // Y con destino mas ancho: desempaqueta a `int` y ensancha.
        chequear("(long) Integer", 11L, (long) i);

        // El caso que motivo todo: sacar un primitivo de una coleccion.
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("n", Integer.valueOf(123));
        chequear("(int) mapa.get", 123L, (long) (int) m.get("n"));

        // Un cast imposible tiene que tirar, no leer basura.
        boolean tiro = false;
        try {
            long malo = (long) CastUnboxTest.comoObject(Integer.valueOf(1));
            chequear("no deberia llegar", 0L, malo);
        } catch (ClassCastException e) {
            tiro = true;
        }
        if (!tiro) {
            System.out.println("FALLA (long) de un Integer no tiro ClassCastException");
            fallas = fallas + 1;
        }

        // `boolean` no ensancha a nada: su unico origen es `Boolean`.
        Object b = Boolean.TRUE;
        if (!((boolean) b)) {
            System.out.println("FALLA (boolean) Object");
            fallas = fallas + 1;
        }

        // Los de coma flotante.
        chequear("(double) Object", 2L, (long) (double) CastUnboxTest.comoObject(Double.valueOf(2.5d)));
        chequear("(float) Object", 1L, (long) (float) CastUnboxTest.comoObject(Float.valueOf(1.5f)));

        if (fallas == 0) {
            return -1;
        }
        return fallas;
    }

    public static void main(String[] a) {
        System.out.println("CastUnboxTest " + CastUnboxTest.run());
    }
}
