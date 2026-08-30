package java.lang;

import java.lang.module.Configuration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

// KajiLibrary's java.lang.ModuleLayer -- a layer of modules over a Configuration. KajiJDK has no
// module system: the only layers are the empty layer and the boot layer (also empty), and the
// operations that would instantiate modules from a Configuration reject the request. The graph
// queries answer "empty".
public final class ModuleLayer {

    private static final ModuleLayer EMPTY = new ModuleLayer();

    private ModuleLayer() {
    }

    // ---- instantiation (unsupported: KajiJDK defines no modules) ----

    public ModuleLayer defineModulesWithOneLoader(Configuration cf, ClassLoader parentLoader) {
        throw new UnsupportedOperationException("KajiJDK defines no modules");
    }

    public ModuleLayer defineModulesWithManyLoaders(Configuration cf, ClassLoader parentLoader) {
        throw new UnsupportedOperationException("KajiJDK defines no modules");
    }

    public ModuleLayer defineModules(Configuration cf, Function<String, ClassLoader> clf) {
        throw new UnsupportedOperationException("KajiJDK defines no modules");
    }

    public static Controller defineModulesWithOneLoader(Configuration cf,
            List<ModuleLayer> parentLayers, ClassLoader parentLoader) {
        throw new UnsupportedOperationException("KajiJDK defines no modules");
    }

    public static Controller defineModulesWithManyLoaders(Configuration cf,
            List<ModuleLayer> parentLayers, ClassLoader parentLoader) {
        throw new UnsupportedOperationException("KajiJDK defines no modules");
    }

    public static Controller defineModules(Configuration cf, List<ModuleLayer> parentLayers,
            Function<String, ClassLoader> clf) {
        throw new UnsupportedOperationException("KajiJDK defines no modules");
    }

    // ---- graph queries ----

    /** The configuration this layer is derived from. */
    public Configuration configuration() {
        return Configuration.empty();
    }

    /** The parent layers, nearest first. */
    public List<ModuleLayer> parents() {
        return new ArrayList<ModuleLayer>();
    }

    /** The modules in this layer. */
    public Set<Module> modules() {
        return new HashSet<Module>();
    }

    /** Finds a module by name in this layer or its parents. */
    public Optional<Module> findModule(String name) {
        return Optional.empty();
    }

    /** The class loader for the module of the given name. */
    public ClassLoader findLoader(String name) {
        return ClassLoader.getSystemClassLoader();
    }

    public String toString() {
        return "empty";
    }

    /** {@return the empty layer}. */
    public static ModuleLayer empty() {
        return EMPTY;
    }

    /** {@return the boot layer} -- empty in KajiJDK, which loads no modules. */
    public static ModuleLayer boot() {
        return EMPTY;
    }

    /**
     * A controller for a module layer, handing its creator the ability to update the layer's
     * modules (add reads/exports/opens, enable native access). KajiJDK creates no layers through the
     * static factories, so no controller is ever produced.
     */
    public static final class Controller {

        private final ModuleLayer layer;

        private Controller(ModuleLayer layer) {
            this.layer = layer;
        }

        /** The controlled layer. */
        public ModuleLayer layer() {
            return this.layer;
        }

        public Controller addReads(Module source, Module target) {
            return this;
        }

        public Controller addExports(Module source, String pn, Module target) {
            return this;
        }

        public Controller addOpens(Module source, String pn, Module target) {
            return this;
        }

        public Controller enableNativeAccess(Module target) {
            return this;
        }
    }
}
