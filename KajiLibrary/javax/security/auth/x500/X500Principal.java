package javax.security.auth.x500;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KajiLibrary's javax.security.auth.x500.X500Principal -- un nombre distinguido X.501.
 *
 * <p>Es como se nombra a alguien en un certificado: al sujeto, al emisor, al titular de una lista de
 * revocacion. Un DN es una **secuencia ordenada** de pasos --pais, organizacion, unidad, nombre
 * comun-- que van de lo general a lo particular, y el orden importa porque el nombre **es** el camino.
 *
 * <h2>Las dos formas, y por que van al reves</h2>
 *
 * <p>El mismo nombre se escribe de dos maneras y hay que tener las dos en la cabeza, porque el orden
 * es **opuesto**:
 *
 * <ul>
 *   <li>En **texto** (RFC 2253) va de lo particular a lo general:
 *       {@code CN=Juan, OU=Ventas, O=Acme, C=AR}.
 *   <li>En **DER** va de lo general a lo particular: primero el pais, ultimo el nombre comun.
 * </ul>
 *
 * <p>No es un capricho de nadie: el DER refleja la jerarquia del directorio --se baja desde la raiz--
 * y el texto refleja como se lee un nombre en voz alta. Invertirlo es el error mas comun al
 * implementar esta clase, y produce certificados que parecen bien y encadenan mal.
 *
 * <h2>Los tres formatos de salida</h2>
 *
 * <ul>
 *   <li>{@link #RFC2253} -- la forma canonica de escribir un DN. Es la que devuelve {@link #getName()}.
 *   <li>{@link #RFC1779} -- la forma vieja, con espacios despues de las comas y {@code OID.x.y} para
 *       los tipos que no tienen palabra clave.
 *   <li>{@link #CANONICAL} -- la de **comparar**, no la de mostrar: todo en minusculas, sin espacios
 *       de sobra, con los espacios internos colapsados. Dos DN que designan a la misma entidad dan la
 *       misma cadena canonica aunque se hayan escrito distinto, y eso es lo unico para lo que sirve.
 * </ul>
 *
 * <h2>Que hay aca y que no</h2>
 *
 * <p>Esto es **codificacion, no criptografia**: parsear un nombre y volver a escribirlo no toma
 * ninguna decision de confianza. Un error aca da un nombre mal leido o una excepcion, nunca una firma
 * aceptada sin verificar. Por eso se puede implementar entero y de verdad, a diferencia de casi todo
 * lo que lo rodea.
 *
 * <p>Lo que **no** esta: la serializacion propia (`writeObject`/`readObject`). Esta biblioteca no
 * tiene `ObjectOutputStream`, asi que declarar `Serializable` es honesto --el contrato lo pide-- y
 * escribir la serializacion seria inventar un formato que nadie puede leer.
 */
public final class X500Principal implements java.security.Principal, java.io.Serializable {

    /** La forma vieja: espacios despues de las comas, `OID.x.y` para lo que no tiene palabra clave. */
    public static final String RFC1779 = "RFC1779";

    /** La forma normal de escribir un DN. Es la que devuelve {@link #getName()}. */
    public static final String RFC2253 = "RFC2253";

    /** La forma de **comparar**: minusculas, sin espacios de sobra. No es para mostrarle a nadie. */
    public static final String CANONICAL = "CANONICAL";

    // Los pasos del nombre, en el orden del **texto**: del mas particular al mas general. Se guarda
    // asi y no al reves porque es el orden en que se escribe y se lee; el DER lo invierte al salir.
    private final Rdn[] rdns;

    // La codificacion DER original, cuando el nombre vino de bytes. Se guarda **tal cual** en vez de
    // recodificarla, y eso importa: un certificado se firma sobre sus bytes exactos, asi que
    // devolverlos re-codificados --aunque fueran equivalentes-- rompe la verificacion de la firma.
    private final byte[] derOriginal;

    // ---- construccion ----------------------------------------------------------------------------

    /**
     * El nombre escrito en RFC 2253.
     *
     * @throws IllegalArgumentException si no parsea
     */
    public X500Principal(String name) {
        this(name, java.util.Collections.<String, String>emptyMap());
    }

    /**
     * El de arriba con palabras clave **propias**.
     *
     * <p>El mapa va de palabra clave a OID, y sirve para nombres que usan tipos que el estandar no
     * bautizo. Las palabras clave conocidas siguen valiendo; las del mapa se suman.
     *
     * @throws IllegalArgumentException si no parsea, o si un OID del mapa esta mal formado
     */
    public X500Principal(String name, Map<String, String> keywordMap) {
        if (name == null || keywordMap == null) {
            throw new NullPointerException();
        }
        this.rdns = Parser.parse(name, keywordMap);
        this.derOriginal = null;
    }

    /**
     * El nombre codificado en DER.
     *
     * @throws IllegalArgumentException si los bytes no son un `Name` valido
     */
    public X500Principal(byte[] name) {
        if (name == null) {
            throw new NullPointerException();
        }
        byte[] copyOf = new byte[name.length];
        int i = 0;
        while (i < copyOf.length) {
            copyOf[i] = name[i];
            i = i + 1;
        }
        try {
            this.rdns = Der.readName(copyOf);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        this.derOriginal = copyOf;
    }

    /**
     * El nombre leido de un flujo.
     *
     * <p>Lee **solo** el nombre y deja el flujo justo despues, que es lo que permite leer un
     * certificado campo por campo. Para saber donde termina se mira el largo del DER, no el fin del
     * flujo.
     *
     * @throws IllegalArgumentException si no hay un `Name` valido en esa posicion
     */
    public X500Principal(InputStream is) {
        if (is == null) {
            throw new NullPointerException();
        }
        byte[] bytes;
        try {
            bytes = Der.readOneValue(is);
            this.rdns = Der.readName(bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        this.derOriginal = bytes;
    }

    // ---- salida ------------------------------------------------------------------------------------

    /** El nombre en RFC 2253. */
    public String getName() {
        return this.getName(RFC2253);
    }

    /**
     * El nombre en el formato pedido.
     *
     * @throws IllegalArgumentException si el formato no es ninguno de los tres
     */
    public String getName(String format) {
        return this.getName(format, java.util.Collections.<String, String>emptyMap());
    }

    /**
     * El de arriba con OID **propios** traducidos a palabra clave.
     *
     * <p>El mapa va al reves que el del constructor: de OID a palabra clave. Es el mismo diccionario
     * leido en la otra direccion, y va aparte porque no siempre se quiere escribir con las mismas
     * palabras con las que se leyo.
     *
     * @throws IllegalArgumentException si el formato no es ninguno de los tres, o si se le pasa un
     *         mapa no vacio a {@link #CANONICAL}, que no admite traducciones
     */
    public String getName(String format, Map<String, String> oidMap) {
        if (format == null || oidMap == null) {
            throw new NullPointerException();
        }
        if (RFC2253.equalsIgnoreCase(format)) {
            return NameFormat.write(this.rdns, oidMap, false, false);
        }
        if (RFC1779.equalsIgnoreCase(format)) {
            return NameFormat.write(this.rdns, oidMap, true, false);
        }
        if (CANONICAL.equalsIgnoreCase(format)) {
            // El canonico no admite diccionario: si dos programas tradujeran distinto, dos nombres
            // iguales darian cadenas distintas y la forma canonica no serviria para lo unico que
            // sirve, que es comparar.
            if (!oidMap.isEmpty()) {
                throw new IllegalArgumentException("CANONICAL no admite un mapa de OID");
            }
            return NameFormat.write(this.rdns, oidMap, false, true);
        }
        throw new IllegalArgumentException("formato invalido: " + format);
    }

    /**
     * El nombre en DER.
     *
     * <p>Si vino de bytes se devuelven **esos** bytes, no una recodificacion: un certificado se firma
     * sobre su codificacion exacta, y devolver una equivalente pero distinta rompe la verificacion.
     */
    public byte[] getEncoded() {
        byte[] fuente = this.derOriginal != null ? this.derOriginal : Der.writeName(this.rdns);
        byte[] copyOf = new byte[fuente.length];
        int i = 0;
        while (i < copyOf.length) {
            copyOf[i] = fuente[i];
            i = i + 1;
        }
        return copyOf;
    }

    public String toString() {
        return this.getName(RFC1779);
    }

    /**
     * Si los dos nombres designan a la misma entidad.
     *
     * <p>Se compara la forma **canonica** y no los bytes: `CN=Juan,O=Acme` y `cn=juan, o=acme` son el
     * mismo nombre escrito distinto, y comparar el DER diria que no.
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof X500Principal)) {
            return false;
        }
        return this.getName(CANONICAL).equals(((X500Principal) o).getName(CANONICAL));
    }

    public int hashCode() {
        return this.getName(CANONICAL).hashCode();
    }

    // ================================================================================================
    // Un paso del nombre: un conjunto de pares tipo=valor. Casi siempre uno solo; mas de uno es un
    // "RDN multivaluado", que se escribe con `+` y sirve para desempatar dos entidades homonimas.
    // ================================================================================================

    static final class Rdn {
        final String[] types;   // OID en forma de numeros, siempre
        final String[] values;

        Rdn(String[] types, String[] values) {
            this.types = types;
            this.values = values;
        }
    }

    // ================================================================================================
    // El diccionario de palabras clave. Es del estandar (RFC 4514 y los agregados de uso comun), y va
    // en las dos direcciones porque se lee en las dos.
    // ================================================================================================

    static final String[][] CONOCIDOS = {
        {"CN", "2.5.4.3"},
        {"L", "2.5.4.7"},
        {"ST", "2.5.4.8"},
        {"O", "2.5.4.10"},
        {"OU", "2.5.4.11"},
        {"C", "2.5.4.6"},
        {"STREET", "2.5.4.9"},
        {"SERIALNUMBER", "2.5.4.5"},
        {"T", "2.5.4.12"},
        {"SURNAME", "2.5.4.4"},
        {"GIVENNAME", "2.5.4.42"},
        {"INITIALS", "2.5.4.43"},
        {"GENERATION", "2.5.4.44"},
        {"DNQUALIFIER", "2.5.4.46"},
        {"DC", "0.9.2342.19200300.100.1.25"},
        {"UID", "0.9.2342.19200300.100.1.1"},
        {"EMAILADDRESS", "1.2.840.113549.1.9.1"},
    };

    static String oidForWord(String word) {
        int i = 0;
        while (i < CONOCIDOS.length) {
            if (CONOCIDOS[i][0].equalsIgnoreCase(word)) {
                return CONOCIDOS[i][1];
            }
            i = i + 1;
        }
        return null;
    }

    static String wordForOid(String oid) {
        int i = 0;
        while (i < CONOCIDOS.length) {
            if (CONOCIDOS[i][1].equals(oid)) {
                return CONOCIDOS[i][0];
            }
            i = i + 1;
        }
        return null;
    }
}
