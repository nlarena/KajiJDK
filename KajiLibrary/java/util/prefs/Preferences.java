package java.util.prefs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// KajiLibrary's java.util.prefs.Preferences -- un arbol de claves y valores por usuario y por
// aplicacion, con persistencia.
//
// LA IDEA. Hay dos arboles: el del usuario y el del sistema. Cada nodo del arbol tiene un nombre,
// hijos, y su propia tabla de claves a cadenas. La ruta se escribe con barras como la de un
// archivo, y por convencion cada paquete Java se queda con el nodo que le corresponde a su nombre
// --`com.acme.db` vive en `/com/acme/db`-- que es lo que hacen {@link #userNodeForPackage} y
// {@link #systemNodeForPackage}. Asi dos bibliotecas distintas no se pisan sin tener que acordar
// nada.
//
// LOS VALORES SON CADENAS Y NADA MAS. Los `putInt`, `putDouble`, `putByteArray` son todos azucar
// sobre `put(String, String)`: guardan la representacion textual y punto. Eso explica la regla mas
// facil de implementar mal del paquete: **un valor mal tipado se comporta como una clave ausente**.
// `getInt("k", 7)` sobre un valor `"hola"` devuelve `7`; no tira. Es deliberado -- una preferencia
// corrupta la puede haber escrito una version vieja del programa, o un usuario editando el archivo
// a mano, y volcarle una excepcion al programa por eso lo dejaria sin arrancar.
//
// POR LA MISMA RAZON NINGUN `get` NI NINGUN `put` TIRA `BackingStoreException`. Solo la tiran las
// operaciones que no tienen respuesta por omision: enumerar (`keys`, `childrenNames`,
// `nodeExists`), borrar (`removeNode`, `clear`) y forzar la escritura (`flush`, `sync`).
//
// LOS LIMITES SON PARTE DEL CONTRATO, no de la implementacion: una clave no puede pasar de
// {@link #MAX_KEY_LENGTH} caracteres, un nombre de nodo de {@link #MAX_NAME_LENGTH}, un valor de
// {@link #MAX_VALUE_LENGTH}. Estan fijados justamente para que un programa pueda escribirse una vez
// y funcionar sobre cualquier deposito, incluido el registro de Windows.
//
// ---------------------------------------------------------------------------------------------
// DONDE GUARDA ESTO. En este JDK `user.home`, `user.dir` y `java.io.tmpdir` valen `null` y
// `System.getenv` no devuelve nada, asi que no hay un directorio del usuario al que apuntar. Lo que
// si funciona es el sistema de archivos por rutas **relativas** al directorio de trabajo del
// proceso. Por eso el deposito por omision --{@link AlmacenDeArchivos}, via
// {@link FabricaDeArchivos}-- vive en `.java/.userPrefs` y `.java/.systemPrefs` colgando del
// directorio de trabajo, que es la misma estructura que usa el JDK bajo POSIX menos el prefijo del
// hogar. Se puede mover con las propiedades `java.util.prefs.userRoot` y
// `java.util.prefs.systemRoot`, igual que en el JDK. Nada se crea en disco hasta que algo se
// escribe: pedir `userRoot()` y leer no ensucia el directorio.
//
// Dos consecuencias honestas de eso, porque no son las del JDK. La primera: el arbol del usuario y
// el del sistema son dos directorios y no dos ambitos de permisos, asi que
// {@link AbstractPreferences#isUserNode} distingue de cual arbol venis pero no implica ningun
// privilegio distinto. La segunda: "el usuario" es en los hechos "el directorio desde el que se
// lanzo la VM"; dos usuarios que corran desde el mismo directorio comparten las preferencias.
// Ninguna de las dos es una mentira del contrato --el contrato no promete permisos-- pero conviene
// saberlas.
//
// Si el directorio no se puede crear (por ejemplo, un directorio de trabajo de solo lectura) el
// arbol igual funciona en memoria y son `flush()` y `sync()` los que tiran
// {@link BackingStoreException} diciendo cual directorio fallo. Es la unica forma honesta de
// degradar: lo que no se puede hacer se cuenta por donde el contrato deja contarlo.
// ---------------------------------------------------------------------------------------------
//
// LO QUE NO ESTA. Nada: los 42 miembros publicos de la clase estan. `importPreferences` incluido --
// ver {@link Xml}, que trae un analizador de XML propio porque en este arbol no hay ni
// `org.w3c.dom` ni `org.xml.sax` ni `javax.xml.parsers`.
public abstract class Preferences {

    /** El largo maximo de una clave, en caracteres. */
    public static final int MAX_KEY_LENGTH = 80;

    /** El largo maximo del nombre de un nodo, en caracteres. */
    public static final int MAX_NAME_LENGTH = 80;

    /** El largo maximo de un valor, en caracteres. */
    public static final int MAX_VALUE_LENGTH = 8192;

    // Se resuelve una sola vez y se guarda: `isUserNode()` compara la raiz por identidad, asi que
    // una fabrica que devolviera un objeto nuevo en cada llamada romperia esa comparacion.
    private static final PreferencesFactory FABRICA = elegirFabrica();

    private static PreferencesFactory elegirFabrica() {
        String nombre = System.getProperty("java.util.prefs.PreferencesFactory");
        if (nombre != null && nombre.length() != 0) {
            try {
                Class<?> c = Class.forName(nombre, true, ClassLoader.getSystemClassLoader());
                return (PreferencesFactory) c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // Si pediste una fabrica por nombre y no se pudo, callarse y usar otra seria
                // peor que fallar: las preferencias irian a un lado que no elegiste.
                throw new InternalError(
                        "no se pudo instanciar java.util.prefs.PreferencesFactory=" + nombre, e);
            }
        }
        return new FabricaDeArchivos();
    }

    /** Para las subclases; no hay nada que inicializar. */
    protected Preferences() {
    }

    // ---- las raices ------------------------------------------------------------------------

    /** La raiz del arbol del usuario. */
    public static Preferences userRoot() {
        return FABRICA.userRoot();
    }

    /** La raiz del arbol del sistema. */
    public static Preferences systemRoot() {
        return FABRICA.systemRoot();
    }

    /**
     * El nodo del arbol del usuario que le corresponde al paquete de `c`.
     *
     * <p>`com.acme.Db` da `/com/acme`. Una clase del paquete por omision da `/<unnamed>`, que no es
     * un nombre de nodo que se pueda escribir a mano y por eso no colisiona con nada.
     */
    public static Preferences userNodeForPackage(Class<?> c) {
        return nodoDePaquete(c, true);
    }

    /** El nodo del arbol del sistema que le corresponde al paquete de `c`. */
    public static Preferences systemNodeForPackage(Class<?> c) {
        return nodoDePaquete(c, false);
    }

    private static Preferences nodoDePaquete(Class<?> c, boolean deUsuario) {
        if (c.isArray()) {
            // Un arreglo no pertenece a ningun paquete que uno haya escrito: `int[]` no tiene
            // dueño, y `String[]` daria el nodo de `java.lang`, que no es de nadie.
            throw new IllegalArgumentException("Arrays have no associated preferences node.");
        }
        String nombreDeClase = c.getName();
        int punto = nombreDeClase.lastIndexOf('.');
        String paquete = (punto < 0) ? "" : nombreDeClase.substring(0, punto);
        String ruta = paquete.length() == 0 ? "/<unnamed>" : "/" + paquete.replace('.', '/');
        return deUsuario ? userRoot().node(ruta) : systemRoot().node(ruta);
    }

    // ---- claves y valores ------------------------------------------------------------------

    /** Asocia `value` a `key` en este nodo. */
    public abstract void put(String key, String value);

    /** El valor de `key`, o `def` si no esta (o si el deposito no se pudo consultar). */
    public abstract String get(String key, String def);

    /** Borra `key` de este nodo. */
    public abstract void remove(String key);

    /** Borra todas las claves de este nodo. No toca a los hijos. */
    public abstract void clear() throws BackingStoreException;

    /** Guarda `value` como su representacion decimal. */
    public abstract void putInt(String key, int value);

    /** El `int` guardado en `key`, o `def` si falta o no es un `int`. */
    public abstract int getInt(String key, int def);

    /** Guarda `value` como su representacion decimal. */
    public abstract void putLong(String key, long value);

    /** El `long` guardado en `key`, o `def` si falta o no es un `long`. */
    public abstract long getLong(String key, long def);

    /** Guarda `"true"` o `"false"`. */
    public abstract void putBoolean(String key, boolean value);

    /** El `boolean` guardado en `key`, o `def` si falta o no es `"true"`/`"false"`. */
    public abstract boolean getBoolean(String key, boolean def);

    /** Guarda `value` con {@link Float#toString}. */
    public abstract void putFloat(String key, float value);

    /** El `float` guardado en `key`, o `def` si falta o no es un `float`. */
    public abstract float getFloat(String key, float def);

    /** Guarda `value` con {@link Double#toString}. */
    public abstract void putDouble(String key, double value);

    /** El `double` guardado en `key`, o `def` si falta o no es un `double`. */
    public abstract double getDouble(String key, double def);

    /** Guarda `value` en Base64: es la unica forma de meter bytes en un deposito de cadenas. */
    public abstract void putByteArray(String key, byte[] value);

    /** Los bytes guardados en `key`, o `def` si falta o no es Base64 valido. */
    public abstract byte[] getByteArray(String key, byte[] def);

    /** Las claves de este nodo, en cualquier orden. */
    public abstract String[] keys() throws BackingStoreException;

    /** Los nombres simples de los hijos de este nodo. */
    public abstract String[] childrenNames() throws BackingStoreException;

    /** El padre, o `null` si este es la raiz. */
    public abstract Preferences parent();

    /**
     * El nodo en `pathName`, creandolo --y a los ancestros que falten-- si no existia.
     *
     * <p>Una ruta que empieza con `/` se resuelve desde la raiz de **este** arbol; cualquier otra,
     * desde este nodo. `""` es este mismo nodo.
     */
    public abstract Preferences node(String pathName);

    /** Si el nodo en `pathName` existe. `""` pregunta por este nodo y no tira aunque este borrado. */
    public abstract boolean nodeExists(String pathName) throws BackingStoreException;

    /** Borra este nodo y todos sus descendientes. */
    public abstract void removeNode() throws BackingStoreException;

    /** El nombre simple de este nodo; `""` para la raiz. */
    public abstract String name();

    /** La ruta absoluta de este nodo dentro de su arbol. */
    public abstract String absolutePath();

    /** Si este nodo esta en el arbol del usuario. */
    public abstract boolean isUserNode();

    /** `"User Preference Node: <ruta>"` o `"System Preference Node: <ruta>"`. */
    public abstract String toString();

    // ---- deposito --------------------------------------------------------------------------

    /** Empuja al deposito los cambios de este nodo y de los descendientes que esten en memoria. */
    public abstract void flush() throws BackingStoreException;

    /** Como {@link #flush}, y ademas trae los cambios que hizo otra VM. */
    public abstract void sync() throws BackingStoreException;

    // ---- avisos ----------------------------------------------------------------------------

    /** Empieza a avisarle a `pcl` de los cambios de clave de **este** nodo. */
    public abstract void addPreferenceChangeListener(PreferenceChangeListener pcl);

    /** Deja de avisarle a `pcl`. */
    public abstract void removePreferenceChangeListener(PreferenceChangeListener pcl);

    /** Empieza a avisarle a `ncl` de las altas y bajas de hijos de **este** nodo. */
    public abstract void addNodeChangeListener(NodeChangeListener ncl);

    /** Deja de avisarle a `ncl`. */
    public abstract void removeNodeChangeListener(NodeChangeListener ncl);

    // ---- XML -------------------------------------------------------------------------------

    /** Escribe en `os` un documento XML con las claves de este nodo y ningun hijo. */
    public abstract void exportNode(OutputStream os) throws IOException, BackingStoreException;

    /** Escribe en `os` un documento XML con este nodo y todo su subarbol. */
    public abstract void exportSubtree(OutputStream os) throws IOException, BackingStoreException;

    /**
     * Lee un documento como los que escriben {@link #exportNode} y {@link #exportSubtree} y aplica
     * lo que dice.
     *
     * <p>El documento elige solo a que arbol va --el atributo `type` de `<root>`-- y por eso el
     * metodo es estatico y no de instancia: no hay un nodo "sobre el cual" importar.
     */
    public static void importPreferences(InputStream is)
            throws IOException, InvalidPreferencesFormatException {
        Xml.importar(is);
    }
}
