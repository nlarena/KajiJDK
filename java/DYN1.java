import java.util.ArrayList;
import java.util.List;

import jdk.dynalink.DynamicLinker;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.linker.ConversionComparator;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkerServices;

/**
 * Comprueba {@code DynamicLinker} y {@code DynamicLinkerFactory} contra el JDK 25.
 *
 * <h2>Que se puede comparar</h2>
 *
 * <p>La mitad del sistema que decide sobre tipos: si una conversion es posible, cual de dos destinos
 * conviene, y con que error falla cada opcion de la fabrica. Eso es aritmetica sobre {@code Class} y
 * no depende de nada.
 *
 * <p>Lo que no se compara es enlazar. Enlazar es armar una manija de metodo, y esta maquina virtual
 * no tiene manijas --{@code MethodHandles} lo dice en su nota--, asi que {@code link} tira
 * {@code UnsupportedOperationException} donde el JDK devuelve el sitio enlazado. Es la unica
 * diferencia, y esta anotada aca en vez de escondida en una linea que coincida por casualidad.
 *
 * <p>{@link #donde()} devuelve el indice de la primera respuesta que no coincide, o -1.
 */
public class DYN1 {

    static final String[] ESPERADO = {
        "errores-antes|[]",
        "armado|true|[]|true",
        "sitio|null",
        "convierte|101010101111010100",
        "compara|INDETERMINATE|INDETERMINATE|INDETERMINATE",
        "filtro|true",
        "umbral-negativo|IllegalArgumentException",
        "umbral-cero|ok",
        "umbral-ocho|ok",
        "prio-nulo|ok",
        "prio-uno-nulo|NullPointerException",
        "prio-lista-con-nulo|NullPointerException",
        "fall-nulo|ok",
        "fall-lista-con-nulo|NullPointerException",
        "sueltos|ok",
        "otros|ok",
        "sin-ultimo-recurso|true|true",
        "sin-descubrimiento|true|[]",
        "dos-veces|true",
        "enlaza-nulo|NullPointerException",
    };

    /** Lo que hacen las dos clases, una linea por comprobacion. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        final DynamicLinkerFactory f = new DynamicLinkerFactory();
        a.add("errores-antes|" + f.getAutoLoadingErrors());
        final DynamicLinker l = f.createLinker();
        a.add("armado|" + (l != null) + "|" + f.getAutoLoadingErrors()
                + "|" + (l.getLinkerServices() != null));
        a.add("sitio|" + DynamicLinker.getLinkedCallSiteLocation());

        // Las conversiones que el lenguaje permite.
        final LinkerServices s = l.getLinkerServices();
        final Class<?>[][] pares = {
            {int.class, long.class},
            {long.class, int.class},
            {int.class, double.class},
            {double.class, int.class},
            {char.class, int.class},
            {int.class, char.class},
            {byte.class, short.class},
            {short.class, byte.class},
            {int.class, Integer.class},
            {Integer.class, int.class},
            {int.class, Object.class},
            {String.class, Object.class},
            {Object.class, String.class},
            {String.class, CharSequence.class},
            {CharSequence.class, String.class},
            {String.class, String.class},
            {int.class, boolean.class},
            {boolean.class, int.class},
        };
        final StringBuilder conv = new StringBuilder();
        for (int i = 0; i < pares.length; i++) {
            conv.append(s.canConvert(pares[i][0], pares[i][1]) ? '1' : '0');
        }
        a.add("convierte|" + conv);

        // La preferencia entre dos destinos: sin comparadores registrados no hay ninguna.
        a.add("compara|" + s.compareConversion(String.class, Object.class, CharSequence.class)
                + "|" + s.compareConversion(int.class, long.class, double.class)
                + "|" + s.compareConversion(Object.class, Object.class, Object.class));

        // Un objetivo que no se filtra vuelve tal cual.
        a.add("filtro|" + (s.filterInternalObjects(null) == null));

        // La configuracion de la fabrica.
        a.add("umbral-negativo|" + intentar(new Umbral(f, -1)));
        a.add("umbral-cero|" + intentar(new Umbral(f, 0)));
        a.add("umbral-ocho|" + intentar(new Umbral(f, 8)));
        a.add("prio-nulo|" + intentar(new PrioLista(f, null)));
        a.add("prio-uno-nulo|" + intentar(new PrioUno(f)));
        final List<GuardingDynamicLinker> conNulo = new ArrayList<GuardingDynamicLinker>();
        conNulo.add(null);
        a.add("prio-lista-con-nulo|" + intentar(new PrioLista(f, conNulo)));
        a.add("fall-nulo|" + intentar(new FallLista(f, null)));
        a.add("fall-lista-con-nulo|" + intentar(new FallLista(f, conNulo)));
        a.add("sueltos|" + intentar(new PrioSueltos(f)));
        a.add("otros|" + intentar(new Otros(f)));

        // Sin ultimo recurso tambien se arma: no es lo mismo que no decir nada.
        final DynamicLinkerFactory f2 = new DynamicLinkerFactory();
        f2.setFallbackLinkers(new GuardingDynamicLinker[0]);
        final DynamicLinker l2 = f2.createLinker();
        a.add("sin-ultimo-recurso|" + (l2 != null)
                + "|" + l2.getLinkerServices().canConvert(int.class, long.class));

        // Apagar el descubrimiento tampoco impide armarlo.
        final DynamicLinkerFactory f3 = new DynamicLinkerFactory();
        f3.setClassLoader(null);
        a.add("sin-descubrimiento|" + (f3.createLinker() != null) + "|"
                + f3.getAutoLoadingErrors());

        // Se puede armar mas de uno.
        a.add("dos-veces|" + (f.createLinker() != f.createLinker()));

        // Enlazar un sitio nulo falla antes de llegar a las manijas.
        a.add("enlaza-nulo|" + intentar(new EnlazaNulo(l)));

        return a.toArray(new String[a.size()]);
    }

    /** Algo que se corre para ver con que falla. */
    interface Tiro {
        void correr() throws Exception;
    }

    /** Corre eso y devuelve "ok" o el nombre simple de lo que haya tirado. */
    static String intentar(Tiro r) {
        try {
            r.correr();
            return "ok";
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    static class Umbral implements Tiro {
        private final DynamicLinkerFactory f;
        private final int n;

        Umbral(DynamicLinkerFactory f, int n) {
            this.f = f;
            this.n = n;
        }

        public void correr() throws Exception {
            f.setUnstableRelinkThreshold(this.n);
        }
    }

    static class PrioLista implements Tiro {
        private final DynamicLinkerFactory f;
        private final List<GuardingDynamicLinker> l;

        PrioLista(DynamicLinkerFactory f, List<GuardingDynamicLinker> l) {
            this.f = f;
            this.l = l;
        }

        public void correr() throws Exception {
            f.setPrioritizedLinkers(this.l);
        }
    }

    static class FallLista implements Tiro {
        private final DynamicLinkerFactory f;
        private final List<GuardingDynamicLinker> l;

        FallLista(DynamicLinkerFactory f, List<GuardingDynamicLinker> l) {
            this.f = f;
            this.l = l;
        }

        public void correr() throws Exception {
            f.setFallbackLinkers(this.l);
        }
    }

    static class PrioUno implements Tiro {
        private final DynamicLinkerFactory f;

        PrioUno(DynamicLinkerFactory f) {
            this.f = f;
        }

        public void correr() throws Exception {
            f.setPrioritizedLinker(null);
        }
    }

    static class PrioSueltos implements Tiro {
        private final DynamicLinkerFactory f;

        PrioSueltos(DynamicLinkerFactory f) {
            this.f = f;
        }

        public void correr() throws Exception {
            f.setPrioritizedLinkers(new GuardingDynamicLinker[0]);
            f.setFallbackLinkers(new GuardingDynamicLinker[0]);
        }
    }

    static class Otros implements Tiro {
        private final DynamicLinkerFactory f;

        Otros(DynamicLinkerFactory f) {
            this.f = f;
        }

        public void correr() throws Exception {
            f.setSyncOnRelink(true);
            f.setPrelinkTransformer(null);
            f.setAutoConversionStrategy(null);
            f.setInternalObjectsFilter(null);
        }
    }

    static class EnlazaNulo implements Tiro {
        private final DynamicLinker l;

        EnlazaNulo(DynamicLinker l) {
            this.l = l;
        }

        public void correr() throws Exception {
            l.link(null);
        }
    }

    /**
     * El indice de la primera respuesta que no coincide con la del JDK, o -1.
     *
     * @return el indice, o -1
     */
    public static int donde() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != ESPERADO.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(ESPERADO[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = donde();
        System.out.println(i < 0 ? "sin diferencias"
                : i + ":\n  nuestro=" + a[i] + "\n  jdk    =" + ESPERADO[i]);
    }
}
