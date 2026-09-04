import jdk.internal.vm.Stack;

/**
 * La introspeccion de pila, que hasta hace un rato no existia.
 *
 * <p>Es lo que le faltaba a `SecurityManager.getClassContext()` --el ultimo miembro de `java.lang`--
 * y tambien a `StackWalker` y a las trazas de `Throwable`. La VM tenia la pila de cuadros y no la
 * exponia; ahora la expone por {@link Stack#frames()}, como intrinseco del interprete (el puente de
 * nativos no ve los frames, que son justo lo que hay que leer).
 *
 * <p>Se prueba el mecanismo y no `getClassContext()` directamente porque el constructor de
 * `SecurityManager` tira --divergencia aparte y anterior-- asi que no hay instancia desde la cual
 * llamarlo. Lo que se verifica es exactamente lo que ese metodo hace: leer los cuadros, quedarse con
 * la clase, y resolverla.
 *
 * <p>NO corre contra el JDK real: `jdk.internal.vm` es un paquete de `java.base`, y una clase del
 * classpath en un paquete que un modulo ya define no se carga nunca. Alla se probaria el JDK a si
 * mismo.
 */
public class StackCtxTest {

    static int fallas = 0;

    static void ok(String que, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + que);
            fallas = fallas + 1;
        }
    }

    static String[] nivel3() { return Stack.frames(); }
    static String[] nivel2() { return StackCtxTest.nivel3(); }
    static String[] nivel1() { return StackCtxTest.nivel2(); }

    public static int run() throws Exception {
        fallas = 0;

        String[] f = StackCtxTest.nivel1();
        ok("la pila no es nula", f != null);
        ok("hay al menos cuatro cuadros", f != null && f.length >= 4);

        // El orden es de arriba hacia abajo: el primero es quien llamo.
        ok("el primero es el mas reciente", f[0].endsWith("|nivel3"));
        ok("el segundo es su llamador", f[1].endsWith("|nivel2"));
        ok("el tercero tambien", f[2].endsWith("|nivel1"));
        ok("y el cuarto es run", f[3].endsWith("|run"));

        // El formato es "clase|metodo", con la clase en forma binaria.
        int barra = f[0].indexOf('|');
        ok("el formato lleva la barra", barra > 0);
        String clase = f[0].substring(0, barra);
        ok("la clase es la nuestra", "StackCtxTest".equals(clase.replace('/', '.')));

        // Y esto es lo que hace getClassContext: resolver cada nombre a su Class.
        Class<?> c = Class.forName(clase.replace('/', '.'));
        ok("el nombre resuelve a la clase", c == StackCtxTest.class);

        // La pila crece con la profundidad: no es una lista fija.
        String[] somero = Stack.frames();
        ok("mas hondo da mas cuadros", f.length > somero.length);

        if (fallas == 0) {
            return -1;
        }
        return fallas;
    }

    public static void main(String[] a) throws Exception {
        System.out.println("StackCtxTest " + StackCtxTest.run());
    }
}
