package java.lang.module;

import java.net.URI;
import java.util.Optional;

// KajiLibrary's java.lang.module.ModuleReference -- a reference to a module: its descriptor, an
// optional location, and a way to open it for reading. KajiJDK resolves no modules, so no reference
// is ever created at runtime; the type exists for the module-system surface (e.g. ResolvedModule).
public abstract class ModuleReference {

    private final ModuleDescriptor descriptor;
    private final URI location;

    protected ModuleReference(ModuleDescriptor descriptor, URI location) {
        this.descriptor = descriptor;
        this.location = location;
    }

    /** The descriptor of the referenced module. */
    public final ModuleDescriptor descriptor() {
        return this.descriptor;
    }

    /** The location of the referenced module, if known. */
    public final Optional<URI> location() {
        return Optional.ofNullable(this.location);
    }

    /** Opens the module for reading. */
    public abstract ModuleReader open() throws java.io.IOException;
}
