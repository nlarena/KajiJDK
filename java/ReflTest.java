import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import jdk.internal.reflect.ConstructorAccessor;
import jdk.internal.reflect.FieldAccessor;
import jdk.internal.reflect.MethodAccessor;
import jdk.internal.reflect.Reflection;
import jdk.internal.reflect.ReflectionFactory;

/**
 * Prueba de comportamiento de `jdk.internal.reflect`: los tres accesores, la fabrica y las dos
 * mitades de `Reflection`.
 *
 * <p>Corre igual contra el JDK 25 real (con `--add-exports java.base/jdk.internal.reflect=ALL-UNNAMED`)
 * y contra esta VM, y `run()` tiene que devolver -1 en las dos.
 */
public class ReflTest {

    public static class Sujeto {
        public final int fin = 7;
        public int mut = 3;
        public long grande = 11L;

        public Sujeto() {
        }

        public Sujeto(int m) {
            this.mut = m;
        }

        public int doble(int x) {
            return x + x;
        }
    }

    public abstract static class Abstracta {
        public Abstracta() {
        }
    }

    public static int suma(int a, int b) {
        return a + b;
    }

    public static int cero() {
        return 42;
    }

    public static void explota() {
        throw new IllegalStateException("a proposito");
    }

    public static int run() throws Exception {
        ReflectionFactory rf = ReflectionFactory.getReflectionFactory();

        // 0: la fabrica es una sola.
        if (rf == null || rf != ReflectionFactory.getReflectionFactory()) {
            return 0;
        }
        // 1: el comodin es un conjunto de un elemento.
        if (Reflection.ALL_MEMBERS.size() != 1 || !Reflection.ALL_MEMBERS.contains("*")) {
            return 1;
        }

        // ---- MethodAccessor ----
        Method suma = ReflTest.class.getDeclaredMethod("suma", Integer.TYPE, Integer.TYPE);
        MethodAccessor ma = rf.newMethodAccessor(suma, false);
        // 2: estatico con argumentos boxeados.
        Object r = ma.invoke(null, new Object[] {Integer.valueOf(2), Integer.valueOf(3)});
        if (!(r instanceof Integer) || ((Integer) r).intValue() != 5) {
            return 2;
        }
        // 3: de instancia.
        Sujeto s = new Sujeto();
        Method doble = Sujeto.class.getDeclaredMethod("doble", Integer.TYPE);
        Object r3 = rf.newMethodAccessor(doble, false).invoke(s, new Object[] {Integer.valueOf(21)});
        if (((Integer) r3).intValue() != 42) {
            return 3;
        }
        // 4: `null` significa "sin argumentos".
        Method cero = ReflTest.class.getDeclaredMethod("cero");
        if (((Integer) rf.newMethodAccessor(cero, false).invoke(null, null)).intValue() != 42) {
            return 4;
        }
        // 5: lo que tire el destino sale envuelto.
        Method boom = ReflTest.class.getDeclaredMethod("explota");
        boolean envuelto = false;
        try {
            rf.newMethodAccessor(boom, false).invoke(null, new Object[0]);
        } catch (InvocationTargetException e) {
            envuelto = e.getCause() instanceof IllegalStateException;
        }
        if (!envuelto) {
            return 5;
        }

        // ---- FieldAccessor ----
        Field mut = Sujeto.class.getDeclaredField("mut");
        FieldAccessor fa = rf.newFieldAccessor(mut, false);
        // 6: ida y vuelta sin boxeo sobre un campo que no es final.
        fa.setInt(s, 99);
        if (fa.getInt(s) != 99 || s.mut != 99) {
            return 6;
        }
        // 7: lectura boxeada.
        Object caja = fa.get(s);
        if (!(caja instanceof Integer) || ((Integer) caja).intValue() != 99) {
            return 7;
        }
        // 8: un `final` sin `override` sale de solo lectura.
        Field fin = Sujeto.class.getDeclaredField("fin");
        FieldAccessor ro = rf.newFieldAccessor(fin, false);
        if (ro.getInt(s) != 7) {
            return 8;
        }
        boolean nego = false;
        try {
            ro.setInt(s, 1);
        } catch (IllegalAccessException e) {
            nego = true;
        }
        if (!nego) {
            return 9;
        }
        // 10: con `override` el mismo campo se escribe.
        FieldAccessor rw = rf.newFieldAccessor(fin, true);
        rw.setInt(s, 8);
        if (rw.getInt(s) != 8) {
            return 10;
        }
        // 11: los anchos tambien.
        Field grande = Sujeto.class.getDeclaredField("grande");
        FieldAccessor fl = rf.newFieldAccessor(grande, false);
        fl.setLong(s, 1234567890123L);
        if (fl.getLong(s) != 1234567890123L) {
            return 11;
        }

        // ---- ConstructorAccessor ----
        Constructor<?> ct = Sujeto.class.getDeclaredConstructor(Integer.TYPE);
        ConstructorAccessor ca = rf.newConstructorAccessor(ct);
        Object nuevo = ca.newInstance(new Object[] {Integer.valueOf(55)});
        if (!(nuevo instanceof Sujeto) || ((Sujeto) nuevo).mut != 55) {
            return 12;
        }
        // 13: `null` es "sin argumentos" tambien aca.
        Object vacio = rf.newConstructorAccessor(Sujeto.class.getDeclaredConstructor())
                .newInstance(null);
        if (((Sujeto) vacio).mut != 3) {
            return 13;
        }
        // 14: una clase abstracta no se instancia.
        boolean abstracta = false;
        try {
            rf.newConstructorAccessor(Abstracta.class.getDeclaredConstructor())
                    .newInstance(new Object[0]);
        } catch (InstantiationException e) {
            abstracta = true;
        }
        if (!abstracta) {
            return 14;
        }
        // 15: `newInstance` de la fabrica.
        // El literal de clase va a una variable a proposito: usado como receptor directo, nuestro
        // javac le borra el argumento generico y `getDeclaredConstructor` deja de devolver un
        // `Constructor<Sujeto>` (finding #390).
        Class<Sujeto> claseS = Sujeto.class;
        Constructor<Sujeto> ctS = claseS.getDeclaredConstructor(Integer.TYPE);
        Sujeto porFabrica = rf.newInstance(ctS, new Object[] {Integer.valueOf(77)}, ReflTest.class);
        if (porFabrica.mut != 77) {
            return 15;
        }

        // ---- Reflection: chequeo de acceso ----
        // 16: consigo misma, siempre.
        if (!Reflection.verifyMemberAccess(Sujeto.class, Sujeto.class, null, Modifier.PRIVATE)) {
            return 16;
        }
        // 17: publico de una clase publica, siempre.
        if (!Reflection.verifyMemberAccess(ReflTest.class, String.class, null, Modifier.PUBLIC)) {
            return 17;
        }
        // 18: privado de otra clase de otro paquete, nunca.
        if (Reflection.verifyMemberAccess(ReflTest.class, String.class, null, Modifier.PRIVATE)) {
            return 18;
        }
        // 19: privado entre companeras de nido, si.
        if (!Reflection.verifyMemberAccess(ReflTest.class, Sujeto.class, null, Modifier.PRIVATE)) {
            return 19;
        }
        // 20: un `protected` heredado, sobre el propio tipo.
        if (!Reflection.verifyMemberAccess(ReflTest.class, Object.class, ReflTest.class,
                Modifier.PROTECTED)) {
            return 20;
        }
        // 21: el mismo `protected` sobre un tipo ajeno, no (JLS 6.6.2).
        if (Reflection.verifyMemberAccess(ReflTest.class, Object.class, String.class,
                Modifier.PROTECTED)) {
            return 21;
        }
        // 22: `ensureMemberAccess` tira donde `verify` dice que no.
        boolean tiro = false;
        try {
            Reflection.ensureMemberAccess(ReflTest.class, String.class, null, Modifier.PRIVATE);
        } catch (IllegalAccessException e) {
            tiro = true;
        }
        if (!tiro) {
            return 22;
        }
        // 23: y no tira donde dice que si.
        Reflection.ensureMemberAccess(ReflTest.class, String.class, null, Modifier.PUBLIC);
        // 24: el caso barato.
        if (!Reflection.verifyPublicMemberAccess(String.class, Modifier.PUBLIC)) {
            return 24;
        }
        if (Reflection.verifyPublicMemberAccess(String.class, Modifier.PRIVATE)) {
            return 25;
        }
        // 26: nidos.
        if (!Reflection.areNestMates(ReflTest.class, Sujeto.class)
                || Reflection.areNestMates(ReflTest.class, String.class)) {
            return 26;
        }
        // 27: la excepcion se redacta sin tirarse.
        if (Reflection.newIllegalAccessException(ReflTest.class, String.class, null,
                Modifier.PRIVATE) == null) {
            return 27;
        }

        // ---- Reflection: el filtro ----
        Field[] tres = new Field[] {mut, fin, grande};
        // 28: sin filtro registrado, el arreglo pasa entero.
        if (Reflection.filterFields(ReflTest.class, tres).length != 3) {
            return 28;
        }
        // 29: las entradas que el JDK trae de fabrica esconden todo el cargador de clases.
        if (Reflection.filterFields(ClassLoader.class, tres).length != 0) {
            return 29;
        }
        // 30: registrar por nombre saca ese y deja los otros.
        Reflection.registerFieldsToFilter(Sujeto.class, Set.of("fin"));
        Field[] filtrados = Reflection.filterFields(Sujeto.class, tres);
        if (filtrados.length != 2 || filtrados[0] != mut || filtrados[1] != grande) {
            return 30;
        }
        // 31: el arreglo que sale sigue siendo `Field[]`.
        if (!(filtrados instanceof Field[])) {
            return 31;
        }
        // 32: registrar dos veces la misma clase es un error, no un reemplazo.
        boolean dosVeces = false;
        try {
            Reflection.registerFieldsToFilter(Sujeto.class, Set.of("mut"));
        } catch (IllegalArgumentException e) {
            dosVeces = true;
        }
        if (!dosVeces) {
            return 32;
        }
        // 33: el comodin esconde todo.
        Reflection.registerMethodsToFilter(Sujeto.class, Reflection.ALL_MEMBERS);
        Method[] dos = new Method[] {suma, doble};
        if (Reflection.filterMethods(Sujeto.class, dos).length != 0) {
            return 33;
        }
        // 34: y solo para la clase registrada.
        if (Reflection.filterMethods(ReflTest.class, dos).length != 2) {
            return 34;
        }
        // 35: un arreglo vacio sale vacio y no rompe.
        if (Reflection.filterFields(Sujeto.class, new Field[0]).length != 0) {
            return 35;
        }

        return -1;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(ReflTest.run());
    }
}
