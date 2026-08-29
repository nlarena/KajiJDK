// Por import y nombre simple: una llamada estatica calificada no resuelve (finding #274).
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;

/**
 * Exercises the half of java.lang.Class that reads class file ATTRIBUTES: nesting, the nest,
 * sealing and records. Every method returns the number of things that came out wrong, so 0 is a
 * pass.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts.
 *
 * <p>What is being tested is the reconstruction of something the class file loses. {@code Outer}
 * and {@code Outer$Inner} are two separate files whose names happen to share a prefix, and
 * {@code $} is a legal character in an identifier -- so none of these questions can be answered by
 * looking at a name. They are answered by {@code InnerClasses}, {@code EnclosingMethod},
 * {@code NestHost}, {@code NestMembers}, {@code PermittedSubclasses} and {@code Record}.
 */
public class ClassAttrTest {

    static int eq(String got, String want) {
        if (got == null) {
            if (want == null) {
                return 0;
            }
            return 1;
        }
        if (got.equals(want)) {
            return 0;
        }
        return 1;
    }

    // Los sujetos, adentro de esta clase a proposito: el anidamiento es lo que se mide.
    static class Anidada {
        int x;

        static class MasAdentro {
            int y;
        }
    }

    class Interna {
        int y;
    }

    record Punto(int x, int y) {
    }

    // Una local y una anonima, devueltas para poder preguntarles.
    static Class<?> laLocal() {
        class Local {
            int z;
        }
        Local l = new Local();
        return l.getClass();
    }

    static Class<?> laAnonima() {
        Runnable r = new Runnable() {
            public void run() {
            }
        };
        return r.getClass();
    }

    /** Member, local and anonymous, which the class file tells apart without saying so. */
    public static int anidamiento() {
        int bad = 0;
        Class<?> anidada = Anidada.class;
        if (!anidada.isMemberClass() || anidada.isLocalClass() || anidada.isAnonymousClass()) {
            bad = bad + 1;
        }
        if (anidada.getDeclaringClass() != ClassAttrTest.class) {
            bad = bad + 1;
        }
        if (anidada.getEnclosingClass() != ClassAttrTest.class) {
            bad = bad + 1;
        }
        bad = bad + ClassAttrTest.eq(anidada.getSimpleName(), "Anidada");
        bad = bad + ClassAttrTest.eq(anidada.getName(), "ClassAttrTest$Anidada");

        // Dos niveles: la de adentro declara a la de mas adentro y no a la de mas afuera.
        Class<?> masAdentro = Anidada.MasAdentro.class;
        if (masAdentro.getDeclaringClass() != Anidada.class) {
            bad = bad + 1;
        }
        bad = bad + ClassAttrTest.eq(masAdentro.getSimpleName(), "MasAdentro");

        // Una local: declarada adentro de un METODO, asi que tiene clase que la encierra y no
        // clase que la declara. Esa es toda la diferencia entre los dos metodos.
        Class<?> local = ClassAttrTest.laLocal();
        if (!local.isLocalClass() || local.isMemberClass() || local.isAnonymousClass()) {
            bad = bad + 1;
        }
        if (local.getDeclaringClass() != null) {
            bad = bad + 1;
        }
        if (local.getEnclosingClass() != ClassAttrTest.class) {
            bad = bad + 1;
        }
        bad = bad + ClassAttrTest.eq(local.getSimpleName(), "Local");
        Method enclosing = local.getEnclosingMethod();
        if (enclosing == null) {
            bad = bad + 1;
        } else {
            bad = bad + ClassAttrTest.eq(enclosing.getName(), "laLocal");
        }
        if (local.getEnclosingConstructor() != null) {
            bad = bad + 1;
        }

        // Una anonima: sin nombre simple, y eso es un dato, no un hueco.
        Class<?> anonima = ClassAttrTest.laAnonima();
        if (!anonima.isAnonymousClass() || anonima.isMemberClass() || anonima.isLocalClass()) {
            bad = bad + 1;
        }
        bad = bad + ClassAttrTest.eq(anonima.getSimpleName(), "");
        if (anonima.getDeclaringClass() != null) {
            bad = bad + 1;
        }
        if (anonima.getEnclosingClass() != ClassAttrTest.class) {
            bad = bad + 1;
        }
        if (anonima.getCanonicalName() != null) {
            bad = bad + 1;
        }

        // Una de nivel superior no esta anidada en nada.
        if (ClassAttrTest.class.getDeclaringClass() != null
                || ClassAttrTest.class.getEnclosingClass() != null) {
            bad = bad + 1;
        }
        if (ClassAttrTest.class.isMemberClass() || ClassAttrTest.class.isLocalClass()
                || ClassAttrTest.class.isAnonymousClass()) {
            bad = bad + 1;
        }
        if (String.class.getDeclaringClass() != null || int.class.isMemberClass()) {
            bad = bad + 1;
        }

        // getDeclaredClasses trae las que ESTA declara, no las que menciona.
        Class<?>[] declaradas = ClassAttrTest.class.getDeclaredClasses();
        int vistas = 0;
        int i = 0;
        while (i < declaradas.length) {
            String simple = declaradas[i].getSimpleName();
            if (simple.equals("Anidada") || simple.equals("Interna") || simple.equals("Punto")) {
                vistas = vistas + 1;
            }
            // Ni la local ni la anonima son miembros, asi que no estan.
            if (simple.equals("Local") || simple.length() == 0) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        if (vistas != 3) {
            bad = bad + 1;
        }
        if (Anidada.class.getDeclaredClasses().length != 1) {
            bad = bad + 1;
        }
        // Una clase sin anidadas declara cero. `String` NO sirve de ejemplo: el JDK le mete
        // un comparador anidado, y la nuestra no -- una diferencia de implementacion legitima
        // que haria fallar la prueba en un lado y no en el otro.
        if (Circulo.class.getDeclaredClasses().length != 0) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The nest: who shares private access with whom. */
    public static int nido() {
        int bad = 0;
        // Todas las de este archivo comparten nido, y el anfitrion es la de afuera.
        if (Anidada.class.getNestHost() != ClassAttrTest.class) {
            bad = bad + 1;
        }
        if (Punto.class.getNestHost() != ClassAttrTest.class) {
            bad = bad + 1;
        }
        if (ClassAttrTest.class.getNestHost() != ClassAttrTest.class) {
            bad = bad + 1;
        }
        // Una clase suelta es su propio nido, de un solo miembro.
        if (String.class.getNestHost() != String.class) {
            bad = bad + 1;
        }
        if (!Anidada.class.isNestmateOf(Punto.class)) {
            bad = bad + 1;
        }
        if (!Anidada.class.isNestmateOf(ClassAttrTest.class)) {
            bad = bad + 1;
        }
        if (Anidada.class.isNestmateOf(String.class)) {
            bad = bad + 1;
        }
        if (!String.class.isNestmateOf(String.class)) {
            bad = bad + 1;
        }
        // Los miembros del nido se piden al anfitrion y salen todos, el anfitrion incluido.
        Class<?>[] miembros = ClassAttrTest.class.getNestMembers();
        if (miembros.length < 4) {
            bad = bad + 1;
        }
        boolean estaElAnfitrion = false;
        int i = 0;
        while (i < miembros.length) {
            if (miembros[i] == ClassAttrTest.class) {
                estaElAnfitrion = true;
            }
            i = i + 1;
        }
        if (!estaElAnfitrion) {
            bad = bad + 1;
        }
        // Preguntarle a un miembro da la misma lista que preguntarle al anfitrion.
        if (Anidada.class.getNestMembers().length != miembros.length) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Sealing, where null and empty are different answers. */
    public static int sellado() {
        int bad = 0;
        if (!Figura.class.isSealed()) {
            bad = bad + 1;
        }
        if (ClassAttrTest.class.isSealed() || String.class.isSealed()) {
            bad = bad + 1;
        }
        if (int.class.isSealed() || String[].class.isSealed()) {
            bad = bad + 1;
        }
        Class<?>[] permitidas = Figura.class.getPermittedSubclasses();
        if (permitidas == null) {
            bad = bad + 1;
        } else {
            if (permitidas.length != 2) {
                bad = bad + 1;
            } else {
                boolean circulo = permitidas[0] == Circulo.class
                        || permitidas[1] == Circulo.class;
                boolean cuadrado = permitidas[0] == Cuadrado.class
                        || permitidas[1] == Cuadrado.class;
                if (!circulo || !cuadrado) {
                    bad = bad + 1;
                }
            }
        }
        // Null y vacio son respuestas distintas: null es "cualquiera puede".
        if (ClassAttrTest.class.getPermittedSubclasses() != null) {
            bad = bad + 1;
        }
        if (Circulo.class.getPermittedSubclasses() != null) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Records: the components, in the order the canonical constructor takes them. */
    public static int records() {
        int bad = 0;
        if (!Punto.class.isRecord()) {
            bad = bad + 1;
        }
        if (ClassAttrTest.class.isRecord() || String.class.isRecord()) {
            bad = bad + 1;
        }
        RecordComponent[] partes = Punto.class.getRecordComponents();
        if (partes == null) {
            bad = bad + 1;
        } else {
            if (partes.length != 2) {
                bad = bad + 1;
            } else {
                bad = bad + ClassAttrTest.eq(partes[0].getName(), "x");
                bad = bad + ClassAttrTest.eq(partes[1].getName(), "y");
                if (partes[0].getType() != int.class || partes[1].getType() != int.class) {
                    bad = bad + 1;
                }
                if (partes[0].getDeclaringRecord() != Punto.class) {
                    bad = bad + 1;
                }
            }
        }
        // Lo que no es un record contesta null, no un array vacio.
        if (ClassAttrTest.class.getRecordComponents() != null) {
            bad = bad + 1;
        }
        // El constructor canonico toma los componentes en ese orden.
        try {
            Constructor<?> canonico = Punto.class.getDeclaredConstructor(int.class, int.class);
            Object p = canonico.newInstance(Integer.valueOf(3), Integer.valueOf(4));
            Method x = Punto.class.getDeclaredMethod("x");
            if (((Integer) x.invoke(p)).intValue() != 3) {
                bad = bad + 1;
            }
        } catch (Exception ex) {
            bad = bad + 1;
        }
        return bad;
    }

    public static int todo() {
        return ClassAttrTest.anidamiento() + ClassAttrTest.nido() + ClassAttrTest.sellado()
                + ClassAttrTest.records();
    }

    public static void main(String[] args) {
        System.out.println("anidamiento  " + ClassAttrTest.anidamiento());
        System.out.println("nido         " + ClassAttrTest.nido());
        System.out.println("sellado      " + ClassAttrTest.sellado());
        System.out.println("records      " + ClassAttrTest.records());
        System.out.println("TOTAL        " + ClassAttrTest.todo());
    }
}

// Sellada, y de nivel superior para que el nido de arriba no la incluya.
sealed interface Figura permits Circulo, Cuadrado {
}

final class Circulo implements Figura {
}

final class Cuadrado implements Figura {
}
