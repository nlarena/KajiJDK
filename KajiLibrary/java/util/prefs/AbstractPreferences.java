package java.util.prefs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;

// KajiLibrary's java.util.prefs.AbstractPreferences -- toda la logica del arbol, para que un
// deposito nuevo sea nueve metodos y no cuarenta y ocho.
//
// EL REPARTO. Los nueve `...Spi` son lo unico que una subclase escribe, y son deliberadamente
// tontos: `getSpi`/`putSpi`/`removeSpi` tocan **una** clave de **este** nodo, `keysSpi` y
// `childrenNamesSpi` enumeran, `childSpi` fabrica un hijo, `removeNodeSpi` borra este nodo,
// `flushSpi`/`syncSpi` hablan con el deposito. Ninguno valida nada, ninguno avisa a nadie, ninguno
// sabe que existe una ruta. Todo eso --validar largos, partir rutas, heredar por omision, mantener
// la cache de hijos, disparar los avisos, marcar el nodo como borrado-- pasa aca arriba una sola
// vez.
//
// LA CACHE DE HIJOS ES EL CORAZON. Un nodo que ya se materializo queda en `kidCache` y no se
// vuelve a fabricar, y de eso dependen dos cosas del contrato. La primera es que `node("/a/b")`
// llamado dos veces devuelve **el mismo objeto**, que es lo que hace que registrar un oyente sobre
// el resultado sirva de algo. La segunda es `childrenNames()`, que une lo que dice el deposito con
// lo que hay en la cache: un hijo recien creado y todavia no escrito tiene que aparecer igual.
//
// EL NODO BORRADO NO SE REVIVE. Despues de `removeNode()` casi todo tira `IllegalStateException`,
// incluso los `get`. Las dos excepciones estan puestas a mano y valen la pena: `nodeExists("")`
// devuelve `false` en vez de tirar --es la unica manera de preguntar "seguis vivo?" sin un
// `try`-- y `flush()` funciona, porque hay depositos que necesitan ese ultimo empujon para que el
// borrado llegue a disco. Ojo con la asimetria: `node("")` **si** tira sobre un nodo borrado,
// porque ahi la comprobacion viene antes.
//
// LOS AVISOS SALEN FUERA DEL CANDADO. El JDK los encola y los entrega en un hilo aparte; aca se
// entregan en el hilo que hizo el cambio, pero **despues** de soltar `lock`. Es una diferencia real
// y conviene tenerla clara: a favor, el aviso ya llego cuando `put()` vuelve --deterministico, sin
// un hilo demonio vivo para siempre-- y un oyente que reentre al nodo no se traba contra su propio
// candado. En contra, un oyente lento demora al que escribio. El contrato no dice nada sobre cual
// hilo entrega ni cuando, asi que ninguna de las dos formas lo incumple.
public abstract class AbstractPreferences extends Preferences {

    private final String name;
    private final AbstractPreferences parent;

    // La raiz del arbol. Se guarda y no se recorre cada vez porque `isUserNode()` la compara por
    // identidad en cada llamada.
    final AbstractPreferences root;

    private final String absolutePath;

    /**
     * El candado de este nodo. Es `protected` y no privado porque una subclase que necesite hacer
     * dos operaciones del deposito de forma atomica tiene que poder tomarlo.
     *
     * <p>Es **por nodo** y no uno global: dos nodos distintos se pueden tocar en paralelo. El precio
     * es que las operaciones que cruzan niveles --`removeNode`, `node` con ruta-- toman varios, y por
     * eso siempre lo hacen **de arriba hacia abajo**, que es lo que evita el abrazo mortal.
     */
    protected final Object lock = new Object();

    /**
     * Si este nodo no existia en el deposito cuando {@link #childSpi} lo fabrico.
     *
     * <p>Lo pone la subclase en el constructor, y de eso depende que se dispare o no
     * {@link NodeChangeListener#childAdded}: sin esta bandera esta clase no tiene como distinguir
     * un nodo recien creado de uno que ya estaba en disco.
     */
    protected boolean newNode = false;

    // Los hijos ya materializados, por nombre simple.
    private final Map<String, AbstractPreferences> kidCache =
            new HashMap<String, AbstractPreferences>();

    private boolean removed = false;

    private final ArrayList<PreferenceChangeListener> oyentesDeClave =
            new ArrayList<PreferenceChangeListener>();
    private final ArrayList<NodeChangeListener> oyentesDeNodo =
            new ArrayList<NodeChangeListener>();

    private static final String[] SIN_CADENAS = new String[0];
    private static final AbstractPreferences[] SIN_NODOS = new AbstractPreferences[0];

    /**
     * Un nodo llamado `name` colgando de `parent`.
     *
     * <p>La raiz se construye con `parent` en `null` y `name` en `""`, y las dos cosas van juntas:
     * una raiz con nombre o un hijo sin nombre serian arboles que no se pueden recorrer, asi que
     * las dos combinaciones tiran `IllegalArgumentException`. Un nombre con `/` tambien: la barra
     * es el separador de rutas y un nodo que la lleve en el nombre haria ambigua cualquier ruta que
     * lo atraviese.
     *
     * <p>El largo del nombre **no** se comprueba aca --lo comprueba `node()` antes de llamar a
     * `childSpi`-- porque una subclase puede legitimamente reconstruir desde el deposito un nodo
     * que escribio una version anterior.
     */
    protected AbstractPreferences(AbstractPreferences parent, String name) {
        if (parent == null) {
            if (!name.equals("")) {
                throw new IllegalArgumentException("Root name '" + name + "' must be \"\"");
            }
            this.absolutePath = "/";
            this.root = this;
        } else {
            if (name.indexOf('/') != -1) {
                throw new IllegalArgumentException("Name '" + name + "' contains '/'");
            }
            if (name.equals("")) {
                throw new IllegalArgumentException("Illegal name: empty string");
            }
            this.root = parent.root;
            this.absolutePath = (parent == this.root ? "/" + name
                                                     : parent.absolutePath() + "/" + name);
        }
        this.name = name;
        this.parent = parent;
    }

    // ---- claves y valores ------------------------------------------------------------------

    public void put(String key, String value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Key too long: " + key);
        }
        if (value.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("Value too long: " + value.length());
        }
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            putSpi(key, value);
        }
        avisarCambioDeClave(key, value);
    }

    public String get(String key, String def) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            String result = null;
            try {
                result = getSpi(key);
            } catch (Exception e) {
                // El deposito fallo. No hay a quien contarselo --`get` no tira-- y el contrato ya
                // tiene una respuesta prevista para "no esta": el valor por omision.
            }
            return result == null ? def : result;
        }
    }

    public void remove(String key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            removeSpi(key);
        }
        avisarCambioDeClave(key, null);
    }

    public void clear() throws BackingStoreException {
        synchronized (lock) {
            String[] claves = keys();
            for (int i = 0; i < claves.length; i++) {
                remove(claves[i]);
            }
        }
    }

    public void putInt(String key, int value) {
        put(key, Integer.toString(value));
    }

    public int getInt(String key, int def) {
        int result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                result = Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // Un valor mal tipado se comporta como una clave ausente: ver el encabezado de
            // Preferences.
        }
        return result;
    }

    public void putLong(String key, long value) {
        put(key, Long.toString(value));
    }

    public long getLong(String key, long def) {
        long result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                result = Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
        }
        return result;
    }

    public void putBoolean(String key, boolean value) {
        put(key, String.valueOf(value));
    }

    public boolean getBoolean(String key, boolean def) {
        boolean result = def;
        String value = get(key, null);
        if (value != null) {
            // Sin `Boolean.parseBoolean`: ese devuelve `false` para cualquier cosa que no sea
            // "true", y aca "cualquier cosa" tiene que dar el valor por omision, no `false`.
            if (value.equalsIgnoreCase("true")) {
                result = true;
            } else if (value.equalsIgnoreCase("false")) {
                result = false;
            }
        }
        return result;
    }

    public void putFloat(String key, float value) {
        put(key, Float.toString(value));
    }

    public float getFloat(String key, float def) {
        float result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                result = Float.parseFloat(value);
            }
        } catch (NumberFormatException e) {
        }
        return result;
    }

    public void putDouble(String key, double value) {
        put(key, Double.toString(value));
    }

    public double getDouble(String key, double def) {
        double result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                result = Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
        }
        return result;
    }

    public void putByteArray(String key, byte[] value) {
        put(key, java.util.Base64.getEncoder().encodeToString(value));
    }

    public byte[] getByteArray(String key, byte[] def) {
        byte[] result = def;
        String value = get(key, null);
        try {
            if (value != null) {
                // El largo multiplo de cuatro se exige a mano: el decodificador de `java.util`
                // tolera la falta de relleno y el deposito no, asi que sin esto un valor truncado
                // se leeria como bytes buenos en vez de caer en el valor por omision.
                if (value.length() % 4 != 0) {
                    return def;
                }
                result = java.util.Base64.getDecoder().decode(value);
            }
        } catch (RuntimeException e) {
            result = def;
        }
        return result;
    }

    public String[] keys() throws BackingStoreException {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            return keysSpi();
        }
    }

    // ---- el arbol --------------------------------------------------------------------------

    public String[] childrenNames() throws BackingStoreException {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            // La union de lo que hay en disco y lo que hay en la cache: un hijo recien creado
            // todavia puede no estar escrito, y omitirlo seria decir que no existe.
            TreeSet<String> s = new TreeSet<String>(kidCache.keySet());
            String[] delDeposito = childrenNamesSpi();
            for (int i = 0; i < delDeposito.length; i++) {
                s.add(delDeposito[i]);
            }
            return s.toArray(SIN_CADENAS);
        }
    }

    /** Los hijos que ya estan materializados en memoria, sin tocar el deposito. */
    protected final AbstractPreferences[] cachedChildren() {
        synchronized (lock) {
            return kidCache.values().toArray(SIN_NODOS);
        }
    }

    public Preferences parent() {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            return parent;
        }
    }

    public Preferences node(String path) {
        ArrayList<AbstractPreferences> nuevos = new ArrayList<AbstractPreferences>();
        Preferences resultado;
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            if (path.equals("")) {
                return this;
            }
            if (path.equals("/")) {
                return root;
            }
            if (path.charAt(0) != '/') {
                resultado = node(new StringTokenizer(path, "/", true), nuevos);
                avisarAltasDeNodo(nuevos);
                return resultado;
            }
        }
        // Ruta absoluta. Se sale del candado propio a proposito antes de tomar el de la raiz:
        // tomarlos al reves --de abajo hacia arriba-- es la unica forma de trabar dos hilos.
        resultado = root.node(new StringTokenizer(path.substring(1), "/", true), nuevos);
        avisarAltasDeNodo(nuevos);
        return resultado;
    }

    private Preferences node(StringTokenizer path, ArrayList<AbstractPreferences> nuevos) {
        String token = path.nextToken();
        if (token.equals("/")) {
            throw new IllegalArgumentException("Consecutive slashes in path");
        }
        synchronized (lock) {
            AbstractPreferences child = kidCache.get(token);
            if (child == null) {
                if (token.length() > MAX_NAME_LENGTH) {
                    throw new IllegalArgumentException("Node name " + token + " too long");
                }
                child = childSpi(token);
                if (child.newNode) {
                    nuevos.add(child);
                }
                kidCache.put(token, child);
            }
            if (!path.hasMoreTokens()) {
                return child;
            }
            path.nextToken(); // consume la barra
            if (!path.hasMoreTokens()) {
                throw new IllegalArgumentException("Path ends with slash");
            }
            return child.node(path, nuevos);
        }
    }

    public boolean nodeExists(String path) throws BackingStoreException {
        synchronized (lock) {
            // "" antes que la comprobacion de borrado, y no al reves: es la unica pregunta que un
            // nodo borrado tiene que poder contestar.
            if (path.equals("")) {
                return !removed;
            }
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            if (path.equals("/")) {
                return true;
            }
            if (path.charAt(0) != '/') {
                return nodeExists(new StringTokenizer(path, "/", true));
            }
        }
        return root.nodeExists(new StringTokenizer(path.substring(1), "/", true));
    }

    private boolean nodeExists(StringTokenizer path) throws BackingStoreException {
        String token = path.nextToken();
        if (token.equals("/")) {
            throw new IllegalArgumentException("Consecutive slashes in path");
        }
        synchronized (lock) {
            AbstractPreferences child = kidCache.get(token);
            if (child == null) {
                if (token.length() > MAX_NAME_LENGTH) {
                    throw new IllegalArgumentException("Node name " + token + " too long");
                }
                child = getChild(token);
                if (child == null) {
                    return false;
                }
                kidCache.put(token, child);
            }
            if (!path.hasMoreTokens()) {
                return true;
            }
            path.nextToken();
            if (!path.hasMoreTokens()) {
                throw new IllegalArgumentException("Path ends with slash");
            }
            return child.nodeExists(path);
        }
    }

    public void removeNode() throws BackingStoreException {
        if (this == root) {
            // No hay a quien sacarlo: la raiz no tiene padre que la olvide, y `Preferences.userRoot()`
            // la volveria a entregar acto seguido.
            throw new UnsupportedOperationException("Can't remove the root!");
        }
        ArrayList<AbstractPreferences> bajas = new ArrayList<AbstractPreferences>();
        synchronized (parent.lock) {
            removeNode2(bajas);
            parent.kidCache.remove(name);
        }
        avisarBajasDeNodo(bajas);
    }

    private void removeNode2(ArrayList<AbstractPreferences> bajas) throws BackingStoreException {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node already removed.");
            }
            // Los hijos que estan en disco pero todavia no en memoria se materializan aca: sin eso
            // el borrado dejaria descendientes vivos en el deposito.
            String[] nombres = childrenNamesSpi();
            for (int i = 0; i < nombres.length; i++) {
                if (!kidCache.containsKey(nombres[i])) {
                    kidCache.put(nombres[i], childSpi(nombres[i]));
                }
            }
            for (Iterator<AbstractPreferences> i = kidCache.values().iterator(); i.hasNext();) {
                AbstractPreferences child = i.next();
                try {
                    child.removeNode2(bajas);
                } catch (BackingStoreException x) {
                    // Un hijo que no se pudo borrar no puede abortar el borrado del padre: el
                    // arbol quedaria a medio camino y sin forma de terminarlo.
                }
                i.remove();
            }
            removeNodeSpi();
            removed = true;
            bajas.add(this);
        }
    }

    public String name() {
        return name;
    }

    public String absolutePath() {
        return absolutePath;
    }

    public boolean isUserNode() {
        return root == Preferences.userRoot();
    }

    public String toString() {
        return (isUserNode() ? "User" : "System") + " Preference Node: " + absolutePath();
    }

    // ---- avisos ----------------------------------------------------------------------------

    public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        if (pcl == null) {
            throw new NullPointerException("Change listener is null.");
        }
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            synchronized (oyentesDeClave) {
                oyentesDeClave.add(pcl);
            }
        }
    }

    public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            synchronized (oyentesDeClave) {
                if (!oyentesDeClave.remove(pcl)) {
                    throw new IllegalArgumentException("Listener not registered.");
                }
            }
        }
    }

    public void addNodeChangeListener(NodeChangeListener ncl) {
        if (ncl == null) {
            throw new NullPointerException("Change listener is null.");
        }
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            synchronized (oyentesDeNodo) {
                oyentesDeNodo.add(ncl);
            }
        }
    }

    public void removeNodeChangeListener(NodeChangeListener ncl) {
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            synchronized (oyentesDeNodo) {
                if (!oyentesDeNodo.remove(ncl)) {
                    throw new IllegalArgumentException("Listener not registered.");
                }
            }
        }
    }

    private void avisarCambioDeClave(String key, String nuevo) {
        PreferenceChangeListener[] copia;
        synchronized (oyentesDeClave) {
            if (oyentesDeClave.isEmpty()) {
                return;
            }
            copia = oyentesDeClave.toArray(new PreferenceChangeListener[0]);
        }
        PreferenceChangeEvent evt = new PreferenceChangeEvent(this, key, nuevo);
        for (int i = 0; i < copia.length; i++) {
            copia[i].preferenceChange(evt);
        }
    }

    private static void avisarAltasDeNodo(ArrayList<AbstractPreferences> nuevos) {
        for (int i = 0; i < nuevos.size(); i++) {
            AbstractPreferences hijo = nuevos.get(i);
            hijo.parent.avisarNodo(hijo, true);
        }
    }

    private static void avisarBajasDeNodo(ArrayList<AbstractPreferences> bajas) {
        for (int i = 0; i < bajas.size(); i++) {
            AbstractPreferences hijo = bajas.get(i);
            hijo.parent.avisarNodo(hijo, false);
        }
    }

    private void avisarNodo(AbstractPreferences hijo, boolean alta) {
        NodeChangeListener[] copia;
        synchronized (oyentesDeNodo) {
            if (oyentesDeNodo.isEmpty()) {
                return;
            }
            copia = oyentesDeNodo.toArray(new NodeChangeListener[0]);
        }
        NodeChangeEvent evt = new NodeChangeEvent(this, hijo);
        for (int i = 0; i < copia.length; i++) {
            if (alta) {
                copia[i].childAdded(evt);
            } else {
                copia[i].childRemoved(evt);
            }
        }
    }

    // ---- deposito --------------------------------------------------------------------------

    public void sync() throws BackingStoreException {
        sync2();
    }

    private void sync2() throws BackingStoreException {
        AbstractPreferences[] hijos;
        synchronized (lock) {
            if (removed) {
                throw new IllegalStateException("Node has been removed");
            }
            syncSpi();
            hijos = cachedChildren();
        }
        // Los hijos se recorren **fuera** del candado del padre: hacerlo adentro tomaria todo el
        // subarbol de una y cualquier otro hilo que tocara una hoja quedaria esperando la raiz.
        for (int i = 0; i < hijos.length; i++) {
            hijos[i].sync2();
        }
    }

    public void flush() throws BackingStoreException {
        flush2();
    }

    private void flush2() throws BackingStoreException {
        AbstractPreferences[] hijos;
        synchronized (lock) {
            flushSpi();
            // A diferencia de `sync`, sobre un nodo borrado no tira: hay depositos que necesitan
            // este ultimo `flushSpi` para que el borrado llegue a disco.
            if (removed) {
                return;
            }
            hijos = cachedChildren();
        }
        for (int i = 0; i < hijos.length; i++) {
            hijos[i].flush2();
        }
    }

    /** Si este nodo ya fue borrado. */
    protected boolean isRemoved() {
        synchronized (lock) {
            return removed;
        }
    }

    /**
     * El hijo llamado `nodeName` si **ya existe** en el deposito, o `null`.
     *
     * <p>Es la contracara de {@link #childSpi}, que crea. La implementacion por omision enumera y
     * compara, que sirve siempre pero cuesta; una subclase con manera de preguntar directamente por
     * un nodo deberia reemplazarla.
     */
    protected AbstractPreferences getChild(String nodeName) throws BackingStoreException {
        synchronized (lock) {
            String[] nombres = childrenNames();
            for (int i = 0; i < nombres.length; i++) {
                if (nombres[i].equals(nodeName)) {
                    return childSpi(nombres[i]);
                }
            }
        }
        return null;
    }

    // ---- XML -------------------------------------------------------------------------------

    public void exportNode(OutputStream os) throws IOException, BackingStoreException {
        Xml.exportar(os, this, false);
    }

    public void exportSubtree(OutputStream os) throws IOException, BackingStoreException {
        Xml.exportar(os, this, true);
    }

    // ---- lo que escribe la subclase --------------------------------------------------------

    /** Asocia `value` a `key` en este nodo, sin validar nada. */
    protected abstract void putSpi(String key, String value);

    /** El valor de `key` en este nodo, o `null` si no esta. */
    protected abstract String getSpi(String key);

    /** Borra `key` de este nodo. */
    protected abstract void removeSpi(String key);

    /**
     * Borra este nodo del deposito.
     *
     * <p>Lo llama {@link #removeNode} cuando los hijos ya se borraron, asi que la implementacion
     * puede dar por sentado que el nodo esta vacio de descendientes.
     */
    protected abstract void removeNodeSpi() throws BackingStoreException;

    /** Las claves de este nodo. Nunca `null`. */
    protected abstract String[] keysSpi() throws BackingStoreException;

    /** Los nombres simples de los hijos que hay en el deposito. Nunca `null`. */
    protected abstract String[] childrenNamesSpi() throws BackingStoreException;

    /**
     * El objeto que representa al hijo `name`, creandolo en el deposito si no existia.
     *
     * <p>No tiene que consultar `kidCache` --de eso se encarga {@link #node}, que es el unico que lo
     * llama y solo cuando el hijo no esta en la cache-- pero **si** tiene que poner
     * {@link #newNode} cuando lo acaba de crear.
     */
    protected abstract AbstractPreferences childSpi(String name);

    /** Empuja al deposito los cambios de este nodo. */
    protected abstract void flushSpi() throws BackingStoreException;

    /** Como {@link #flushSpi}, y ademas trae los cambios que hizo otro proceso. */
    protected abstract void syncSpi() throws BackingStoreException;
}
