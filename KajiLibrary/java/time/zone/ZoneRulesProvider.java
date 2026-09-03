package java.time.zone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

// KajiLibrary's java.time.zone.ZoneRulesProvider -- la busqueda de "de este id de zona, cuales son
// las reglas".
//
// **Es un SPI, no un accesor**, y esa es la diferencia que esta version arregla. Antes era una clase
// abstracta con dos estaticos que leian la tabla embebida directo: la forma de un SPI sin el
// mecanismo. Un `provideRules` sobrescrito no lo llamaba nadie, porque no habia registro.
//
// Ahora si: hay una lista de proveedores, `registerProvider` la extiende, y los dos estaticos
// recorren la lista. El proveedor de la tabla embebida (`TzData`) es simplemente el primero que se
// registra, y deja de ser un caso especial.
//
// Por que importa aunque hoy solo haya uno: es la unica manera de que el dia que se pueda leer la
// base IANA en tiempo de ejecucion, se la enchufe **sin tocar esta clase**. Y mientras tanto, el
// codigo que registra un proveedor propio --una zona de prueba, una zona historica-- funciona.
//
// Los proveedores se buscan **del ultimo registrado al primero**, para que uno agregado despues
// pueda tapar a la tabla embebida y no al reves.
public abstract class ZoneRulesProvider {

    // La lista de proveedores. Arranca con el de la tabla embebida.
    private static final List<ZoneRulesProvider> PROVEEDORES = crearLista();

    private static List<ZoneRulesProvider> crearLista() {
        List<ZoneRulesProvider> lista = new ArrayList<ZoneRulesProvider>();
        lista.add(new TzDataProvider());
        return lista;
    }

    protected ZoneRulesProvider() {
    }

    // ---- los dos estaticos de consulta -------------------------------------------------------------

    /** Todos los ids que algun proveedor conoce. */
    public static Set<String> getAvailableZoneIds() {
        Set<String> ids = new HashSet<String>();
        synchronized (PROVEEDORES) {
            int i = 0;
            while (i < PROVEEDORES.size()) {
                ZoneRulesProvider p = PROVEEDORES.get(i);
                ids.addAll(p.provideZoneIds());
                i = i + 1;
            }
        }
        return ids;
    }

    /**
     * Las reglas de esa zona.
     *
     * <p>`forCaching` se acepta y **se ignora**: las reglas de esta biblioteca son valores inmutables
     * de una tabla que no cambia, asi que no hay nada que cachear ni que invalidar. El parametro
     * existe en el JDK para que un proveedor dinamico pueda devolver un objeto que no se deba
     * guardar.
     *
     * @throws ZoneRulesException si ningun proveedor conoce esa zona
     */
    public static ZoneRules getRules(String zoneId, boolean forCaching) {
        if (zoneId == null) {
            throw new NullPointerException("zoneId");
        }
        synchronized (PROVEEDORES) {
            int i = PROVEEDORES.size() - 1;
            while (i >= 0) {
                ZoneRulesProvider p = PROVEEDORES.get(i);
                ZoneRules r = p.provideRules(zoneId, forCaching);
                if (r != null) {
                    return r;
                }
                i = i - 1;
            }
        }
        throw new ZoneRulesException("Unknown time-zone ID: " + zoneId);
    }

    /**
     * Las versiones de las reglas de esa zona, de la mas vieja a la mas nueva.
     *
     * <p>Existe porque las reglas de una zona **cambian con el tiempo** --un pais mueve su horario de
     * verano-- y una fecha guardada hace cinco anios puede haberse calculado con reglas que ya no
     * son. El mapa las tiene todas, con la version de tzdb como clave.
     *
     * @throws ZoneRulesException si ningun proveedor conoce esa zona
     */
    public static NavigableMap<String, ZoneRules> getVersions(String zoneId) {
        if (zoneId == null) {
            throw new NullPointerException("zoneId");
        }
        synchronized (PROVEEDORES) {
            int i = PROVEEDORES.size() - 1;
            while (i >= 0) {
                ZoneRulesProvider p = PROVEEDORES.get(i);
                NavigableMap<String, ZoneRules> v = p.provideVersions(zoneId);
                if (v != null && !v.isEmpty()) {
                    return v;
                }
                i = i - 1;
            }
        }
        throw new ZoneRulesException("Unknown time-zone ID: " + zoneId);
    }

    /**
     * Registra un proveedor.
     *
     * <p>Queda **al final** de la lista y por lo tanto se consulta **primero**: un proveedor agregado
     * despues tapa a los anteriores para los ids que conozca, que es lo que uno quiere de un
     * agregado.
     *
     * @throws NullPointerException si `provider` es `null`
     */
    public static void registerProvider(ZoneRulesProvider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        synchronized (PROVEEDORES) {
            PROVEEDORES.add(provider);
        }
    }

    /**
     * Le pide a cada proveedor que recargue sus datos.
     *
     * @return si alguno cambio
     */
    public static boolean refresh() {
        boolean cambio = false;
        synchronized (PROVEEDORES) {
            int i = 0;
            while (i < PROVEEDORES.size()) {
                ZoneRulesProvider p = PROVEEDORES.get(i);
                if (p.provideRefresh()) {
                    cambio = true;
                }
                i = i + 1;
            }
        }
        return cambio;
    }

    // ---- lo que un proveedor implementa ------------------------------------------------------------

    /** Los ids que **este** proveedor conoce. */
    protected abstract Set<String> provideZoneIds();

    /** Las reglas de esa zona segun **este** proveedor, o `null` si no la conoce. */
    protected abstract ZoneRules provideRules(String zoneId, boolean forCaching);

    /** Las versiones de esa zona segun **este** proveedor, o vacio si no la conoce. */
    protected abstract NavigableMap<String, ZoneRules> provideVersions(String zoneId);

    /**
     * Recarga los datos de este proveedor.
     *
     * <p>No es abstracto porque la respuesta correcta para un proveedor de datos **fijos** es "no
     * cambio nada", y ese es el caso comun. Solo lo sobrescribe uno que lea de afuera.
     *
     * @return si algo cambio
     */
    protected boolean provideRefresh() {
        return false;
    }
}

// El proveedor de la tabla embebida. Es el primero de la lista y no tiene nada de especial: la unica
// diferencia con uno que alguien escriba es que este viene registrado.
//
// `provideVersions` devuelve una sola entrada porque la tabla **es** una sola version: no se guardan
// las reglas historicas de cada zona, solo las vigentes. Decirlo asi --una version, con nombre-- es
// mas honesto que devolver vacio, que significaria "no conozco esta zona".
final class TzDataProvider extends ZoneRulesProvider {

    protected Set<String> provideZoneIds() {
        Set<String> ids = new HashSet<String>();
        String[] todas = TzData.zoneIds();
        int i = 0;
        while (i < todas.length) {
            ids.add(todas[i]);
            i = i + 1;
        }
        return ids;
    }

    protected ZoneRules provideRules(String zoneId, boolean forCaching) {
        int index = TzData.zoneIndex(zoneId);
        if (index < 0) {
            return null;
        }
        return ZoneRules.ofZone(index);
    }

    protected NavigableMap<String, ZoneRules> provideVersions(String zoneId) {
        TreeMap<String, ZoneRules> versiones = new TreeMap<String, ZoneRules>();
        ZoneRules r = this.provideRules(zoneId, false);
        if (r != null) {
            versiones.put("KajiTzData", r);
        }
        return versiones;
    }
}
