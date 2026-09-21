package java.util.prefs;

// The default factory: two {@link FilePreferences}, one per tree.
//
// WHERE. The JDK uses the registry on Windows and `~/.java/.userPrefs` on POSIX. Here there is
// neither: there is no access to the registry, and `user.home`, `user.dir` and `java.io.tmpdir` are
// `null` --as is all of `System.getenv`-- so there is no user directory to name. What does work is
// the filesystem through paths **relative** to the process's working directory, and that is where it
// goes: `.java/.userPrefs` and `.java/.systemPrefs`. It is the same tree as the JDK's under POSIX
// minus the home prefix, which is what we do not have.
//
// It can be moved with `java.util.prefs.userRoot` and `java.util.prefs.systemRoot`, which are the
// same properties the JDK recognises and with the same meaning: the **parent** directory inside
// which `.userPrefs` or `.systemPrefs` is created.
//
// The two roots are built once and kept because {@link AbstractPreferences#isUserNode} compares the
// root by identity: a factory that returned a new object on every call would make `isUserNode()`
// give `false` over the user tree.
final class FilePreferencesFactory implements PreferencesFactory {

    private final FilePreferences user;
    private final FilePreferences system;

    FilePreferencesFactory() {
        user = new FilePreferences(parent("java.util.prefs.userRoot") + "/.userPrefs");
        system = new FilePreferences(parent("java.util.prefs.systemRoot") + "/.systemPrefs");
    }

    private static String parent(String property) {
        String v = System.getProperty(property);
        return (v == null || v.length() == 0) ? ".java" : v;
    }

    public Preferences userRoot() {
        return user;
    }

    public Preferences systemRoot() {
        return system;
    }
}
