package java.util.jar;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Los pares nombre/valor de una seccion del manifiesto de un JAR.
 *
 * <p>Es un `Map` con dos restricciones que no se ven en la firma: las claves son
 * {@link Attributes.Name} --nunca `String`-- y los valores son `String`. `put` **castea**, asi que
 * meter un `String` como clave tira `ClassCastException`; el atajo para no escribir el `Name` a mano
 * es {@link #putValue}.
 *
 * <p>El orden importa y por eso el mapa de abajo es un `LinkedHashMap`: el manifiesto se escribe en
 * el orden en que se cargaron los atributos, y una ida-y-vuelta tiene que dar los mismos bytes. El
 * JDK hace lo mismo desde 9.
 *
 * <h2>Lo que queda afuera, y por que</h2>
 *
 * <p>Nada de la superficie publica. Los cuatro miembros que el JDK declara y aca no estan
 * --`write`, `writeMain` y los dos `read`-- son **de paquete** en el JDK tambien, o sea internos; aca
 * existen los dos primeros con esa misma visibilidad y los `read` viven en `Manifest`, que es quien
 * tiene el lector de lineas. Por la regla del contrato, lo interno es libre.
 */
public class Attributes implements Map<Object, Object>, Cloneable {

    /** El mapa que guarda los pares. Es `protected` porque el JDK lo expone asi. */
    protected Map<Object, Object> map;

    /** Un juego de atributos vacio. */
    public Attributes() {
        this(11);
    }

    /** Un juego de atributos vacio con lugar para `size` pares. */
    public Attributes(int size) {
        this.map = new LinkedHashMap<Object, Object>(size);
    }

    /** Una copia de `attr`. */
    public Attributes(Attributes attr) {
        this.map = new LinkedHashMap<Object, Object>(attr.map);
    }

    /**
     * El valor de ese atributo, o `null`.
     *
     * <p>La clave tiene que ser un `Name`: pasarle un `String` devuelve `null`, no el valor. Es la
     * trampa clasica de esta clase y esta en el JDK igual.
     */
    public Object get(Object name) {
        return this.map.get(name);
    }

    /** El valor de ese atributo, buscado por nombre y **sin distinguir mayusculas**. */
    public String getValue(String name) {
        return (String) this.map.get(new Name(name));
    }

    /** El valor de ese atributo. */
    public String getValue(Name name) {
        return (String) this.map.get(name);
    }

    /**
     * Asocia un valor a un atributo.
     *
     * @throws ClassCastException si `name` no es un `Name` o `value` no es un `String`
     */
    public Object put(Object name, Object value) {
        Name k = (Name) name;
        String v = (String) value;
        return this.map.put(k, v);
    }

    /** Como `put`, pero armando el `Name` a partir del texto. */
    public String putValue(String name, String value) {
        return (String) put(new Name(name), value);
    }

    /** Saca ese atributo y devuelve el valor que tenia. */
    public Object remove(Object name) {
        return this.map.remove(name);
    }

    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    public boolean containsKey(Object name) {
        return this.map.containsKey(name);
    }

    /**
     * Copia todos los pares de `attr`.
     *
     * @throws ClassCastException si `attr` no es un `Attributes`
     */
    public void putAll(Map<?, ?> attr) {
        // El JDK exige que sea un `Attributes` y no un mapa cualquiera. No es capricho: un mapa
        // cualquiera puede tener claves `String`, y entonces el `put` de arriba tiraria a mitad de
        // camino dejando la copia por la mitad. Chequear primero hace que falle entera o no falle.
        if (!(attr instanceof Attributes)) {
            throw new ClassCastException();
        }
        Attributes otro = (Attributes) attr;
        for (Map.Entry<Object, Object> e : otro.map.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public void clear() {
        this.map.clear();
    }

    public int size() {
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public Set<Object> keySet() {
        return this.map.keySet();
    }

    public Collection<Object> values() {
        return this.map.values();
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return this.map.entrySet();
    }

    public boolean equals(Object o) {
        if (!(o instanceof Attributes)) {
            return false;
        }
        return this.map.equals(((Attributes) o).map);
    }

    public int hashCode() {
        return this.map.hashCode();
    }

    /** Una copia. */
    public Object clone() {
        return new Attributes(this);
    }

    // ---- escritura ------------------------------------------------------------------------------

    /** Escribe esta seccion --sin la linea `Name:`-- y la termina con la linea en blanco. */
    void write(DataOutputStream out) throws IOException {
        for (Map.Entry<Object, Object> e : this.map.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(((Name) e.getKey()).toString());
            sb.append(": ");
            sb.append((String) e.getValue());
            Manifest.println72(out, sb.toString());
        }
        Manifest.println(out);
    }

    /**
     * Escribe la seccion **principal**, que tiene dos reglas propias.
     *
     * <p>La primera: `Manifest-Version` va **primero** aunque no sea el primero que se cargo, y si no
     * esta se prueba con `Signature-Version`. Es lo que hace que un manifiesto sea reconocible por
     * la primera linea.
     *
     * <p>La segunda es rara y se replica a proposito: si **ninguna** de las dos esta, el JDK no
     * escribe **ningun** atributo --solo la linea en blanco--. Se verifico contra el JDK 25 antes de
     * copiarlo. Un manifiesto sin version no es un manifiesto, asi que perder los atributos es
     * consistente con eso, pero la razon de fondo es que el bucle esta guardado por `version != null`.
     */
    void writeMain(DataOutputStream out) throws IOException {
        String vername = Name.MANIFEST_VERSION.toString();
        String version = getValue(vername);
        if (version == null) {
            vername = Name.SIGNATURE_VERSION.toString();
            version = getValue(vername);
        }
        if (version != null) {
            // El JDK usa `writeBytes`, que se queda con el byte bajo de cada `char` y no pliega.
            // Aca va por `println72`: para las versiones reales --"1.0"-- sale byte a byte igual, y
            // para una version larga o con no-ASCII sale bien en vez de mal.
            Manifest.println72(out, vername + ": " + version);
        }
        if (version != null) {
            for (Map.Entry<Object, Object> e : this.map.entrySet()) {
                String name = ((Name) e.getKey()).toString();
                if (!name.equalsIgnoreCase(vername)) {
                    Manifest.println72(out, name + ": " + (String) e.getValue());
                }
            }
        }
        Manifest.println(out);
    }

    /**
     * El nombre de un atributo del manifiesto.
     *
     * <p>Es una clase aparte y no un `String` por una sola razon, y es la que justifica todo lo
     * demas: los nombres de atributo **no distinguen mayusculas**. `Class-Path` y `class-path` son el
     * mismo atributo, y un `String` como clave del mapa no puede expresar eso.
     *
     * <p>El juego de caracteres permitido es cerrado --letras, digitos, `-` y `_`-- y el largo va de
     * 1 a 70. No es decorativo: un nombre con `:` o con un espacio genera un manifiesto que no se
     * puede volver a leer, asi que se rechaza al construirlo y no al escribirlo.
     */
    public static class Name {

        private final String name;
        private int hash;

        /**
         * Un nombre de atributo.
         *
         * @throws NullPointerException si `name` es `null`
         * @throws IllegalArgumentException si no es un nombre valido de cabecera
         */
        public Name(String name) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            if (!esValido(name)) {
                throw new IllegalArgumentException(name);
            }
            this.name = name;
            this.hash = 0;
        }

        private static boolean esValido(String s) {
            int n = s.length();
            if (n > 70 || n == 0) {
                return false;
            }
            int i = 0;
            while (i < n) {
                char c = s.charAt(i);
                boolean ok = (c >= 'a' && c <= 'z')
                        || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9')
                        || c == '_' || c == '-';
                if (!ok) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        }

        /** Dos nombres son iguales si difieren solo en mayusculas. */
        public boolean equals(Object o) {
            if (!(o instanceof Name)) {
                return false;
            }
            return this.name.equalsIgnoreCase(((Name) o).name);
        }

        /**
         * El hash de la version en minusculas, que es lo unico compatible con el `equals` de arriba.
         *
         * <p>Se pliega solo el rango ASCII a mano en vez de llamar a `toLowerCase()`: el
         * constructor ya garantizo que no hay nada fuera de ASCII, y asi el hash no depende de la
         * localidad.
         */
        public int hashCode() {
            if (this.hash == 0) {
                int h = 0;
                int i = 0;
                while (i < this.name.length()) {
                    char c = this.name.charAt(i);
                    if (c >= 'A' && c <= 'Z') {
                        c = (char) (c + 32);
                    }
                    h = 31 * h + c;
                    i = i + 1;
                }
                this.hash = h;
            }
            return this.hash;
        }

        /** El nombre tal cual se escribio. */
        public String toString() {
            return this.name;
        }

        /** `Manifest-Version` */
        public static final Name MANIFEST_VERSION = new Name("Manifest-Version");
        /** `Signature-Version` */
        public static final Name SIGNATURE_VERSION = new Name("Signature-Version");
        /** `Content-Type` */
        public static final Name CONTENT_TYPE = new Name("Content-Type");
        /** `Class-Path` */
        public static final Name CLASS_PATH = new Name("Class-Path");
        /** `Main-Class` */
        public static final Name MAIN_CLASS = new Name("Main-Class");
        /** `Sealed` */
        public static final Name SEALED = new Name("Sealed");
        /** `Extension-List` */
        public static final Name EXTENSION_LIST = new Name("Extension-List");
        /** `Extension-Name` */
        public static final Name EXTENSION_NAME = new Name("Extension-Name");
        /** `Extension-Installation` */
        public static final Name EXTENSION_INSTALLATION = new Name("Extension-Installation");
        /** `Implementation-Title` */
        public static final Name IMPLEMENTATION_TITLE = new Name("Implementation-Title");
        /** `Implementation-Version` */
        public static final Name IMPLEMENTATION_VERSION = new Name("Implementation-Version");
        /** `Implementation-Vendor` */
        public static final Name IMPLEMENTATION_VENDOR = new Name("Implementation-Vendor");
        /** `Implementation-Vendor-Id` */
        public static final Name IMPLEMENTATION_VENDOR_ID = new Name("Implementation-Vendor-Id");
        /** `Implementation-URL` */
        public static final Name IMPLEMENTATION_URL = new Name("Implementation-URL");
        /** `Specification-Title` */
        public static final Name SPECIFICATION_TITLE = new Name("Specification-Title");
        /** `Specification-Version` */
        public static final Name SPECIFICATION_VERSION = new Name("Specification-Version");
        /** `Specification-Vendor` */
        public static final Name SPECIFICATION_VENDOR = new Name("Specification-Vendor");
        /** `Multi-Release` */
        public static final Name MULTI_RELEASE = new Name("Multi-Release");
    }
}
