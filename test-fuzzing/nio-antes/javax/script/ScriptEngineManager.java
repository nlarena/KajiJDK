package javax.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * KajiLibrary's javax.script.ScriptEngineManager -- the one that finds engines and finds none.
 *
 * <p>It does three things. It discovers the {@link ScriptEngineFactory}s there are on the class
 * path via {@link ServiceLoader}; it searches among them by short name, by file extension or by
 * MIME type; and it keeps the global {@link Bindings} shared by all the engines that come out of
 * here -- that last one is the reason the manager exists and a bare `ServiceLoader` is not enough.
 *
 * <h2>What this is going to return in practice</h2>
 *
 * <p><b>Null.</b> {@code getEngineByName("js")} returns null, and {@code getEngineFactories()}
 * returns an empty list. It is not a limitation of KajiLibrary: **a real JDK 25 does exactly the
 * same**. Nashorn, the only engine the JDK shipped, was deprecated in 11 and removed in 15; since
 * then `java.scripting` is the API with no implementation inside, and a freshly built
 * `ScriptEngineManager` has nothing to offer unless the class path brings a third-party engine. It
 * is checked by running the same program with the real `java`.
 *
 * <p>Our ceiling is one step lower and it is as well to say so all the same: here a third-party
 * engine would not be found even if it were properly declared on the class path. The difference is
 * not visible from outside while there are no engines, but it exists, and there are **two**
 * independent brakes -- fixing only one is not enough:
 *
 * <ul>
 *   <li>{@code ServiceLoader} does not read `META-INF/services`: its discovery step returns the
 *       empty list without consulting any resource. All the rest of that class --parsing the file,
 *       instantiating, `ServiceConfigurationError`-- is written and works on whatever that step
 *       returns.
 *   <li>{@link ClassLoader}'s resource lookup exists as API but serves nothing: {@code
 *       getSystemResource("java/lang/Object.class")} returns null, even though it is the class the
 *       program started with. It is not that the resource is not on the class path; it is that the
 *       built-in loaders' `findResource`/`findResources` do not look.
 * </ul>
 *
 * <p>Checked with the same program against both VMs, with a real `META-INF/services` on the class
 * path: JDK 25's `java` finds the resource and the provider, and we count zero in both. (The note
 * said our `ServiceLoader`'s comment blamed a missing `ClassLoader.getResources`; that comment has
 * since been corrected and names the built-in loaders' `findResources`, which is the second brake
 * above.)
 *
 * <p>What does work end to end is the manual registration --
 * {@link #registerEngineName(String, ScriptEngineFactory)} and its two siblings --, which does not
 * depend on discovery: whoever has a factory in hand associates it with a name and the search finds
 * it. The manual associations are looked at **before** the discovered ones.
 */
public class ScriptEngineManager {

    /** The discovered factories. Discovery order. */
    private final Set<ScriptEngineFactory> engineSpis;

    /** Short name -&gt; factory, registered by hand. */
    private final HashMap<String, ScriptEngineFactory> nameAssociations;

    /** Extension -&gt; factory, registered by hand. */
    private final HashMap<String, ScriptEngineFactory> extensionAssociations;

    /** MIME type -&gt; factory, registered by hand. */
    private final HashMap<String, ScriptEngineFactory> mimeTypeAssociations;

    /** The global scope given to every engine that comes out of here. */
    private Bindings globalScope;

    /**
     * Discovers with the current thread's context loader.
     *
     * <p>That loader is the one a container changes per application, and that is why it is the
     * right one when the manager is built from inside one.
     */
    public ScriptEngineManager() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Discovers with `loader`.
     *
     * <p>With a null `loader` only the engines installed with the platform are looked for, which in
     * a modern JDK are zero.
     */
    public ScriptEngineManager(ClassLoader loader) {
        engineSpis = new LinkedHashSet<ScriptEngineFactory>();
        nameAssociations = new HashMap<String, ScriptEngineFactory>();
        extensionAssociations = new HashMap<String, ScriptEngineFactory>();
        mimeTypeAssociations = new HashMap<String, ScriptEngineFactory>();
        globalScope = new SimpleBindings();
        initEngines(loader);
    }

    /**
     * Gathers the factories the `ServiceLoader` can give.
     *
     * <p>A broken factory does not bring discovery down: it is skipped and the rest go on, which is
     * the only reasonable thing when somebody else puts the class path together.
     */
    private void initEngines(ClassLoader loader) {
        try {
            ServiceLoader<ScriptEngineFactory> sl;
            if (loader != null) {
                sl = ServiceLoader.load(ScriptEngineFactory.class, loader);
            } else {
                sl = ServiceLoader.loadInstalled(ScriptEngineFactory.class);
            }
            for (ScriptEngineFactory spi : sl) {
                if (spi != null) {
                    engineSpis.add(spi);
                }
            }
        } catch (ServiceConfigurationError err) {
            // A badly declared provider cannot leave the manager unbuilt.
        } catch (RuntimeException exp) {
            // Likewise for a factory that blows up in its own constructor.
        }
    }

    /**
     * Changes the global scope.
     *
     * @throws IllegalArgumentException if `bindings` is null -- careful, it is not an NPE
     */
    public void setBindings(Bindings bindings) {
        if (bindings == null) {
            throw new IllegalArgumentException("Global scope cannot be null.");
        }
        globalScope = bindings;
    }

    /** The global scope. Never null. */
    public Bindings getBindings() {
        return globalScope;
    }

    /**
     * Defines `key` in the global scope.
     *
     * @throws NullPointerException if `key` is null
     * @throws IllegalArgumentException if `key` is empty
     */
    public void put(String key, Object value) {
        globalScope.put(key, value);
    }

    /**
     * Whatever `key` is worth in the global scope.
     *
     * @throws NullPointerException if `key` is null
     * @throws IllegalArgumentException if `key` is empty
     */
    public Object get(String key) {
        return globalScope.get(key);
    }

    /**
     * An engine whose short name is `shortName`, or null if there is none.
     *
     * @throws NullPointerException if `shortName` is null
     */
    public ScriptEngine getEngineByName(String shortName) {
        Objects.requireNonNull(shortName);
        return find(shortName, nameAssociations, KEY_NAMES);
    }

    /**
     * An engine that serves the extension `extension`, or null.
     *
     * @throws NullPointerException if `extension` is null
     */
    public ScriptEngine getEngineByExtension(String extension) {
        Objects.requireNonNull(extension);
        return find(extension, extensionAssociations, KEY_EXTENSIONS);
    }

    /**
     * An engine that serves the MIME type `mimeType`, or null.
     *
     * @throws NullPointerException if `mimeType` is null
     */
    public ScriptEngine getEngineByMimeType(String mimeType) {
        Objects.requireNonNull(mimeType);
        return find(mimeType, mimeTypeAssociations, KEY_MIME_TYPES);
    }

    /** Which of a factory's three lists to look at. Internal, not part of the contract. */
    private static final int KEY_NAMES = 0;
    private static final int KEY_EXTENSIONS = 1;
    private static final int KEY_MIME_TYPES = 2;

    /** The keys `spi` publishes for the criterion asked for. */
    private static List<String> keysOf(ScriptEngineFactory spi, int criterion) {
        if (criterion == KEY_NAMES) {
            return spi.getNames();
        } else if (criterion == KEY_EXTENSIONS) {
            return spi.getExtensions();
        }
        return spi.getMimeTypes();
    }

    /**
     * First what was registered by hand, then what was discovered; the first one that serves.
     *
     * <p>That the manual one wins is not a detail: registering is the way of saying "for this name
     * I want this one", and it would be useless if a discovered one could get ahead of it.
     */
    private ScriptEngine find(String key, Map<String, ScriptEngineFactory> registered,
            int criterion) {
        ScriptEngineFactory factory = registered.get(key);
        if (factory != null) {
            ScriptEngine engine = engineOf(factory);
            if (engine != null) {
                return engine;
            }
        }
        for (ScriptEngineFactory spi : engineSpis) {
            List<String> keys;
            try {
                keys = keysOf(spi, criterion);
            } catch (RuntimeException exp) {
                continue;
            }
            if (keys == null) {
                continue;
            }
            for (String c : keys) {
                if (key.equals(c)) {
                    ScriptEngine engine = engineOf(spi);
                    if (engine != null) {
                        return engine;
                    }
                }
            }
        }
        return null;
    }

    /**
     * An engine from that factory, already connected to the manager's global scope, or null if the
     * factory failed.
     *
     * <p>Connecting the global scope here is all the value the manager adds: two engines asked of
     * the same manager share what was put with {@link #put(String, Object)}.
     */
    private ScriptEngine engineOf(ScriptEngineFactory spi) {
        try {
            ScriptEngine engine = spi.getScriptEngine();
            if (engine != null) {
                engine.setBindings(getBindings(), ScriptContext.GLOBAL_SCOPE);
            }
            return engine;
        } catch (RuntimeException exp) {
            return null;
        }
    }

    /**
     * The discovered factories, in an immutable list.
     *
     * <p>It does not include the ones registered by hand, just like the original: `register*`
     * associates a key, it does not add a provider.
     */
    public List<ScriptEngineFactory> getEngineFactories() {
        return List.copyOf(new ArrayList<ScriptEngineFactory>(engineSpis));
    }

    /**
     * Associates the short name `name` with `factory`.
     *
     * @throws NullPointerException if either is null
     */
    public void registerEngineName(String name, ScriptEngineFactory factory) {
        register(nameAssociations, name, factory);
    }

    /**
     * Associates the MIME type `type` with `factory`.
     *
     * @throws NullPointerException if either is null
     */
    public void registerEngineMimeType(String type, ScriptEngineFactory factory) {
        register(mimeTypeAssociations, type, factory);
    }

    /**
     * Associates the extension `extension` with `factory`.
     *
     * @throws NullPointerException if either is null
     */
    public void registerEngineExtension(String extension, ScriptEngineFactory factory) {
        register(extensionAssociations, extension, factory);
    }

    /** The three registrations are the same one with a different map. */
    private static void register(Map<String, ScriptEngineFactory> map, String key,
            ScriptEngineFactory factory) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(factory);
        map.put(key, factory);
    }
}
