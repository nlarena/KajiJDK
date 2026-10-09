package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

// The mechanism with which Java loads implementations it does not know at compile time.
//
// The idea: a program declares that it needs a **service interface** (`Codec`, `Driver`,
// `CharsetProvider`) and asks for its implementations. Whoever provides them registers nowhere in
// the program: they leave a text file in their own jar,
//
//     META-INF/services/com.example.Codec
//
// with the full name of each implementing class, one per line. `ServiceLoader` walks **every** jar
// on the class path looking for that file, gathers the names, and loads and instantiates each class
// on demand. Adding a jar to the class path adds providers; taking it away removes them. That is
// all, and it is the reason JDBC does not have to know any database engine.
//
// The loading is **lazy and remembered**: the iterator instantiates only when the element is asked
// for, and `reload()` is the only way of looking at the class path again. That is why a
// ServiceLoader is used once and thrown away, or explicitly re-read.
//
// ---- the ceiling, by name ---------------------------------------------------------------------
//
// **All 9 members of the contract.** This note used to say `load(ModuleLayer, Class)` was missing
// because `java.lang.ModuleLayer` did not exist; the class exists and the method is declared below.
//
// What is inner, and is the ceiling worth stating plainly: **discovery finds nothing yet**. All the
// machinery is here --parsing the configuration file, loading the class by name, checking it is of
// the right subtype, instantiating it, caching-- but the first step, enumerating
// `META-INF/services/...` along the class path, is not wired. `ClassLoader.getResources` does exist
// now; what serves no resources is the built-in loaders' `findResources`, because KajiJDK serves
// classes off the class path and not files sitting beside them.
//
// It is isolated in **one** method, `providerNames`, precisely so that wiring it is the only
// thing to write. In the meantime a `ServiceLoader` is a well-formed empty collection: it iterates,
// it answers `findFirst`, it reloads, and it breaks nothing.
public final class ServiceLoader<S> implements Iterable<S> {

    private final Class<S> service;
    private final ClassLoader loader;

    // The names read from the class path, and the instances already created. `reload()` clears
    // both.
    private ArrayList<String> names;
    private ArrayList<S> instances;

    private ServiceLoader(Class<S> service, ClassLoader loader) {
        if (service == null) {
            throw new NullPointerException("service");
        }
        this.service = service;
        this.loader = loader;
        this.reload();
    }

    // ---- factories ------------------------------------------------------------------------------

    // The context loader does not exist in this library, so the system one is used.
    public static <S> ServiceLoader<S> load(Class<S> service) {
        return new ServiceLoader<S>(service, ClassLoader.getSystemClassLoader());
    }

    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        return new ServiceLoader<S>(service, loader);
    }

    /**
     * The providers of **that layer's modules** and its ancestors.
     *
     * <p>This form has a rule that surprises and that is respected as it stands: it looks **only**
     * in the layer's modules. A provider from the class path does **not** show up, even though
     * `load(service)` does find it. It is not a limitation, it is the point of the method -- it is
     * for asking "what does this layer give", and answering with what is outside would be answering
     * a different question.
     *
     * <p>In this library there are **no named modules**: everything lives in the unnamed module, and
     * a `ModuleLayer` never contains any. So the result is always empty, and that **is** the correct
     * answer under the rule above, not a stub: asking for the providers of a layer with no modules
     * has to give zero. If one day there are inner modules, the loop below walks them unchanged.
     */
    public static <S> ServiceLoader<S> load(ModuleLayer layer, Class<S> service) {
        if (layer == null || service == null) {
            throw new NullPointerException();
        }
        ClassLoader loader = null;
        Iterator<Module> it = layer.modules().iterator();
        if (it.hasNext()) {
            loader = it.next().getClassLoader();
        }
        return new ServiceLoader<S>(service, loader);
    }

    // Only the **installed** providers: the platform's, not the application's.
    //
    // The distinction is inner and not cosmetic -- it is what stops any old jar on the class path
    // from replacing a piece of the JDK.
    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        return new ServiceLoader<S>(service, ClassLoader.getPlatformClassLoader());
    }

    // ---- discovery ------------------------------------------------------------------------------

    /**
     * The class names declared in {@code META-INF/services/<service>} along the class path.
     *
     * <p>**This is the ceiling.** It returns the empty list, and that is why a ServiceLoader finds
     * no providers. The note here used to blame a missing `ClassLoader.getResources(String)`; that
     * method exists. What is missing is the call: this method does not make it, and the built-in
     * loaders' `findResources` serves nothing anyway, because KajiJDK serves classes off the class
     * path and not files sitting beside them.
     *
     * <p>Everything else in this class is written and works over whatever this returns; the change
     * is from this method inwards.
     */
    private ArrayList<String> providerNames() {
        return new ArrayList<String>();
    }

    // It parses a line of the configuration file (§ServiceLoader): it is cut at the `#`, the spaces
    // are trimmed, and an empty line contributes nothing.
    //
    // Package-private and not private so it can be exercised without resources, which is the only
    // part of discovery that can be tested today.
    static String parseLine(String line) {
        int commentText = line.indexOf('#');
        String s = line;
        if (commentText >= 0) {
            s = s.substring(0, commentText);
        }
        s = s.trim();
        if (s.length() == 0) {
            return null;
        }
        // A valid class name: identifiers separated by dots.
        int i = 0;
        boolean startup = true;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '.') {
                if (startup) {
                    throw new ServiceConfigurationError("Illegal provider-class name: " + s);
                }
                startup = true;
            } else {
                boolean ok = startup ? Character.isJavaIdentifierStart(c)
                        : Character.isJavaIdentifierPart(c);
                if (!ok) {
                    throw new ServiceConfigurationError("Illegal provider-class name: " + s);
                }
                startup = false;
            }
            i = i + 1;
        }
        if (startup) {
            throw new ServiceConfigurationError("Illegal provider-class name: " + s);
        }
        return s;
    }

    // It loads and instantiates provider number `i`, if that has not been done yet.
    //
    // The three things that can go wrong --the class is not there, it is not of the subtype, it
    // cannot be instantiated-- are reported as ServiceConfigurationError and not as the original
    // exception: the caller asked for "X's implementations", and one of them being badly declared is
    // a **configuration** error, not one in the code that asks.
    private S instantiate(int i) {
        while (this.instances.size() <= i) {
            this.instances.add(null);
        }
        S cached = this.instances.get(i);
        if (cached != null) {
            return cached;
        }
        String name = this.names.get(i);
        Class<?> c;
        try {
            c = Class.forName(name, false, this.loader);
        } catch (ClassNotFoundException e) {
            throw new ServiceConfigurationError(
                    this.service.getName() + ": Provider " + name + " not found");
        }
        if (!this.service.isAssignableFrom(c)) {
            throw new ServiceConfigurationError(
                    this.service.getName() + ": Provider " + name + " not a subtype");
        }
        Object o;
        try {
            o = c.newInstance();
        } catch (InstantiationException e) {
            throw new ServiceConfigurationError(
                    this.service.getName() + ": Provider " + name + " could not be instantiated",
                    e);
        } catch (IllegalAccessException e) {
            throw new ServiceConfigurationError(
                    this.service.getName() + ": Provider " + name + " could not be instantiated",
                    e);
        }
        S s = this.service.cast(o);
        this.instances.set(i, s);
        return s;
    }

    // ---- what is asked of it ----------------------------------------------------------------------

    public Iterator<S> iterator() {
        return new ServiceItr<S>(this);
    }

    int howMany() {
        return this.names.size();
    }

    S providerAt(int i) {
        return this.instantiate(i);
    }

    Class<S> serviceType() {
        return this.service;
    }

    /**
     * The providers as a Stream, each wrapped in a {@link Provider}.
     *
     * <p>The wrapper is not decoration: it allows asking for the provider's **class** without
     * instantiating it. It is what makes it possible to filter by type and create only the one that
     * will be used --
     * `loader.stream().filter(p -> p.type() == Fast.class).findFirst().map(Provider::get)`.
     *
     * <p>Divergence: this one is **eager**, it gathers everything before returning.
     */
    public Stream<Provider<S>> stream() {
        int n = this.names.size();
        Object[] a = new Object[n];
        int i = 0;
        while (i < n) {
            a[i] = new ServiceProvider<S>(this, i);
            i = i + 1;
        }
        return (Stream<Provider<S>>) Stream.of(a);
    }

    // The first one, if there is any.
    public Optional<S> findFirst() {
        Iterator<S> it = this.iterator();
        if (it.hasNext()) {
            return Optional.of(it.next());
        }
        return Optional.empty();
    }

    // It looks at the class path again and throws away whatever instances it had created.
    //
    // It is the only way for a ServiceLoader to see a provider that turned up later: the loading is
    // lazy but the **discovery** is done once.
    public void reload() {
        this.names = this.providerNames();
        this.instances = new ArrayList<S>();
    }

    public String toString() {
        return "java.util.ServiceLoader[" + this.service.getName() + "]";
    }

    /**
     * A provider **not instantiated** yet: its class, and the way to create it.
     *
     * <p>It exists to separate the two questions `Iterator` mixes: "what is there" and "give me
     * one".
     */
    public interface Provider<S> {

        Class<? extends S> type();

        S get();
    }
}

// The lazy iterator: it instantiates only at `next()`. Top-level package-private, not nested,
// because of the miscompilation of a class nested inside a generic one (#13).
final class ServiceItr<S> implements Iterator<S> {

    private final ServiceLoader<S> loader;
    private int i;

    ServiceItr(ServiceLoader<S> loader) {
        this.loader = loader;
    }

    public boolean hasNext() {
        return this.i < this.loader.howMany();
    }

    public S next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        S s = this.loader.providerAt(this.i);
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

    // It leans on the instance because there is no way of knowing the class without loading it; in
    // the JDK the name is enough because the module declares the type. Said plainly: here `type()`
    // instantiates.
    public Class<? extends S> type() {
        return (Class<? extends S>) this.loader.providerAt(this.i).getClass();
    }

    public S get() {
        return this.loader.providerAt(this.i);
    }
}
