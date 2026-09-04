package jdk.dynalink.beans;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;

/**
 * El enlazador de objetos Java comunes: propiedades, metodos, arreglos y colecciones.
 *
 * <h2>Que expone de un objeto</h2>
 *
 * <p>Lo que un lenguaje dinamico espera poder escribir. {@code obj.prop} busca un {@code getProp()}
 * o un campo {@code prop}; {@code obj.metodo(1)} busca un metodo; {@code arr[0]} y {@code lista[0]}
 * indexan.
 *
 * <p>La traduccion de nombres es la de JavaBeans, con su rareza incluida: {@code getURL()} da la
 * propiedad {@code URL} y no {@code uRL}, porque cuando las dos primeras letras son mayusculas el
 * nombre se deja como esta. Es lo que hace {@code Introspector.decapitalize} y esta clase lo
 * reproduce.
 *
 * <h2>La faceta estatica y la de instancia no se miran igual</h2>
 *
 * <p>Es la asimetria menos obvia de esta clase, y esta verificada contra el JDK. La faceta de
 * <strong>instancia</strong> usa todos los miembros publicos, heredados incluidos: un campo del
 * padre es propiedad del hijo. La faceta <strong>estatica</strong> usa solo los
 * <strong>declarados</strong>: un campo estatico del padre no es propiedad estatica del hijo, y un
 * metodo estatico del padre no aparece entre los del hijo.
 *
 * <p>Tiene sentido: los miembros estaticos no se heredan de verdad —no hay despacho— y exponer los
 * del padre en el hijo sugeriria una relacion que no existe. Las clases anidadas son la excepcion y
 * si se heredan, que es por lo que {@code Point} expone {@code Double} y {@code Float}, que son de
 * {@code Point2D}.
 *
 * <h2>Reglas finas, todas comprobadas contra el JDK 25</h2>
 *
 * <ul>
 *   <li>{@code getX()} es propiedad sin importar que devuelva: hasta {@code void getVoid()} cuenta.
 *       Lo que importa es que no tome argumentos.
 *   <li>{@code isX()} solo cuenta si devuelve {@code boolean} <strong>primitivo</strong>; con
 *       {@code Boolean} no.
 *   <li>{@code get()} e {@code is()} pelados no son propiedades: no queda nombre despues del
 *       prefijo.
 *   <li>Un campo {@code final} es legible y no escribible.
 *   <li>{@code class} siempre esta entre las propiedades estaticas legibles, aunque la clase no
 *       tenga ningun miembro estatico.
 * </ul>
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Los seis metodos de introspeccion son reales y estan verificados uno a uno contra el JDK. El
 * enlace propiamente dicho —{@link #getLinkerForClass} y {@link #getGuardedInvocation}— necesita
 * fabricar {@code MethodHandle}, que esta VM todavia no puede; llegan hasta ese punto y fallan
 * nombrandolo.
 *
 * @since 9
 */
public class BeansLinker implements GuardingDynamicLinker {

    private static final String NO_HAY_HANDLES =
            "enlazar un bean necesita fabricar MethodHandle, que esta VM no soporta todavia";

    private final MissingMemberHandlerFactory missingMemberFactory;

    /** Un enlazador que deja fallar los miembros que no existen. */
    public BeansLinker() {
        this(null);
    }

    /**
     * Un enlazador con una respuesta propia para los miembros que no existen.
     *
     * @param missingMemberHandlerFactory la fabrica, o {@code null} para el comportamiento comun
     */
    public BeansLinker(final MissingMemberHandlerFactory missingMemberHandlerFactory) {
        this.missingMemberFactory = missingMemberHandlerFactory;
    }

    /**
     * El enlazador que le corresponde a esa clase.
     *
     * @param clazz la clase
     * @return el enlazador
     * @throws UnsupportedOperationException en esta VM; ver la nota de la clase
     */
    public TypeBasedGuardingDynamicLinker getLinkerForClass(final Class<?> clazz) {
        throw new UnsupportedOperationException(NO_HAY_HANDLES);
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException en esta VM; ver la nota de la clase
     */
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest,
            final LinkerServices linkerServices) throws Exception {
        throw new UnsupportedOperationException(NO_HAY_HANDLES);
    }

    /**
     * Si ese objeto es uno de los metodos dinamicos que este enlazador produce.
     *
     * <p>Siempre {@code false}, y es la respuesta correcta: los metodos dinamicos son objetos que
     * fabrica el propio enlazador, esta VM no llega a fabricar ninguno, y por lo tanto ningun objeto
     * que alguien pueda pasar aca lo es.
     *
     * @param obj el objeto
     * @return {@code false}
     */
    public static boolean isDynamicMethod(final Object obj) {
        return false;
    }

    /**
     * Si ese objeto es uno de los constructores dinamicos que este enlazador produce.
     *
     * <p>Siempre {@code false}, por la misma razon que {@link #isDynamicMethod}.
     *
     * @param obj el objeto
     * @return {@code false}
     */
    public static boolean isDynamicConstructor(final Object obj) {
        return false;
    }

    /**
     * El constructor de esa clase con esa firma, como objeto de metodo dinamico.
     *
     * <p>Falla en vez de devolver {@code null}: {@code null} significa "esa clase no tiene un
     * constructor con esa firma", que seria mentira para casi cualquier entrada.
     *
     * @param clazz la clase
     * @param signature la firma, con los tipos separados por comas
     * @return no vuelve
     * @throws UnsupportedOperationException en esta VM; ver la nota de la clase
     */
    public static Object getConstructorMethod(final Class<?> clazz, final String signature) {
        throw new UnsupportedOperationException(
                "los objetos de metodo dinamico los fabrica el enlazador, y enlazar necesita "
                + "MethodHandle, que esta VM no soporta todavia");
    }

    // ---- introspeccion ----

    /**
     * Las propiedades de instancia que se pueden leer.
     *
     * @param clazz la clase
     * @return los nombres
     */
    public static Set<String> getReadableInstancePropertyNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        for (final Field f : clazz.getFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                out.add(f.getName());
            }
        }
        for (final Method m : clazz.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                agregarLectura(out, m);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * Las propiedades de instancia que se pueden escribir.
     *
     * @param clazz la clase
     * @return los nombres
     */
    public static Set<String> getWritableInstancePropertyNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        for (final Field f : clazz.getFields()) {
            final int mod = f.getModifiers();
            if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod)) {
                out.add(f.getName());
            }
        }
        for (final Method m : clazz.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                agregarEscritura(out, m);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * Los nombres de los metodos de instancia.
     *
     * <p>Incluye a los que ademas son accesores: {@code getProp} aparece aca y {@code prop} aparece
     * en las propiedades. Son dos formas de llegar a lo mismo y las dos estan expuestas.
     *
     * @param clazz la clase
     * @return los nombres
     */
    public static Set<String> getInstanceMethodNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        for (final Method m : clazz.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                out.add(m.getName());
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * Las propiedades estaticas que se pueden leer.
     *
     * <p>Incluye siempre {@code class}, y las clases anidadas por su nombre simple.
     *
     * @param clazz la clase
     * @return los nombres
     */
    public static Set<String> getReadableStaticPropertyNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        // La pseudo-propiedad que devuelve el Class representado. Esta siempre, incluso en una
        // clase sin ningun miembro estatico.
        out.add("class");
        for (final Field f : clazz.getDeclaredFields()) {
            if (esEstaticoPublico(f.getModifiers())) {
                out.add(f.getName());
            }
        }
        for (final Method m : clazz.getDeclaredMethods()) {
            if (esEstaticoPublico(m.getModifiers())) {
                agregarLectura(out, m);
            }
        }
        // Las clases anidadas si se heredan, a diferencia del resto de la faceta estatica.
        for (final Class<?> k : clazz.getClasses()) {
            out.add(k.getSimpleName());
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * Las propiedades estaticas que se pueden escribir.
     *
     * @param clazz la clase
     * @return los nombres
     */
    public static Set<String> getWritableStaticPropertyNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        for (final Field f : clazz.getDeclaredFields()) {
            final int mod = f.getModifiers();
            if (esEstaticoPublico(mod) && !Modifier.isFinal(mod)) {
                out.add(f.getName());
            }
        }
        for (final Method m : clazz.getDeclaredMethods()) {
            if (esEstaticoPublico(m.getModifiers())) {
                agregarEscritura(out, m);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * Los nombres de los metodos estaticos declarados por esa clase.
     *
     * @param clazz la clase
     * @return los nombres
     */
    public static Set<String> getStaticMethodNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        for (final Method m : clazz.getDeclaredMethods()) {
            if (esEstaticoPublico(m.getModifiers())) {
                out.add(m.getName());
            }
        }
        return Collections.unmodifiableSet(out);
    }

    private static boolean esEstaticoPublico(final int mod) {
        return Modifier.isStatic(mod) && Modifier.isPublic(mod);
    }

    /** Si el metodo es un accesor de lectura, agrega la propiedad que nombra. */
    private static void agregarLectura(final Set<String> out, final Method m) {
        if (m.getParameterTypes().length != 0) {
            return;
        }
        final String n = m.getName();
        if (n.length() > 3 && n.startsWith("get")) {
            // Sin mirar el tipo de retorno: hasta un `void getVoid()` cuenta como propiedad.
            out.add(decapitalizar(n.substring(3)));
        } else if (n.length() > 2 && n.startsWith("is") && m.getReturnType() == boolean.class) {
            // Aca si importa, y tiene que ser el primitivo: con Boolean no cuenta.
            out.add(decapitalizar(n.substring(2)));
        }
    }

    /** Si el metodo es un accesor de escritura, agrega la propiedad que nombra. */
    private static void agregarEscritura(final Set<String> out, final Method m) {
        if (m.getParameterTypes().length != 1) {
            return;
        }
        final String n = m.getName();
        if (n.length() > 3 && n.startsWith("set")) {
            out.add(decapitalizar(n.substring(3)));
        }
    }

    /**
     * La regla de JavaBeans para pasar de {@code getURL} a {@code URL} y de {@code getX} a
     * {@code x}.
     *
     * <p>Si las dos primeras letras son mayusculas el nombre queda intacto. La razon es que esos
     * nombres suelen ser siglas —{@code URL}, {@code HTTP}, {@code ID}— y bajarle la primera letra
     * las volveria irreconocibles.
     */
    private static String decapitalizar(final String s) {
        if (s.length() > 1 && Character.isUpperCase(s.charAt(0))
                && Character.isUpperCase(s.charAt(1))) {
            return s;
        }
        final char[] c = s.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }
}
