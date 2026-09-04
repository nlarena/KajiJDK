package java.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

// La lista de proveedores del proceso, en orden, y las propiedades de seguridad.
//
// ===============================================================================================
// EL ORDEN ES EL API
// ===============================================================================================
//
// Lo unico que hace esta clase es mantener una lista **ordenada**, y ese orden es toda su
// semantica: `MessageDigest.getInstance("SHA-256")` se queda con el primer proveedor que lo
// ofrezca. Por eso `insertProviderAt(p, 1)` es una operacion privilegiada de verdad — mete un
// proveedor adelante de todos y con eso redefine que codigo corre detras de cada algoritmo del
// proceso, sin tocar una linea del que lo llama.
//
// ===============================================================================================
// LO QUE NO ESTA, Y POR QUE
// ===============================================================================================
//
// **No lee el archivo `java.security` del sistema.** En un JDK real la lista inicial de proveedores
// y las propiedades salen de `$JAVA_HOME/conf/security/java.security`. Aca la lista inicial es un
// solo proveedor —`KajiProvider`, con los digests implementados en esta biblioteca— y las
// propiedades arrancan vacias. Leer el archivo del JDK que este instalado seria peor que no
// leerlo: prometeria algoritmos que esta biblioteca no tiene.
//
// **No hay descubrimiento por `ServiceLoader`.** Un proveedor se agrega llamando a `addProvider`.
//
// **No hay chequeo de permisos.** En el JDK cada uno de estos metodos consulta un
// `SecurityPermission` con el `SecurityManager`, que desde JDK 24 esta permanentemente
// deshabilitado y no chequea nada. No se simula un control que no existe.
public final class Security {

    // La lista, en orden de preferencia.
    private static final ArrayList<Provider> proveedores = new ArrayList<Provider>();

    // Las propiedades de seguridad. Arrancan vacias: ver la cabecera.
    private static final Properties props = new Properties();

    static {
        proveedores.add(new KajiProvider());
    }

    // Estatica pura: no se instancia.
    private Security() {
    }

    // Deprecado desde 1.2 y sin reemplazo directo. Busca una propiedad de la forma
    // "<propName>.<algName>" y devuelve su valor.
    @Deprecated
    public static String getAlgorithmProperty(String algName, String propName) {
        if (algName == null || propName == null) {
            return null;
        }
        return props.getProperty(propName + "." + algName);
    }

    // Inserta el proveedor en la posicion dada (1 es la primera) y devuelve donde quedo, o -1 si
    // ya habia uno con ese nombre.
    //
    // Devolver -1 en vez de reemplazar es deliberado: si insertar pisara al que ya estaba, agregar
    // un proveedor propio podria desactivar en silencio a otro con el mismo nombre. Para cambiarlo
    // hay que sacarlo primero, y eso se ve en el codigo.
    public static synchronized int insertProviderAt(Provider provider, int position) {
        String nombre = provider.getName();
        if (getProvider(nombre) != null) {
            return -1;
        }
        int n = proveedores.size();
        if (position < 1 || position > n) {
            position = n + 1;
        }
        proveedores.add(position - 1, provider);
        return position;
    }

    // Agrega el proveedor al final. Devuelve su posicion, o -1 si ya estaba.
    public static int addProvider(Provider provider) {
        return insertProviderAt(provider, 0);
    }

    // Saca el proveedor con ese nombre. Si no esta, no hace nada — no es un error.
    //
    // Los que quedan **se corren hacia adelante**: sacar el segundo de tres deja al tercero
    // segundo. Un proveedor que quiera conservar su posicion tiene que volver a insertarse.
    public static synchronized void removeProvider(String name) {
        int i = 0;
        while (i < proveedores.size()) {
            if (proveedores.get(i).getName().equals(name)) {
                proveedores.remove(i);
                return;
            }
            i = i + 1;
        }
    }

    // Todos los proveedores, en orden. Es una copia: reordenar el arreglo devuelto no reordena
    // nada.
    public static synchronized Provider[] getProviders() {
        Provider[] a = new Provider[proveedores.size()];
        int i = 0;
        while (i < proveedores.size()) {
            a[i] = proveedores.get(i);
            i = i + 1;
        }
        return a;
    }

    public static synchronized Provider getProvider(String name) {
        int i = 0;
        while (i < proveedores.size()) {
            Provider p = proveedores.get(i);
            if (p.getName().equals(name)) {
                return p;
            }
            i = i + 1;
        }
        return null;
    }

    // Los proveedores que satisfacen el filtro, o null si ninguno.
    //
    // El filtro es una cadena con dos formas: "MessageDigest.SHA-256" —tiene el servicio— o
    // "MessageDigest.SHA-256 ImplementedIn:Software" —lo tiene y con ese atributo. Devolver null
    // en vez de un arreglo vacio es feo pero es el contrato, y hay codigo que compara contra null.
    public static Provider[] getProviders(String filter) {
        if (filter == null) {
            throw new NullPointerException("filter cannot be null");
        }
        String f = filter.trim();
        if (f.isEmpty()) {
            throw new InvalidParameterException("filter cannot be empty");
        }
        String clave = f;
        String valor = null;
        int dosPuntos = f.indexOf(':');
        if (dosPuntos >= 0) {
            clave = f.substring(0, dosPuntos).trim();
            valor = f.substring(dosPuntos + 1).trim();
        }
        java.util.HashMap<String, String> m = new java.util.HashMap<String, String>();
        m.put(clave, valor);
        return getProviders(m);
    }

    // La version con varios filtros: un proveedor tiene que cumplirlos **todos**.
    public static Provider[] getProviders(Map<String, String> filter) {
        if (filter == null) {
            throw new NullPointerException("filter cannot be null");
        }
        if (filter.isEmpty()) {
            return getProviders();
        }
        Provider[] todos = getProviders();
        ArrayList<Provider> ok = new ArrayList<Provider>();
        int i = 0;
        while (i < todos.length) {
            if (cumple(todos[i], filter)) {
                ok.add(todos[i]);
            }
            i = i + 1;
        }
        if (ok.isEmpty()) {
            return null;
        }
        Provider[] a = new Provider[ok.size()];
        int j = 0;
        while (j < ok.size()) {
            a[j] = ok.get(j);
            j = j + 1;
        }
        return a;
    }

    private static boolean cumple(Provider p, Map<String, String> filter) {
        Iterator<String> it = filter.keySet().iterator();
        while (it.hasNext()) {
            String clave = it.next();
            if (clave == null) {
                return false;
            }
            String k = clave.trim();
            int punto = k.indexOf('.');
            if (punto <= 0 || punto >= k.length() - 1) {
                throw new InvalidParameterException("Invalid filter key: " + clave);
            }
            String tipo = k.substring(0, punto);
            String resto = k.substring(punto + 1);
            String atributo = null;
            int esp = resto.indexOf(' ');
            if (esp > 0) {
                atributo = resto.substring(esp + 1).trim();
                resto = resto.substring(0, esp);
            }
            Provider.Service s = p.getService(tipo, resto);
            if (s == null) {
                return false;
            }
            if (atributo != null) {
                String tiene = s.getAttribute(atributo);
                String quiere = filter.get(clave);
                if (tiene == null) {
                    return false;
                }
                if (quiere != null && !quiere.isEmpty() && !quiere.equalsIgnoreCase(tiene)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static void setProperty(String key, String datum) {
        props.put(key, datum);
    }

    // Los nombres de algoritmo disponibles para un tipo de servicio, en mayusculas y sin repetir.
    public static Set<String> getAlgorithms(String serviceName) {
        if (serviceName == null) {
            throw new NullPointerException("serviceName cannot be null");
        }
        if (serviceName.isEmpty()) {
            return new HashSet<String>();
        }
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        Provider[] todos = getProviders();
        int i = 0;
        while (i < todos.length) {
            Iterator<Provider.Service> it = todos[i].getServices().iterator();
            while (it.hasNext()) {
                Provider.Service s = it.next();
                if (s.getType().equalsIgnoreCase(serviceName)) {
                    out.add(s.getAlgorithm().toUpperCase());
                }
            }
            i = i + 1;
        }
        return out;
    }
}
