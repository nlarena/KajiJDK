package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

// El mecanismo con el que Java carga implementaciones que no conoce en tiempo de compilacion.
//
// La idea: un programa declara que necesita una **interfaz de servicio** (`Codec`, `Driver`,
// `CharsetProvider`) y pregunta por sus implementaciones. Quien las provee no se registra en
// ningun lado del programa: deja un archivo de texto en su propio jar,
//
//     META-INF/services/com.ejemplo.Codec
//
// con el nombre completo de cada clase implementadora, una por linea. `ServiceLoader` recorre
// **todos** los jars del classpath buscando ese archivo, junta los nombres, y carga e instancia
// cada clase a demanda. Agregar un jar al classpath agrega proveedores; sacarlo los saca. Eso es
// todo, y es la razon por la que JDBC no tiene que conocer a ningun motor de base de datos.
//
// La carga es **perezosa y con memoria**: el iterador instancia recien cuando se le pide el
// elemento, y `reload()` es la unica forma de volver a mirar el classpath. Por eso un
// ServiceLoader se usa una vez y se tira, o se relee explicitamente.
//
// ---- el techo, con nombre --------------------------------------------------------------------
//
// **8 de los 9 miembros del contrato.** El que falta es `load(ModuleLayer, Class)`: pide
// `java.lang.ModuleLayer`, que no existe en esta biblioteca (ni el sistema de modulos que lo
// respalda).
//
// Y hay un techo mas importante que ese, que conviene decir sin vueltas: **el descubrimiento no
// encuentra nada todavia**. Toda la maquinaria esta --parsear el archivo de configuracion, cargar
// la clase por nombre, verificar que sea del subtipo correcto, instanciarla, cachear-- pero el
// primer paso, enumerar `META-INF/services/...` en el classpath, necesita
// `ClassLoader.getResources`, y nuestro `ClassLoader` tiene `loadClass` y nada de recursos.
//
// Esta aislado en **un** metodo, `nombresDeProveedores`, justamente para que el dia que existan
// los recursos sea eso lo unico que haya que escribir. Mientras tanto un `ServiceLoader` es una
// coleccion vacia bien formada: se itera, se le pide `findFirst`, se recarga, y no rompe nada.
public final class ServiceLoader<S> implements Iterable<S> {

    private final Class<S> service;
    private final ClassLoader loader;

    // Los nombres leidos del classpath, y las instancias ya creadas. `reload()` limpia los dos.
    private ArrayList<String> nombres;
    private ArrayList<S> instancias;

    private ServiceLoader(Class<S> service, ClassLoader loader) {
        if (service == null) {
            throw new NullPointerException("service");
        }
        this.service = service;
        this.loader = loader;
        this.reload();
    }

    // ---- fabricas ---------------------------------------------------------------------------------

    // El cargador de contexto no existe en esta biblioteca, asi que se usa el del sistema.
    public static <S> ServiceLoader<S> load(Class<S> service) {
        return new ServiceLoader<S>(service, ClassLoader.getSystemClassLoader());
    }

    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        return new ServiceLoader<S>(service, loader);
    }

    // Solo los proveedores **instalados**: los de la plataforma, no los de la aplicacion.
    //
    // La distincion es real y no cosmetica -- es la que evita que un jar cualquiera del classpath
    // sustituya una pieza del JDK.
    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        return new ServiceLoader<S>(service, ClassLoader.getPlatformClassLoader());
    }

    // ---- descubrimiento ----------------------------------------------------------------------------

    /**
     * Los nombres de clase declarados en {@code META-INF/services/<servicio>} a lo largo del
     * classpath.
     *
     * <p>**Este es el techo.** Enumerar ese recurso en cada elemento del classpath pide
     * `ClassLoader.getResources(String)`, que esta biblioteca no tiene: nuestro `ClassLoader`
     * sabe cargar clases y nada mas. Devuelve la lista vacia, y por eso un ServiceLoader no
     * encuentra proveedores.
     *
     * <p>Todo lo demas de esta clase esta escrito y funciona sobre lo que esto devuelva; el dia
     * que existan los recursos, el cambio es de este metodo para adentro.
     */
    private ArrayList<String> nombresDeProveedores() {
        return new ArrayList<String>();
    }

    // Parsea una linea del archivo de configuracion (§ServiceLoader): se corta en el `#`, se
    // recortan los espacios, y una linea vacia no aporta nada.
    //
    // Package-private y no privado para poder ejercitarlo sin recursos, que es lo unico que se
    // puede probar del descubrimiento hoy.
    static String parsearLinea(String linea) {
        int comentario = linea.indexOf('#');
        String s = linea;
        if (comentario >= 0) {
            s = s.substring(0, comentario);
        }
        s = s.trim();
        if (s.length() == 0) {
            return null;
        }
        // Un nombre de clase valido: identificadores separados por puntos.
        int i = 0;
        boolean arranque = true;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '.') {
                if (arranque) {
                    throw new ServiceConfigurationError("Illegal provider-class name: " + s);
                }
                arranque = true;
            } else {
                boolean ok = arranque ? Character.isJavaIdentifierStart(c)
                        : Character.isJavaIdentifierPart(c);
                if (!ok) {
                    throw new ServiceConfigurationError("Illegal provider-class name: " + s);
                }
                arranque = false;
            }
            i = i + 1;
        }
        if (arranque) {
            throw new ServiceConfigurationError("Illegal provider-class name: " + s);
        }
        return s;
    }

    // Carga e instancia el proveedor numero `i`, si todavia no se hizo.
    //
    // Las tres cosas que pueden salir mal --la clase no esta, no es del subtipo, no se puede
    // instanciar-- se reportan como ServiceConfigurationError y no como la excepcion original: el
    // que llama pidio "las implementaciones de X", y que una este mal declarada es un error de
    // **configuracion**, no del codigo que pregunta.
    private S instanciar(int i) {
        while (this.instancias.size() <= i) {
            this.instancias.add(null);
        }
        S ya = this.instancias.get(i);
        if (ya != null) {
            return ya;
        }
        String nombre = this.nombres.get(i);
        Class<?> c;
        try {
            c = Class.forName(nombre, false, this.loader);
        } catch (ClassNotFoundException e) {
            throw new ServiceConfigurationError(
                    this.service.getName() + ": Provider " + nombre + " not found");
        }
        if (!this.service.isAssignableFrom(c)) {
            throw new ServiceConfigurationError(
                    this.service.getName() + ": Provider " + nombre + " not a subtype");
        }
        Object o;
        try {
            o = c.newInstance();
        } catch (InstantiationException e) {
            throw new ServiceConfigurationError(
                    this.service.getName() + ": Provider " + nombre + " could not be instantiated",
                    e);
        } catch (IllegalAccessException e) {
            throw new ServiceConfigurationError(
                    this.service.getName() + ": Provider " + nombre + " could not be instantiated",
                    e);
        }
        S s = this.service.cast(o);
        this.instancias.set(i, s);
        return s;
    }

    // ---- lo que se le pide -----------------------------------------------------------------------

    public Iterator<S> iterator() {
        return new ServiceItr<S>(this);
    }

    int cuantos() {
        return this.nombres.size();
    }

    S dame(int i) {
        return this.instanciar(i);
    }

    Class<S> servicio() {
        return this.service;
    }

    /**
     * Los proveedores como Stream, cada uno envuelto en un {@link Provider}.
     *
     * <p>La envoltura no es decoracion: deja preguntar por la **clase** del proveedor sin
     * instanciarlo. Es lo que permite filtrar por tipo y crear solo el que se va a usar --
     * `loader.stream().filter(p -> p.type() == Rapido.class).findFirst().map(Provider::get)`.
     *
     * <p>Divergencia: este es **ansioso**, junta todo antes de devolver.
     */
    public Stream<Provider<S>> stream() {
        int n = this.nombres.size();
        Object[] a = new Object[n];
        int i = 0;
        while (i < n) {
            a[i] = new ServiceProvider<S>(this, i);
            i = i + 1;
        }
        return (Stream<Provider<S>>) Stream.of(a);
    }

    // El primero, si hay alguno.
    public Optional<S> findFirst() {
        Iterator<S> it = this.iterator();
        if (it.hasNext()) {
            return Optional.of(it.next());
        }
        return Optional.empty();
    }

    // Vuelve a mirar el classpath y tira las instancias que hubiera creado.
    //
    // Es la unica forma de que un ServiceLoader vea un proveedor que aparecio despues: la carga es
    // perezosa pero el **descubrimiento** se hace una vez.
    public void reload() {
        this.nombres = this.nombresDeProveedores();
        this.instancias = new ArrayList<S>();
    }

    public String toString() {
        return "java.util.ServiceLoader[" + this.service.getName() + "]";
    }

    /**
     * Un proveedor todavia **no instanciado**: su clase, y la forma de crearlo.
     *
     * <p>Existe para separar las dos preguntas que `Iterator` mezcla: "que hay" y "dame uno".
     */
    public interface Provider<S> {

        Class<? extends S> type();

        S get();
    }
}

// El iterador perezoso: instancia recien en `next()`. Top-level package-private, no anidado, por
// el miscompilado de una clase anidada dentro de una generica (#13).
final class ServiceItr<S> implements Iterator<S> {

    private final ServiceLoader<S> loader;
    private int i;

    ServiceItr(ServiceLoader<S> loader) {
        this.loader = loader;
    }

    public boolean hasNext() {
        return this.i < this.loader.cuantos();
    }

    public S next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        S s = this.loader.dame(this.i);
        this.i = this.i + 1;
        return s;
    }
}

final class ServiceProvider<S> implements ServiceLoader.Provider<S> {

    private final ServiceLoader<S> loader;
    private final int i;

    ServiceProvider(ServiceLoader<S> loader, int i) {
        this.loader = loader;
        this.i = i;
    }

    // Se apoya en la instancia porque no hay forma de saber la clase sin cargarla; en el JDK el
    // nombre alcanza porque el modulo declara el tipo. Queda dicho: aca `type()` instancia.
    public Class<? extends S> type() {
        return (Class<? extends S>) this.loader.dame(this.i).getClass();
    }

    public S get() {
        return this.loader.dame(this.i);
    }
}
