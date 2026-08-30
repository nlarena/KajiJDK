package java.lang.module;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

// The empty ModuleFinder KajiJDK hands back everywhere: no module path, no system modules.
final class KajiModuleFinder implements ModuleFinder {

    public Optional<ModuleReference> find(String name) {
        return Optional.empty();
    }

    public Set<ModuleReference> findAll() {
        return new HashSet<ModuleReference>();
    }
}
