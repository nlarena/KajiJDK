package javax.tools;

// KajiLibrary's javax.tools.StandardLocation — the thirteen locations every standard file
// manager understands. Naming them as an enum (instead of leaving them strings) is what lets
// a compiler ask for "the class output" without agreeing on a spelling with its caller.
//
// The `implements JavaFileManager.Location` clause **is here**. The earlier note explained that it
// had been left out because the frozen javac could not name a type nested in another unit -- with the
// qualified name it was a hard error and with an `import` the clause was discarded **silently**, that
// is, a phantom superinterface. That was fixed, and the note said that the day it was, adding the
// clause would be enough: it was.
//
// `locationFor(String)` comes in with it, since its return type is that same nested type.
public enum StandardLocation implements JavaFileManager.Location {

    CLASS_OUTPUT,
    SOURCE_OUTPUT,
    CLASS_PATH,
    SOURCE_PATH,
    ANNOTATION_PROCESSOR_PATH,
    ANNOTATION_PROCESSOR_MODULE_PATH,
    PLATFORM_CLASS_PATH,
    NATIVE_HEADER_OUTPUT,
    MODULE_SOURCE_PATH,
    UPGRADE_MODULE_PATH,
    SYSTEM_MODULES,
    MODULE_PATH,
    PATCH_MODULE_PATH;

    /**
     * The location with that name, creating a new one if it is not one of the standard ones.
     *
     * <p>That it can **create** one is the point: locations are not a closed set, and a tool may
     * define its own. The created ones are remembered, so that two calls with the same name give the
     * **same** location -- otherwise a `Map` keyed by location would never find anything.
     */
    public static JavaFileManager.Location locationFor(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        StandardLocation[] standard = StandardLocation.values();
        int i = 0;
        while (i < standard.length) {
            if (standard[i].getName().equals(name)) {
                return standard[i];
            }
            i = i + 1;
        }
        synchronized (CREATED) {
            JavaFileManager.Location existing = CREATED.get(name);
            if (existing != null) {
                return existing;
            }
            JavaFileManager.Location fresh = new CustomLocation(name);
            CREATED.put(name, fresh);
            return fresh;
        }
    }

    // The locations `locationFor` invented. A map and not a list because the question is always
    // "the one with this name".
    private static final java.util.HashMap<String, JavaFileManager.Location> CREATED =
            new java.util.HashMap<String, JavaFileManager.Location>();

    public String getName() {
        return name();
    }

    // The three the compiler WRITES to.
    public boolean isOutputLocation() {
        return this == CLASS_OUTPUT || this == SOURCE_OUTPUT || this == NATIVE_HEADER_OUTPUT;
    }

    // The real JDK asks whether the name contains "MODULE"; without String.contains in the library,
    // the list is enumerated — which is the same answer, constant by constant.
    public boolean isModuleOrientedLocation() {
        return this == ANNOTATION_PROCESSOR_MODULE_PATH
            || this == MODULE_SOURCE_PATH
            || this == UPGRADE_MODULE_PATH
            || this == SYSTEM_MODULES
            || this == MODULE_PATH
            || this == PATCH_MODULE_PATH;
    }
}

// The location `StandardLocation.locationFor` invents for a name that is not one of the standard
// ones. It is top-level and package-private: the JDK has it nested and anonymous, and here a named
// class reads better and does not depend on capturing the environment.
//
// **It is not an output location.** An invented location cannot know, and saying that it is would
// make a tool try to write into it.
final class CustomLocation implements JavaFileManager.Location {

    private final String name;

    CustomLocation(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isOutputLocation() {
        return false;
    }

    public String toString() {
        return this.name;
    }
}
