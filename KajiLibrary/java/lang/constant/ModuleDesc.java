package java.lang.constant;

// A nominal descriptor for a module. Unlike a package, a module name has no internal form —
// the class file stores it as-is — so this one is a single name and nothing else.
public interface ModuleDesc {

    public static ModuleDesc of(String name) {
        return new ConstantModuleDesc(name);
    }

    String name();

    boolean equals(Object o);
}

// See `PackageDesc` for why the implementation shares the file.
class ConstantModuleDesc implements ModuleDesc {

    private final String name;

    ConstantModuleDesc(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public boolean equals(Object o) {
        boolean same = false;
        if (o instanceof ConstantModuleDesc) {
            ConstantModuleDesc other = (ConstantModuleDesc) o;
            same = name.equals(other.name);
        }
        return same;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        return "ModuleDesc[" + name + "]";
    }
}
