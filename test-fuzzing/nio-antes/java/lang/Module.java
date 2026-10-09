package java.lang;

import java.io.InputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;
import java.util.HashSet;

// KajiLibrary's java.lang.Module — the run-time handle for a module. KajiJDK has no module system:
// classes are loaded from a flat class path, so every class belongs to the single, permissive
// UNNAMED module. That decides every answer here — it has no name, reads every other module, and
// exports and opens every package to everyone. {@link Class#getModule()} returns one of these.
//
// The reflective views of the module graph -- {@code getDescriptor()} and {@code getLayer()} --
// answer as an UNNAMED module must: with {@code null}. That is not a gap but the specified answer;
// an unnamed module has no descriptor and belongs to no layer. Everything an ordinary program asks
// of a module -- its name, its reads/exports/opens, its resources -- is here.
public final class Module implements AnnotatedElement {

    // The loader classes of this module come from; the unnamed module of a loader carries it so
    // that {@code getClassLoader()} is answerable. Null models the bootstrap loader.
    private final ClassLoader loader;

    // Package-private: only the class library mints these (one unnamed module per relevant loader).
    Module(ClassLoader loader) {
        this.loader = loader;
    }

    /** Whether this is a named module. The unnamed module never is. */
    public boolean isNamed() {
        return false;
    }

    /** The module name, or {@code null} for the unnamed module — which is the only kind here. */
    public String getName() {
        return null;
    }

    /**
     * {@return the descriptor of this module, or null}. An unnamed module has no descriptor, so this
     * is always null in KajiJDK (every class is in the unnamed module).
     */
    public ModuleDescriptor getDescriptor() {
        return null;
    }

    /**
     * {@return the layer that contains this module, or null}. The unnamed module belongs to no
     * layer, so this is always null in KajiJDK.
     */
    public ModuleLayer getLayer() {
        return null;
    }

    /** The loader that owns this unnamed module. */
    public ClassLoader getClassLoader() {
        return this.loader;
    }

    /** Whether restricted native methods may be called from this module. Unrestricted, here. */
    public boolean isNativeAccessEnabled() {
        return true;
    }

    // ---- the module graph: the unnamed module reads, exports and opens everything ----

    public boolean canRead(Module other) {
        return true;
    }

    public Module addReads(Module other) {
        return this;
    }

    public boolean isExported(String packageName, Module other) {
        return true;
    }

    public boolean isOpen(String packageName, Module other) {
        return true;
    }

    public boolean isExported(String packageName) {
        return true;
    }

    public boolean isOpen(String packageName) {
        return true;
    }

    public Module addExports(String packageName, Module other) {
        return this;
    }

    public Module addOpens(String packageName, Module other) {
        return this;
    }

    public Module addUses(Class<?> service) {
        return this;
    }

    public boolean canUse(Class<?> service) {
        return true;
    }

    /** The packages in this module. The unnamed module's set is not enumerated here — empty. */
    public Set<String> getPackages() {
        return new HashSet<String>();
    }

    // ---- resources ----

    /**
     * Opens a resource in this module for reading. KajiJDK serves classes, not co-located
     * resources, so there is nothing to open.
     *
     * @return {@code null}, always
     */
    public InputStream getResourceAsStream(String name) throws IOException {
        return null;
    }

    // ---- annotations (only a named module can be annotated; the unnamed one carries none) ----

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return null;
    }

    public Annotation[] getAnnotations() {
        return new Annotation[0];
    }

    public Annotation[] getDeclaredAnnotations() {
        return new Annotation[0];
    }

    public String toString() {
        return "unnamed module";
    }

    // Package-private internal the reference exposes: with no native-access restrictions to lift,
    // there is nothing to do.
    static void implAddEnableNativeAccessToAllUnnamed() {
    }
}
