package java.util.prefs;

// Where the two roots come from.
//
// It is the package's only extension point: `Preferences.userRoot()` and `Preferences.systemRoot()`
// do nothing but ask the factory. Whoever wants to store the preferences in a database, on a server
// or in memory implements this interface and touches nothing else.
//
// The two roots are asked for separately and are expected to be **stable**: calling twice has to
// give the same object, because `AbstractPreferences.isUserNode()` compares the root by identity.
public interface PreferencesFactory {

    // The root of the system tree, shared by every user of the machine.
    Preferences systemRoot();

    // The root of the tree of the user running the VM.
    Preferences userRoot();
}
