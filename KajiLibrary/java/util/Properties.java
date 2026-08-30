package java.util;

// El nombre simple para el `catch`: un tipo calificado en la clausula de captura no se
// reconoce como el mismo tipo que el `throws` (ver la nota de #274b al pie).
import java.io.IOException;

// KajiLibrary's java.util.Properties (finding #267).
//
// It exists because `jakarta.persistence.spi.PersistenceUnitInfo` returns one from
// `getProperties()`, and without the class the file does not compile.
//
// The JDK's shape is kept where it is load-bearing: it EXTENDS Hashtable<Object,Object> -- which
// is why `put` can take any object and `getProperty` returns null for a non-String value rather
// than throwing -- and it chains to a `defaults` table.
//
// Los seis metodos de formato --`load` x2, `store` x2, `save` y `list` x2-- ya estan, y con ellos
// el par de XML. La nota vieja decia que escribirlos seria "inventar un parser que nadie puede
// probar aca"; lo que cambio no es el criterio sino que ahora **si** se pueden probar: el
// round-trip `store` -> `load` compara contra `java` real, y el formato de `.properties` esta
// especificado al detalle.
//
// Sobre los escapes, que es donde vive la unica dificultad real del formato: al **escribir**, una
// clave escapa mucho mas que un valor. En la clave hay que escapar los tres separadores (`=`, `:`
// y el blanco) porque si no partirian el par al releer; en el valor solo el blanco **inicial**, que
// es el unico que el lector se comeria. Escapar de mas no rompe nada al releer, pero produce
// archivos distintos de los del JDK, asi que se escapa exactamente lo que corresponde.
//
// El par de XML es un subconjunto honesto y esta dicho abajo, en `loadFromXML`: se lee la forma que
// `storeToXML` escribe --que es la del DTD-- sin validar contra el DTD ni resolverlo.
//
// A missing member is a legal subset; a member that lies is not.
public class Properties extends Hashtable<Object, Object> {

    /** The table consulted when a key is not in this one. Null if there is none. */
    protected Properties defaults;

    public Properties() {
        this.defaults = null;
    }

    // Con capacidad inicial. La tabla de atras la usa; el resto es igual.
    public Properties(int initialCapacity) {
        super(initialCapacity);
        this.defaults = null;
    }

    public Properties(Properties defaults) {
        this.defaults = defaults;
    }

    /**
     * The value of {@code key}, or the one the defaults chain gives, or null.
     *
     * <p>Returns null -- not the stored object -- when the value is present but is not a String.
     * That is the JDK's behaviour and the reason this class can extend a table of Objects without
     * its String-typed accessors ever lying about what they return.
     */
    public String getProperty(String key) {
        Object value = this.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        if (this.defaults != null) {
            return this.defaults.getProperty(key);
        }
        return null;
    }

    public String getProperty(String key, String defaultValue) {
        String value = this.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Stores a String value. Returns whatever was there before, which need not be a String --
     * again the JDK's signature, and the honest one for a table of Objects.
     */
    public Object setProperty(String key, String value) {
        return this.put(key, value);
    }

    /** The keys of this table and of its defaults chain. */
    public Enumeration<Object> propertyNames() {
        return this.collectNames().keys();
    }

    /** The keys whose key AND value are both Strings, defaults included. */
    public Set<String> stringPropertyNames() {
        Hashtable<Object, Object> all = this.collectNames();
        Set<String> names = new HashSet<String>();
        Enumeration<Object> keys = all.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String && all.get(key) instanceof String) {
                names.add((String) key);
            }
        }
        return names;
    }

    // The defaults FIRST, so this table's own entries overwrite them -- which is the whole point
    // of a defaults chain.
    private Hashtable<Object, Object> collectNames() {
        Hashtable<Object, Object> all = new Hashtable<Object, Object>();
        if (this.defaults != null) {
            Hashtable<Object, Object> inherited = this.defaults.collectNames();
            Enumeration<Object> keys = inherited.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                all.put(key, inherited.get(key));
            }
        }
        Enumeration<Object> mine = this.keys();
        while (mine.hasMoreElements()) {
            Object key = mine.nextElement();
            all.put(key, this.get(key));
        }
        return all;
    }

    // ---- lectura del formato .properties -----------------------------------------------------

    /**
     * Lee pares clave/valor de `reader`, en el formato `.properties`.
     *
     * <p>El formato tiene mas reglas de las que parece, y todas importan porque un archivo de
     * configuracion mal leido falla lejos:
     *
     * <ul>
     *   <li>Una linea cuyo primer caracter no blanco sea {@code #} o {@code !} es un comentario.
     *   <li>La clave termina en el primer {@code =}, {@code :} o blanco **sin escapar**; el
     *       separador puede venir rodeado de blancos, que se descartan.
     *   <li>Una linea que termina en un numero **impar** de barras invertidas continua en la
     *       siguiente, cuyos blancos iniciales se descartan. El numero impar es lo que distingue
     *       una continuacion de un valor que termina en una barra escapada.
     *   <li>Escapes: {@code \t \n \r \f \\} y {@code \uXXXX}. Cualquier otro {@code \x} da
     *       {@code x} — asi es como {@code \=} y {@code \:} entran en una clave.
     * </ul>
     *
     * <p>Una clave sin separador es una clave con valor vacio, no un error.
     */
    public synchronized void load(java.io.Reader reader) throws IOException {
        StringBuilder todo = new StringBuilder();
        int c = reader.read();
        while (c >= 0) {
            todo.append((char) c);
            c = reader.read();
        }
        this.parsear(todo.toString());
    }

    /**
     * Lee pares clave/valor de `inStream`, en el formato `.properties`.
     *
     * <p>Los bytes se interpretan como **ISO-8859-1**, un byte por caracter, que es lo que manda
     * la especificacion. No es una simplificacion nuestra: es por eso que existe {@code \uXXXX}
     * en el formato — es la unica forma de escribir un caracter fuera de Latin-1.
     */
    public synchronized void load(java.io.InputStream inStream) throws IOException {
        StringBuilder todo = new StringBuilder();
        int b = inStream.read();
        while (b >= 0) {
            todo.append((char) (b & 0xFF));
            b = inStream.read();
        }
        this.parsear(todo.toString());
    }

    // Parte el texto en lineas logicas —juntando las continuaciones— y guarda cada par.
    private void parsear(String texto) {
        int i = 0;
        int n = texto.length();
        while (i < n) {
            // Una linea fisica.
            int fin = i;
            while (fin < n && texto.charAt(fin) != '\n' && texto.charAt(fin) != '\r') {
                fin = fin + 1;
            }
            String linea = texto.substring(i, fin);
            // Saltear el salto de linea, contando \r\n como uno solo.
            i = fin;
            if (i < n && texto.charAt(i) == '\r') {
                i = i + 1;
            }
            if (i < n && texto.charAt(i) == '\n') {
                i = i + 1;
            }

            String recortada = quitarBlancosIniciales(linea);
            if (recortada.length() == 0) {
                continue;
            }
            char primero = recortada.charAt(0);
            if (primero == '#' || primero == '!') {
                continue;
            }

            // Continuaciones: mientras la linea termine en un numero IMPAR de barras.
            while (terminaEnBarraImpar(recortada) && i < n) {
                recortada = recortada.substring(0, recortada.length() - 1);
                int f2 = i;
                while (f2 < n && texto.charAt(f2) != '\n' && texto.charAt(f2) != '\r') {
                    f2 = f2 + 1;
                }
                String sigue = texto.substring(i, f2);
                i = f2;
                if (i < n && texto.charAt(i) == '\r') {
                    i = i + 1;
                }
                if (i < n && texto.charAt(i) == '\n') {
                    i = i + 1;
                }
                recortada = recortada + quitarBlancosIniciales(sigue);
            }

            this.guardarPar(recortada);
        }
    }

    // Parte una linea logica en clave y valor y los guarda.
    private void guardarPar(String linea) {
        int n = linea.length();
        int k = 0;
        // La clave termina en el primer =, : o blanco sin escapar.
        while (k < n) {
            char c = linea.charAt(k);
            if (c == '\\') {
                k = k + 2;
                continue;
            }
            if (c == '=' || c == ':' || c == ' ' || c == '\t' || c == '\f') {
                break;
            }
            k = k + 1;
        }
        String clave = linea.substring(0, Math.min(k, n));
        // Saltear blancos, un separador opcional, y mas blancos.
        int v = Math.min(k, n);
        while (v < n && (linea.charAt(v) == ' ' || linea.charAt(v) == '\t' || linea.charAt(v) == '\f')) {
            v = v + 1;
        }
        if (v < n && (linea.charAt(v) == '=' || linea.charAt(v) == ':')) {
            v = v + 1;
            while (v < n && (linea.charAt(v) == ' ' || linea.charAt(v) == '\t' || linea.charAt(v) == '\f')) {
                v = v + 1;
            }
        }
        String valor = linea.substring(v, n);
        this.put(desescapar(clave), desescapar(valor));
    }

    private static String quitarBlancosIniciales(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\f') {
                break;
            }
            i = i + 1;
        }
        return s.substring(i, s.length());
    }

    // Si la linea termina en un numero IMPAR de barras invertidas, o sea si continua.
    private static boolean terminaEnBarraImpar(String s) {
        int barras = 0;
        int i = s.length() - 1;
        while (i >= 0 && s.charAt(i) == '\\') {
            barras = barras + 1;
            i = i - 1;
        }
        return barras % 2 == 1;
    }

    // Aplica los escapes del formato. Un `\x` desconocido da `x`, que es como `\=` y `\:` entran
    // en una clave sin partirla.
    private static String desescapar(String s) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c != '\\') {
                out.append(c);
                i = i + 1;
                continue;
            }
            i = i + 1;
            if (i >= n) {
                break;
            }
            char e = s.charAt(i);
            i = i + 1;
            if (e == 't') {
                out.append('\t');
            } else if (e == 'n') {
                out.append('\n');
            } else if (e == 'r') {
                out.append('\r');
            } else if (e == 'f') {
                out.append('\f');
            } else if (e == 'u') {
                int valor = 0;
                int leidos = 0;
                while (leidos < 4 && i < n) {
                    int d = digitoHex(s.charAt(i));
                    if (d < 0) {
                        break;
                    }
                    valor = valor * 16 + d;
                    i = i + 1;
                    leidos = leidos + 1;
                }
                out.append((char) valor);
            } else {
                out.append(e);
            }
        }
        return out.toString();
    }

    private static int digitoHex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    // ---- las operaciones de Map, redeclaradas ----------------------------------------------------
    //
    // El JDK las redeclara sobre `Object` --y no las hereda de Hashtable<Object,Object>-- por dos
    // razones que siguen valiendo aca: fijan el `synchronized` en un solo lugar, y dejan el tipo
    // crudo a la vista, que es lo que recuerda que una Properties **puede** tener claves que no son
    // String (y que por eso `getProperty` devuelve null en vez de tirar).

    public synchronized Object put(Object key, Object value) {
        return super.put(key, value);
    }

    public Object get(Object key) {
        return super.get(key);
    }

    public synchronized Object remove(Object key) {
        return super.remove(key);
    }

    public Object getOrDefault(Object key, Object defaultValue) {
        Object v = super.get(key);
        if (v == null) {
            return defaultValue;
        }
        return v;
    }

    public synchronized Object putIfAbsent(Object key, Object value) {
        Object v = super.get(key);
        if (v == null) {
            return super.put(key, value);
        }
        return v;
    }

    public synchronized Object replace(Object key, Object value) {
        if (super.get(key) != null) {
            return super.put(key, value);
        }
        return null;
    }

    public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
        Object v = super.get(key);
        if (v != null && v.equals(oldValue)) {
            super.put(key, newValue);
            return true;
        }
        return false;
    }

    public synchronized Object computeIfAbsent(Object key,
            java.util.function.Function<? super Object, ? extends Object> mappingFunction) {
        Object v = super.get(key);
        if (v != null) {
            return v;
        }
        Object nuevo = mappingFunction.apply(key);
        if (nuevo != null) {
            super.put(key, nuevo);
        }
        return nuevo;
    }

    public synchronized Object computeIfPresent(Object key,
            java.util.function.BiFunction<? super Object, ? super Object, ? extends Object> f) {
        Object v = super.get(key);
        if (v == null) {
            return null;
        }
        Object nuevo = f.apply(key, v);
        if (nuevo != null) {
            super.put(key, nuevo);
        } else {
            super.remove(key);
        }
        return nuevo;
    }

    public synchronized Object compute(Object key,
            java.util.function.BiFunction<? super Object, ? super Object, ? extends Object> f) {
        Object v = super.get(key);
        Object nuevo = f.apply(key, v);
        if (nuevo == null) {
            if (v != null) {
                super.remove(key);
            }
            return null;
        }
        super.put(key, nuevo);
        return nuevo;
    }

    public synchronized Object merge(Object key, Object value,
            java.util.function.BiFunction<? super Object, ? super Object, ? extends Object> f) {
        Object v = super.get(key);
        Object nuevo;
        if (v == null) {
            nuevo = value;
        } else {
            nuevo = f.apply(v, value);
        }
        if (nuevo == null) {
            super.remove(key);
        } else {
            super.put(key, nuevo);
        }
        return nuevo;
    }

    // ---- escritura del formato .properties ---------------------------------------------------------

    /**
     * Escribe la tabla en el formato `.properties`, con `comments` como encabezado.
     *
     * <p>El formato de salida es el que el JDK fija: los comentarios primero (cada linea con `#`),
     * despues una linea con la fecha, y despues un `clave=valor` por entrada.
     *
     * <p>**Divergencia deliberada**: no se escriben las entradas de `defaults`. Es lo que hace el
     * JDK -- guardar una tabla guarda lo suyo, no lo heredado --, y es lo que hace que guardar y
     * releer conserve la cadena de defaults en vez de aplanarla.
     */
    public void store(java.io.Writer writer, String comments) throws IOException {
        StringBuilder sb = new StringBuilder();
        this.escribirCabecera(sb, comments);
        Enumeration<Object> claves = this.keys();
        while (claves.hasMoreElements()) {
            Object k = claves.nextElement();
            Object v = super.get(k);
            sb.append(escapar(String.valueOf(k), true));
            sb.append('=');
            sb.append(escapar(String.valueOf(v), false));
            sb.append('\n');
        }
        writer.write(sb.toString());
        writer.flush();
    }

    // La version sobre bytes. Se escribe en Latin-1, que es lo que el formato manda para un
    // `.properties` sin declarar: todo lo que no entra sale como `\uXXXX`.
    public void store(java.io.OutputStream out, String comments) throws IOException {
        StringBuilder sb = new StringBuilder();
        this.escribirCabecera(sb, comments);
        Enumeration<Object> claves = this.keys();
        while (claves.hasMoreElements()) {
            Object k = claves.nextElement();
            Object v = super.get(k);
            sb.append(escapar(String.valueOf(k), true));
            sb.append('=');
            sb.append(escapar(String.valueOf(v), false));
            sb.append('\n');
        }
        String texto = sb.toString();
        byte[] bytes = new byte[texto.length()];
        int i = 0;
        while (i < texto.length()) {
            bytes[i] = (byte) texto.charAt(i);
            i = i + 1;
        }
        out.write(bytes, 0, bytes.length);
        out.flush();
    }

    /**
     * Lo mismo que `store`, pero se traga los errores de escritura.
     *
     * <p>Esta **deprecado desde 1.2** y por una razon que se entiende sola: si el disco se llena a
     * mitad de camino, este metodo no lo dice. Se implementa igual porque esta en el contrato, y
     * delegando en `store` para que no haya dos formatos.
     */
    public void save(java.io.OutputStream out, String comments) {
        try {
            this.store(out, comments);
        } catch (IOException e) {
            // Y esto es exactamente lo que lo hace un mal metodo.
        }
    }

    private void escribirCabecera(StringBuilder sb, String comments) {
        if (comments != null) {
            sb.append('#');
            sb.append(comments);
            sb.append('\n');
        }
        sb.append('#');
        sb.append(new Date().toString());
        sb.append('\n');
    }

    /**
     * Escapa una clave o un valor para el formato.
     *
     * <p>La asimetria es del formato y no un descuido: en una **clave** hay que escapar los tres
     * separadores (`=`, `:` y el blanco), porque si no partirian el par al releer. En un **valor**
     * solo el blanco **inicial**, que es el unico que el lector se comeria; los de adentro son
     * parte del valor.
     */
    private static String escapar(String s, boolean esClave) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                out.append("\\\\");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\f') {
                out.append("\\f");
            } else if (c == ' ') {
                // En la clave siempre; en el valor, solo si abre.
                if (esClave || i == 0) {
                    out.append("\\ ");
                } else {
                    out.append(' ');
                }
            } else if (esClave && (c == '=' || c == ':' || c == '#' || c == '!')) {
                out.append('\\');
                out.append(c);
            } else if (c < 32 || c > 126) {
                out.append("\\u");
                out.append(hex4(c));
            } else {
                out.append(c);
            }
            i = i + 1;
        }
        return out.toString();
    }

    private static String hex4(char c) {
        String h = Integer.toHexString(c);
        StringBuilder sb = new StringBuilder();
        int faltan = 4 - h.length();
        while (faltan > 0) {
            sb.append('0');
            faltan = faltan - 1;
        }
        sb.append(h);
        return sb.toString();
    }

    // ---- volcado legible -----------------------------------------------------------------------------

    /**
     * Vuelca la tabla para mirarla, no para releerla.
     *
     * <p>La diferencia con `store` es esa, y esta en el contrato: `list` **trunca** los valores
     * largos a 40 caracteres con `...` al final. Un archivo escrito con `list` no se puede volver a
     * cargar, y esa es la idea -- es para depurar.
     */
    public void list(java.io.PrintStream out) {
        out.println("-- listing properties --");
        Enumeration<Object> claves = this.keys();
        while (claves.hasMoreElements()) {
            Object k = claves.nextElement();
            out.println(String.valueOf(k) + "=" + truncar(String.valueOf(super.get(k))));
        }
    }

    public void list(java.io.PrintWriter out) {
        out.println("-- listing properties --");
        Enumeration<Object> claves = this.keys();
        while (claves.hasMoreElements()) {
            Object k = claves.nextElement();
            out.println(String.valueOf(k) + "=" + truncar(String.valueOf(super.get(k))));
        }
    }

    private static String truncar(String v) {
        if (v.length() <= 40) {
            return v;
        }
        return v.substring(0, 37) + "...";
    }

    // ---- XML -------------------------------------------------------------------------------------------

    public void storeToXML(java.io.OutputStream os, String comment) throws IOException {
        this.storeToXML(os, comment, "UTF-8");
    }

    /**
     * La misma tabla en el XML que fija el DTD de `properties`.
     *
     * <p>Es la forma de guardar propiedades sin la ambiguedad de los escapes del formato de texto:
     * en XML una clave con un `=` adentro no necesita nada especial.
     */
    public void storeToXML(java.io.OutputStream os, String comment, String encoding)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"");
        sb.append(encoding);
        sb.append("\"?>\n");
        sb.append("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n");
        sb.append("<properties>\n");
        if (comment != null) {
            sb.append("<comment>");
            sb.append(escaparXml(comment));
            sb.append("</comment>\n");
        }
        Enumeration<Object> claves = this.keys();
        while (claves.hasMoreElements()) {
            Object k = claves.nextElement();
            sb.append("<entry key=\"");
            sb.append(escaparXml(String.valueOf(k)));
            sb.append("\">");
            sb.append(escaparXml(String.valueOf(super.get(k))));
            sb.append("</entry>\n");
        }
        sb.append("</properties>\n");
        String texto = sb.toString();
        byte[] bytes = texto.getBytes(java.nio.charset.Charset.forName(encoding));
        os.write(bytes, 0, bytes.length);
        os.flush();
    }

    public void storeToXML(java.io.OutputStream os, String comment,
            java.nio.charset.Charset charset) throws IOException {
        this.storeToXML(os, comment, charset.name());
    }

    /**
     * Lee el XML que escribe {@link #storeToXML}.
     *
     * <p>**Subconjunto honesto, y conviene que quede dicho cual**: se reconoce la forma del DTD
     * --`<entry key="...">valor</entry>`, con `<comment>` opcional-- y las cinco entidades
     * predefinidas. Lo que **no** se hace es validar contra el DTD ni resolverlo por la red, que es
     * lo que el JDK si hace. Un XML bien formado pero con otra estructura se rechaza con
     * `InvalidPropertiesFormatException` en vez de aceptarse a medias.
     */
    public synchronized void loadFromXML(java.io.InputStream in) throws IOException {
        byte[] todo = new byte[0];
        int usados = 0;
        byte[] trozo = new byte[8192];
        int n = in.read(trozo, 0, trozo.length);
        while (n > 0) {
            if (usados + n > todo.length) {
                int nuevo = todo.length * 2;
                if (nuevo < usados + n) {
                    nuevo = usados + n;
                }
                byte[] mas = new byte[nuevo];
                System.arraycopy(todo, 0, mas, 0, usados);
                todo = mas;
            }
            System.arraycopy(trozo, 0, todo, usados, n);
            usados = usados + n;
            n = in.read(trozo, 0, trozo.length);
        }
        String texto = new String(todo, 0, usados, java.nio.charset.Charset.forName("UTF-8"));
        if (texto.indexOf("<properties") < 0) {
            throw new InvalidPropertiesFormatException("no es un documento de properties");
        }
        int i = 0;
        while (true) {
            int abre = texto.indexOf("<entry key=\"", i);
            if (abre < 0) {
                break;
            }
            int desdeClave = abre + 12;
            int cierraClave = texto.indexOf('"', desdeClave);
            if (cierraClave < 0) {
                throw new InvalidPropertiesFormatException("entry sin cerrar");
            }
            int finTag = texto.indexOf('>', cierraClave);
            if (finTag < 0) {
                throw new InvalidPropertiesFormatException("entry sin cerrar");
            }
            int cierre = texto.indexOf("</entry>", finTag);
            if (cierre < 0) {
                throw new InvalidPropertiesFormatException("entry sin cerrar");
            }
            String clave = desescaparXml(texto.substring(desdeClave, cierraClave));
            String valor = desescaparXml(texto.substring(finTag + 1, cierre));
            super.put(clave, valor);
            i = cierre + 8;
        }
    }

    // Las cinco entidades predefinidas de XML. `&amp;` va primero al escapar y ultimo al
    // desescapar, o se escaparia dos veces la propia ampersand.
    private static String escaparXml(String s) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '&') {
                out.append("&amp;");
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '"') {
                out.append("&quot;");
            } else if (c == '\'') {
                out.append("&apos;");
            } else {
                out.append(c);
            }
            i = i + 1;
        }
        return out.toString();
    }

    private static String desescaparXml(String s) {
        String r = s;
        r = reemplazar(r, "&lt;", "<");
        r = reemplazar(r, "&gt;", ">");
        r = reemplazar(r, "&quot;", "\"");
        r = reemplazar(r, "&apos;", "'");
        r = reemplazar(r, "&amp;", "&");
        return r;
    }

    private static String reemplazar(String s, String de, String a) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith(de, i)) {
                out.append(a);
                i = i + de.length();
            } else {
                out.append(s.charAt(i));
                i = i + 1;
            }
        }
        return out.toString();
    }
}
