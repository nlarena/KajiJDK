package javax.security.auth.kerberos;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * KajiLibrary's javax.security.auth.kerberos.KeyTab -- un archivo de claves de servicio.
 *
 * <p>Un keytab guarda las claves de largo plazo de uno o mas principales, para que un servicio se
 * autentique sin contrasena. Esta clase es una <b>referencia</b> al archivo: no lo lee al crearse
 * sino cada vez que se piden claves, asi que refleja el archivo tal como esta en ese momento.
 *
 * <h2>Ligado o no</h2>
 *
 * <p>Un keytab ligado ({@link #isBound}) es para un principal: {@link #getInstance(KerberosPrincipal)}
 * lo liga a ese, y {@link #getInstance()} lo liga "a alguien", el principal que se resuelva en el
 * momento de usarlo. Uno no ligado --{@link #getUnboundInstance}-- sirve para cualquiera. La
 * distincion existe para que un modulo de login sepa a quien representa el archivo.
 *
 * <h2>El archivo por omision</h2>
 *
 * <p>Sale de la propiedad {@code java.security.krb5.keytab} si esta, y si no es
 * {@code krb5.keytab} en el directorio del usuario. KajiJDK no lee {@code krb5.conf}.
 *
 * <h2>El formato</h2>
 *
 * <p>Se lee la version 0x0502 --la de MIT y Heimdal desde los anos noventa--: dos bytes de version y
 * despues entradas, cada una con su largo, el principal por componentes, la marca de tiempo, la
 * version de la clave y la clave con su tipo. Un archivo que no exista o no se pueda leer da cero
 * claves, no un error: es lo que el JDK hace, y lo que un servicio sin keytab necesita para poder
 * decir "no tengo claves" en vez de caerse.
 */
public final class KeyTab {

    /** La version del formato que se entiende. */
    private static final int KEYTAB_VERSION = 0x0502;

    /** El archivo, o null por el de por omision. */
    private final File file;

    /** A quien esta ligado, o null si esta ligado "a alguien" o no esta ligado. */
    private final KerberosPrincipal princ;

    /** Si esta ligado. Ver la nota de la clase. */
    private final boolean bound;

    /** Se llega por los {@code getInstance}. */
    private KeyTab(KerberosPrincipal princ, File file, boolean bound) {
        this.princ = princ;
        this.file = file;
        this.bound = bound;
    }

    /**
     * Ese archivo, ligado a alguien.
     *
     * @throws NullPointerException si el archivo es null
     */
    public static KeyTab getInstance(File file) {
        if (file == null) {
            throw new NullPointerException("file must be non null");
        }
        return new KeyTab(null, file, true);
    }

    /**
     * Ese archivo, sin ligar.
     *
     * @throws NullPointerException si el archivo es null
     */
    public static KeyTab getUnboundInstance(File file) {
        if (file == null) {
            throw new NullPointerException("file must be non null");
        }
        return new KeyTab(null, file, false);
    }

    /**
     * Ese archivo, ligado a ese principal.
     *
     * @throws NullPointerException si cualquiera es null
     */
    public static KeyTab getInstance(KerberosPrincipal princ, File file) {
        if (princ == null) {
            throw new NullPointerException("princ must be non null");
        }
        if (file == null) {
            throw new NullPointerException("file must be non null");
        }
        return new KeyTab(princ, file, true);
    }

    /** El archivo por omision, ligado a alguien. */
    public static KeyTab getInstance() {
        return new KeyTab(null, null, true);
    }

    /** El archivo por omision, sin ligar. */
    public static KeyTab getUnboundInstance() {
        return new KeyTab(null, null, false);
    }

    /**
     * El archivo por omision, ligado a ese principal.
     *
     * @throws NullPointerException si es null
     */
    public static KeyTab getInstance(KerberosPrincipal princ) {
        if (princ == null) {
            throw new NullPointerException("princ must be non null");
        }
        return new KeyTab(princ, null, true);
    }

    /**
     * Las claves de ese principal que haya en el archivo, en el orden del archivo.
     *
     * <p>Vacio si el archivo no existe o no se puede leer. Ver la nota de la clase.
     *
     * @throws NullPointerException si el principal es null
     */
    public KerberosKey[] getKeys(KerberosPrincipal principal) {
        String wanted = principal.getName();
        List<KerberosKey> keys = new ArrayList<KerberosKey>();
        File source = resolveFile();
        if (source.isFile()) {
            InputStream in = null;
            try {
                in = new FileInputStream(source);
                readEntries(new DataInputStream(in), principal, wanted, keys);
            } catch (IOException e) {
                // Un archivo truncado o de otra version: lo que se leyo hasta ahi es lo que hay.
            } catch (RuntimeException e) {
                // Idem para un largo negativo o un componente que no entra.
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // Ya no hay nada que leer.
                    }
                }
            }
        }
        return keys.toArray(new KerberosKey[keys.size()]);
    }

    /** Recorre las entradas y guarda las del principal pedido. Ver la nota de la clase. */
    private static void readEntries(DataInputStream in, KerberosPrincipal principal, String wanted,
                                    List<KerberosKey> keys) throws IOException {
        if (in.readUnsignedShort() != KEYTAB_VERSION) {
            return;
        }
        while (true) {
            int size;
            try {
                size = in.readInt();
            } catch (IOException e) {
                return;
            }
            if (size == 0) {
                return;
            }
            if (size < 0) {
                // Una entrada borrada: el largo es negativo y hay que saltar su valor absoluto.
                skipFully(in, -size);
                continue;
            }
            byte[] entry = new byte[size];
            in.readFully(entry);
            KerberosKey key = parseEntry(new DataInputStream(
                new java.io.ByteArrayInputStream(entry)), principal);
            if (key != null && key.getPrincipal().getName().equals(wanted)) {
                keys.add(key);
            }
        }
    }

    /** Una entrada, o null si el principal no se pudo armar. */
    private static KerberosKey parseEntry(DataInputStream in, KerberosPrincipal wanted)
            throws IOException {
        int components = in.readUnsignedShort();
        String realm = readString(in);
        StringBuilder name = new StringBuilder();
        int i = 0;
        while (i < components) {
            if (i > 0) {
                name.append('/');
            }
            name.append(readString(in));
            i = i + 1;
        }
        int nameType = in.readInt();
        in.readInt();
        int versionNumber = in.readUnsignedByte();
        int keyType = in.readUnsignedShort();
        int keyLength = in.readUnsignedShort();
        byte[] keyBytes = new byte[keyLength];
        in.readFully(keyBytes);
        if (in.available() >= 4) {
            // La version de 32 bits, opcional al final, pisa la de un byte cuando esta.
            int longVersion = in.readInt();
            if (longVersion != 0) {
                versionNumber = longVersion;
            }
        }
        KerberosPrincipal principal;
        try {
            principal = new KerberosPrincipal(name + "@" + realm,
                nameType >= 0 && nameType <= 5 || nameType == 10 ? nameType
                    : KerberosPrincipal.KRB_NT_PRINCIPAL);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return new KerberosKey(principal, keyBytes, keyType, versionNumber);
    }

    /** Una cadena con su largo adelante. */
    private static String readString(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    /** Salta esos bytes, todos. */
    private static void skipFully(DataInputStream in, int count) throws IOException {
        int remaining = count;
        while (remaining > 0) {
            int skipped = in.skipBytes(remaining);
            if (skipped <= 0) {
                throw new IOException("keytab truncado");
            }
            remaining = remaining - skipped;
        }
    }

    /** Si el archivo existe. */
    public boolean exists() {
        return resolveFile().isFile();
    }

    /** El archivo que corresponde: el dado, o el de por omision. Ver la nota de la clase. */
    private File resolveFile() {
        if (this.file != null) {
            return this.file;
        }
        String configured = System.getProperty("java.security.krb5.keytab");
        if (configured != null && !configured.isEmpty()) {
            return new File(configured);
        }
        return new File(System.getProperty("user.home", "."), "krb5.keytab");
    }

    /** El archivo y a quien esta ligado. */
    @Override
    public String toString() {
        String source = this.file == null ? "Default keytab" : this.file.toString();
        if (!this.bound) {
            return source;
        }
        return source + " for " + (this.princ == null ? "someone" : this.princ.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.file, this.princ, this.bound);
    }

    /** Iguales si son el mismo archivo --como se nombro, no como se resuelve-- y la misma ligadura. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof KeyTab)) {
            return false;
        }
        KeyTab other = (KeyTab) obj;
        return Objects.equals(this.file, other.file) && Objects.equals(this.princ, other.princ)
            && this.bound == other.bound;
    }

    /** A quien esta ligado; null si esta ligado "a alguien" o no esta ligado. */
    public KerberosPrincipal getPrincipal() {
        return this.princ;
    }

    /** Si esta ligado. Ver la nota de la clase. */
    public boolean isBound() {
        return this.bound;
    }
}
