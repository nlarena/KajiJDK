package java.lang.module;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

// KajiLibrary's java.lang.module.ModuleFinder -- locates module references. KajiJDK has no module
// path and no system modules, so every finder it produces is empty (find returns empty, findAll
// returns an empty set).
public interface ModuleFinder {

    /** Finds the module reference for {@code name}, if any. */
    Optional<ModuleReference> find(String name);

    /** All module references this finder can locate. */
    Set<ModuleReference> findAll();

    /** A finder over the system (run-time image) modules. Empty in KajiJDK. */
    static ModuleFinder ofSystem() {
        return new KajiModuleFinder();
    }

    /** A finder over the given directory/file paths. Empty in KajiJDK (no module path). */
    static ModuleFinder of(Path... entries) {
        return new KajiModuleFinder();
    }

    /** A finder that composes the given finders in order. Empty in KajiJDK. */
    static ModuleFinder compose(ModuleFinder... finders) {
        return new KajiModuleFinder();
    }
}
