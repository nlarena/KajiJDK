package java.security;

import java.io.Serializable;
import java.util.ArrayList;

// Una identidad del sistema de gestion de claves **viejo**, obsoleto desde 1.2.
//
// Lo reemplazo `KeyStore` mas `java.security.cert`, y por buenos motivos: esta API mezclaba en un
// solo objeto el nombre, la clave publica, los certificados que la respaldan y el ambito donde
// vive, con una nocion de igualdad rara —dos identidades son iguales si tienen el mismo nombre
// completo **o** el mismo nombre corto y la misma clave— que es facil de leer mal.
//
// Se implementa porque sigue en las firmas del JDK y porque su parte estructural es toda honesta:
// no hay una sola operacion criptografica en esta clase. Lo que si tiene es una **invariante que
// vale la pena**: la clave publica y los certificados no pueden contradecirse. `addCertificate`
// rechaza un certificado cuya clave no sea la de la identidad, y `setPublicKey` tira los
// certificados viejos en vez de dejarlos hablando de una clave que ya no es. Sin eso, una
// identidad podria afirmar una clave y exhibir certificados de otra.
@Deprecated
public abstract class Identity implements Principal, Serializable {

    private String name;

    private PublicKey publicKey;

    // Informacion libre sobre la identidad. Package-private en el JDK.
    String info = "No further information available.";

    // El ambito al que pertenece, o null si es de nivel superior.
    IdentityScope scope;

    private final ArrayList<Certificate> certificados = new ArrayList<Certificate>();

    // Solo para deserializar. El nombre se sobreescribe al leer el flujo.
    protected Identity() {
        this("restoring...");
    }

    // Una identidad dentro de un ambito. Se da de alta en el ambito al construirse: si el ambito
    // ya tiene una identidad con ese nombre o con esa clave, la alta falla y esta identidad no
    // llega a existir a medias.
    public Identity(String name, IdentityScope scope) throws KeyManagementException {
        this(name);
        if (scope != null) {
            scope.addIdentity(this);
        }
        this.scope = scope;
    }

    public Identity(String name) {
        this.name = name;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    public final IdentityScope getScope() {
        return this.scope;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    // Cambia la clave publica y **borra los certificados**. Ver la cabecera: un certificado habla
    // de una clave concreta, y dejarlo despues de cambiarla lo convertiria en una afirmacion
    // falsa.
    public void setPublicKey(PublicKey key) throws KeyManagementException {
        this.publicKey = key;
        this.certificados.clear();
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfo() {
        return this.info;
    }

    // Agrega un certificado. Si la identidad ya tiene clave publica, la del certificado tiene que
    // ser la misma; si no la tiene, la adopta.
    public void addCertificate(Certificate certificate) throws KeyManagementException {
        if (this.publicKey != null) {
            if (!clavesIguales(this.publicKey, certificate.getPublicKey())) {
                throw new KeyManagementException("public key different from cert public key");
            }
        } else {
            this.publicKey = certificate.getPublicKey();
        }
        this.certificados.add(certificate);
    }

    // Compara dos claves por su codificacion y no por `equals`.
    //
    // Es a proposito: dos implementaciones distintas de `PublicKey` que representan la misma clave
    // no son `equals` entre si —cada proveedor tiene su clase— pero codifican los mismos bytes. Si
    // se comparara por identidad de objeto, un certificado emitido por otro proveedor seria
    // rechazado sin motivo.
    private static boolean clavesIguales(PublicKey a, PublicKey b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        byte[] ea = a.getEncoded();
        byte[] eb = b.getEncoded();
        if (ea == null || eb == null) {
            return false;
        }
        return MessageDigest.isEqual(ea, eb);
    }

    public void removeCertificate(Certificate certificate) throws KeyManagementException {
        if (!this.certificados.contains(certificate)) {
            throw new KeyManagementException("certificate not registered");
        }
        this.certificados.remove(certificate);
    }

    // Una copia del arreglo de certificados.
    public Certificate[] certificates() {
        Certificate[] a = new Certificate[this.certificados.size()];
        int i = 0;
        while (i < this.certificados.size()) {
            a[i] = this.certificados.get(i);
            i = i + 1;
        }
        return a;
    }

    // `final` porque el contrato de igualdad de esta clase es raro y no se puede dejar que una
    // subclase lo cambie: primero prueba nombre completo, y si no coincide delega en
    // `identityEquals`, que una subclase **si** puede afinar.
    @Override
    public final boolean equals(Object identity) {
        if (identity == this) {
            return true;
        }
        if (!(identity instanceof Identity)) {
            return false;
        }
        Identity other = (Identity) identity;
        if (this.fullName().equals(other.fullName())) {
            return true;
        }
        return this.identityEquals(other);
    }

    // Igualdad por nombre corto mas clave. Una subclase puede ajustarla; `equals` no.
    protected boolean identityEquals(Identity identity) {
        if (!this.name.equalsIgnoreCase(identity.name)) {
            return false;
        }
        if ((this.publicKey == null) != (identity.publicKey == null)) {
            return false;
        }
        if (this.publicKey != null) {
            return this.publicKey.equals(identity.publicKey);
        }
        return true;
    }

    // El nombre calificado por el ambito. Package-private, como en el JDK.
    String fullName() {
        if (this.scope != null) {
            return this.name + "." + this.scope.getName();
        }
        return this.name;
    }

    @Override
    public String toString() {
        if (this.scope != null) {
            return this.name + "[" + this.scope.getName() + "]";
        }
        return this.name;
    }

    // La forma larga: clave, certificados e informacion libre.
    public String toString(boolean detailed) {
        String out = this.toString();
        if (!detailed) {
            return out;
        }
        out = out + "\n";
        out = out + this.printKeys();
        out = out + "\n" + this.printCertificates();
        if (this.info != null) {
            out = out + "\n\t" + this.info;
        } else {
            out = out + "\n\tno additional information available.";
        }
        return out;
    }

    String printKeys() {
        if (this.publicKey != null) {
            return "\tpublic key initialized";
        }
        return "\tno public key";
    }

    String printCertificates() {
        if (this.certificados.isEmpty()) {
            return "\tno certificates";
        }
        StringBuilder b = new StringBuilder();
        b.append("\tcertificates: \n");
        int i = 1;
        int k = 0;
        while (k < this.certificados.size()) {
            b.append("\tcertificate ");
            b.append(i);
            b.append("\t");
            b.append(this.certificados.get(k).toString());
            b.append("\n");
            i = i + 1;
            k = k + 1;
        }
        return b.toString();
    }

    @Override
    public int hashCode() {
        return this.fullName().hashCode();
    }
}
