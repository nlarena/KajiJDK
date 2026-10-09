package java.lang.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// KajiLibrary's java.lang.module.Configuration -- a resolved graph of modules. KajiJDK resolves no
// modules, so the only Configuration is the empty one, and the resolve/resolveAndBind operations
// (which would build a graph from a module path) reject the request.
public final class Configuration {

    private static final Configuration EMPTY = new Configuration();

    private Configuration() {
    }

    /** {@return the empty configuration}. */
    public static Configuration empty() {
        return EMPTY;
    }

    public Configuration resolve(ModuleFinder before, ModuleFinder after, Collection<String> roots) {
        throw new UnsupportedOperationException("KajiJDK resolves no modules");
    }

    public Configuration resolveAndBind(ModuleFinder before, ModuleFinder after,
            Collection<String> roots) {
        throw new UnsupportedOperationException("KajiJDK resolves no modules");
    }

    public static Configuration resolve(ModuleFinder before, List<Configuration> parents,
            ModuleFinder after, Collection<String> roots) {
        throw new UnsupportedOperationException("KajiJDK resolves no modules");
    }

    public static Configuration resolveAndBind(ModuleFinder before, List<Configuration> parents,
            ModuleFinder after, Collection<String> roots) {
        throw new UnsupportedOperationException("KajiJDK resolves no modules");
    }

    /** The parent configurations. */
    public List<Configuration> parents() {
        return new ArrayList<Configuration>();
    }

    /** The resolved modules in this configuration. */
    public Set<ResolvedModule> modules() {
        return new HashSet<ResolvedModule>();
    }

    /** Finds a resolved module by name. */
    public Optional<ResolvedModule> findModule(String name) {
        return Optional.empty();
    }

    public String toString() {
        return "[]";
    }
}
