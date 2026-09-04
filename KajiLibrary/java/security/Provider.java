package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

// Un catalogo de implementaciones criptograficas: que algoritmos sabe hacer, y con que clase.
//
// ===============================================================================================
// POR QUE ES UN `Properties` Y NO UN MAPA CUALQUIERA
// ===============================================================================================
//
// Que herede de `Properties` parece un accidente y es historia: en 1.2 un proveedor **era** un
// archivo de propiedades, con lineas como
//
//     MessageDigest.SHA-256 = com.ejemplo.SHA256
//     Alg.Alias.MessageDigest.SHA256 = SHA-256
//     MessageDigest.SHA-256 ImplementedIn = Software
//
// y toda la busqueda de algoritmos era buscar una clave. En 1.5 se agrego `Provider.Service`, que
// es la forma tipada de decir lo mismo, pero las propiedades no se pudieron sacar porque habia
// codigo leyendolas. Entonces esta clase mantiene **las dos vistas sincronizadas**: `putService`
// escribe tambien las propiedades, y cualquier cambio en las propiedades invalida la tabla de
// servicios para que se reconstruya. Esa es la mayor parte de lo que hace este archivo.
//
// La reconstruccion es perezosa —una bandera y un rearmado completo— y no incremental a proposito:
// un `putAll` o un `replaceAll` pueden tocar cualquier cosa, y seguirlos de a un cambio es donde
// se cuelan las inconsistencias.
//
// ===============================================================================================
// LO QUE NO ESTA
// ===============================================================================================
//
// `Provider` es abstracta y esta clase no registra ningun algoritmo: los algoritmos los pone quien
// la extienda. En esta biblioteca el unico que lo hace es `KajiProvider`, con los seis digests.
//
// No esta `getDefaultSecureRandomService()` porque es package-private en el JDK y no hay
// `SecureRandom` que lo consuma.
public abstract class Provider extends Properties {

    private final String name;
    private final String versionStr;
    private final double version;
    private final String info;

    // Servicios puestos con `putService`, por "tipo.algoritmo" en minusculas.
    private final Map<String, Service> servicios = new LinkedHashMap<String, Service>();

    // "tipo.alias" -> "tipo.algoritmo". Los alias de un servicio tipado se resuelven aca y no
    // releyendo las propiedades: si no, buscar por alias caeria en la copia deducida, que no sabe
    // instanciarse igual que la original.
    private final Map<String, String> aliasDeServicio = new HashMap<String, String>();

    // Servicios deducidos de las propiedades. Se rearma entero cuando `legacyCambiado`.
    private Map<String, Service> legacy = new LinkedHashMap<String, Service>();

    private boolean legacyCambiado = true;

    // Corta la recursion: `putService` escribe propiedades, y esas escrituras no tienen que
    // invalidar la tabla que las genero.
    private boolean escribiendoServicio;

    // Un proveedor con version numerica.
    //
    // Deprecado en el JDK: un `double` no sabe expresar "1.2.3" ni "21-ea", y comparar versiones
    // por resta de flotantes ordena mal en cuanto hay tres componentes.
    @Deprecated
    protected Provider(String name, double version, String info) {
        this.name = name;
        this.version = version;
        this.versionStr = Double.toString(version);
        this.info = info;
    }

    protected Provider(String name, String versionStr, String info) {
        this.name = name;
        this.versionStr = versionStr;
        this.version = parsearVersion(versionStr);
        this.info = info;
    }

    // El `double` que corresponde a los dos primeros componentes de la cadena.
    //
    // Solo existe para que `getVersion()` siga contestando algo razonable. Si no se puede leer,
    // devuelve 0: inventar un numero seria peor que decir "no se".
    private static double parsearVersion(String s) {
        if (s == null) {
            return 0d;
        }
        int i = 0;
        int puntos = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '.') {
                puntos = puntos + 1;
                if (puntos > 1) {
                    break;
                }
            } else if (c < '0' || c > '9') {
                break;
            }
            i = i + 1;
        }
        if (i == 0) {
            return 0d;
        }
        try {
            return Double.parseDouble(s.substring(0, i));
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    // Devuelve un proveedor configurado con `configArg`.
    //
    // La implementacion base **no** sabe configurarse y lo dice tirando. Es el comportamiento del
    // JDK y es el correcto: solo un proveedor que tenga algo que configurar —tipicamente uno que
    // hable con un token PKCS#11— puede saber que significa el argumento.
    public Provider configure(String configArg) {
        throw new UnsupportedOperationException("configure is not supported");
    }

    // Si este proveedor no necesita configuracion, o ya la recibio. La base siempre esta lista.
    public boolean isConfigured() {
        return true;
    }

    public String getName() {
        return this.name;
    }

    @Deprecated
    public double getVersion() {
        return this.version;
    }

    public String getVersionStr() {
        return this.versionStr;
    }

    public String getInfo() {
        return this.info;
    }

    @Override
    public String toString() {
        return this.name + " version " + this.versionStr;
    }

    // -------------------------------------------------------------------------------------------
    // La vista de propiedades. Todo lo que muta invalida la tabla de servicios deducida.
    // -------------------------------------------------------------------------------------------

    @Override
    public synchronized void clear() {
        this.servicios.clear();
        this.aliasDeServicio.clear();
        this.legacyCambiado = true;
        super.clear();
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        this.legacyCambiado = true;
        super.load(inStream);
    }

    @Override
    public synchronized void putAll(Map<?, ?> t) {
        this.legacyCambiado = true;
        super.putAll(t);
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        if (!this.escribiendoServicio) {
            this.legacyCambiado = true;
        }
        return super.put(key, value);
    }

    @Override
    public synchronized Object putIfAbsent(Object key, Object value) {
        this.legacyCambiado = true;
        return super.putIfAbsent(key, value);
    }

    @Override
    public synchronized Object remove(Object key) {
        if (!this.escribiendoServicio) {
            this.legacyCambiado = true;
        }
        return super.remove(key);
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        this.legacyCambiado = true;
        return super.remove(key, value);
    }

    @Override
    public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
        this.legacyCambiado = true;
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public synchronized Object replace(Object key, Object value) {
        this.legacyCambiado = true;
        return super.replace(key, value);
    }

    @Override
    public synchronized void replaceAll(
            BiFunction<? super Object, ? super Object, ? extends Object> function) {
        this.legacyCambiado = true;
        super.replaceAll(function);
    }

    @Override
    public synchronized Object compute(Object key,
            BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        this.legacyCambiado = true;
        return super.compute(key, remappingFunction);
    }

    @Override
    public synchronized Object computeIfAbsent(Object key,
            Function<? super Object, ? extends Object> mappingFunction) {
        this.legacyCambiado = true;
        return super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public synchronized Object computeIfPresent(Object key,
            BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        this.legacyCambiado = true;
        return super.computeIfPresent(key, remappingFunction);
    }

    @Override
    public synchronized Object merge(Object key, Object value,
            BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        this.legacyCambiado = true;
        return super.merge(key, value, remappingFunction);
    }

    // Las tres vistas se entregan **inmodificables**. No es prolijidad: mutar por la vista se
    // saltearia los `put` de arriba y dejaria la tabla de servicios describiendo un catalogo que
    // ya no es el que esta.
    @Override
    public synchronized Set<Map.Entry<Object, Object>> entrySet() {
        return Collections.unmodifiableSet(super.entrySet());
    }

    @Override
    public Set<Object> keySet() {
        return Collections.unmodifiableSet(super.keySet());
    }

    @Override
    public Collection<Object> values() {
        return Collections.unmodifiableCollection(super.values());
    }

    // -------------------------------------------------------------------------------------------
    // La vista de servicios.
    // -------------------------------------------------------------------------------------------

    // El servicio que implementa `algorithm` para `type`, o null.
    //
    // Busca primero entre los puestos con `putService` y despues entre los deducidos de las
    // propiedades: si los dos definen el mismo par, gana el tipado, que es el que trae los
    // atributos completos.
    public synchronized Service getService(String type, String algorithm) {
        if (type == null || algorithm == null) {
            throw new NullPointerException();
        }
        String clave = clave(type, algorithm);
        Service s = this.servicios.get(clave);
        if (s != null) {
            return s;
        }
        String canon = this.aliasDeServicio.get(clave);
        if (canon != null) {
            s = this.servicios.get(canon);
            if (s != null) {
                return s;
            }
        }
        this.asegurarLegacy();
        return this.legacy.get(clave);
    }

    // Todos los servicios de este proveedor, sin repetir.
    //
    // Se deduplica por el par (tipo, algoritmo) **del servicio**, no por la clave con que se lo
    // encontro: un algoritmo con tres alias esta tres veces en las tablas de busqueda y tiene que
    // salir una sola vez de aca. Cuando el mismo par aparece tipado y deducido, gana el tipado.
    public synchronized Set<Service> getServices() {
        this.asegurarLegacy();
        LinkedHashMap<String, Service> vistos = new LinkedHashMap<String, Service>();
        Iterator<String> it = this.legacy.keySet().iterator();
        while (it.hasNext()) {
            Service s = this.legacy.get(it.next());
            vistos.put(clave(s.getType(), s.getAlgorithm()), s);
        }
        Iterator<String> it2 = this.servicios.keySet().iterator();
        while (it2.hasNext()) {
            Service s = this.servicios.get(it2.next());
            vistos.put(clave(s.getType(), s.getAlgorithm()), s);
        }
        LinkedHashSet<Service> out = new LinkedHashSet<Service>();
        Iterator<String> it3 = vistos.keySet().iterator();
        while (it3.hasNext()) {
            out.add(vistos.get(it3.next()));
        }
        return Collections.unmodifiableSet(out);
    }

    // Agrega un servicio, y escribe tambien las propiedades equivalentes para que quien lea el
    // proveedor a la vieja usanza vea lo mismo.
    protected synchronized void putService(Service s) {
        if (s == null) {
            throw new NullPointerException();
        }
        if (s.getProvider() != this) {
            throw new IllegalArgumentException(
                "service.getProvider() must match this Provider object");
        }
        String canon = clave(s.getType(), s.getAlgorithm());
        this.servicios.put(canon, s);
        this.escribiendoServicio = true;
        try {
            super.put(s.getType() + "." + s.getAlgorithm(), s.getClassName());
            List<String> alias = s.getAliases();
            int i = 0;
            while (i < alias.size()) {
                this.aliasDeServicio.put(clave(s.getType(), alias.get(i)), canon);
                super.put("Alg.Alias." + s.getType() + "." + alias.get(i), s.getAlgorithm());
                i = i + 1;
            }
            Iterator<String> at = s.nombresDeAtributos().iterator();
            while (at.hasNext()) {
                String a = at.next();
                super.put(s.getType() + "." + s.getAlgorithm() + " " + a, s.getAttribute(a));
            }
        } finally {
            this.escribiendoServicio = false;
        }
    }

    protected synchronized void removeService(Service s) {
        if (s == null) {
            throw new NullPointerException();
        }
        this.servicios.remove(clave(s.getType(), s.getAlgorithm()));
        this.escribiendoServicio = true;
        try {
            super.remove(s.getType() + "." + s.getAlgorithm());
            List<String> alias = s.getAliases();
            int i = 0;
            while (i < alias.size()) {
                this.aliasDeServicio.remove(clave(s.getType(), alias.get(i)));
                super.remove("Alg.Alias." + s.getType() + "." + alias.get(i));
                i = i + 1;
            }
            Iterator<String> at = s.nombresDeAtributos().iterator();
            while (at.hasNext()) {
                super.remove(s.getType() + "." + s.getAlgorithm() + " " + at.next());
            }
        } finally {
            this.escribiendoServicio = false;
        }
    }

    // Los nombres de tipo y algoritmo son insensibles a mayusculas: "SHA-256" y "sha-256" son el
    // mismo algoritmo, y el catalogo tiene que encontrarlo escrito de cualquier forma.
    private static String clave(String type, String algorithm) {
        return type.toLowerCase() + "." + algorithm.toLowerCase();
    }

    // Rearma la tabla deducida si alguna propiedad cambio.
    //
    // Tres pasadas porque las lineas pueden venir en cualquier orden: primero las que definen
    // clases, despues los atributos —que necesitan que el servicio ya exista— y por ultimo los
    // alias, que apuntan a un algoritmo que puede haberse definido despues.
    private void asegurarLegacy() {
        if (!this.legacyCambiado) {
            return;
        }
        this.legacyCambiado = false;
        LinkedHashMap<String, Service> nuevo = new LinkedHashMap<String, Service>();
        ArrayList<String[]> atributos = new ArrayList<String[]>();
        ArrayList<String[]> alias = new ArrayList<String[]>();

        Iterator<Map.Entry<Object, Object>> it = super.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Object> e = it.next();
            if (!(e.getKey() instanceof String) || !(e.getValue() instanceof String)) {
                continue;
            }
            String k = ((String) e.getKey()).trim();
            String v = ((String) e.getValue()).trim();
            if (k.startsWith("Alg.Alias.")) {
                String resto = k.substring("Alg.Alias.".length());
                int p = resto.indexOf('.');
                if (p > 0 && p < resto.length() - 1) {
                    alias.add(new String[] {resto.substring(0, p), resto.substring(p + 1), v});
                }
                continue;
            }
            int esp = k.indexOf(' ');
            if (esp > 0) {
                String izq = k.substring(0, esp);
                String attr = k.substring(esp + 1).trim();
                int p = izq.indexOf('.');
                if (p > 0 && p < izq.length() - 1 && !attr.isEmpty()) {
                    atributos.add(new String[] {izq.substring(0, p), izq.substring(p + 1), attr, v});
                }
                continue;
            }
            int p = k.indexOf('.');
            if (p > 0 && p < k.length() - 1) {
                String type = k.substring(0, p);
                String alg = k.substring(p + 1);
                nuevo.put(clave(type, alg),
                    new Service(this, type, alg, v, new ArrayList<String>(),
                                new HashMap<String, String>()));
            }
        }

        int i = 0;
        while (i < atributos.size()) {
            String[] a = atributos.get(i);
            Service s = nuevo.get(clave(a[0], a[1]));
            if (s != null) {
                s.addAttribute(a[2], a[3]);
            }
            i = i + 1;
        }

        i = 0;
        while (i < alias.size()) {
            String[] a = alias.get(i);
            Service s = nuevo.get(clave(a[0], a[2]));
            if (s != null) {
                s.agregarAlias(a[1]);
                nuevo.put(clave(a[0], a[1]), s);
            }
            i = i + 1;
        }
        this.legacy = nuevo;
    }

    // ===========================================================================================
    // Un algoritmo concreto ofrecido por un proveedor.
    // ===========================================================================================
    //
    // Lo que aporta sobre la linea de propiedades equivalente es que **sabe instanciarse**: en vez
    // de que cada fabrica lea un nombre de clase y haga reflexion por su cuenta, se le pide al
    // servicio y el decide como. Un proveedor que tenga las clases a mano puede subclasear esto y
    // devolverlas directamente, sin reflexion — que es lo que hace `KajiProvider`.
    public static class Service {

        private final Provider provider;
        private final String type;
        private final String algorithm;
        private final String className;
        private final List<String> aliases;
        private final Map<String, String> attributes;

        public Service(Provider provider, String type, String algorithm, String className,
                       List<String> aliases, Map<String, String> attributes) {
            if (provider == null || type == null || algorithm == null || className == null) {
                throw new NullPointerException();
            }
            this.provider = provider;
            this.type = type;
            this.algorithm = algorithm;
            this.className = className;
            this.aliases = aliases == null
                ? new ArrayList<String>() : new ArrayList<String>(aliases);
            this.attributes = new HashMap<String, String>();
            if (attributes != null) {
                Iterator<String> it = attributes.keySet().iterator();
                while (it.hasNext()) {
                    String k = it.next();
                    this.attributes.put(k.toLowerCase(), attributes.get(k));
                }
            }
        }

        public final String getType() {
            return this.type;
        }

        public final String getAlgorithm() {
            return this.algorithm;
        }

        public final Provider getProvider() {
            return this.provider;
        }

        public final String getClassName() {
            return this.className;
        }

        public final String getAttribute(String name) {
            if (name == null) {
                throw new NullPointerException();
            }
            return this.attributes.get(name.toLowerCase());
        }

        // Los alias de este algoritmo. Package-private en el JDK; aca tambien.
        final List<String> getAliases() {
            return this.aliases;
        }

        final void addAttribute(String type, String value) {
            this.attributes.put(type.toLowerCase(), value);
        }

        final void removeAttribute(String type, String value) {
            this.attributes.remove(type.toLowerCase());
        }

        final void agregarAlias(String alias) {
            if (!this.aliases.contains(alias)) {
                this.aliases.add(alias);
            }
        }

        final Set<String> nombresDeAtributos() {
            return this.attributes.keySet();
        }

        // Una instancia nueva de la implementacion.
        //
        // La base carga la clase por nombre y usa el constructor sin argumentos. `constructorParameter`
        // solo lo aceptan unos pocos tipos de servicio en el JDK —los que reciben una clave o unos
        // parametros al construirse— y ninguno de ellos existe en esta biblioteca, asi que aca
        // pasar algo distinto de null es un error del llamador y se dice como tal.
        public Object newInstance(Object constructorParameter)
                throws NoSuchAlgorithmException {
            if (constructorParameter != null) {
                throw new InvalidParameterException(
                    "constructorParameter not used with " + this.type + " engines");
            }
            try {
                Class<?> c = Class.forName(this.className);
                return c.newInstance();
            } catch (Exception e) {
                throw new NoSuchAlgorithmException(
                    "Error constructing implementation (algorithm: " + this.algorithm
                    + ", provider: " + this.provider.getName() + ", class: " + this.className
                    + ")", e);
            }
        }

        // Si este servicio puede usarse con el parametro dado.
        //
        // Ningun tipo de servicio de esta biblioteca usa parametro, asi que la unica respuesta
        // honesta para un parametro no nulo es rechazarlo, y para null es que si.
        public boolean supportsParameter(Object parameter) {
            if (parameter != null) {
                throw new InvalidParameterException(
                    "supportsParameter() not used with " + this.type + " engines");
            }
            return true;
        }

        @Override
        public String toString() {
            String s = this.provider.getName() + ": " + this.type + "." + this.algorithm
                + " -> " + this.className;
            if (!this.aliases.isEmpty()) {
                s = s + "\n  aliases: " + this.aliases.toString();
            }
            if (!this.attributes.isEmpty()) {
                s = s + "\n  attributes: " + this.attributes.toString();
            }
            return s + "\n";
        }
    }
}
