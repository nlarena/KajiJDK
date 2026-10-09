package java.beans;

import java.util.HashMap;
import java.util.Map;

// The register of which editor to use for each property type. It looks in three places and in this
// order: what was registered by hand, the `<Type>Editor` class next to the type, and that same class
// in the search path's packages.
//
// The editors for the primitive types the JDK ships out of the box (sun.beans.editors.*) are NOT
// here: they are implementation classes, not API, and this tree does not have them. findEditor
// returns null for a type with no registered editor, which is what the JDK also does when it finds
// nothing.
public class PropertyEditorManager {

    private static Map<Class<?>, Class<?>> registered = new HashMap<Class<?>, Class<?>>();

    private static String[] searchPath = new String[] { "sun.beans.editors" };

    public PropertyEditorManager() {
    }

    // It binds an editor to a type. Passing null as the editor deletes the registration.
    public static void registerEditor(Class<?> targetType, Class<?> editorClass) {
        if (targetType == null) {
            throw new NullPointerException();
        }
        syncRegistry(targetType, editorClass);
    }

    private static synchronized void syncRegistry(Class<?> targetType, Class<?> editorClass) {
        if (editorClass == null) {
            registered.remove(targetType);
        } else {
            registered.put(targetType, editorClass);
        }
    }

    private static synchronized Class<?> readRegistry(Class<?> targetType) {
        return registered.get(targetType);
    }

    // The editor for that type, already instantiated, or null if there is none.
    public static PropertyEditor findEditor(Class<?> targetType) {
        if (targetType == null) {
            throw new NullPointerException();
        }
        PropertyEditor ed = instantiateEditor(readRegistry(targetType));
        if (ed == null) {
            ed = instantiateEditor(byName(targetType.getName() + "Editor"));
        }
        if (ed == null) {
            String simple = EventSetDescriptor.simpleName(targetType);
            String[] paths = getEditorSearchPath();
            for (int i = 0; i < paths.length; i++) {
                if (ed == null) {
                    ed = instantiateEditor(byName(paths[i] + "." + simple + "Editor"));
                }
            }
        }
        return ed;
    }

    private static Class<?> byName(String name) {
        Class<?> c = null;
        try {
            c = Class.forName(name);
        } catch (Throwable notThere) {
            c = null;
        }
        return c;
    }

    // A class that is not a PropertyEditor, or that cannot be instantiated, counts as "there is no
    // editor": whoever was looking for one would rather have null than an exception from the
    // register.
    private static PropertyEditor instantiateEditor(Class<?> c) {
        PropertyEditor ed = null;
        if (c != null) {
            try {
                Object o = c.newInstance();
                if (o instanceof PropertyEditor) {
                    ed = (PropertyEditor) o;
                }
            } catch (Throwable couldNotBeDone) {
                ed = null;
            }
        }
        return ed;
    }

    public static synchronized String[] getEditorSearchPath() {
        String[] r = new String[searchPath.length];
        for (int i = 0; i < searchPath.length; i++) {
            r[i] = searchPath[i];
        }
        return r;
    }

    public static synchronized void setEditorSearchPath(String[] path) {
        if (path == null) {
            searchPath = new String[0];
        } else {
            String[] r = new String[path.length];
            for (int i = 0; i < path.length; i++) {
                r[i] = path[i];
            }
            searchPath = r;
        }
    }
}
