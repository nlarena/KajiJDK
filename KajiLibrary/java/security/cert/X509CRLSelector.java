package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;

// Un criterio para elegir CRLs de un `CertStore`.
//
// Igual que `X509CertSelector`: los criterios se acumulan y `match` exige todos. Un selector recien
// creado acepta cualquier CRL X.509.
//
// El criterio que mas se usa —y el que mas se olvida— es la fecha. Sin el, el store puede devolver
// una CRL vencida, y una CRL vencida no dice nada: es una foto de antes de la revocacion que se
// esta buscando. `setDateAndTime` es lo que exige que la CRL cubra el momento que interesa.
//
// Los numeros de CRL sirven para lo mismo desde otro angulo: cada CRL de un emisor lleva un numero
// que crece, asi que pedir un minimo es pedir "una mas nueva que la que ya tengo". Es la defensa
// contra que un atacante que controla la red sirva una CRL vieja pero todavia valida.
//
// ===============================================================================================
// LOS DOS CONJUNTOS DE EMISORES
// ===============================================================================================
//
// El criterio de emisor se guarda **dos veces**, y no es redundancia: es la forma del JDK y hay que
// reproducirla porque los dos accesores devuelven cosas distintas.
//
//   - `issuerX500Principals` es lo que usa `match`: nombres X.500 con forma canonica, que es la
//     unica manera de comparar dos DN sin equivocarse.
//   - `issuerNames` guarda lo que el llamador **puso**, tal cual: un `String` sigue siendo el mismo
//     `String`, y un `X500Principal` o un `byte[]` se guardan como bytes. `getIssuerNames()`
//     devuelve eso, con lo cual una coleccion puede tener `String` y `byte[]` mezclados.
//
// Los dos se ponen y se sacan juntos, siempre: un selector con uno y sin el otro filtraria mal.
// `setIssuerNames(null)` y `setIssuerNames(list vacia)` hacen lo mismo —dejan los dos en null, o
// sea sin criterio— y eso tambien es del JDK.
public class X509CRLSelector implements CRLSelector {

    private static final String OID_CRL_NUMBER = "2.5.29.20";

    private BigInteger minCRL;
    private BigInteger maxCRL;
    private Date dateAndTime;
    private X509Certificate certChecking;
    private HashSet<Object> issuerNames;
    private HashSet<javax.security.auth.x500.X500Principal> issuerX500Principals;

    // Un selector sin criterios: acepta cualquier CRL X.509.
    public X509CRLSelector() {
    }

    // Exige que la CRL sea de alguno de estos emisores. null o vacio quita el criterio.
    public void setIssuers(java.util.Collection<javax.security.auth.x500.X500Principal> issuers) {
        if (issuers == null || issuers.isEmpty()) {
            this.issuerNames = null;
            this.issuerX500Principals = null;
            return;
        }
        this.issuerX500Principals =
            new HashSet<javax.security.auth.x500.X500Principal>(issuers);
        this.issuerNames = new HashSet<Object>();
        java.util.Iterator<javax.security.auth.x500.X500Principal> it =
            this.issuerX500Principals.iterator();
        while (it.hasNext()) {
            this.issuerNames.add(it.next().getEncoded());
        }
    }

    // Idem, con los nombres como `String` en RFC 2253 o como `byte[]` con el DER del `Name`.
    //
    // Los dos conjuntos se arman **completos antes** de asignar ninguno: si un elemento de la mitad
    // esta mal formado, el selector queda como estaba y no a mitad de camino con un criterio que
    // filtra de menos.
    public void setIssuerNames(java.util.Collection<?> names) throws IOException {
        if (names == null || names.isEmpty()) {
            this.issuerNames = null;
            this.issuerX500Principals = null;
            return;
        }
        HashSet<Object> raw = new HashSet<Object>();
        HashSet<javax.security.auth.x500.X500Principal> parsed =
            new HashSet<javax.security.auth.x500.X500Principal>();
        java.util.Iterator<?> it = names.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof String) {
                raw.add(o);
                parsed.add(principalOf((String) o));
            } else if (o instanceof byte[]) {
                byte[] b = (byte[]) o;
                byte[] copyOf = new byte[b.length];
                System.arraycopy(b, 0, copyOf, 0, b.length);
                raw.add(copyOf);
                parsed.add(principalOf(copyOf));
            } else {
                throw new IOException("name not byte array or String");
            }
        }
        this.issuerNames = raw;
        this.issuerX500Principals = parsed;
    }

    // Agrega un emisor al criterio.
    public void addIssuer(javax.security.auth.x500.X500Principal issuer) {
        add(issuer.getEncoded(), issuer);
    }

    // Agrega un emisor escrito en RFC 2253.
    //
    // Desaconsejado en el JDK a favor de `addIssuer`, y con razon: el texto se guarda tal cual y es
    // `getIssuerNames()` quien despues lo devuelve sin canonizar. El criterio en si **si** se
    // canoniza —lo que se compara es el `X500Principal`—, asi que el filtrado es correcto igual.
    public void addIssuerName(String name) throws IOException {
        add(name, principalOf(name));
    }

    // Agrega un emisor a partir del DER de su `Name`.
    public void addIssuerName(byte[] name) throws IOException {
        byte[] copyOf = new byte[name.length];
        System.arraycopy(name, 0, copyOf, 0, name.length);
        add(copyOf, principalOf(copyOf));
    }

    private void add(Object rawBytes, javax.security.auth.x500.X500Principal name) {
        if (this.issuerNames == null) {
            this.issuerNames = new HashSet<Object>();
            this.issuerX500Principals = new HashSet<javax.security.auth.x500.X500Principal>();
        }
        this.issuerNames.add(rawBytes);
        this.issuerX500Principals.add(name);
    }

    // El constructor de `X500Principal` rechaza con `IllegalArgumentException`, pero estos metodos
    // prometen `IOException`. Se traduce en vez de dejar escapar la otra: quien llama a
    // `addIssuerName` espera que un nombre mal escrito sea un error declarado.
    private static javax.security.auth.x500.X500Principal principalOf(String name)
            throws IOException {
        try {
            return new javax.security.auth.x500.X500Principal(name);
        } catch (IllegalArgumentException e) {
            throw new IOException("Incorrect AVA format", e);
        }
    }

    private static javax.security.auth.x500.X500Principal principalOf(byte[] name)
            throws IOException {
        try {
            return new javax.security.auth.x500.X500Principal(name);
        } catch (IllegalArgumentException e) {
            throw new IOException("Incorrect AVA format", e);
        }
    }

    // Los emisores del criterio, o null si no hay. Inmutable: el criterio se cambia por los setters.
    public java.util.Collection<javax.security.auth.x500.X500Principal> getIssuers() {
        if (this.issuerX500Principals == null) {
            return null;
        }
        return java.util.Collections.unmodifiableCollection(this.issuerX500Principals);
    }

    // Los emisores **como se pusieron**, o null si no hay: `String` y `byte[]` mezclados.
    //
    // Es una copia y los `byte[]` van clonados, asi que tocar lo que sale de aca no cambia el
    // criterio. A diferencia de `getIssuers()`, la coleccion en si es modificable —tambien del JDK—.
    public java.util.Collection<Object> getIssuerNames() {
        if (this.issuerNames == null) {
            return null;
        }
        HashSet<Object> copyOf = new HashSet<Object>();
        java.util.Iterator<Object> it = this.issuerNames.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof byte[]) {
                byte[] b = (byte[]) o;
                byte[] c = new byte[b.length];
                System.arraycopy(b, 0, c, 0, b.length);
                copyOf.add(c);
            } else {
                copyOf.add(o);
            }
        }
        return copyOf;
    }

    // El numero de CRL minimo. Es como se pide "una mas nueva que esta".
    public void setMinCRLNumber(BigInteger minCRL) {
        this.minCRL = minCRL;
    }

    public BigInteger getMinCRL() {
        return this.minCRL;
    }

    // El numero de CRL maximo. Sirve para reconstruir el estado en un momento pasado.
    public void setMaxCRLNumber(BigInteger maxCRL) {
        this.maxCRL = maxCRL;
    }

    public BigInteger getMaxCRL() {
        return this.maxCRL;
    }

    // Exige que la CRL cubra este instante: que su `thisUpdate` no sea posterior y su `nextUpdate`
    // no sea anterior. Los dos extremos **entran**.
    public void setDateAndTime(Date dateAndTime) {
        if (dateAndTime == null) {
            this.dateAndTime = null;
        } else {
            this.dateAndTime = new Date(dateAndTime.getTime());
        }
    }

    public Date getDateAndTime() {
        if (this.dateAndTime == null) {
            return null;
        }
        return new Date(this.dateAndTime.getTime());
    }

    // El certificado cuyo estado se esta averiguando.
    //
    // No es un criterio: `match` **no lo mira**, y eso es del JDK. Esta para que un proveedor de
    // `CertStore` sepa a que apuntar la busqueda —por ejemplo, seguir la extension de puntos de
    // distribucion de CRL del certificado— sin tener que adivinarlo.
    public void setCertificateChecking(X509Certificate cert) {
        this.certChecking = cert;
    }

    public X509Certificate getCertificateChecking() {
        return this.certChecking;
    }

    // Si la CRL cumple todos los criterios puestos.
    @Override
    public boolean match(CRL crl) {
        if (!(crl instanceof X509CRL)) {
            return false;
        }
        X509CRL xcrl = (X509CRL) crl;

        // El emisor va primero: es el criterio que decide si esta CRL siquiera habla de los
        // certificados que interesan. Se compara por `X500Principal`, o sea por forma canonica.
        if (this.issuerX500Principals != null) {
            if (!this.issuerX500Principals.contains(xcrl.getIssuerX500Principal())) {
                return false;
            }
        }

        if (this.minCRL != null || this.maxCRL != null) {
            byte[] ext = xcrl.getExtensionValue(OID_CRL_NUMBER);
            // Una CRL sin numero no puede satisfacer un criterio sobre el numero. Decir que no es
            // el lado seguro: aceptarla dejaria pasar justamente la CRL vieja que el criterio
            // queria descartar.
            if (ext == null) {
                return false;
            }
            BigInteger num;
            try {
                byte[] value = DerReader.unwrapOctetString(ext);
                DerReader d = new DerReader(value, 0, value.length);
                int len = d.expect(DerReader.TAG_INTEGER);
                int from = d.skip(len);
                num = d.readInteger(from, len);
            } catch (IOException e) {
                return false;
            }
            if (this.minCRL != null && num.compareTo(this.minCRL) < 0) {
                return false;
            }
            if (this.maxCRL != null && num.compareTo(this.maxCRL) > 0) {
                return false;
            }
        }

        if (this.dateAndTime != null) {
            Date thisUpdate = xcrl.getThisUpdate();
            Date nextUpdate = xcrl.getNextUpdate();
            // Sin `nextUpdate` la CRL no dice hasta cuando vale, asi que no se puede afirmar que
            // cubra el instante pedido.
            if (nextUpdate == null) {
                return false;
            }
            if (this.dateAndTime.after(nextUpdate) || this.dateAndTime.before(thisUpdate)) {
                return false;
            }
        }
        return true;
    }

    // Copia con la que el store se puede quedar. La fecha y los dos conjuntos se copian; el resto es
    // inmutable. Compartir los conjuntos seria el bug clasico: el store se queda con la copia y un
    // `addIssuer` posterior le cambiaria el criterio a mitad de una busqueda.
    @Override
    public Object clone() {
        try {
            X509CRLSelector copyOf = (X509CRLSelector) super.clone();
            copyOf.dateAndTime = this.getDateAndTime();
            if (this.issuerNames != null) {
                copyOf.issuerNames = new HashSet<Object>(this.issuerNames);
                copyOf.issuerX500Principals =
                    new HashSet<javax.security.auth.x500.X500Principal>(this.issuerX500Principals);
            }
            return copyOf;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    // El formato no esta especificado; los nombres de los campos son los del JDK.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X509CRLSelector: [\n");
        if (this.issuerNames != null) {
            sb.append("  IssuerNames:\n");
            java.util.Iterator<Object> it = this.issuerNames.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                sb.append("    " + (o instanceof byte[] ? "(DER)" : o.toString()) + "\n");
            }
        }
        if (this.minCRL != null) {
            sb.append("  minCRLNumber: " + this.minCRL.toString() + "\n");
        }
        if (this.maxCRL != null) {
            sb.append("  maxCRLNumber: " + this.maxCRL.toString() + "\n");
        }
        if (this.dateAndTime != null) {
            sb.append("  dateAndTime: " + this.dateAndTime.toString() + "\n");
        }
        if (this.certChecking != null) {
            sb.append("  Certificate being checked: " + this.certChecking.toString() + "\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
