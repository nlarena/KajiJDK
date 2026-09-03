package java.security.cert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Un `GeneralName` de X.509: una de nueve formas de nombrar algo.
//
// ===============================================================================================
// QUE ES Y POR QUE VIVE EN SU PROPIA CLASE
// ===============================================================================================
//
//   GeneralName ::= CHOICE {
//       otherName                 [0] OtherName,
//       rfc822Name                [1] IA5String,
//       dNSName                   [2] IA5String,
//       x400Address               [3] ORAddress,
//       directoryName             [4] Name,
//       ediPartyName              [5] EDIPartyName,
//       uniformResourceIdentifier [6] IA5String,
//       iPAddress                 [7] OCTET STRING,
//       registeredID              [8] OBJECT IDENTIFIER }
//
// Aparece en tres lugares que este paquete necesita --SubjectAltName, IssuerAltName y
// NameConstraints-- y en los tres hace falta lo mismo: leerlo, escribirlo, compararlo, y decidir si
// un nombre cae **adentro** de otro. Ese ultimo es el que importa y el que no es obvio, asi que
// tiene su propio metodo con su propia explicacion (`contains`).
//
// ===============================================================================================
// LO QUE SE ENTIENDE Y LO QUE SE TRANSPORTA
// ===============================================================================================
//
// Seis de las nueve formas se entienden de verdad: rfc822Name, dNSName, directoryName, URI,
// iPAddress y registeredID. Las otras tres --otherName, x400Address y ediPartyName-- se guardan
// como bytes y **solo se comparan por igualdad exacta**. Es lo honesto: son estructuras con
// semantica propia que casi nadie usa, y un `contains` inventado para ellas seria decir que un
// nombre esta adentro de un subarbol sin saberlo.
//
// Por eso `ofString` las rechaza, igual que el JDK: no hay una forma de texto acordada para ellas.
final class GeneralNameValue {

    static final int OTHER = 0;
    static final int RFC822 = 1;
    static final int DNS = 2;
    static final int X400 = 3;
    static final int DIRECTORY = 4;
    static final int EDI_PARTY = 5;
    static final int URI = 6;
    static final int IP = 7;
    static final int REGISTERED_ID = 8;

    private static final String OID_COMMON_NAME = "2.5.4.3";
    private static final String OID_EMAIL_ADDRESS = "1.2.840.113549.1.9.1";

    private final int type;
    // Para las formas de texto (1, 2, 6, 8). Null en las demas.
    private final String text;
    // Para iPAddress (la direccion, o direccion+mascara en un subarbol) y para las tres opacas.
    private final byte[] bytes;
    // Para directoryName. Se guarda el principal y no los bytes porque la comparacion de nombres
    // X.500 es por forma canonica, no por codificacion.
    private final javax.security.auth.x500.X500Principal dn;

    private GeneralNameValue(int type, String text, byte[] bytes,
            javax.security.auth.x500.X500Principal dn) {
        this.type = type;
        this.text = text;
        this.bytes = bytes;
        this.dn = dn;
    }

    int type() {
        return this.type;
    }

    /**
     * Un nombre escrito como texto, en la forma que le corresponde a su tipo.
     *
     * @throws IOException si el tipo no tiene forma de texto, o si el texto no es valido para el
     */
    static GeneralNameValue ofString(int type, String name) throws IOException {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        switch (type) {
            case RFC822:
                if (name.length() == 0) {
                    throw new IOException("RFC822Name must not be empty");
                }
                return new GeneralNameValue(type, name, null, null);
            case DNS:
                checkDns(name);
                return new GeneralNameValue(type, name, null, null);
            case URI:
                // Tiene que traer esquema: sin el no hay host que comparar, y el host es lo unico
                // que una restriccion de nombres mira de una URI.
                if (name.indexOf(':') < 0) {
                    throw new IOException("URI name must include a scheme: " + name);
                }
                return new GeneralNameValue(type, name, null, null);
            case REGISTERED_ID:
                DerReader.validateOid(name);
                return new GeneralNameValue(type, name, null, null);
            case IP:
                return new GeneralNameValue(type, null, parseIp(name), null);
            case DIRECTORY:
                try {
                    return new GeneralNameValue(type, null, null,
                        new javax.security.auth.x500.X500Principal(name));
                } catch (IllegalArgumentException e) {
                    throw new IOException("Incorrect AVA format", e);
                }
            default:
                // otherName, x400Address, ediPartyName y cualquier numero fuera de rango.
                throw new IOException("unable to parse String names of type " + type);
        }
    }

    /**
     * Un nombre a partir del DER de su **valor**, sin la etiqueta de contexto: un IA5String para
     * los de texto, un `Name` para directoryName, un OCTET STRING para iPAddress.
     *
     * <p>Es la forma que espera {@code X509CertSelector.addSubjectAlternativeName(int, byte[])}, y
     * conviene decirlo porque la otra --con la etiqueta puesta-- es la que uno espera.
     */
    static GeneralNameValue ofValueDer(int type, byte[] der) throws IOException {
        DerReader d = new DerReader(der, 0, der.length);
        int tag = d.readTag();
        int len = d.readLength();
        int at = d.skip(len);
        switch (type) {
            case RFC822:
            case DNS:
            case URI:
                if (tag != 0x16) {
                    throw new IOException("expected an IA5String for name type " + type);
                }
                return ofDerText(type, new String(der, at, len, StandardCharsets.US_ASCII));
            case REGISTERED_ID:
                if (tag != DerReader.TAG_OID) {
                    throw new IOException("expected an OBJECT IDENTIFIER");
                }
                return new GeneralNameValue(type, d.readOid(at, len), null, null);
            case IP:
                if (tag != DerReader.TAG_OCTET_STRING) {
                    throw new IOException("expected an OCTET STRING");
                }
                return new GeneralNameValue(type, null, d.copy(at, len), null);
            case DIRECTORY:
                if (tag != DerReader.TAG_SEQUENCE) {
                    throw new IOException("expected a Name");
                }
                return ofDirectory(der);
            default:
                return new GeneralNameValue(type, null, copyOf(der), null);
        }
    }

    /** Un directoryName a partir del DER de su `Name`. */
    static GeneralNameValue ofDirectory(byte[] nameDer) throws IOException {
        try {
            return new GeneralNameValue(DIRECTORY, null, null,
                new javax.security.auth.x500.X500Principal(nameDer));
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid name", e);
        }
    }

    /**
     * Un nombre leido de una lista --SubjectAltName o un subarbol-- donde viene **con** su etiqueta
     * de contexto.
     *
     * @param at    donde empieza el TLV completo
     * @param total cuanto ocupa, cabecera incluida
     */
    static GeneralNameValue ofTagged(byte[] buf, int at, int total) throws IOException {
        DerReader d = new DerReader(buf, at, total);
        int tag = d.readTag();
        int len = d.readLength();
        int from = d.skip(len);
        int type = tag & 0x1f;
        switch (type) {
            case RFC822:
            case DNS:
            case URI:
                return ofDerText(type, new String(buf, from, len, StandardCharsets.US_ASCII));
            case REGISTERED_ID:
                return new GeneralNameValue(type, d.readOid(from, len), null, null);
            case IP:
                return new GeneralNameValue(type, null, d.copy(from, len), null);
            case DIRECTORY:
                // [4] es CONSTRUIDO y envuelve el `Name` entero, asi que adentro hay otro SEQUENCE.
                return ofDirectory(d.copy(from, len));
            default:
                return new GeneralNameValue(type, null, d.copy(at, total), null);
        }
    }

    /**
     * El valor tal como lo devuelve {@code getSubjectAlternativeNames()}: un {@code String} para las
     * formas de texto, un {@code byte[]} para las demas.
     */
    Object storedValue() {
        if (this.type == DIRECTORY) {
            return this.dn.getName();
        }
        if (this.text != null) {
            return this.text;
        }
        return copyOf(this.bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GeneralNameValue)) {
            return false;
        }
        GeneralNameValue other = (GeneralNameValue) o;
        if (other.type != this.type) {
            return false;
        }
        if (this.type == DIRECTORY) {
            // Por forma canonica: dos nombres X.500 escritos distinto son el mismo nombre.
            return this.dn.equals(other.dn);
        }
        if (this.type == DNS || this.type == RFC822 || this.type == URI) {
            // La caja no cuenta en un nombre de host ni en un dominio de correo.
            return this.text.equalsIgnoreCase(other.text);
        }
        if (this.text != null) {
            return this.text.equals(other.text);
        }
        return sameBytes(this.bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        if (this.type == DIRECTORY) {
            return this.type * 31 + this.dn.hashCode();
        }
        if (this.type == DNS || this.type == RFC822 || this.type == URI) {
            return this.type * 31 + this.text.toLowerCase().hashCode();
        }
        if (this.text != null) {
            return this.type * 31 + this.text.hashCode();
        }
        int h = this.type;
        int i = 0;
        while (i < this.bytes.length) {
            h = h * 31 + this.bytes[i];
            i = i + 1;
        }
        return h;
    }

    @Override
    public String toString() {
        return this.type + ":" + this.storedValue();
    }

    /**
     * Si <b>este</b> nombre, tomado como la base de un subarbol, contiene a {@code name}.
     *
     * <p>Es la operacion que decide si un certificado cae adentro de lo que su CA tenia permitido,
     * asi que cada regla esta escrita con su motivo. Un nombre de otro tipo nunca esta contenido:
     * eso lo decide {@code NameConstraints}, no este metodo.
     */
    boolean contains(GeneralNameValue name) {
        if (name.type != this.type) {
            return false;
        }
        switch (this.type) {
            case DNS:
                return containsDns(this.text, name.text);
            case RFC822:
                return containsRfc822(this.text, name.text);
            case URI:
                return containsUri(this.text, name.text);
            case IP:
                return containsIp(this.bytes, name.bytes);
            case DIRECTORY:
                return containsDirectory(this.dn, name.dn);
            default:
                // registeredID y las tres opacas: solo igualdad. Ver la nota de la clase.
                return this.equals(name);
        }
    }

    // El corte va en el punto, no en cualquier lugar: `acme.com` contiene a `www.acme.com` pero
    // **no** a `xacme.com`. Sin esa condicion, quien registre `malacme.com` quedaria adentro del
    // subarbol de `acme.com`, que es exactamente el agujero que las restricciones evitan.
    private static boolean containsDns(String base, String name) {
        String b = base.toLowerCase();
        String n = name.toLowerCase();
        // Una base con punto adelante viene de certificados que escriben asi los subdominios. No es
        // la forma del RFC para dNSName, pero se encuentra, y leerla como sufijo es lo unico que
        // puede querer decir.
        if (b.startsWith(".")) {
            return n.endsWith(b);
        }
        if (n.equals(b)) {
            return true;
        }
        return n.length() > b.length() && n.endsWith(b)
            && n.charAt(n.length() - b.length() - 1) == '.';
    }

    // Tres formas, y las tres distintas:
    //
    //   - `u@acme.com` (con arroba) es un buzon: solo ese.
    //   - `.acme.com` (con punto adelante) son los subdominios: `u@sub.acme.com` si, `u@acme.com` no.
    //   - `acme.com` (pelado) es **ese host exacto**: `u@acme.com` si, `u@sub.acme.com` no.
    //
    // La tercera es la que sorprende, porque en dNSName el nombre pelado si abarca los subdominios.
    private static boolean containsRfc822(String base, String name) {
        String b = base.toLowerCase();
        String n = name.toLowerCase();
        if (b.indexOf('@') >= 0) {
            return n.equals(b);
        }
        int arroba = n.indexOf('@');
        String host = arroba < 0 ? n : n.substring(arroba + 1);
        if (b.startsWith(".")) {
            // Un host que termina en `.acme.com` es un subdominio; `acme.com` pelado no termina
            // asi, y por eso queda afuera -- que es lo que la forma con punto significa.
            return host.endsWith(b);
        }
        return host.equals(b);
    }

    // De una URI solo se mira el **host**: el camino y la consulta no dicen de quien es el nombre.
    // Con punto adelante son los subdominios; sin punto es el host exacto --y aca no abarca
    // subdominios, a diferencia de dNSName--.
    private static boolean containsUri(String base, String name) {
        String host = hostOf(name);
        if (host == null) {
            return false;
        }
        String b = base.toLowerCase();
        String h = host.toLowerCase();
        if (b.startsWith(".")) {
            return h.endsWith(b);
        }
        return h.equals(b);
    }

    // El host de una URI, sin usuario ni puerto. Null si no tiene autoridad.
    private static String hostOf(String uri) {
        int scheme = uri.indexOf("://");
        if (scheme < 0) {
            return null;
        }
        int from = scheme + 3;
        int end = uri.length();
        int i = from;
        while (i < end) {
            char c = uri.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
            i = i + 1;
        }
        String authority = uri.substring(from, end);
        int arroba = authority.lastIndexOf('@');
        if (arroba >= 0) {
            authority = authority.substring(arroba + 1);
        }
        int colon = authority.lastIndexOf(':');
        if (colon >= 0 && authority.indexOf(']') < colon) {
            authority = authority.substring(0, colon);
        }
        return authority.length() == 0 ? null : authority;
    }

    // En un subarbol la direccion viene con su mascara pegada atras: 8 bytes para IPv4 y 32 para
    // IPv6. En un nombre viene sola. Se comparan los bits que la mascara deja pasar.
    private static boolean containsIp(byte[] base, byte[] name) {
        if (name == null || base == null) {
            return false;
        }
        if (base.length == name.length) {
            return sameBytes(base, name);
        }
        if (base.length != name.length * 2) {
            return false;
        }
        int n = name.length;
        int i = 0;
        while (i < n) {
            if (((base[i] ^ name[i]) & base[n + i]) != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // El subarbol es un **prefijo** del nombre en el orden del DER, que es el orden inverso al del
    // texto RFC 2253. Por eso aca se compara por sufijo: `o=acme` contiene a `cn=juan,o=acme`.
    //
    // Se compara sobre la forma canonica --minusculas, espacios colapsados-- porque dos nombres
    // X.500 escritos distinto son el mismo nombre, y el corte va en la coma: en forma canonica una
    // coma adentro de un valor viene escapada, asi que una coma pelada siempre separa.
    private static boolean containsDirectory(javax.security.auth.x500.X500Principal base,
            javax.security.auth.x500.X500Principal name) {
        String b = base.getName(javax.security.auth.x500.X500Principal.CANONICAL);
        String n = name.getName(javax.security.auth.x500.X500Principal.CANONICAL);
        if (b.length() == 0) {
            // La raiz del directorio contiene a todos.
            return true;
        }
        if (n.equals(b)) {
            return true;
        }
        return n.length() > b.length() && n.endsWith(b)
            && n.charAt(n.length() - b.length() - 1) == ',';
    }

    // Un nombre DNS: al menos una etiqueta, etiquetas de letras, digitos y guiones, sin punto al
    // principio ni al final ni dos seguidos. Es lo que valida el JDK y por eso `CN=Juan Perez` no
    // se toma por un nombre de host y `CN=x` si.
    private static void checkDns(String name) throws IOException {
        if (name.length() == 0) {
            throw new IOException("DNSName must not be null or empty");
        }
        int i = 0;
        int labelLen = 0;
        while (i < name.length()) {
            char c = name.charAt(i);
            if (c == '.') {
                if (labelLen == 0) {
                    throw new IOException("DNSName with an empty label: " + name);
                }
                labelLen = 0;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-') {
                labelLen = labelLen + 1;
            } else {
                throw new IOException(
                    "DNSName components must consist of letters, digits, and hyphens");
            }
            i = i + 1;
        }
        if (labelLen == 0) {
            throw new IOException("DNSName with an empty label: " + name);
        }
    }

    /**
     * Un nombre de texto leido de un DER, sin la validacion de forma.
     *
     * <p>La validacion es para lo que <b>escribe</b> el llamador: un nombre mal escrito ahi es un
     * error suyo y hay que decirselo. Lo que ya esta adentro de un certificado, en cambio, hay que
     * poder leerlo aunque no sea del todo conforme -- rechazarlo no lo arregla, solo deja al
     * certificado sin comparar, que es peor.
     */
    private static GeneralNameValue ofDerText(int type, String text) {
        return new GeneralNameValue(type, text, null, null);
    }

    /** Si ese texto sirve como nombre DNS. Lo usa la regla del CN de {@code NameConstraints}. */
    static boolean looksLikeDns(String text) {
        try {
            checkDns(text);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Una direccion IP escrita como texto, o una direccion con mascara para un subarbol.
     *
     * <p><b>Diferencia anotada con el JDK</b>: el JDK pasa por {@code InetAddress}, que acepta ademas
     * las formas abreviadas de BSD --{@code "10.0.0"}, {@code "10.1"}-- donde la ultima parte llena
     * los bytes que faltan. Aca se exigen las cuatro partes. La diferencia es siempre hacia el lado
     * seguro: lo que aca se rechaza, alla se aceptaba con un valor que casi nadie predice bien.
     */
    private static byte[] parseIp(String name) throws IOException {
        int slash = name.indexOf('/');
        if (slash >= 0) {
            byte[] dir = parseAddress(name.substring(0, slash));
            String tail = name.substring(slash + 1);
            byte[] mask;
            if (tail.indexOf('.') >= 0 || tail.indexOf(':') >= 0) {
                mask = parseAddress(tail);
            } else {
                mask = maskOfBits(dir.length, tail);
            }
            if (mask.length != dir.length) {
                throw new IOException("address and mask are of different families: " + name);
            }
            byte[] out = new byte[dir.length * 2];
            System.arraycopy(dir, 0, out, 0, dir.length);
            System.arraycopy(mask, 0, out, dir.length, mask.length);
            return out;
        }
        return parseAddress(name);
    }

    private static byte[] maskOfBits(int size, String bitCount) throws IOException {
        int n;
        try {
            n = Integer.parseInt(bitCount);
        } catch (NumberFormatException e) {
            throw new IOException("bad prefix length: " + bitCount);
        }
        if (n < 0 || n > size * 8) {
            throw new IOException("bad prefix length: " + bitCount);
        }
        byte[] m = new byte[size];
        int i = 0;
        while (i < size) {
            int enEste = n - i * 8;
            if (enEste >= 8) {
                m[i] = (byte) 0xff;
            } else if (enEste > 0) {
                m[i] = (byte) (0xff << (8 - enEste));
            }
            i = i + 1;
        }
        return m;
    }

    private static byte[] parseAddress(String s) throws IOException {
        if (s.indexOf(':') >= 0) {
            return parseIpv6(s);
        }
        String[] partes = split(s, '.');
        if (partes.length != 4) {
            throw new IOException("not an IPv4 address: " + s);
        }
        byte[] b = new byte[4];
        int i = 0;
        while (i < 4) {
            int v = numberAt(partes[i], 10, s);
            if (v < 0 || v > 255) {
                throw new IOException("IPv4 octet out of range: " + s);
            }
            b[i] = (byte) v;
            i = i + 1;
        }
        return b;
    }

    // IPv6 con `::` una sola vez. No se aceptan los ultimos cuatro bytes en forma decimal
    // (`::ffff:10.0.0.1`): esa forma tiene dos codificaciones del mismo valor y no aporta nada aca.
    private static byte[] parseIpv6(String s) throws IOException {
        int doble = s.indexOf("::");
        if (doble != s.lastIndexOf("::")) {
            throw new IOException("more than one :: in " + s);
        }
        byte[] b = new byte[16];
        String izq = doble < 0 ? s : s.substring(0, doble);
        String der = doble < 0 ? "" : s.substring(doble + 2);
        String[] a = izq.length() == 0 ? new String[0] : split(izq, ':');
        String[] c = der.length() == 0 ? new String[0] : split(der, ':');
        if (doble < 0 && a.length != 8) {
            throw new IOException("not an IPv6 address: " + s);
        }
        if (a.length + c.length > 8) {
            throw new IOException("too many groups in " + s);
        }
        int i = 0;
        while (i < a.length) {
            writeGroup(b, i * 2, numberAt(a[i], 16, s));
            i = i + 1;
        }
        int j = 0;
        while (j < c.length) {
            writeGroup(b, 16 - (c.length - j) * 2, numberAt(c[j], 16, s));
            j = j + 1;
        }
        return b;
    }

    private static void writeGroup(byte[] b, int at, int v) {
        b[at] = (byte) (v >> 8);
        b[at + 1] = (byte) v;
    }

    private static int numberAt(String s, int base, String whole) throws IOException {
        if (s.length() == 0 || s.length() > (base == 16 ? 4 : 3)) {
            throw new IOException("bad address component in " + whole);
        }
        int v = 0;
        int i = 0;
        while (i < s.length()) {
            int d = Character.digit(s.charAt(i), base);
            if (d < 0) {
                throw new IOException("bad address component in " + whole);
            }
            v = v * base + d;
            i = i + 1;
        }
        return v;
    }

    private static String[] split(String s, char sep) {
        int n = 1;
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == sep) {
                n = n + 1;
            }
            i = i + 1;
        }
        String[] out = new String[n];
        int k = 0;
        int from = 0;
        i = 0;
        while (i <= s.length()) {
            if (i == s.length() || s.charAt(i) == sep) {
                out[k] = s.substring(from, i);
                k = k + 1;
                from = i + 1;
            }
            i = i + 1;
        }
        return out;
    }

    /** El OID del atributo `CN`, para la regla heredada de {@code NameConstraints}. */
    static String commonNameOid() {
        return OID_COMMON_NAME;
    }

    /** El OID del atributo `EMAILADDRESS`, idem. */
    static String emailAddressOid() {
        return OID_EMAIL_ADDRESS;
    }

    private static byte[] copyOf(byte[] b) {
        if (b == null) {
            return null;
        }
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    private static boolean sameBytes(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length != b.length) {
            return false;
        }
        int i = 0;
        while (i < a.length) {
            if (a[i] != b[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }
}
