// Por import y nombre simple: una llamada estatica calificada no resuelve (finding #274).
import java.lang.reflect.Modifier;

/**
 * Exercises java.lang.ClassLoader. Every method returns the number of things that came out wrong,
 * so 0 is a pass.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts.
 *
 * <p>The interesting group is {@link #definir()}, which builds a class file BYTE BY BYTE and hands
 * it to {@code defineClass}. That is the only path by which a type enters the VM without coming
 * off the classpath, and a probe that loaded one from a file would not exercise it at all.
 */
public class LoaderTest {

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

    /** The loader itself: one of them, and it says so consistently. */
    public static int cargador() {
        int bad = 0;
        ClassLoader system = ClassLoader.getSystemClassLoader();
        if (system == null) {
            bad = bad + 1;
        }
        if (ClassLoader.getSystemClassLoader() != system) {
            bad = bad + 1;
        }
        // Una clase del bootclasspath no tiene cargador, y null es la respuesta especificada.
        if (String.class.getClassLoader() != null) {
            bad = bad + 1;
        }
        if (!system.isRegisteredAsParallelCapable()) {
            bad = bad + 1;
        }
        // El candado de carga hay que pedirlo desde una subclase, porque es protegido -- lo
        // cual es parte del contrato: nadie de afuera sincroniza la carga ajena.
        Object lock = new Propio().candado("java.lang.String");
        if (lock == null) {
            bad = bad + 1;
        }
        return bad;
    }

    // Un cargador propio, para llegar a los metodos protegidos.
    static class Propio extends ClassLoader {

        Class<?> define(String name, byte[] bytes) {
            return this.defineClass(name, bytes, 0, bytes.length);
        }

        Class<?> yaCargada(String name) {
            return this.findLoadedClass(name);
        }

        Class<?> delSistema(String name) throws ClassNotFoundException {
            return this.findSystemClass(name);
        }

        void resolver(Class<?> c) {
            this.resolveClass(c);
        }

        void firmar(Class<?> c) {
            this.setSigners(c, new Object[0]);
        }

        String biblioteca(String name) {
            return this.findLibrary(name);
        }

        Class<?> buscar(String name) throws ClassNotFoundException {
            return this.findClass(name);
        }

        Object candado(String name) {
            return this.getClassLoadingLock(name);
        }
    }

    /** Finding a type, and the difference between finding and loading. */
    public static int encontrar() {
        int bad = 0;
        Propio propio = new Propio();
        try {
            if (propio.loadClass("java.lang.String") != String.class) {
                bad = bad + 1;
            }
            if (propio.delSistema("java.lang.Integer") != Integer.class) {
                bad = bad + 1;
            }
        } catch (ClassNotFoundException ex) {
            bad = bad + 1;
        }
        // `findLoadedClass` pregunta si ESTE cargador la definio, no si esta cargada en algun
        // lado. `String` la definio el bootstrap, asi que para un cargador propio la respuesta
        // es null -- y esa es la diferencia entre "cargada" y "cargada por mi".
        if (propio.yaCargada("java.lang.String") != null) {
            bad = bad + 1;
        }
        if (propio.yaCargada("no.existe.NingunaClase") != null) {
            bad = bad + 1;
        }
        // El `findClass` de base no encuentra nada, que es lo que hace que `loadClass` delegue.
        bad = bad + LoaderTest.expectNotFound(propio);
        // Y `resolveClass` y `setSigners` son no-ops que no explotan.
        propio.resolver(String.class);
        propio.firmar(String.class);
        if (propio.biblioteca("cualquiera") != null) {
            bad = bad + 1;
        }
        return bad;
    }

    static int expectNotFound(Propio propio) {
        try {
            propio.buscar("lo.que.sea.Cosa");
        } catch (ClassNotFoundException ex) {
            return 0;
        }
        return 1;
    }

    // El archivo de clase mas chico que existe: nombre, superclase Object, y nada mas. Escrito a
    // mano porque el punto es que `defineClass` acepte BYTES, no un archivo.
    static byte[] claseMinima(String binaryName) {
        byte[] name = LoaderTest.utf8(binaryName);
        byte[] object = LoaderTest.utf8("java/lang/Object");
        int size = 10 // magic + version + constant_pool_count
                + 3 + name.length      // #1 Utf8 el nombre
                + 3                    // #2 Class -> #1
                + 3 + object.length    // #3 Utf8 java/lang/Object
                + 3                    // #4 Class -> #3
                + 14;                  // flags, this, super, y los cuatro contadores en cero
        byte[] out = new byte[size];
        int at = 0;
        out[at] = (byte) 0xca;
        out[at + 1] = (byte) 0xfe;
        out[at + 2] = (byte) 0xba;
        out[at + 3] = (byte) 0xbe;
        at = at + 4;
        at = LoaderTest.putShort(out, at, 0);   // minor
        at = LoaderTest.putShort(out, at, 52);  // major: Java 8, el mas viejo que todo acepta
        at = LoaderTest.putShort(out, at, 5);   // constant_pool_count = entradas + 1
        out[at] = 1;                             // #1 CONSTANT_Utf8
        at = at + 1;
        at = LoaderTest.putShort(out, at, name.length);
        System.arraycopy(name, 0, out, at, name.length);
        at = at + name.length;
        out[at] = 7;                             // #2 CONSTANT_Class
        at = at + 1;
        at = LoaderTest.putShort(out, at, 1);
        out[at] = 1;                             // #3 CONSTANT_Utf8
        at = at + 1;
        at = LoaderTest.putShort(out, at, object.length);
        System.arraycopy(object, 0, out, at, object.length);
        at = at + object.length;
        out[at] = 7;                             // #4 CONSTANT_Class
        at = at + 1;
        at = LoaderTest.putShort(out, at, 3);
        at = LoaderTest.putShort(out, at, 0x0021); // ACC_PUBLIC | ACC_SUPER
        at = LoaderTest.putShort(out, at, 2);      // this_class
        at = LoaderTest.putShort(out, at, 4);      // super_class
        at = LoaderTest.putShort(out, at, 0);      // interfaces
        at = LoaderTest.putShort(out, at, 0);      // fields
        at = LoaderTest.putShort(out, at, 0);      // methods
        at = LoaderTest.putShort(out, at, 0);      // attributes
        return out;
    }

    static int putShort(byte[] out, int at, int value) {
        out[at] = (byte) ((value >> 8) & 0xff);
        out[at + 1] = (byte) (value & 0xff);
        return at + 2;
    }

    // Los nombres de clase son ASCII, asi que el UTF-8 modificado es byte por byte.
    static byte[] utf8(String s) {
        byte[] out = new byte[s.length()];
        int i = 0;
        while (i < s.length()) {
            out[i] = (byte) s.charAt(i);
            i = i + 1;
        }
        return out;
    }

    /** Bytes in, a loaded type out, with no file anywhere. */
    public static int definir() {
        int bad = 0;
        Propio propio = new Propio();
        byte[] bytes = LoaderTest.claseMinima("DefinidaAlVuelo");
        Class<?> definida = propio.define("DefinidaAlVuelo", bytes);
        if (definida == null) {
            return bad + 100;
        }
        bad = bad + LoaderTest.eq(definida.getName(), "DefinidaAlVuelo");
        bad = bad + LoaderTest.eq(definida.getSimpleName(), "DefinidaAlVuelo");
        if (definida.getSuperclass() != Object.class) {
            bad = bad + 1;
        }
        if (definida.getInterfaces().length != 0) {
            bad = bad + 1;
        }
        if (definida.getDeclaredFields().length != 0
                || definida.getDeclaredMethods().length != 0) {
            bad = bad + 1;
        }
        if (definida.getDeclaredConstructors().length != 0) {
            bad = bad + 1;
        }
        if (!Modifier.isPublic(definida.getModifiers())) {
            bad = bad + 1;
        }
        if (definida.isInterface() || definida.isArray() || definida.isPrimitive()) {
            bad = bad + 1;
        }
        // Y ahora esta cargada de verdad.
        if (propio.yaCargada("DefinidaAlVuelo") != definida) {
            bad = bad + 1;
        }
        // Los bytes dicen un nombre y el llamador otro: eso no puede entrar.
        bad = bad + LoaderTest.expectFormatError(propio, bytes);
        return bad;
    }

    static int expectFormatError(Propio propio, byte[] bytes) {
        try {
            propio.define("OtroNombre", bytes);
        } catch (ClassFormatError ex) {
            return 0;
        } catch (Throwable ex) {
            // El JDK tira NoClassDefFoundError acá, no ClassFormatError; ambos son Errors de
            // linkage y lo que importa es que NO se defina.
            return 0;
        }
        return 1;
    }

    /**
     * Assertions, which are the one piece of real state a loader carries here.
     *
     * <p>Note what is NOT tested: that a class already initialised changes behaviour. It does not,
     * in any JVM -- {@code assert} is desugared to a guard on a field the class reads once at
     * initialisation -- and that is why these setters are documented as affecting types loaded
     * afterwards.
     */
    public static int aserciones() {
        int bad = 0;
        ClassLoader system = ClassLoader.getSystemClassLoader();
        system.clearAssertionStatus();
        system.setDefaultAssertionStatus(true);
        system.setClassAssertionStatus("uno.dos.Tres", false);
        system.setPackageAssertionStatus("cuatro.cinco", false);
        system.clearAssertionStatus();
        // Despues de limpiar, todo vuelve a apagado.
        system.setDefaultAssertionStatus(false);
        return bad;
    }

    public static int todo() {
        return LoaderTest.cargador() + LoaderTest.encontrar() + LoaderTest.definir()
                + LoaderTest.aserciones();
    }

    public static void main(String[] args) {
        System.out.println("cargador    " + LoaderTest.cargador());
        System.out.println("encontrar   " + LoaderTest.encontrar());
        System.out.println("definir     " + LoaderTest.definir());
        System.out.println("aserciones  " + LoaderTest.aserciones());
        System.out.println("TOTAL       " + LoaderTest.todo());
    }
}
