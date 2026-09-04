package javax.security.auth.kerberos;

import java.io.Serializable;
import java.security.Principal;

/**
 * KajiLibrary's javax.security.auth.kerberos.KerberosPrincipal -- un nombre de Kerberos.
 *
 * <p>Tiene la forma {@code componente/componente@REINO}: un usuario es {@code ana@EMPRESA.COM}, un
 * servicio es {@code host/servidor.empresa.com@EMPRESA.COM}. El reino va en mayusculas por
 * convencion, pero esta clase no lo impone: es un nombre, no una regla.
 *
 * <h2>El reino por omision</h2>
 *
 * <p>Un nombre sin {@code @REINO} toma el reino de la configuracion de Kerberos. KajiJDK no lee
 * {@code krb5.conf}; toma la propiedad {@code java.security.krb5.realm} si esta, y si no falla con
 * {@link IllegalArgumentException}, igual que el JDK sin configuracion. Es la causa mas comun de que
 * un programa que anda en una maquina no ande en otra: no es el codigo, es que la otra no tiene
 * configurado el reino.
 *
 * <h2>El tipo de nombre no cuenta para la igualdad</h2>
 *
 * <p>{@link #getNameType} dice si es un usuario, un servicio, un host. Dos principales con el mismo
 * nombre y distinto tipo son <b>iguales</b> y tienen el mismo hash; el tipo es una pista para el KDC,
 * no parte de la identidad.
 *
 * <h2>La arroba se puede escapar</h2>
 *
 * <p>{@code a\\@b@REINO} es el usuario {@code a@b} del reino {@code REINO}: la primera arroba va
 * precedida de una barra y no separa nada. Esta clase corta en la primera arroba <b>sin</b> escapar y
 * deja el nombre tal cual, con sus barras.
 */
public final class KerberosPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -7374788026156829911L;

    /** Tipo desconocido. */
    public static final int KRB_NT_UNKNOWN = 0;

    /** Un usuario, o un servicio con nombre propio. */
    public static final int KRB_NT_PRINCIPAL = 1;

    /** Un servicio con una instancia: {@code servicio/instancia}. */
    public static final int KRB_NT_SRV_INST = 2;

    /** Un servicio de un host: {@code servicio/host}. */
    public static final int KRB_NT_SRV_HST = 3;

    /** Un servicio de un host, con el host como componentes separados. */
    public static final int KRB_NT_SRV_XHST = 4;

    /** Un identificador numerico. */
    public static final int KRB_NT_UID = 5;

    /** Un nombre de empresa, del estilo {@code usuario@dominio}. */
    public static final int KRB_NT_ENTERPRISE = 10;

    /** El nombre completo, con el reino. */
    private final String fullName;

    /** El reino. */
    private final String realm;

    /** De que tipo es. */
    private final int nameType;

    /**
     * Un principal de tipo {@link #KRB_NT_PRINCIPAL}.
     *
     * @throws IllegalArgumentException si el nombre es null, esta vacio, esta mal formado, o no
     *     tiene reino y no hay reino por omision
     */
    public KerberosPrincipal(String name) {
        this(name, KRB_NT_PRINCIPAL);
    }

    /**
     * Un principal de ese tipo.
     *
     * @throws IllegalArgumentException si el nombre no sirve, o si el tipo no es uno de los siete
     */
    public KerberosPrincipal(String name, int nameType) {
        if (name == null) {
            throw new IllegalArgumentException("Null name not allowed");
        }
        if (!isLegalType(nameType)) {
            throw new IllegalArgumentException("Illegal name type");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Empty nameString not allowed");
        }
        int at = unescapedAt(name);
        String namePart;
        String realmPart;
        if (at < 0) {
            namePart = name;
            realmPart = defaultRealm();
        } else {
            namePart = name.substring(0, at);
            realmPart = name.substring(at + 1);
        }
        checkNamePart(namePart);
        checkRealm(realmPart);
        this.fullName = namePart + "@" + realmPart;
        this.realm = realmPart;
        this.nameType = nameType;
    }

    /** El reino. */
    public String getRealm() {
        return this.realm;
    }

    /** El hash del nombre completo; el tipo no cuenta. Ver la nota de la clase. */
    @Override
    public int hashCode() {
        return this.fullName.hashCode();
    }

    /** Iguales si el nombre completo es el mismo; el tipo no cuenta. Ver la nota de la clase. */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof KerberosPrincipal)) {
            return false;
        }
        return this.fullName.equals(((KerberosPrincipal) other).fullName);
    }

    /** El nombre completo, con el reino. */
    @Override
    public String getName() {
        return this.fullName;
    }

    /** De que tipo es. */
    public int getNameType() {
        return this.nameType;
    }

    /** El nombre completo. */
    @Override
    public String toString() {
        return this.fullName;
    }

    /** Si es uno de los siete tipos. */
    private static boolean isLegalType(int nameType) {
        return (nameType >= KRB_NT_UNKNOWN && nameType <= KRB_NT_UID)
            || nameType == KRB_NT_ENTERPRISE;
    }

    /** La posicion de la primera arroba sin escapar, o -1. Ver la nota de la clase. */
    private static int unescapedAt(String name) {
        int i = 0;
        while (i < name.length()) {
            char c = name.charAt(i);
            if (c == '\\') {
                i = i + 2;
                continue;
            }
            if (c == '@') {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /** Que la parte del nombre tenga componentes y ninguno este vacio en el medio. */
    private static void checkNamePart(String namePart) {
        if (namePart.isEmpty()) {
            throw new IllegalArgumentException("Empty nameStrings not allowed");
        }
        // Un componente vacio en el medio --"host//x"-- no es un nombre; una barra al final si se
        // tolera, como en el JDK.
        int i = 0;
        while (i < namePart.length()) {
            char c = namePart.charAt(i);
            if (c == '\\') {
                i = i + 2;
                continue;
            }
            if (c == '/' && i + 1 < namePart.length() && namePart.charAt(i + 1) == '/') {
                throw new IllegalArgumentException("Empty nameString not allowed");
            }
            i = i + 1;
        }
    }

    /** Que el reino no este vacio ni tenga caracteres que el protocolo no admite. */
    private static void checkRealm(String realm) {
        if (realm.isEmpty()) {
            throw new IllegalArgumentException("empty realm part not allowed");
        }
        int i = 0;
        while (i < realm.length()) {
            char c = realm.charAt(i);
            if (c == '/' || c == ':' || c == '@' || c == '\0') {
                throw new IllegalArgumentException(
                    "Illegal character in realm name; one of: '/', ':', '@' (600)");
            }
            i = i + 1;
        }
    }

    /** El reino por omision. Ver la nota de la clase. */
    private static String defaultRealm() {
        String configured = System.getProperty("java.security.krb5.realm");
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        throw new IllegalArgumentException("KrbException: Cannot locate default realm");
    }
}
