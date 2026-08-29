// Por import y nombre simple: una llamada estatica calificada
// (`java.lang.reflect.Modifier.isPublic(...)`) no resuelve, de la misma familia que el
// finding #210.
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Exercises Method, and the half of java.lang.Class that hands them out. Every
 * method returns the number of things that came out wrong, so 0 is a pass.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts.
 *
 * <p>The subject classes at the bottom are written to have NO bridge and NO synthetic methods --
 * no generics, no covariant overrides, no inner classes, no lambdas. That is not tidiness: a
 * bridge is a real entry in the class file and {@code getDeclaredMethods} reports it, so a
 * subject that produced one would make the counts depend on which compiler built the probe rather
 * than on whether reflection works.
 */
public class MethodTest {

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

    static Method find(Class<?> owner, String name) {
        Method[] all = owner.getDeclaredMethods();
        int i = 0;
        while (i < all.length) {
            if (all[i].getName().equals(name)) {
                return all[i];
            }
            i = i + 1;
        }
        return null;
    }

    /** What the VM reads out of a class file, and what it deliberately leaves out. */
    public static int descubrir() {
        int bad = 0;
        Method[] declared = MethodSubject.class.getDeclaredMethods();
        // Seventeen methods, and neither the constructor nor the static initializer among them
        // -- which is the part worth asserting, since both ARE entries in the class file.
        if (declared.length != 18) {
            bad = bad + 1;
        }
        int i = 0;
        while (i < declared.length) {
            String name = declared[i].getName();
            if (name.equals("<init>") || name.equals("<clinit>")) {
                bad = bad + 1;
            }
            if (declared[i].getDeclaringClass() != MethodSubject.class) {
                bad = bad + 1;
            }
            i = i + 1;
        }

        Method doble = MethodTest.find(MethodSubject.class, "doble");
        if (doble == null) {
            return bad + 100;
        }
        bad = bad + MethodTest.eq(doble.getName(), "doble");
        if (doble.getReturnType() != int.class) {
            bad = bad + 1;
        }
        if (doble.getParameterCount() != 1 || doble.getParameterTypes().length != 1) {
            bad = bad + 1;
        }
        if (doble.getParameterTypes()[0] != int.class) {
            bad = bad + 1;
        }
        if (doble.getExceptionTypes().length != 0) {
            bad = bad + 1;
        }
        if (!Modifier.isPublic(doble.getModifiers())
                || Modifier.isStatic(doble.getModifiers())) {
            bad = bad + 1;
        }

        // A static method with several parameters of mixed width: long and double take two slots
        // each, which is where a descriptor rebuilt by hand goes wrong if it counts arguments
        // instead of reading types.
        Method mezcla = MethodTest.find(MethodSubject.class, "mezcla");
        if (mezcla == null) {
            return bad + 100;
        }
        if (!Modifier.isStatic(mezcla.getModifiers())) {
            bad = bad + 1;
        }
        Class<?>[] types = mezcla.getParameterTypes();
        if (types.length != 5) {
            bad = bad + 1;
        } else {
            if (types[0] != int.class || types[1] != long.class || types[2] != double.class
                    || types[3] != String.class || types[4] != int[].class) {
                bad = bad + 1;
            }
        }
        if (mezcla.getReturnType() != String.class) {
            bad = bad + 1;
        }

        // `throws` is an attribute, not part of the descriptor, and it has to be read separately.
        Method falla = MethodTest.find(MethodSubject.class, "falla");
        if (falla == null) {
            return bad + 100;
        }
        Class<?>[] throwsList = falla.getExceptionTypes();
        if (throwsList.length != 2) {
            bad = bad + 1;
        } else {
            if (throwsList[0] != IOException.class
                    || throwsList[1] != InterruptedException.class) {
                bad = bad + 1;
            }
        }

        // A void method reports the `void` mirror, not null.
        Method nada = MethodTest.find(MethodSubject.class, "nada");
        if (nada == null || nada.getReturnType() != void.class) {
            bad = bad + 1;
        }

        // An array and a primitive declare no methods at all.
        if (int[].class.getDeclaredMethods().length != 0
                || int.class.getDeclaredMethods().length != 0) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Looking one up, by name and by exact parameter types. */
    public static int buscar() {
        int bad = 0;
        try {
            Method doble =
                    MethodSubject.class.getDeclaredMethod("doble", int.class);
            bad = bad + MethodTest.eq(doble.getName(), "doble");
            Method nada = MethodSubject.class.getDeclaredMethod("nada");
            if (nada.getParameterCount() != 0) {
                bad = bad + 1;
            }
            // The two overloads differ only in their parameter types, so this is the check that
            // the lookup uses them and not just the name.
            Method uno =
                    MethodSubject.class.getDeclaredMethod("elegir", int.class);
            Method otro =
                    MethodSubject.class.getDeclaredMethod("elegir", String.class);
            if (uno.getReturnType() != int.class || otro.getReturnType() != String.class) {
                bad = bad + 1;
            }
            if (uno.equals(otro)) {
                bad = bad + 1;
            }
        } catch (NoSuchMethodException ex) {
            bad = bad + 1;
        }
        // Exact and not assignable: asking for (Object) must not find (String).
        bad = bad + MethodTest.expectMissing(1);
        bad = bad + MethodTest.expectMissing(2);
        // A private method is declared but not public, so getMethod must not find it.
        bad = bad + MethodTest.expectMissing(3);

        try {
            // getMethod reaches an inherited public method...
            Method heredado =
                    MethodSubject.class.getMethod("delBase");
            if (heredado.getDeclaringClass() != MethodBase.class) {
                bad = bad + 1;
            }
            // ...and a default method of an interface, which is neither declared here nor in a
            // superclass.
            Method porDefecto = MethodSubject.class.getMethod("saludo");
            if (porDefecto.getDeclaringClass() != MethodIface.class) {
                bad = bad + 1;
            }
            // An override appears ONCE, from the class that overrides it.
            Method pisado = MethodSubject.class.getMethod("pisado");
            if (pisado.getDeclaringClass() != MethodSubject.class) {
                bad = bad + 1;
            }
        } catch (NoSuchMethodException ex) {
            bad = bad + 1;
        }

        Method[] publicos = MethodSubject.class.getMethods();
        int pisados = 0;
        int i = 0;
        while (i < publicos.length) {
            if (publicos[i].getName().equals("pisado")) {
                pisados = pisados + 1;
            }
            if (publicos[i].getName().equals("privado")) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        if (pisados != 1) {
            bad = bad + 1;
        }
        // Object's public methods come along, so the list is never just the class's own.
        if (publicos.length <= MethodSubject.class.getDeclaredMethods().length) {
            bad = bad + 1;
        }
        return bad;
    }

    static int expectMissing(int which) {
        try {
            MethodTest.missingCase(which);
        } catch (NoSuchMethodException ex) {
            return 0;
        }
        return 1;
    }

    static Method missingCase(int which) throws NoSuchMethodException {
        if (which == 1) {
            return MethodSubject.class.getDeclaredMethod("elegir", Object.class);
        }
        if (which == 2) {
            return MethodSubject.class.getDeclaredMethod("noExiste");
        }
        return MethodSubject.class.getMethod("privado");
    }

    /** The flags and the printed forms. */
    public static int propiedades() {
        int bad = 0;
        Method variable = MethodTest.find(MethodSubject.class, "variable");
        if (variable == null) {
            return bad + 100;
        }
        // The parameter is an array either way; only ACC_VARARGS tells the two spellings apart.
        if (!variable.isVarArgs()) {
            bad = bad + 1;
        }
        if (variable.getParameterTypes()[0] != int[].class) {
            bad = bad + 1;
        }
        Method doble = MethodTest.find(MethodSubject.class, "doble");
        if (doble.isVarArgs() || doble.isSynthetic() || doble.isBridge()) {
            bad = bad + 1;
        }
        if (doble.isDefault()) {
            bad = bad + 1;
        }
        Method porDefecto = MethodTest.find(MethodIface.class, "saludo");
        if (porDefecto == null || !porDefecto.isDefault()) {
            bad = bad + 1;
        }
        Method abstracto = MethodTest.find(MethodIface.class, "requerido");
        if (abstracto == null || abstracto.isDefault()) {
            bad = bad + 1;
        }

        // Two Method objects for the same method are different objects and equal values.
        Method again = MethodTest.find(MethodSubject.class, "doble");
        if (again == doble) {
            bad = bad + 1;
        }
        if (!again.equals(doble) || again.hashCode() != doble.hashCode()) {
            bad = bad + 1;
        }
        if (doble.equals("doble") || doble.equals(null)) {
            bad = bad + 1;
        }

        // The generic model answers the erased types, which is what a method with no Signature
        // attribute reports.
        if (doble.getGenericReturnType() != int.class) {
            bad = bad + 1;
        }
        if (doble.getGenericParameterTypes().length != 1
                || doble.getGenericParameterTypes()[0] != int.class) {
            bad = bad + 1;
        }
        Method falla = MethodTest.find(MethodSubject.class, "falla");
        if (falla.getGenericExceptionTypes().length != 2) {
            bad = bad + 1;
        }
        if (doble.getTypeParameters().length != 0) {
            bad = bad + 1;
        }

        bad = bad + MethodTest.eq(doble.toString(), "public int MethodSubject.doble(int)");
        bad = bad + MethodTest.eq(doble.toGenericString(),
                "public int MethodSubject.doble(int)");
        Method nada = MethodTest.find(MethodSubject.class, "nada");
        bad = bad + MethodTest.eq(nada.toString(), "public void MethodSubject.nada()");
        bad = bad + MethodTest.eq(falla.toString(),
                "public void MethodSubject.falla() throws java.io.IOException,"
                        + "java.lang.InterruptedException");
        return bad;
    }

    /** The point of the whole class: calling it. */
    public static int llamar() {
        int bad = 0;
        MethodSubject subject = new MethodSubject();
        subject.acumulado = 10;
        try {
            // An instance method, an int in and an int out: both cross the boxing frontier.
            Method doble =
                    MethodSubject.class.getDeclaredMethod("doble", int.class);
            Object result = doble.invoke(subject, Integer.valueOf(21));
            if (!(result instanceof Integer)) {
                bad = bad + 1;
            } else {
                if (((Integer) result).intValue() != 42) {
                    bad = bad + 1;
                }
            }
            // It really ran on THAT receiver: the method reads a field.
            Method leer =
                    MethodSubject.class.getDeclaredMethod("leerAcumulado");
            if (((Integer) leer.invoke(subject)).intValue() != 10) {
                bad = bad + 1;
            }
            // A void method answers null, and its side effect happened.
            Method sumar =
                    MethodSubject.class.getDeclaredMethod("sumar", int.class);
            if (sumar.invoke(subject, Integer.valueOf(5)) != null) {
                bad = bad + 1;
            }
            if (subject.acumulado != 15) {
                bad = bad + 1;
            }
            // A static method: no receiver, and null is the right thing to pass.
            Method mezcla = MethodSubject.class.getDeclaredMethod(
                    "mezcla", int.class, long.class, double.class, String.class, int[].class);
            int[] extra = new int[2];
            extra[0] = 7;
            extra[1] = 8;
            Object mixed = mezcla.invoke(null, Integer.valueOf(1), Long.valueOf(2L),
                    Double.valueOf(0.5d), "x", extra);
            bad = bad + MethodTest.eq((String) mixed, "1|2|0.5|x|2");
            // Every primitive return width, boxed on the way out.
            Method largo = MethodSubject.class.getDeclaredMethod("largo");
            if (((Long) largo.invoke(null)).longValue() != 9223372036854775807L) {
                bad = bad + 1;
            }
            Method flotante =
                    MethodSubject.class.getDeclaredMethod("flotante");
            if (((Double) flotante.invoke(null)).doubleValue() != 0.25d) {
                bad = bad + 1;
            }
            Method verdad = MethodSubject.class.getDeclaredMethod("verdad");
            if (!((Boolean) verdad.invoke(null)).booleanValue()) {
                bad = bad + 1;
            }
            Method letra = MethodSubject.class.getDeclaredMethod("letra");
            if (((Character) letra.invoke(null)).charValue() != 'k') {
                bad = bad + 1;
            }
            // A reference return passes through untouched.
            Method texto = MethodSubject.class.getDeclaredMethod("texto");
            bad = bad + MethodTest.eq((String) texto.invoke(null), "hola");
            // A null argument for a reference parameter is fine.
            Method opcional =
                    MethodSubject.class.getDeclaredMethod("elegir", String.class);
            bad = bad + MethodTest.eq((String) opcional.invoke(subject, new Object[] {null}),
                    "nada");
            // An inherited method, invoked through the subclass's mirror.
            Method delBase = MethodSubject.class.getMethod("delBase");
            if (((Integer) delBase.invoke(subject)).intValue() != 99) {
                bad = bad + 1;
            }
            // A private method, once the check is suppressed -- which it always is here.
            Method privado =
                    MethodSubject.class.getDeclaredMethod("privado");
            privado.setAccessible(true);
            bad = bad + MethodTest.eq((String) privado.invoke(subject), "secreto");
        } catch (NoSuchMethodException ex) {
            bad = bad + 1;
        } catch (IllegalAccessException ex) {
            bad = bad + 1;
        } catch (InvocationTargetException ex) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * What happens when the invoked method throws.
     *
     * <p>The exception arrives WRAPPED in {@code InvocationTargetException}, and the wrapping is
     * the whole point: without it a caller cannot tell "the method I called threw" from "invoke
     * could not call it" -- an argument that does not fit, a method that is not there. The
     * wrapper puts that boundary in the type.
     */
    public static int propagar() {
        int bad = 0;
        MethodSubject subject = new MethodSubject();
        try {
            Method revienta = MethodSubject.class.getDeclaredMethod("revienta");
            revienta.invoke(subject);
            bad = bad + 1; // it had to throw
        } catch (NoSuchMethodException ex) {
            bad = bad + 1;
        } catch (IllegalAccessException ex) {
            bad = bad + 1;
        } catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            if (!(target instanceof IllegalStateException)) {
                bad = bad + 1;
            }
            if (ex.getCause() != target) {
                bad = bad + 1;
            }
        }
        // An argument of the wrong type never reaches the method, so it is NOT wrapped.
        bad = bad + MethodTest.expectIllegalArgument();
        return bad;
    }

    static int expectIllegalArgument() {
        try {
            Method doble = MethodSubject.class.getDeclaredMethod("doble", int.class);
            doble.invoke(new MethodSubject(), "no soy un int");
        } catch (IllegalArgumentException ex) {
            return 0;
        } catch (Exception ex) {
            return 1;
        }
        return 1;
    }

    public static int todo() {
        return MethodTest.descubrir() + MethodTest.buscar() + MethodTest.propiedades()
                + MethodTest.llamar() + MethodTest.propagar();
    }

    public static void main(String[] args) {
        System.out.println("descubrir    " + MethodTest.descubrir());
        System.out.println("buscar       " + MethodTest.buscar());
        System.out.println("propiedades  " + MethodTest.propiedades());
        System.out.println("llamar       " + MethodTest.llamar());
        System.out.println("propagar     " + MethodTest.propagar());
        System.out.println("TOTAL        " + MethodTest.todo());
    }
}

// The subjects. Prefixed on purpose: `java/` is one flat directory shared by every fixture.
interface MethodIface {

    String requerido();

    default String saludo() {
        return "hola";
    }
}

class MethodBase {

    public int delBase() {
        return 99;
    }

    public String pisado() {
        return "base";
    }
}

class MethodSubject extends MethodBase implements MethodIface {

    public int acumulado;

    public int doble(int n) {
        return n * 2;
    }

    public int leerAcumulado() {
        return this.acumulado;
    }

    public void sumar(int n) {
        this.acumulado = this.acumulado + n;
    }

    public void nada() {
    }

    public void falla() throws IOException, InterruptedException {
    }

    public int elegir(int n) {
        return n;
    }

    public String elegir(String s) {
        if (s == null) {
            return "nada";
        }
        return s;
    }

    public int variable(int... values) {
        return values.length;
    }

    public static String mezcla(int a, long b, double c, String d, int[] e) {
        return a + "|" + b + "|" + c + "|" + d + "|" + e.length;
    }

    public static long largo() {
        return 9223372036854775807L;
    }

    public static double flotante() {
        return 0.25d;
    }

    public static boolean verdad() {
        return true;
    }

    public static char letra() {
        return 'k';
    }

    public static String texto() {
        return "hola";
    }

    public String requerido() {
        return "requerido";
    }

    public String pisado() {
        return "subject";
    }

    private String privado() {
        return "secreto";
    }

    public void revienta() {
        throw new IllegalStateException("a proposito");
    }
}
