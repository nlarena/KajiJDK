package javax.management;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El nombre de un MBean: {@code dominio:clave=valor,clave=valor,...}.
 *
 * <p>Es la clase con mas logica de todo el paquete, y no por gusto. Un `ObjectName` cumple tres
 * papeles a la vez y cada uno le impone una condicion:
 *
 * <ul>
 *   <li><b>identidad</b> -- dos nombres son el mismo MBean si sus <i>formas canonicas</i> coinciden.
 *       De ahi que {@code d:b=2,a=1} y {@code d:a=1,b=2} sean iguales y compartan `hashCode`: el
 *       orden en que se escribieron las claves no significa nada;
 *   <li><b>literal</b> -- pero {@link #toString()} devuelve el orden <i>original</i>, no el
 *       canonico. Quien escribio el nombre lo vuelve a ver como lo escribio;
 *   <li><b>patron</b> -- implementa {@link QueryExp}, asi que un nombre con comodines es al mismo
 *       tiempo una consulta que se aplica con {@link #apply}.
 * </ul>
 *
 * <h2>Tres clases de comodin, no una</h2>
 *
 * <p>El paquete distingue tres cosas que suelen confundirse, y cada una tiene su predicado:
 *
 * <ul>
 *   <li>{@link #isDomainPattern()} -- el <b>dominio</b> lleva {@code *} o {@code ?}: {@code *:k=v};
 *   <li>{@link #isPropertyListPattern()} -- hay un {@code *} suelto en la lista, que significa
 *       "ademas de estas claves, cualquier otra": {@code d:k=v,*};
 *   <li>{@link #isPropertyValuePattern()} -- algun <b>valor</b> lleva comodin: {@code d:k=a*b}.
 * </ul>
 *
 * <p>El {@code *} de la lista se escriba donde se escriba, la forma canonica lo manda al final:
 * {@code d:*,k=v} canoniza a {@code d:k=v,*}.
 *
 * <h2>Citar no apaga los comodines; la barra si</h2>
 *
 * <p>Es la sutileza que mas sorprende, y se verifico contra el JDK: {@code d:k="a*b"} <b>es</b> un
 * patron de valor. Las comillas sirven para meter en un valor caracteres que la gramatica prohibe
 * ({@code , : = "}), no para apagar el comodin. Lo que apaga el comodin es la barra invertida:
 * {@code d:k="a\*b"} es un valor literal. Por eso {@link #quote} escapa {@code *} y {@code ?}: lo
 * que sale de ahi nunca es un patron.
 *
 * <h2>Serializacion: se declara, no se inventa</h2>
 *
 * <p>Se hereda `Serializable` de {@link QueryExp} porque el contrato lo pide -- un `ObjectName`
 * viaja al agente remoto. Pero el JDK le da <b>forma serializada propia</b>, con `writeObject` y
 * `readObject` que escriben la cadena y reconstruyen el analisis. Esta biblioteca <b>no</b> tiene
 * `ObjectOutputStream`, asi que esos dos metodos no estan. Declarar la interfaz es honesto:
 * describe el contrato. Escribir un `writeObject` que nadie puede llamar y que no coincidiria con
 * el formato del JDK seria mentir.
 */
public class ObjectName implements Comparable<ObjectName>, QueryExp {

    private static final long serialVersionUID = 1081892073854801359L;

    /** El nombre que matchea todo: {@code *:*}. */
    public static final ObjectName WILDCARD = comodin();

    /**
     * Una clave con su valor tal cual se escribio (el valor conserva las comillas si las tenia) y
     * si ese valor es un patron.
     */
    private static class Propiedad {
        final String clave;
        final String valor;
        final boolean patron;

        Propiedad(String clave, String valor, boolean patron) {
            this.clave = clave;
            this.valor = valor;
            this.patron = patron;
        }
    }

    private String dominio;
    private Propiedad[] enOrden;      // como se escribieron
    private Propiedad[] canonicas;    // ordenadas por clave
    private String canonico;
    private boolean patronDominio;
    private boolean patronLista;
    private boolean patronValor;

    /** Uso interno: arma {@code *:*} sin pasar por el analizador. */
    private static ObjectName comodin() {
        ObjectName n = new ObjectName();
        n.dominio = "*";
        n.enOrden = new Propiedad[0];
        n.canonicas = new Propiedad[0];
        n.patronDominio = true;
        n.patronLista = true;
        n.canonico = "*:*";
        return n;
    }

    private ObjectName() {
    }

    /**
     * Analiza {@code dominio:lista}.
     *
     * <p>La cadena vacia no es un error: significa {@code *:*}. Es asi en el JDK y es lo que hace
     * que {@code new ObjectName("")} sea un comodin util y no una excepcion.
     */
    public ObjectName(String name) throws MalformedObjectNameException {
        construir(name);
    }

    /** Atajo para el caso de una sola clave. */
    public ObjectName(String domain, String key, String value) throws MalformedObjectNameException {
        Map<String, String> tabla = new LinkedHashMap<String, String>();
        if (key == null) {
            throw new NullPointerException("Invalid key (null)");
        }
        if (value == null) {
            throw new NullPointerException("Invalid value (null)");
        }
        tabla.put(key, value);
        construir(domain, tabla);
    }

    /**
     * Con la lista de claves ya armada.
     *
     * <p>El orden literal sale del recorrido de la tabla: una `Hashtable` no promete ninguno, asi
     * que {@link #toString()} de un nombre armado asi no es predecible. La forma <b>canonica</b> si
     * lo es, y es la que define la identidad.
     */
    public ObjectName(String domain, Hashtable<String, String> table)
            throws MalformedObjectNameException {
        if (table == null) {
            throw new NullPointerException("key property list cannot be null");
        }
        Map<String, String> copia = new LinkedHashMap<String, String>();
        Iterator<Map.Entry<String, String>> it = table.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getKey() == null) {
                throw new NullPointerException("Invalid key (null)");
            }
            if (e.getValue() == null) {
                throw new NullPointerException("Invalid value (null)");
            }
            copia.put(e.getKey(), e.getValue());
        }
        construir(domain, copia);
    }

    // ---- fabricas ------------------------------------------------------------------------------

    /**
     * Igual que el constructor, pero puede devolver una instancia compartida.
     *
     * <p>El JDK la prefiere sobre `new` porque una subclase de `ObjectName` no se puede confiar --
     * podria mentir en `equals`-- y esta fabrica siempre devuelve un `ObjectName` exacto.
     */
    public static ObjectName getInstance(String name) throws MalformedObjectNameException {
        return new ObjectName(name);
    }

    public static ObjectName getInstance(String domain, String key, String value)
            throws MalformedObjectNameException {
        return new ObjectName(domain, key, value);
    }

    public static ObjectName getInstance(String domain, Hashtable<String, String> table)
            throws MalformedObjectNameException {
        return new ObjectName(domain, table);
    }

    /**
     * Devuelve `name` si ya es un `ObjectName` exacto; si es de una subclase, lo copia.
     *
     * <p>Ese es todo el punto del metodo: garantizar que lo devuelto no sea una subclase con
     * comportamiento propio.
     */
    public static ObjectName getInstance(ObjectName name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        if (name.getClass() == ObjectName.class) {
            return name;
        }
        ObjectName copia = new ObjectName();
        copia.dominio = name.dominio;
        copia.enOrden = name.enOrden;
        copia.canonicas = name.canonicas;
        copia.canonico = name.canonico;
        copia.patronDominio = name.patronDominio;
        copia.patronLista = name.patronLista;
        copia.patronValor = name.patronValor;
        return copia;
    }

    // ---- analisis ------------------------------------------------------------------------------

    private void construir(String name) throws MalformedObjectNameException {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        if (name.length() == 0) {
            dominio = "*";
            enOrden = new Propiedad[0];
            canonicas = new Propiedad[0];
            patronDominio = true;
            patronLista = true;
            canonico = "*:*";
            return;
        }
        int corte = name.indexOf(':');
        if (corte < 0) {
            throw new MalformedObjectNameException("Key properties cannot be empty");
        }
        dominio = name.substring(0, corte);
        revisarDominio(dominio);
        patronDominio = dominio.indexOf('*') >= 0 || dominio.indexOf('?') >= 0;

        String lista = name.substring(corte + 1);
        if (lista.length() == 0) {
            throw new MalformedObjectNameException("Key properties cannot be empty");
        }
        analizarLista(lista);
        armarCanonico();
    }

    /**
     * El dominio solo prohibe dos caracteres.
     *
     * <p>Los `:` ya no pueden estar porque cortamos en el primero; el salto de linea si hay que
     * mirarlo. Todo lo demas --espacios, comas, comillas, iguales-- es dominio valido, por raro que
     * parezca; se comprobo contra el JDK.
     */
    private static void revisarDominio(String d) throws MalformedObjectNameException {
        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (c == ':') {
                throw new MalformedObjectNameException("Invalid character ':' in domain name");
            }
            if (c == '\n') {
                throw new MalformedObjectNameException("Invalid character '\\n' in domain name");
            }
        }
    }

    private void analizarLista(String s) throws MalformedObjectNameException {
        // Una `List` y no un `LinkedHashMap`. La razon es concreta: el orden de escritura es parte
        // del contrato --`toString()` tiene que devolver el nombre tal como se escribio-- y el
        // `LinkedHashMap` de esta biblioteca declara que sus vistas (`keySet`, y por lo tanto
        // `values` y `entrySet`) no conservan el orden de insercion. Depender de el hacia que
        // `d:z=1,a=2,m=3` se imprimiera reordenado. La deteccion de claves repetidas se hace con un
        // recorrido: las listas de claves de un MBean son de unos pocos elementos.
        List<Propiedad> vistas = new ArrayList<Propiedad>();
        int i = 0;
        int n = s.length();
        while (true) {
            if (s.charAt(i) == '*') {
                if (patronLista) {
                    throw new MalformedObjectNameException(
                            "Cannot have several '*' characters in pattern property list");
                }
                patronLista = true;
                i++;
                if (i == n) {
                    break;
                }
                if (s.charAt(i) != ',') {
                    throw new MalformedObjectNameException(
                            "Invalid character found after '*': end of name or ',' expected");
                }
                i++;
                // Rareza comprobada del JDK: despues de "*," se acepta el fin de cadena, aunque
                // despues de "k=v," no. No es simetrico y se respeta tal cual.
                if (i == n) {
                    break;
                }
                continue;
            }

            int arranqueClave = i;
            while (i < n && s.charAt(i) != '=') {
                char c = s.charAt(i);
                if (c == ':' || c == ',' || c == '*' || c == '?' || c == '\n') {
                    throw new MalformedObjectNameException(
                            "Invalid character '" + c + "' in key part of property");
                }
                i++;
            }
            if (i == n) {
                throw new MalformedObjectNameException("Unterminated key property part");
            }
            String clave = s.substring(arranqueClave, i);
            if (clave.length() == 0) {
                throw new MalformedObjectNameException("Invalid key (empty)");
            }
            i++; // el '='

            int arranqueValor = i;
            boolean valorPatron = false;
            if (i < n && s.charAt(i) == '"') {
                i++;
                boolean cerrado = false;
                while (i < n) {
                    char c = s.charAt(i);
                    if (c == '\\') {
                        if (i + 1 >= n) {
                            throw new MalformedObjectNameException("Unterminated quoted value");
                        }
                        char e = s.charAt(i + 1);
                        if (e != 'n' && e != '\\' && e != '"' && e != '*' && e != '?') {
                            throw new MalformedObjectNameException(
                                    "Invalid escape sequence '\\" + e + "' in quoted value");
                        }
                        i += 2;
                        continue;
                    }
                    if (c == '\n') {
                        throw new MalformedObjectNameException("Newline in quoted value");
                    }
                    if (c == '*' || c == '?') {
                        valorPatron = true;
                        i++;
                        continue;
                    }
                    if (c == '"') {
                        cerrado = true;
                        i++;
                        break;
                    }
                    i++;
                }
                if (!cerrado) {
                    throw new MalformedObjectNameException("Unterminated quoted value");
                }
                if (i < n && s.charAt(i) != ',') {
                    throw new MalformedObjectNameException(
                            "Invalid ending character `" + s.charAt(i) + "'");
                }
            } else {
                while (i < n) {
                    char c = s.charAt(i);
                    if (c == ',') {
                        break;
                    }
                    if (c == '=' || c == ':' || c == '"' || c == '\n') {
                        throw new MalformedObjectNameException(
                                "Invalid character '" + c + "' in value part of property");
                    }
                    if (c == '*' || c == '?') {
                        valorPatron = true;
                    }
                    i++;
                }
            }
            String valor = s.substring(arranqueValor, i);
            for (int v = 0; v < vistas.size(); v++) {
                if (vistas.get(v).clave.equals(clave)) {
                    throw new MalformedObjectNameException("key `" + clave + "' already defined");
                }
            }
            vistas.add(new Propiedad(clave, valor, valorPatron));
            if (valorPatron) {
                patronValor = true;
            }

            if (i == n) {
                break;
            }
            i++; // la ','
            if (i == n) {
                throw new MalformedObjectNameException("Invalid ending comma");
            }
        }
        enOrden = vistas.toArray(new Propiedad[vistas.size()]);
    }

    /**
     * Camino de los constructores que ya reciben las claves partidas.
     *
     * <p>Aca no hay lista que analizar, pero cada clave y cada valor pasan por las mismas reglas de
     * caracteres: si no las cumplieran, el nombre no se podria volver a leer de su propia cadena.
     */
    private void construir(String domain, Map<String, String> tabla)
            throws MalformedObjectNameException {
        if (domain == null) {
            throw new NullPointerException("domain cannot be null");
        }
        if (domain.indexOf(':') >= 0 || domain.indexOf('\n') >= 0) {
            throw new MalformedObjectNameException("Invalid domain: " + domain);
        }
        dominio = domain;
        patronDominio = domain.indexOf('*') >= 0 || domain.indexOf('?') >= 0;
        if (tabla.isEmpty()) {
            throw new MalformedObjectNameException("key property list cannot be empty");
        }
        Propiedad[] props = new Propiedad[tabla.size()];
        int k = 0;
        Iterator<Map.Entry<String, String>> it = tabla.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            String clave = e.getKey();
            String valor = e.getValue();
            revisarClave(clave);
            boolean pat = revisarValor(valor);
            if (pat) {
                patronValor = true;
            }
            props[k++] = new Propiedad(clave, valor, pat);
        }
        enOrden = props;
        armarCanonico();
    }

    private static void revisarClave(String clave) throws MalformedObjectNameException {
        if (clave.length() == 0) {
            throw new MalformedObjectNameException("Invalid key (empty)");
        }
        for (int i = 0; i < clave.length(); i++) {
            char c = clave.charAt(i);
            if (c == '=' || c == ':' || c == ',' || c == '*' || c == '?' || c == '\n') {
                throw new MalformedObjectNameException("Invalid character in key: `" + c + "'");
            }
        }
    }

    /** Devuelve si el valor es un patron; tira si no es un valor legal. */
    private static boolean revisarValor(String valor) throws MalformedObjectNameException {
        if (valor.length() == 0) {
            return false;
        }
        boolean pat = false;
        if (valor.charAt(0) == '"') {
            int i = 1;
            int n = valor.length();
            boolean cerrado = false;
            while (i < n) {
                char c = valor.charAt(i);
                if (c == '\\') {
                    if (i + 1 >= n) {
                        throw new MalformedObjectNameException("Unterminated quoted value");
                    }
                    char e = valor.charAt(i + 1);
                    if (e != 'n' && e != '\\' && e != '"' && e != '*' && e != '?') {
                        throw new MalformedObjectNameException(
                                "Invalid escape sequence '\\" + e + "' in quoted value");
                    }
                    i += 2;
                    continue;
                }
                if (c == '\n') {
                    throw new MalformedObjectNameException("Newline in quoted value");
                }
                if (c == '*' || c == '?') {
                    pat = true;
                }
                if (c == '"') {
                    cerrado = true;
                    i++;
                    break;
                }
                i++;
            }
            if (!cerrado || i != n) {
                throw new MalformedObjectNameException("Invalid value: " + valor);
            }
            return pat;
        }
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c == '=' || c == ':' || c == ',' || c == '"' || c == '\n') {
                throw new MalformedObjectNameException("Invalid character in value: `" + c + "'");
            }
            if (c == '*' || c == '?') {
                pat = true;
            }
        }
        return pat;
    }

    /**
     * Ordena las claves y arma la cadena que define la identidad.
     *
     * <p>El orden es el de `String.compareTo` sobre la clave, o sea por unidad de codigo: las
     * mayusculas van antes que las minusculas, de modo que {@code B=2,a=1,C=3} canoniza a
     * {@code B=2,C=3,a=1}. No es alfabetico "humano" y no debe serlo -- tiene que ser el mismo
     * orden en toda implementacion de JMX o dos agentes no se entenderian.
     */
    private void armarCanonico() {
        Propiedad[] orden = new Propiedad[enOrden.length];
        System.arraycopy(enOrden, 0, orden, 0, enOrden.length);
        // Insercion: las listas de claves de un MBean son de unos pocos elementos.
        for (int i = 1; i < orden.length; i++) {
            Propiedad p = orden[i];
            int j = i - 1;
            while (j >= 0 && orden[j].clave.compareTo(p.clave) > 0) {
                orden[j + 1] = orden[j];
                j--;
            }
            orden[j + 1] = p;
        }
        canonicas = orden;
        StringBuilder b = new StringBuilder();
        b.append(dominio).append(':');
        b.append(unir(canonicas));
        if (patronLista) {
            if (canonicas.length > 0) {
                b.append(',');
            }
            b.append('*');
        }
        canonico = b.toString();
    }

    private static String unir(Propiedad[] props) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < props.length; i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(props[i].clave).append('=').append(props[i].valor);
        }
        return b.toString();
    }

    // ---- consultas de forma --------------------------------------------------------------------

    /** Si el nombre lleva algun comodin, de la clase que sea. */
    public boolean isPattern() {
        return patronDominio || patronLista || patronValor;
    }

    /** Si el comodin esta en el dominio. */
    public boolean isDomainPattern() {
        return patronDominio;
    }

    /**
     * Si el nombre admite claves que no menciona.
     *
     * <p>Ojo: en el JDK esto es {@code isPropertyListPattern() || isPropertyValuePattern()}, no
     * solo lo primero. El nombre del metodo enga&ntilde;a y por eso existen los dos mas precisos.
     */
    public boolean isPropertyPattern() {
        return patronLista || patronValor;
    }

    /** Si hay un {@code *} suelto en la lista de claves. */
    public boolean isPropertyListPattern() {
        return patronLista;
    }

    /** Si algun valor lleva comodin. */
    public boolean isPropertyValuePattern() {
        return patronValor;
    }

    /**
     * Si el valor de esa clave en particular lleva comodin.
     *
     * @throws IllegalArgumentException si la clave no esta en el nombre -- preguntarlo de una clave
     *     ausente es un error del que llama, no un "no"
     */
    public boolean isPropertyValuePattern(String property) {
        if (property == null) {
            throw new NullPointerException("key property can't be null");
        }
        for (int i = 0; i < canonicas.length; i++) {
            if (canonicas[i].clave.equals(property)) {
                return canonicas[i].patron;
            }
        }
        throw new IllegalArgumentException("key property not found");
    }

    // ---- accesores -----------------------------------------------------------------------------

    /** La forma que define la identidad: claves ordenadas, {@code *} de lista al final. */
    public String getCanonicalName() {
        return canonico;
    }

    /** El dominio, sin los dos puntos. */
    public String getDomain() {
        return dominio;
    }

    /** El valor de una clave, o `null` si el nombre no la lleva. */
    public String getKeyProperty(String property) {
        for (int i = 0; i < canonicas.length; i++) {
            if (canonicas[i].clave.equals(property)) {
                return canonicas[i].valor;
            }
        }
        return null;
    }

    /**
     * Las claves como tabla.
     *
     * <p>Es una copia nueva en cada llamada: modificarla no toca el nombre, que es inmutable. El
     * {@code *} de la lista no aparece -- no es una clave.
     */
    public Hashtable<String, String> getKeyPropertyList() {
        Hashtable<String, String> t = new Hashtable<String, String>();
        for (int i = 0; i < enOrden.length; i++) {
            t.put(enOrden[i].clave, enOrden[i].valor);
        }
        return t;
    }

    /**
     * Las claves en el orden en que se escribieron, sin el {@code *}.
     */
    public String getKeyPropertyListString() {
        return unir(enOrden);
    }

    /** Las claves en orden canonico, sin el {@code *}. */
    public String getCanonicalKeyPropertyListString() {
        return unir(canonicas);
    }

    /**
     * El nombre tal como se escribio -- <b>no</b> el canonico.
     *
     * <p>La unica correccion que aplica es mandar el {@code *} de la lista al final: {@code d:*,k=v}
     * se imprime {@code d:k=v,*}.
     */
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(dominio).append(':');
        b.append(unir(enOrden));
        if (patronLista) {
            if (enOrden.length > 0) {
                b.append(',');
            }
            b.append('*');
        }
        return b.toString();
    }

    /** Identidad por forma canonica. */
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof ObjectName)) {
            return false;
        }
        return canonico.equals(((ObjectName) object).canonico);
    }

    /** El del canonico, para que dos nombres iguales lo compartan. */
    public int hashCode() {
        return canonico.hashCode();
    }

    // ---- citado --------------------------------------------------------------------------------

    /**
     * Envuelve `s` en comillas escapando todo lo que la gramatica podria malinterpretar.
     *
     * <p>Escapa `\`, `"`, `*`, `?` y el salto de linea. Los dos comodines estan en la lista por lo
     * dicho arriba: citar no los apaga, escaparlos si. Lo que sale de aca es siempre un valor
     * literal, nunca un patron -- que es justo para lo que sirve el metodo.
     */
    public static String quote(String s) {
        if (s == null) {
            throw new NullPointerException("s cannot be null");
        }
        StringBuilder b = new StringBuilder();
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                b.append("\\n");
            } else if (c == '\\' || c == '"' || c == '*' || c == '?') {
                b.append('\\').append(c);
            } else {
                b.append(c);
            }
        }
        b.append('"');
        return b.toString();
    }

    /**
     * La inversa de {@link #quote}.
     *
     * <p>Es <b>mas estricta</b> que el analizador de nombres: un {@code *} sin escapar dentro de las
     * comillas es un error aca, aunque {@code d:k="a*b"} sea un nombre valido. La diferencia tiene
     * sentido: el analizador acepta patrones, y este metodo promete devolver el texto literal que
     * {@code quote} habia recibido, cosa que un patron no tiene.
     */
    public static String unquote(String q) {
        if (q == null) {
            throw new NullPointerException("q cannot be null");
        }
        int n = q.length();
        if (n < 2 || q.charAt(0) != '"' || q.charAt(n - 1) != '"') {
            throw new IllegalArgumentException("Argument not quoted");
        }
        StringBuilder b = new StringBuilder();
        int i = 1;
        while (i < n - 1) {
            char c = q.charAt(i);
            if (c == '\\') {
                if (i + 1 >= n - 1) {
                    throw new IllegalArgumentException("Trailing backslash");
                }
                char e = q.charAt(i + 1);
                if (e == 'n') {
                    b.append('\n');
                } else if (e == '\\' || e == '"' || e == '*' || e == '?') {
                    b.append(e);
                } else {
                    throw new IllegalArgumentException("Bad character '" + e + "' after backslash");
                }
                i += 2;
                continue;
            }
            if (c == '"' || c == '*' || c == '?' || c == '\n') {
                throw new IllegalArgumentException(
                        "Invalid unescaped character '" + c + "' in the string to unquote");
            }
            b.append(c);
            i++;
        }
        return b.toString();
    }

    // ---- el nombre como consulta ---------------------------------------------------------------

    /**
     * Si `name` es uno de los MBeans que este patron designa.
     *
     * <p>Dos reglas que no son obvias y se verificaron:
     *
     * <ul>
     *   <li>si `name` es a su vez un patron, la respuesta es <b>siempre</b> `false`. Un patron no
     *       designa a otro patron; designa nombres concretos;
     *   <li>si este nombre no es patron, se reduce a la igualdad canonica.
     * </ul>
     */
    public boolean apply(ObjectName name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        if (name.isPattern()) {
            return false;
        }
        if (!isPattern()) {
            return canonico.equals(name.canonico);
        }
        return coincideDominio(name) && coincideClaves(name);
    }

    private boolean coincideDominio(ObjectName name) {
        if (!patronDominio) {
            return dominio.equals(name.dominio);
        }
        return comodinCoincide(dominio, name.dominio);
    }

    private boolean coincideClaves(ObjectName name) {
        // Sin '*' de lista, los conjuntos de claves tienen que ser identicos; con el, basta que las
        // claves nombradas esten.
        if (!patronLista && canonicas.length != name.canonicas.length) {
            return false;
        }
        for (int i = 0; i < canonicas.length; i++) {
            Propiedad p = canonicas[i];
            String otro = name.getKeyProperty(p.clave);
            if (otro == null) {
                return false;
            }
            if (p.patron) {
                if (!comodinCoincide(p.valor, otro)) {
                    return false;
                }
            } else if (!p.valor.equals(otro)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Coincidencia con {@code *} (cualquier cantidad) y {@code ?} (exactamente uno).
     *
     * <p>Retroceso simple con dos indices, sin recursion: los patrones de un `ObjectName` son cortos
     * y esto no puede desbordar la pila con una entrada hostil.
     */
    private static boolean comodinCoincide(String patron, String texto) {
        int p = 0;
        int t = 0;
        int estrella = -1;
        int marca = 0;
        while (t < texto.length()) {
            if (p < patron.length() && (patron.charAt(p) == '?'
                    || patron.charAt(p) == texto.charAt(t))) {
                p++;
                t++;
            } else if (p < patron.length() && patron.charAt(p) == '*') {
                estrella = p;
                marca = t;
                p++;
            } else if (estrella >= 0) {
                p = estrella + 1;
                marca++;
                t = marca;
            } else {
                return false;
            }
        }
        while (p < patron.length() && patron.charAt(p) == '*') {
            p++;
        }
        return p == patron.length();
    }

    /**
     * No hace nada, y no puede hacer otra cosa: un `ObjectName` se evalua mirando solo el nombre.
     *
     * <p>Esta porque {@link QueryExp} lo exige, no porque haga falta.
     */
    public void setMBeanServer(MBeanServer mbs) {
    }

    // ---- orden ---------------------------------------------------------------------------------

    /**
     * Orden total: dominio, despues la clave {@code type}, despues la forma canonica.
     *
     * <p>El escalon del medio es la rareza que hay que conocer: JMX privilegia {@code type} sobre
     * las demas claves porque es la que agrupa MBeans afines, y ordenar por ella deja juntos en un
     * listado a todos los del mismo tipo. Un nombre sin {@code type} cuenta como si lo tuviera
     * vacio, o sea va primero.
     */
    public int compareTo(ObjectName name) {
        int d = dominio.compareTo(name.dominio);
        if (d != 0) {
            return d;
        }
        String miTipo = getKeyProperty("type");
        String suTipo = name.getKeyProperty("type");
        if (miTipo == null) {
            miTipo = "";
        }
        if (suTipo == null) {
            suTipo = "";
        }
        int t = miTipo.compareTo(suTipo);
        if (t != 0) {
            return t;
        }
        return canonico.compareTo(name.canonico);
    }
}
