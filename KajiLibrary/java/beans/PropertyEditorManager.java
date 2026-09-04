package java.beans;

import java.util.HashMap;
import java.util.Map;

// El registro de que editor usar para cada tipo de propiedad. Busca en tres lugares y en este
// orden: lo registrado a mano, la clase `<Tipo>Editor` al lado del tipo, y esa misma clase en los
// paquetes del search path.
//
// Los editores de los tipos primitivos que el JDK trae de fabrica (sun.beans.editors.*) NO estan
// aca: son clases de implementacion, no API, y este arbol no las tiene. findEditor devuelve null
// para un tipo sin editor registrado, que es lo que el JDK tambien hace cuando no encuentra nada.
public class PropertyEditorManager {

    private static Map<Class<?>, Class<?>> registrados = new HashMap<Class<?>, Class<?>>();

    private static String[] searchPath = new String[] { "sun.beans.editors" };

    public PropertyEditorManager() {
    }

    // Ata un editor a un tipo. Pasar null como editor borra el registro.
    public static void registerEditor(Class<?> targetType, Class<?> editorClass) {
        if (targetType == null) {
            throw new NullPointerException();
        }
        sincronizarRegistro(targetType, editorClass);
    }

    private static synchronized void sincronizarRegistro(Class<?> targetType, Class<?> editorClass) {
        if (editorClass == null) {
            registrados.remove(targetType);
        } else {
            registrados.put(targetType, editorClass);
        }
    }

    private static synchronized Class<?> leerRegistro(Class<?> targetType) {
        return registrados.get(targetType);
    }

    // El editor para ese tipo, ya instanciado, o null si no hay ninguno.
    public static PropertyEditor findEditor(Class<?> targetType) {
        if (targetType == null) {
            throw new NullPointerException();
        }
        PropertyEditor ed = instanciar(leerRegistro(targetType));
        if (ed == null) {
            ed = instanciar(porNombre(targetType.getName() + "Editor"));
        }
        if (ed == null) {
            String simple = EventSetDescriptor.nombreSimple(targetType);
            String[] rutas = getEditorSearchPath();
            for (int i = 0; i < rutas.length; i++) {
                if (ed == null) {
                    ed = instanciar(porNombre(rutas[i] + "." + simple + "Editor"));
                }
            }
        }
        return ed;
    }

    private static Class<?> porNombre(String nombre) {
        Class<?> c = null;
        try {
            c = Class.forName(nombre);
        } catch (Throwable noEsta) {
            c = null;
        }
        return c;
    }

    // Una clase que no es PropertyEditor, o que no se puede instanciar, cuenta como "no hay
    // editor": el que buscaba uno prefiere null antes que una excepcion desde el registro.
    private static PropertyEditor instanciar(Class<?> c) {
        PropertyEditor ed = null;
        if (c != null) {
            try {
                Object o = c.newInstance();
                if (o instanceof PropertyEditor) {
                    ed = (PropertyEditor) o;
                }
            } catch (Throwable noSePudo) {
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
