/**
 * Finding #540: un `import static X.*` no importa los tipos miembro de `X` (7.5.4).
 *
 * Un import estatico on-demand importa los **miembros estaticos accesibles** de un tipo, y un tipo
 * miembro anidado y estatico es uno de ellos. El JDK compila esto; nosotros rechazamos `Entry`.
 *
 * La segunda forma esta para delimitar: no es que `java.util.Map$Entry` no se cargue --el campo lo
 * fuerza-- sino que la **resolucion** de un nombre de tipo nunca mira los imports estaticos
 * on-demand. Rechazamos un programa legal, que es el lado caro del error.
 */
import static java.util.Map.*;

public class Finding540 {

    java.util.Map<String, String> fuerzaLaCarga;

    public static String clave(Entry<String, String> e) {
        return e.getKey();
    }

    public static int run() {
        java.util.Map<String, String> m = new java.util.HashMap<String, String>();
        m.put("a", "b");
        for (Entry<String, String> e : m.entrySet()) {
            System.out.println("//estatico " + clave(e));
        }
        return 0;
    }

    public static void main(String[] a) {
        run();
    }
}
