package java.lang.module;

import java.util.HashSet;
import java.util.Set;

// KajiLibrary's java.lang.module.ResolvedModule -- a module in a resolved Configuration, together
// with the modules it reads. KajiJDK resolves nothing, so a Configuration holds no resolved modules
// and none is ever created; the type exists for the module-system surface.
public final class ResolvedModule {

    private final Configuration configuration;
    private final ModuleReference reference;

    ResolvedModule(Configuration configuration, ModuleReference reference) {
        this.configuration = configuration;
        this.reference = reference;
    }

    /** The configuration this resolved module is in. */
    public Configuration configuration() {
        return this.configuration;
    }

    /** The reference to the module. */
    public ModuleReference reference() {
        return this.reference;
    }

    /** The module name. */
    public String name() {
        return this.reference.descriptor().name();
    }

    /** The set of resolved modules that this module reads. */
    public Set<ResolvedModule> reads() {
        return new HashSet<ResolvedModule>();
    }

    public int hashCode() {
        return this.name().hashCode();
    }

    public boolean equals(Object ob) {
        return (ob instanceof ResolvedModule)
                && this.name().equals(((ResolvedModule) ob).name());
    }

    public String toString() {
        return this.name();
    }
}
