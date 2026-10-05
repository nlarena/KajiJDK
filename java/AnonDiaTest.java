// `new C<>() { }`: el diamante de una clase anonima (§15.9.3).
//
// Sus argumentos se infieren del tipo OBJETIVO, y el hoisting de la anonima corre en `enter`, antes
// de la atribucion, asi que ahi el objetivo solo esta disponible cuando el `new` es el inicializador
// de un local. Lo que el chequeo mira es el `extends` SINTACTICO de la anonima --medido: cambiar el
// supertipo resuelto, crudo o con argumentos, no mueve nada-- y los argumentos se copian del
// objetivo, donde ya estan escritos con nombres que resuelven.
//
// `cuerpo` es la que custodia que no se rompio llamar a un miembro declarado en el cuerpo de la
// anonima, que ya andaba antes de esto.
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnonDiaTest {
    // El caso canonico: objetivo con OTRA base que el diamante.
    static int basico() {
        List<String> l = new ArrayList<>() { };
        l.add("a");
        return l.size();
    }
    // Con inicializador de instancia, que llena la lista en la propia anonima.
    static String conInit() {
        List<String> l = new ArrayList<>() { { this.add("x"); this.add("y"); } };
        return l.get(0) + l.get(1) + l.size();
    }
    // Objetivo con la MISMA base.
    static int mismaBase() {
        ArrayList<String> l = new ArrayList<>() { };
        l.add("z");
        return l.size() * 10;
    }
    // Dos parametros de tipo.
    static String dosParams() {
        Map<String, Integer> m = new HashMap<>() { };
        m.put("k", 5);
        return "" + m.get("k") + m.size();
    }
    // Un miembro declarado EN EL CUERPO, llamado sobre la expresion.
    static int cuerpo() {
        return new ArrayList<String>() { int extra() { return 7; } }.extra();
    }

    public static void main(String[] z) {
        System.out.println(basico());
        System.out.println(conInit());
        System.out.println(mismaBase());
        System.out.println(dosParams());
        System.out.println(cuerpo());
    }
}
