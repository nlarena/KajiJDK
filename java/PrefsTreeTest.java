import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

// Las reglas de java.util.prefs que el contrato fija y que es facil implementar mal.
//
// Corre igual contra KajiLibrary y contra el JDK real, y por eso NO usa `Preferences.userRoot()`:
// en Windows eso abriria el registro del usuario, y ademas un almacen propio es lo unico que hace
// la prueba independiente de donde persista cada implementacion. Lo que se compara es
// `AbstractPreferences`, que es donde vive toda la logica.
//
//   ./bin/run-headless.exe java/PrefsTreeTest.class run
//   "H:/jdk-25.0.2/bin/java.exe" -cp java scratchpad/zz319/Corre.java PrefsTreeTest
//
// Devuelve -1 si esta todo bien; si no, el numero del control que fallo.
public class PrefsTreeTest {

    public static int run() {
        int r;
        r = limites();
        if (r != 0) {
            return r;
        }
        r = rutas();
        if (r != 0) {
            return r;
        }
        r = omisiones();
        if (r != 0) {
            return r;
        }
        r = borrado();
        if (r != 0) {
            return r;
        }
        r = arbol();
        if (r != 0) {
            return r;
        }
        r = oyentes();
        if (r != 0) {
            return r;
        }
        return -1;
    }

    // ---- 1xx: los largos maximos ------------------------------------------------------------

    private static int limites() {
        if (Preferences.MAX_KEY_LENGTH != 80) {
            return 101;
        }
        if (Preferences.MAX_NAME_LENGTH != 80) {
            return 102;
        }
        if (Preferences.MAX_VALUE_LENGTH != 8192) {
            return 103;
        }
        PrefsMem raiz = new PrefsMem();

        // Una clave de exactamente 80 entra; una de 81 no.
        raiz.put(repetir('k', 80), "v");
        if (!"v".equals(raiz.get(repetir('k', 80), null))) {
            return 104;
        }
        try {
            raiz.put(repetir('k', 81), "v");
            return 105;
        } catch (IllegalArgumentException e) {
            // esperado
        }

        // Y lo mismo del lado del valor, con 8192.
        raiz.put("g", repetir('v', 8192));
        try {
            raiz.put("g", repetir('v', 8193));
            return 106;
        } catch (IllegalArgumentException e) {
        }

        // La clave vacia SI es legal: el limite es de largo maximo y no de minimo.
        raiz.put("", "vacia");
        if (!"vacia".equals(raiz.get("", null))) {
            return 107;
        }

        // Un nombre de nodo de mas de 80 lo rechaza `node`, no el constructor.
        try {
            raiz.node(repetir('n', 81));
            return 108;
        } catch (IllegalArgumentException e) {
        }
        if (raiz.node(repetir('n', 80)) == null) {
            return 109;
        }

        // Un nombre de nodo con `/` no puede llegar al constructor.
        try {
            new PrefsMem(raiz, "a/b");
            return 110;
        } catch (IllegalArgumentException e) {
        }
        // Ni una raiz con nombre, ni un hijo sin nombre.
        try {
            new PrefsMem(null, "conNombre");
            return 111;
        } catch (IllegalArgumentException e) {
        }
        try {
            new PrefsMem(raiz, "");
            return 112;
        } catch (IllegalArgumentException e) {
        }
        return 0;
    }

    // ---- 2xx: rutas -------------------------------------------------------------------------

    private static int rutas() {
        PrefsMem raiz = new PrefsMem();
        Preferences a = raiz.node("a");
        Preferences ab = raiz.node("a/b");

        // `node("")` es el nodo mismo, y da EL MISMO objeto.
        if (ab.node("") != ab) {
            return 201;
        }
        // `node("/")` es la raiz del arbol, se pida desde donde se pida.
        if (ab.node("/") != raiz) {
            return 202;
        }
        // La cache de hijos hace que dos llamadas devuelvan el mismo objeto.
        if (raiz.node("a/b") != ab) {
            return 203;
        }
        // Una ruta ABSOLUTA pedida desde un hijo se resuelve desde la raiz: no tira, y no la
        // interpreta como relativa.
        if (ab.node("/a") != a) {
            return 204;
        }
        if (ab.node("/x/y") != raiz.node("x/y")) {
            return 205;
        }
        // Las rutas
        if (!"/".equals(raiz.absolutePath())) {
            return 206;
        }
        if (!"/a".equals(a.absolutePath())) {
            return 207;
        }
        if (!"/a/b".equals(ab.absolutePath())) {
            return 208;
        }
        if (!"".equals(raiz.name()) || !"b".equals(ab.name())) {
            return 209;
        }
        if (raiz.parent() != null || ab.parent() != a) {
            return 210;
        }

        // Barras consecutivas y barra al final: las dos tiran.
        try {
            raiz.node("a//b");
            return 211;
        } catch (IllegalArgumentException e) {
        }
        try {
            raiz.node("a/");
            return 212;
        } catch (IllegalArgumentException e) {
        }
        try {
            raiz.node("//a");
            return 213;
        } catch (IllegalArgumentException e) {
        }
        try {
            raiz.node(null);
            return 214;
        } catch (NullPointerException e) {
        }

        // nodeExists no crea nada.
        try {
            if (raiz.nodeExists("noEsta")) {
                return 215;
            }
            if (!raiz.nodeExists("a/b")) {
                return 216;
            }
            if (!raiz.nodeExists("/")) {
                return 217;
            }
            if (!ab.nodeExists("")) {
                return 218;
            }
        } catch (BackingStoreException e) {
            return 219;
        }
        return 0;
    }

    // ---- 3xx: los valores por omision --------------------------------------------------------

    private static int omisiones() {
        PrefsMem p = new PrefsMem();

        // Clave ausente: sale el valor por omision, en los ocho tipos.
        if (!"d".equals(p.get("no", "d"))) {
            return 301;
        }
        if (p.getInt("no", 7) != 7) {
            return 302;
        }
        if (p.getLong("no", 7L) != 7L) {
            return 303;
        }
        if (!p.getBoolean("no", true)) {
            return 304;
        }
        if (p.getFloat("no", 1.5f) != 1.5f) {
            return 305;
        }
        if (p.getDouble("no", 1.5) != 1.5) {
            return 306;
        }
        if (p.getByteArray("no", new byte[] {9})[0] != 9) {
            return 307;
        }
        if (p.get("no", null) != null) {
            return 308;
        }

        // Clave PRESENTE pero mal tipada: tambien sale el valor por omision, y NO tira. Esta es la
        // regla que mas facil se implementa mal.
        p.put("k", "hola");
        if (p.getInt("k", 7) != 7) {
            return 311;
        }
        if (p.getLong("k", 7L) != 7L) {
            return 312;
        }
        if (p.getFloat("k", 1.5f) != 1.5f) {
            return 313;
        }
        if (p.getDouble("k", 1.5) != 1.5) {
            return 314;
        }
        // Y para booleano: cualquier cosa que no sea "true"/"false" da el valor por omision, no
        // `false` -- que es lo que daria `Boolean.parseBoolean`.
        if (!p.getBoolean("k", true)) {
            return 315;
        }
        if (p.getBoolean("k", false)) {
            return 316;
        }

        // Desbordar un int tampoco tira.
        p.put("grande", "99999999999999999999");
        if (p.getInt("grande", 7) != 7) {
            return 317;
        }
        // Pero un long valido si se lee como long aunque no entre en un int.
        p.putLong("l", 4294967296L);
        if (p.getInt("l", 7) != 7) {
            return 318;
        }
        if (p.getLong("l", 7L) != 4294967296L) {
            return 319;
        }

        // Booleano: se compara sin distinguir mayusculas.
        p.put("b1", "TRUE");
        p.put("b2", "False");
        if (!p.getBoolean("b1", false) || p.getBoolean("b2", true)) {
            return 320;
        }

        // Los tipos que si son validos vuelven redondos.
        p.putInt("i", -42);
        p.putLong("j", Long.MIN_VALUE);
        p.putDouble("d", 0.1);
        p.putFloat("f", 0.25f);
        p.putBoolean("t", true);
        if (p.getInt("i", 0) != -42 || p.getLong("j", 0) != Long.MIN_VALUE) {
            return 321;
        }
        if (p.getDouble("d", 0) != 0.1 || p.getFloat("f", 0) != 0.25f || !p.getBoolean("t", false)) {
            return 322;
        }

        // Bytes: van y vuelven, y un valor que no es Base64 da el valor por omision.
        byte[] datos = new byte[] {0, 1, 2, (byte) 0xff, 65, 66};
        p.putByteArray("bytes", datos);
        byte[] vuelta = p.getByteArray("bytes", null);
        if (vuelta == null || vuelta.length != datos.length) {
            return 323;
        }
        for (int i = 0; i < datos.length; i++) {
            if (vuelta[i] != datos[i]) {
                return 324;
            }
        }
        p.put("noB64", "no es base64 !!!");
        if (p.getByteArray("noB64", new byte[] {9})[0] != 9) {
            return 325;
        }
        // "hola" son cuatro caracteres validos de Base64, asi que SI decodifica -- a tres bytes
        // que no tienen nada que ver con el texto. Es la contracara de la regla: el deposito guarda
        // cadenas y `getByteArray` no tiene como saber que la cadena no venia de `putByteArray`.
        p.put("hola", "hola");
        if (p.getByteArray("hola", new byte[] {9}).length != 3) {
            return 326;
        }

        // Un `put` con nulos tira NPE; un `get` con clave nula, tambien.
        try {
            p.put(null, "v");
            return 331;
        } catch (NullPointerException e) {
        }
        try {
            p.put("k", null);
            return 332;
        } catch (NullPointerException e) {
        }
        try {
            p.get(null, "d");
            return 333;
        } catch (NullPointerException e) {
        }
        try {
            p.remove(null);
            return 334;
        } catch (NullPointerException e) {
        }
        return 0;
    }

    // ---- 4xx: el nodo borrado ----------------------------------------------------------------

    private static int borrado() {
        PrefsMem raiz = new PrefsMem();
        Preferences a = raiz.node("a");
        Preferences ab = raiz.node("a/b");
        a.put("k", "v");
        ab.put("k", "v");

        try {
            a.removeNode();
        } catch (BackingStoreException e) {
            return 401;
        }

        // Casi todo tira IllegalStateException, incluidos los `get`.
        try {
            a.get("k", "d");
            return 402;
        } catch (IllegalStateException e) {
        }
        try {
            a.put("k", "v");
            return 403;
        } catch (IllegalStateException e) {
        }
        try {
            a.keys();
            return 404;
        } catch (IllegalStateException e) {
        } catch (BackingStoreException e) {
            return 405;
        }
        try {
            a.parent();
            return 406;
        } catch (IllegalStateException e) {
        }
        // `node("")` TAMBIEN tira, aunque devolveria el nodo mismo: la comprobacion de borrado
        // viene antes.
        try {
            a.node("");
            return 407;
        } catch (IllegalStateException e) {
        }
        // `sync()` tira...
        try {
            a.sync();
            return 408;
        } catch (IllegalStateException e) {
        } catch (BackingStoreException e) {
            return 409;
        }
        // ...pero `flush()` NO: el deposito puede necesitar ese ultimo empujon.
        try {
            a.flush();
        } catch (IllegalStateException e) {
            return 410;
        } catch (BackingStoreException e) {
            return 411;
        }
        // Y `nodeExists("")` contesta `false` en vez de tirar: es la unica forma de preguntar
        // "seguis vivo?" sin un try.
        try {
            if (a.nodeExists("")) {
                return 412;
            }
        } catch (Exception e) {
            return 413;
        }

        // El hijo del nodo borrado tambien quedo borrado.
        try {
            ab.get("k", "d");
            return 414;
        } catch (IllegalStateException e) {
        }
        // Y el padre ya no lo lista.
        try {
            if (raiz.nodeExists("a")) {
                return 415;
            }
            String[] hijos = raiz.childrenNames();
            for (int i = 0; i < hijos.length; i++) {
                if (hijos[i].equals("a")) {
                    return 416;
                }
            }
        } catch (BackingStoreException e) {
            return 417;
        }
        // Borrar dos veces tira.
        try {
            a.removeNode();
            return 418;
        } catch (IllegalStateException e) {
        } catch (BackingStoreException e) {
            return 419;
        }
        // La raiz no se borra.
        try {
            raiz.removeNode();
            return 420;
        } catch (UnsupportedOperationException e) {
        } catch (BackingStoreException e) {
            return 421;
        }
        return 0;
    }

    // ---- 5xx: enumerar y limpiar --------------------------------------------------------------

    private static int arbol() {
        PrefsMem raiz = new PrefsMem();
        raiz.node("zeta");
        raiz.node("alfa");
        raiz.node("medio");
        try {
            String[] hijos = raiz.childrenNames();
            if (hijos.length != 3) {
                return 501;
            }
            // childrenNames viene ORDENADO.
            if (!hijos[0].equals("alfa") || !hijos[1].equals("medio") || !hijos[2].equals("zeta")) {
                return 502;
            }
        } catch (BackingStoreException e) {
            return 503;
        }

        Preferences a = raiz.node("alfa");
        a.put("k1", "v1");
        a.put("k2", "v2");
        a.node("nieto");
        try {
            if (a.keys().length != 2) {
                return 504;
            }
            // `clear()` borra las claves y NO toca a los hijos.
            a.clear();
            if (a.keys().length != 0) {
                return 505;
            }
            if (a.childrenNames().length != 1) {
                return 506;
            }
        } catch (BackingStoreException e) {
            return 507;
        }

        // `remove` de una clave que no esta no tira.
        a.remove("noEsta");

        // Un hijo recien creado y todavia no persistido igual aparece en childrenNames.
        PrefsMem r2 = new PrefsMem();
        r2.node("recien");
        try {
            if (r2.childrenNames().length != 1) {
                return 508;
            }
        } catch (BackingStoreException e) {
            return 509;
        }
        return 0;
    }

    // ---- 6xx: oyentes ------------------------------------------------------------------------

    private static int oyentes() {
        PrefsMem raiz = new PrefsMem();
        PrefsEspia pespia = new PrefsEspia();

        try {
            raiz.addPreferenceChangeListener(null);
            return 601;
        } catch (NullPointerException e) {
        }
        try {
            raiz.addNodeChangeListener(null);
            return 602;
        } catch (NullPointerException e) {
        }
        // Sacar uno que no esta registrado tira IllegalArgumentException.
        try {
            raiz.removePreferenceChangeListener(pespia);
            return 603;
        } catch (IllegalArgumentException e) {
        }
        try {
            raiz.removeNodeChangeListener(pespia);
            return 604;
        } catch (IllegalArgumentException e) {
        }

        raiz.addPreferenceChangeListener(pespia);
        raiz.addNodeChangeListener(pespia);

        raiz.put("k", "v");
        // El JDK entrega los avisos en un hilo aparte y KajiLibrary en el hilo que escribio; se
        // espera con plazo para que la prueba valga en los dos.
        if (!esperar(pespia, 1, 0)) {
            return 605;
        }
        if (!"k".equals(pespia.clave) || !"v".equals(pespia.valor)) {
            return 606;
        }

        raiz.remove("k");
        if (!esperar(pespia, 2, 0)) {
            return 607;
        }
        // El borrado se reconoce porque el valor nuevo es null.
        if (!"k".equals(pespia.clave) || pespia.valor != null) {
            return 608;
        }

        Preferences hijo = raiz.node("nuevo");
        if (!esperar(pespia, 2, 1)) {
            return 609;
        }
        if (pespia.hijo != hijo) {
            return 610;
        }
        // Pedir el mismo nodo otra vez NO vuelve a avisar: ya existia.
        raiz.node("nuevo");
        if (pespia.altas != 1) {
            return 611;
        }

        try {
            hijo.removeNode();
        } catch (BackingStoreException e) {
            return 612;
        }
        if (!esperarBaja(pespia)) {
            return 613;
        }
        if (pespia.bajas != 1) {
            return 614;
        }

        raiz.removePreferenceChangeListener(pespia);
        raiz.put("otra", "x");
        // Ya no deberia llegar nada; se le da tiempo al hilo del JDK por si estuviera en camino.
        dormir(50);
        if (pespia.cambios != 2) {
            return 615;
        }
        return 0;
    }

    // Espera hasta que lleguen los avisos esperados, o hasta que se acabe el plazo.
    private static boolean esperar(PrefsEspia e, int cambios, int altas) {
        for (int i = 0; i < 200; i++) {
            if (e.cambios >= cambios && e.altas >= altas) {
                return e.cambios == cambios && e.altas == altas;
            }
            dormir(10);
        }
        return false;
    }

    private static boolean esperarBaja(PrefsEspia e) {
        for (int i = 0; i < 200; i++) {
            if (e.bajas >= 1) {
                return true;
            }
            dormir(10);
        }
        return false;
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String repetir(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}

// Un oyente que anota lo ultimo que le llego.
class PrefsEspia implements PreferenceChangeListener, NodeChangeListener {
    volatile int cambios;
    volatile int altas;
    volatile int bajas;
    volatile String clave;
    volatile String valor;
    volatile Preferences hijo;

    public void preferenceChange(PreferenceChangeEvent evt) {
        clave = evt.getKey();
        valor = evt.getNewValue();
        cambios++;
    }

    public void childAdded(NodeChangeEvent evt) {
        hijo = evt.getChild();
        altas++;
    }

    public void childRemoved(NodeChangeEvent evt) {
        hijo = evt.getChild();
        bajas++;
    }
}

// Un deposito en memoria: lo minimo para ejercitar `AbstractPreferences` sin tocar ni el disco ni
// el registro, de modo que la prueba diga lo mismo en las dos VM.
class PrefsMem extends AbstractPreferences {

    private final java.util.TreeMap<String, String> mapa = new java.util.TreeMap<String, String>();
    private final java.util.TreeMap<String, PrefsMem> hijos =
            new java.util.TreeMap<String, PrefsMem>();

    PrefsMem() {
        super(null, "");
    }

    PrefsMem(PrefsMem padre, String nombre) {
        super(padre, nombre);
        newNode = true;
        padre.hijos.put(nombre, this);
    }

    protected void putSpi(String key, String value) {
        mapa.put(key, value);
    }

    protected String getSpi(String key) {
        return mapa.get(key);
    }

    protected void removeSpi(String key) {
        mapa.remove(key);
    }

    protected void removeNodeSpi() {
        PrefsMem padre = (PrefsMem) parent();
        if (padre != null) {
            padre.hijos.remove(name());
        }
    }

    protected String[] keysSpi() {
        return mapa.keySet().toArray(new String[0]);
    }

    protected String[] childrenNamesSpi() {
        return hijos.keySet().toArray(new String[0]);
    }

    protected AbstractPreferences childSpi(String nombre) {
        PrefsMem c = hijos.get(nombre);
        return c != null ? c : new PrefsMem(this, nombre);
    }

    protected void syncSpi() {
    }

    protected void flushSpi() {
    }
}
