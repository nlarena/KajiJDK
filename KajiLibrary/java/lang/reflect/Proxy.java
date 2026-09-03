package java.lang.reflect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import jdk.internal.reflect.ProxyDispatcher;

/**
 * KajiLibrary's java.lang.reflect.Proxy.
 *
 * <p>A proxy class is a class that is <em>fabricated at run time</em>: given a list of interfaces
 * and an {@link InvocationHandler}, {@link #newProxyInstance} builds a real {@code .class} in
 * memory, hands it to the VM, and returns an instance of it. Every method of every interface on
 * that instance ends up as one call to {@code handler.invoke(proxy, method, args)}.
 *
 * <p>That is not a shape or a stand-in -- there is no other way to get a type that implements an
 * interface nobody wrote an implementation for. The bytes are emitted by
 * {@link ProxyGenerator} (which explains its own format choices) and enter the VM through
 * {@link ClassLoader#defineClass(String, byte[], int, int)}.
 *
 * <h2>One loader</h2>
 *
 * <p>The {@code loader} argument is accepted and ignored, and the cache is keyed on the interface
 * list alone. That is not a shortcut -- KajiJDK has one loader (see {@link ClassLoader}), so two
 * requests for the same interfaces genuinely cannot produce two distinct types here. In a VM with
 * a real delegation hierarchy they could, and that is exactly the difference the parameter
 * encodes; with one loader there is nothing for it to select.
 *
 * <h2>What a proxy does NOT intercept</h2>
 *
 * <p>{@code hashCode}, {@code equals} and {@code toString} go to the handler, with the
 * {@link Method} objects of {@link Object}. Every other method of {@code Object} is {@code final}
 * and keeps its inherited behaviour. Static and private interface methods are not intercepted
 * either: they are not inherited, so there is nothing to override.
 */
public class Proxy implements Serializable {

    private static final long serialVersionUID = -2222568056686623797L;

    /** The invocation handler every call on this proxy is routed to. */
    protected InvocationHandler h;

    // ---- estado global de la fabrica ----
    //
    // Un candado para el cache y el contador de nombres: los dos se tocan una vez por CLASE de
    // proxy, no una vez por instancia, asi que no hay nada que ganar afinandolos.

    private static final Object CANDADO = new Object();

    /** De la lista de interfaces (sus nombres, en orden) a la clase generada. */
    private static final HashMap<String, Class<?>> CACHE = new HashMap<String, Class<?>>();

    /**
     * Las clases que esta fabrica genero, para que {@link #isProxyClass} no adivine.
     *
     * <p>Concurrente y fuera del candado a proposito: {@link #getInvocationHandler} la consulta, y
     * {@code getInvocationHandler} esta en el camino de CADA llamada a CADA proxy del programa.
     * Un candado global ahi convertiria a todo proxy en un punto de serializacion. Se escribe una
     * sola vez por clase, con el candado tomado igual.
     */
    private static final ConcurrentHashMap<Class<?>, Boolean> GENERADAS =
            new ConcurrentHashMap<Class<?>, Boolean>();

    private static int siguienteNumero;

    /**
     * El cargador que define las clases generadas.
     *
     * <p>`defineClass` es `protected`: la unica forma de llegarle es desde una subclase, y por eso
     * hay una. No es una jerarquia de delegacion -- es el permiso, escrito como herencia.
     */
    private static final class CargadorDeProxy extends ClassLoader {
        Class<?> definir(String nombre, byte[] bytes) {
            return this.defineClass(nombre, bytes, 0, bytes.length);
        }
    }

    private static final CargadorDeProxy CARGADOR = new CargadorDeProxy();

    /**
     * The constructor a generated proxy class calls.
     *
     * @param h the handler to route calls to
     */
    protected Proxy(InvocationHandler h) {
        if (h == null) {
            throw new NullPointerException("h");
        }
        this.h = h;
    }

    // Sin manejador no hay proxy: existe para que nadie herede de Proxy y se saltee el argumento.
    private Proxy() {
    }

    /**
     * Returns the proxy class for {@code interfaces}, generating it on first request.
     *
     * @param loader ignored -- see the class notes on KajiJDK's single loader
     * @param interfaces the interfaces the class must implement, in order
     * @return the proxy class
     * @throws IllegalArgumentException if the interface list is not legal
     * @deprecated Prefer {@link #newProxyInstance}, which does not expose the generated class.
     */
    @Deprecated
    public static Class<?> getProxyClass(ClassLoader loader, Class<?>... interfaces)
            throws IllegalArgumentException {
        return Proxy.claseDeProxy(Proxy.copiarYValidar(interfaces));
    }

    /**
     * Creates a proxy instance for {@code interfaces} that routes every call to {@code h}.
     *
     * @param loader ignored -- see the class notes on KajiJDK's single loader
     * @param interfaces the interfaces the proxy must implement, in order
     * @param h the handler
     * @return the new proxy instance
     * @throws IllegalArgumentException if the interface list is not legal
     * @throws NullPointerException if {@code interfaces} or {@code h} is null
     */
    public static Object newProxyInstance(ClassLoader loader, Class<?>[] interfaces,
            InvocationHandler h) {
        if (h == null) {
            throw new NullPointerException("h");
        }
        Class<?>[] copia = Proxy.copiarYValidar(interfaces);
        Class<?> clase = Proxy.claseDeProxy(copia);
        try {
            Constructor<?> ctor = clase.getDeclaredConstructor(
                    new Class<?>[] { InvocationHandler.class });
            ctor.setAccessible(true);
            return ctor.newInstance(new Object[] { h });
        } catch (InvocationTargetException fallo) {
            // El constructor generado es `super(h)` y nada mas; si eso tira, el error viene de
            // Proxy, no del usuario, y devolverlo desnudo es lo unico util.
            Throwable causa = fallo.getCause();
            if (causa instanceof RuntimeException) {
                throw (RuntimeException) causa;
            }
            if (causa instanceof Error) {
                throw (Error) causa;
            }
            throw new InternalError(fallo.toString());
        } catch (NoSuchMethodException imposible) {
            throw new InternalError(imposible.toString());
        }
    }

    /**
     * Whether {@code cl} is a class this factory generated.
     *
     * @param cl the class to test
     * @return true if it is a proxy class
     */
    public static boolean isProxyClass(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException("cl");
        }
        return Proxy.GENERADAS.containsKey(cl);
    }

    /**
     * The handler of a proxy instance.
     *
     * @param proxy the proxy instance
     * @return its handler
     * @throws IllegalArgumentException if {@code proxy} is not a proxy instance
     */
    public static InvocationHandler getInvocationHandler(Object proxy)
            throws IllegalArgumentException {
        if (!Proxy.isProxyClass(proxy.getClass())) {
            throw new IllegalArgumentException("not a proxy instance");
        }
        return ((Proxy) proxy).h;
    }

    // ------------------------------------------------------------------ fabrica

    /**
     * Copia el arreglo de interfaces y lo valida.
     *
     * <p>La copia no es paranoia: entre validar y generar hay reflexion sobre cada elemento, y un
     * arreglo que el llamador puede seguir escribiendo convierte "ya lo valide" en una mentira.
     */
    private static Class<?>[] copiarYValidar(Class<?>[] interfaces) {
        if (interfaces == null) {
            throw new NullPointerException("interfaces");
        }
        Class<?>[] copia = new Class<?>[interfaces.length];
        System.arraycopy(interfaces, 0, copia, 0, interfaces.length);
        if (copia.length > 65535) {
            throw new IllegalArgumentException("interface limit exceeded: " + copia.length);
        }
        int i = 0;
        while (i < copia.length) {
            Class<?> intf = copia[i];
            if (intf == null) {
                throw new NullPointerException("interfaces[" + i + "]");
            }
            if (!intf.isInterface()) {
                throw new IllegalArgumentException(intf.getName() + " is not an interface");
            }
            int j = 0;
            while (j < i) {
                if (copia[j] == intf) {
                    throw new IllegalArgumentException("repeated interface: " + intf.getName());
                }
                j = j + 1;
            }
            i = i + 1;
        }
        return copia;
    }

    private static Class<?> claseDeProxy(Class<?>[] interfaces) {
        String clave = Proxy.clave(interfaces);
        synchronized (Proxy.CANDADO) {
            Class<?> ya = Proxy.CACHE.get(clave);
            if (ya != null) {
                return ya;
            }
            Class<?> nueva = Proxy.generar(interfaces);
            Proxy.CACHE.put(clave, nueva);
            Proxy.GENERADAS.put(nueva, Boolean.TRUE);
            return nueva;
        }
    }

    /**
     * La clave del cache: los nombres de las interfaces en orden, separados por un caracter que no
     * puede aparecer en un nombre binario -- si no, `{A, BC}` y `{AB, C}` colisionarian.
     */
    private static String clave(Class<?>[] interfaces) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < interfaces.length) {
            sb.append(interfaces[i].getName());
            sb.append(';');
            i = i + 1;
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ tabla de metodos

    /**
     * Un metodo del proxy mientras se lo arma: el `Method` que va a recibir el manejador, su
     * descriptor y las excepciones que declara despues de fundir todas las interfaces que lo
     * declaran.
     */
    private static final class Entrada {
        final Method metodo;
        final String firmaCorta;
        final Class<?> retorno;
        Class<?>[] excepciones;

        Entrada(Method metodo, String firmaCorta) {
            this.metodo = metodo;
            this.firmaCorta = firmaCorta;
            this.retorno = metodo.getReturnType();
            this.excepciones = metodo.getExceptionTypes();
        }
    }

    private static Class<?> generar(Class<?>[] interfaces) {
        ArrayList<Entrada> entradas = new ArrayList<Entrada>();
        // De firma completa (nombre + parametros + retorno) a la entrada ya creada. Es lo que hace
        // que un metodo declarado por dos interfaces se implemente UNA vez -- y, como la primera
        // que lo declara es la que dejo la entrada, es tambien lo que decide que `Method` recibe
        // el manejador.
        HashMap<String, Entrada> porFirma = new HashMap<String, Entrada>();

        // Object primero: hashCode, equals y toString son los tres metodos de Object que no son
        // finales, y por eso son los tres -- y los unicos -- que un proxy puede interceptar.
        Proxy.agregar(entradas, porFirma, Proxy.metodoDeObject("hashCode", new Class<?>[0]));
        Proxy.agregar(entradas, porFirma,
                Proxy.metodoDeObject("equals", new Class<?>[] { Object.class }));
        Proxy.agregar(entradas, porFirma, Proxy.metodoDeObject("toString", new Class<?>[0]));

        int i = 0;
        while (i < interfaces.length) {
            Method[] publicos = interfaces[i].getMethods();
            int j = 0;
            while (j < publicos.length) {
                Method m = publicos[j];
                // Los estaticos de una interfaz no se heredan: no hay nada que sobrescribir.
                if (!Modifier.isStatic(m.getModifiers())) {
                    Proxy.agregar(entradas, porFirma, m);
                }
                j = j + 1;
            }
            i = i + 1;
        }

        Proxy.verificarRetornos(entradas);

        Method[] metodos = new Method[entradas.size()];
        Class<?>[][] declaradas = new Class<?>[entradas.size()][];
        i = 0;
        while (i < entradas.size()) {
            metodos[i] = entradas.get(i).metodo;
            declaradas[i] = entradas.get(i).excepciones;
            i = i + 1;
        }

        boolean publica = Proxy.todasPublicas(interfaces);
        String paquete = Proxy.paquete(interfaces, publica);
        String nombre;
        synchronized (Proxy.CANDADO) {
            // Sin paquete no va el punto: "$Proxy0" es un nombre binario valido, ".$Proxy0" no.
            String prefijo = paquete.isEmpty() ? "" : paquete + ".";
            nombre = prefijo + "$Proxy" + Proxy.siguienteNumero;
            Proxy.siguienteNumero = Proxy.siguienteNumero + 1;
        }

        byte[] bytes = ProxyGenerator.generar(nombre, interfaces, publica, metodos);
        Class<?> clase = Proxy.CARGADOR.definir(nombre, bytes);
        // Registrar ANTES de que exista la primera instancia: en este hueco la clase ya esta
        // definida pero todavia no la puede invocar nadie.
        ProxyDispatcher.registrar(clase, metodos, declaradas);
        return clase;
    }

    private static Method metodoDeObject(String nombre, Class<?>[] parametros) {
        try {
            return Object.class.getMethod(nombre, parametros);
        } catch (NoSuchMethodException imposible) {
            throw new InternalError(imposible.toString());
        }
    }

    private static void agregar(ArrayList<Entrada> entradas, HashMap<String, Entrada> porFirma,
            Method m) {
        String corta = Proxy.firmaCorta(m);
        String completa = corta + ProxyGenerator.descriptor(m.getReturnType());
        Entrada ya = porFirma.get(completa);
        if (ya != null) {
            // Mismo metodo por segunda vez: la entrada no se duplica, pero las excepciones si se
            // funden. Un proxy solo puede declarar lo que TODAS las interfaces permiten -- si una
            // declara IOException y la otra no declara nada, el proxy no puede tirar IOException.
            ya.excepciones = Proxy.fundirExcepciones(ya.excepciones, m.getExceptionTypes());
            return;
        }
        Entrada nueva = new Entrada(m, corta);
        porFirma.put(completa, nueva);
        entradas.add(nueva);
    }

    /** Nombre + descriptores de los parametros: lo que define "el mismo metodo" al sobrescribir. */
    private static String firmaCorta(Method m) {
        Class<?>[] parametros = m.getParameterTypes();
        StringBuilder sb = new StringBuilder(m.getName());
        sb.append('(');
        int i = 0;
        while (i < parametros.length) {
            sb.append(ProxyGenerator.descriptor(parametros[i]));
            i = i + 1;
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * La interseccion util de dos listas de excepciones declaradas: de cada lista, lo que la otra
     * ya cubre. No es la interseccion literal -- si una declara `IOException` y la otra
     * `FileNotFoundException`, lo que queda es `FileNotFoundException`, que es lo unico que las
     * dos firmas admiten.
     */
    private static Class<?>[] fundirExcepciones(Class<?>[] a, Class<?>[] b) {
        ArrayList<Class<?>> salida = new ArrayList<Class<?>>();
        Proxy.recogerCubiertas(a, b, salida);
        Proxy.recogerCubiertas(b, a, salida);
        return salida.toArray(new Class<?>[salida.size()]);
    }

    private static void recogerCubiertas(Class<?>[] desde, Class<?>[] contra,
            ArrayList<Class<?>> salida) {
        int i = 0;
        while (i < desde.length) {
            int j = 0;
            while (j < contra.length) {
                if (contra[j].isAssignableFrom(desde[i])) {
                    if (!salida.contains(desde[i])) {
                        salida.add(desde[i]);
                    }
                    j = contra.length;
                } else {
                    j = j + 1;
                }
            }
            i = i + 1;
        }
    }

    /**
     * Rechaza dos interfaces que declaran el mismo metodo con retornos que no se pueden reconciliar.
     *
     * <p>Retornos DISTINTOS no son un error por si mismos: un archivo de clase admite dos metodos
     * con el mismo nombre y los mismos parametros y distinto retorno, y eso es lo que se genera
     * para retornos covariantes (`Object valor()` y `String valor()` conviven). Lo que no se puede
     * es que ninguno de los retornos cubra a los demas -- `String` e `Integer` no tienen un metodo
     * que sirva para los dos --, y ahi el pedido es imposible y hay que decirlo antes de generar
     * nada. Un primitivo nunca cubre ni es cubierto, asi que basta con que aparezca uno en un grupo
     * de mas de uno para que el grupo sea imposible.
     */
    private static void verificarRetornos(ArrayList<Entrada> entradas) {
        HashMap<String, ArrayList<Entrada>> grupos = new HashMap<String, ArrayList<Entrada>>();
        int i = 0;
        while (i < entradas.size()) {
            Entrada e = entradas.get(i);
            ArrayList<Entrada> grupo = grupos.get(e.firmaCorta);
            if (grupo == null) {
                grupo = new ArrayList<Entrada>();
                grupos.put(e.firmaCorta, grupo);
            }
            grupo.add(e);
            i = i + 1;
        }
        java.util.Iterator<ArrayList<Entrada>> it = grupos.values().iterator();
        while (it.hasNext()) {
            ArrayList<Entrada> grupo = it.next();
            if (grupo.size() < 2) {
                continue;
            }
            ArrayList<Class<?>> sinCubrir = new ArrayList<Class<?>>();
            int k = 0;
            while (k < grupo.size()) {
                Class<?> nuevo = grupo.get(k).retorno;
                if (nuevo.isPrimitive()) {
                    throw new IllegalArgumentException("methods with same signature "
                            + grupo.get(0).firmaCorta + " but incompatible return types");
                }
                boolean cubierto = false;
                int t = 0;
                while (t < sinCubrir.size()) {
                    Class<?> viejo = sinCubrir.get(t);
                    if (nuevo.isAssignableFrom(viejo)) {
                        cubierto = true;
                        t = sinCubrir.size();
                    } else if (viejo.isAssignableFrom(nuevo)) {
                        sinCubrir.remove(t);
                    } else {
                        t = t + 1;
                    }
                }
                if (!cubierto) {
                    sinCubrir.add(nuevo);
                }
                k = k + 1;
            }
            if (sinCubrir.size() > 1) {
                throw new IllegalArgumentException("methods with same signature "
                        + grupo.get(0).firmaCorta + " but incompatible return types");
            }
        }
    }

    private static boolean todasPublicas(Class<?>[] interfaces) {
        int i = 0;
        while (i < interfaces.length) {
            if (!Modifier.isPublic(interfaces[i].getModifiers())) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * El paquete de la clase generada.
     *
     * <p>Con todas las interfaces publicas va a uno propio y la clase es publica. Con alguna que no
     * lo es, la clase TIENE que caer en el paquete de esa interfaz -- una interfaz de paquete no se
     * puede implementar desde afuera --, y por eso dos interfaces no publicas de paquetes distintos
     * no se pueden proxiar juntas: no existe un paquete donde las dos sean visibles.
     */
    private static String paquete(Class<?>[] interfaces, boolean publica) {
        if (publica) {
            return "com.sun.proxy";
        }
        String elegido = null;
        int i = 0;
        while (i < interfaces.length) {
            if (!Modifier.isPublic(interfaces[i].getModifiers())) {
                String p = interfaces[i].getPackageName();
                if (elegido == null) {
                    elegido = p;
                } else if (!elegido.equals(p)) {
                    throw new IllegalArgumentException(
                            "non-public interfaces from different packages");
                }
            }
            i = i + 1;
        }
        return elegido;
    }
}
