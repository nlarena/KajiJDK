package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// Los parametros de una validacion PKIX: contra que anclas, con que fecha, con que reglas.
//
// Es la clase donde se configura toda la politica de la validacion, y por lo tanto donde se cometen
// los errores que importan. Los defaults estan elegidos por el lado seguro y desactivarlos es facil
// y silencioso:
//
//   - `revocationEnabled` arranca en **true**. Apagarlo hace que un certificado revocado valide
//     igual. Es la linea que mas aparece en codigo que "arreglo" un problema de conectividad.
//   - `policyQualifiersRejected` arranca en **true**: un calificador de politica critico que el
//     validador no procesa hace fallar. Ponerlo en false obliga a que el llamador los procese el
//     mismo, y casi nadie lo hace.
//   - `date` en null significa **ahora**. Fijarla sirve para verificar una firma vieja, y ahi es
//     legitimo; fijarla en el pasado para que un certificado vencido pase es como no validar.
//
// La otra cosa que importa es que el objeto es **mutable** y el validador se lo queda: por eso
// implementa `clone()`, por eso `getTrustAnchors()` devuelve un conjunto inmutable, y por eso las
// listas se copian al entrar y al salir. Sin eso, cambiar los parametros a mitad de una validacion
// cambiaria las reglas mientras corre.
//
// El constructor que toma un `KeyStore` es una comodidad con un filtro que conviene tener presente:
// solo mira las entradas de **certificado de confianza**, no las de clave privada. Un almacen que
// tiene la clave del servidor y nada mas no aporta ninguna ancla, y el resultado no es un objeto
// vacio sino una `InvalidAlgorithmParameterException` —lo que hay que hacer con un conjunto de
// anclas vacio es fallar, no validar contra nada—.
public class PKIXParameters implements CertPathParameters {

    private Set<TrustAnchor> unmodTrustAnchors;
    private Date date;
    private List<PKIXCertPathChecker> certPathCheckers;
    private String sigProvider;
    private boolean revocationEnabled = true;
    private Set<String> unmodInitialPolicies;
    private boolean explicitPolicyRequired = false;
    private boolean policyMappingInhibited = false;
    private boolean anyPolicyInhibited = false;
    private boolean policyQualifiersRejected = true;
    private List<CertStore> certStores;
    private CertSelector certSelector;

    // El conjunto de anclas no puede estar vacio: sin ancla no hay donde terminar la cadena, y una
    // validacion que no puede terminar no es una validacion.
    public PKIXParameters(Set<TrustAnchor> trustAnchors)
            throws InvalidAlgorithmParameterException {
        setTrustAnchors(trustAnchors);
        this.unmodInitialPolicies = Collections.emptySet();
        this.certPathCheckers = new ArrayList<PKIXCertPathChecker>();
        this.certStores = new ArrayList<CertStore>();
    }

    // Las anclas salen de las entradas de certificado de confianza del almacen.
    //
    // Las entradas de clave privada se saltean a proposito: el certificado que acompaña a una clave
    // propia es la identidad de uno, no una CA en la que confiar. Meterlo como ancla es como
    // firmarse los propios certificados y creerse.
    public PKIXParameters(java.security.KeyStore keystore)
            throws java.security.KeyStoreException, InvalidAlgorithmParameterException {
        if (keystore == null) {
            throw new NullPointerException("the keystore parameter must be non-null");
        }
        Set<TrustAnchor> anclas = new HashSet<TrustAnchor>();
        java.util.Enumeration<String> alias = keystore.aliases();
        while (alias.hasMoreElements()) {
            String a = alias.nextElement();
            if (keystore.isCertificateEntry(a)) {
                java.security.cert.Certificate c = keystore.getCertificate(a);
                if (c instanceof X509Certificate) {
                    anclas.add(new TrustAnchor((X509Certificate) c, null));
                }
            }
        }
        // Sin `setTrustAnchors` un almacen sin certificados de confianza daria un objeto que parece
        // valido y no valida nada. Es el que tira si el conjunto quedo vacio.
        setTrustAnchors(anclas);
        this.unmodInitialPolicies = Collections.emptySet();
        this.certPathCheckers = new ArrayList<PKIXCertPathChecker>();
        this.certStores = new ArrayList<CertStore>();
    }

    // Copia inmutable de las anclas. La copia es defensiva en los dos sentidos: quien las paso no
    // puede sacar una despues, y quien las recibe no puede agregar una.
    public Set<TrustAnchor> getTrustAnchors() {
        return this.unmodTrustAnchors;
    }

    public void setTrustAnchors(Set<TrustAnchor> trustAnchors)
            throws InvalidAlgorithmParameterException {
        if (trustAnchors == null) {
            throw new NullPointerException("the trustAnchors parameters must"
                + " be non-null");
        }
        if (trustAnchors.isEmpty()) {
            throw new InvalidAlgorithmParameterException("the trustAnchors "
                + "parameter must be non-empty");
        }
        // El chequeo de tipo es explicito porque el `Set` puede venir crudo: sin esto, un elemento
        // que no es `TrustAnchor` no explotaria hasta el medio de la validacion.
        Iterator<TrustAnchor> it = trustAnchors.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (!(o instanceof TrustAnchor)) {
                throw new ClassCastException("all elements of set must be "
                    + "of type java.security.cert.TrustAnchor");
            }
        }
        this.unmodTrustAnchors = Collections.unmodifiableSet(
            new HashSet<TrustAnchor>(trustAnchors));
    }

    // Los OIDs de las politicas que el llamador acepta. Vacio significa **cualquiera**, no ninguna.
    public Set<String> getInitialPolicies() {
        return this.unmodInitialPolicies;
    }

    public void setInitialPolicies(Set<String> initialPolicies) {
        if (initialPolicies != null) {
            Iterator<String> it = initialPolicies.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                if (!(o instanceof String)) {
                    throw new ClassCastException("all elements of set must be "
                        + "of type java.lang.String");
                }
            }
            this.unmodInitialPolicies = Collections.unmodifiableSet(
                new HashSet<String>(initialPolicies));
        } else {
            this.unmodInitialPolicies = Collections.emptySet();
        }
    }

    // De donde sacar certificados y CRLs que no vinieron en el camino. Null limpia la lista.
    public void setCertStores(List<CertStore> stores) {
        if (stores == null) {
            this.certStores = new ArrayList<CertStore>();
        } else {
            List<CertStore> copyOf = new ArrayList<CertStore>(stores);
            Iterator<CertStore> it = copyOf.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                if (!(o instanceof CertStore)) {
                    throw new ClassCastException("all elements of list must be "
                        + "of type java.security.cert.CertStore");
                }
            }
            this.certStores = copyOf;
        }
    }

    public void addCertStore(CertStore store) {
        if (store != null) {
            this.certStores.add(store);
        }
    }

    public List<CertStore> getCertStores() {
        return Collections.unmodifiableList(new ArrayList<CertStore>(this.certStores));
    }

    // Si se comprueba revocacion. **Arranca en true y apagarlo es una decision de seguridad**: un
    // certificado revocado valida igual a partir de ahi.
    public void setRevocationEnabled(boolean val) {
        this.revocationEnabled = val;
    }

    public boolean isRevocationEnabled() {
        return this.revocationEnabled;
    }

    // Si se exige que la cadena entera sostenga alguna politica explicita.
    public void setExplicitPolicyRequired(boolean val) {
        this.explicitPolicyRequired = val;
    }

    public boolean isExplicitPolicyRequired() {
        return this.explicitPolicyRequired;
    }

    // Si se prohibe el mapeo de politicas entre dominios distintos.
    public void setPolicyMappingInhibited(boolean val) {
        this.policyMappingInhibited = val;
    }

    public boolean isPolicyMappingInhibited() {
        return this.policyMappingInhibited;
    }

    // Si se prohibe que anyPolicy (2.5.29.32.0) satisfaga la exigencia de politica. Con anyPolicy
    // permitido, "cualquier politica sirve" y el mecanismo entero deja de restringir nada.
    public void setAnyPolicyInhibited(boolean val) {
        this.anyPolicyInhibited = val;
    }

    public boolean isAnyPolicyInhibited() {
        return this.anyPolicyInhibited;
    }

    // Si se rechaza un certificado con calificadores de politica que el validador no procesa.
    // Arranca en true, que es el lado seguro.
    public void setPolicyQualifiersRejected(boolean qualifiersRejected) {
        this.policyQualifiersRejected = qualifiersRejected;
    }

    public boolean getPolicyQualifiersRejected() {
        return this.policyQualifiersRejected;
    }

    // La fecha contra la que se valida, o null para "ahora". Se copia en las dos direcciones porque
    // `Date` es mutable: sin copiar, quien la paso podria correrla despues y mover el momento
    // contra el que se comprueba la vigencia de la cadena.
    //
    // La copia se hace con `new Date(getTime())` y no con `clone()` porque el `java.util.Date` de
    // esta biblioteca todavia no implementa `Cloneable`. El efecto es el mismo.
    public Date getDate() {
        if (this.date == null) {
            return null;
        }
        return new Date(this.date.getTime());
    }

    public void setDate(Date date) {
        if (date != null) {
            this.date = new Date(date.getTime());
        } else {
            this.date = null;
        }
    }

    // Los checkers extra. Se copia la lista y **tambien cada checker**: tienen estado mutable, y
    // compartirlos haria que dos validaciones concurrentes se pisen.
    public void setCertPathCheckers(List<PKIXCertPathChecker> checkers) {
        if (checkers != null) {
            List<PKIXCertPathChecker> copyOf = new ArrayList<PKIXCertPathChecker>();
            Iterator<PKIXCertPathChecker> it = checkers.iterator();
            while (it.hasNext()) {
                copyOf.add((PKIXCertPathChecker) it.next().clone());
            }
            this.certPathCheckers = copyOf;
        } else {
            this.certPathCheckers = new ArrayList<PKIXCertPathChecker>();
        }
    }

    public List<PKIXCertPathChecker> getCertPathCheckers() {
        List<PKIXCertPathChecker> copyOf = new ArrayList<PKIXCertPathChecker>();
        Iterator<PKIXCertPathChecker> it = this.certPathCheckers.iterator();
        while (it.hasNext()) {
            copyOf.add((PKIXCertPathChecker) it.next().clone());
        }
        return Collections.unmodifiableList(copyOf);
    }

    public void addCertPathChecker(PKIXCertPathChecker checker) {
        if (checker != null) {
            this.certPathCheckers.add((PKIXCertPathChecker) checker.clone());
        }
    }

    // El proveedor con el que verificar las firmas, o null para el que se encuentre.
    public String getSigProvider() {
        return this.sigProvider;
    }

    public void setSigProvider(String sigProvider) {
        this.sigProvider = sigProvider;
    }

    // El criterio que tiene que cumplir el certificado del final del camino, o null si ninguno.
    public CertSelector getTargetCertConstraints() {
        if (this.certSelector != null) {
            return (CertSelector) this.certSelector.clone();
        }
        return null;
    }

    public void setTargetCertConstraints(CertSelector selector) {
        if (selector != null) {
            this.certSelector = (CertSelector) selector.clone();
        } else {
            this.certSelector = null;
        }
    }

    // Copia con la que el validador se puede quedar sin que el llamador pueda cambiarla despues.
    // Las listas se copian; las anclas ya son inmutables.
    @Override
    public Object clone() {
        try {
            PKIXParameters copyOf = (PKIXParameters) super.clone();
            if (this.certStores != null) {
                copyOf.certStores = new ArrayList<CertStore>(this.certStores);
            }
            if (this.certPathCheckers != null) {
                copyOf.certPathCheckers = new ArrayList<PKIXCertPathChecker>();
                Iterator<PKIXCertPathChecker> it = this.certPathCheckers.iterator();
                while (it.hasNext()) {
                    copyOf.certPathCheckers.add((PKIXCertPathChecker) it.next().clone());
                }
            }
            return copyOf;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        if (this.unmodTrustAnchors != null) {
            sb.append("  Trust Anchors: " + this.unmodTrustAnchors.toString() + "\n");
        }
        if (this.unmodInitialPolicies != null) {
            if (this.unmodInitialPolicies.isEmpty()) {
                sb.append("  Initial Policy OIDs: any\n");
            } else {
                sb.append("  Initial Policy OIDs: ["
                    + this.unmodInitialPolicies.toString() + "]\n");
            }
        }
        sb.append("  Validity Date: " + String.valueOf(this.date) + "\n");
        sb.append("  Signature Provider: " + String.valueOf(this.sigProvider) + "\n");
        sb.append("  Default Revocation Enabled: " + this.revocationEnabled + "\n");
        sb.append("  Explicit Policy Required: " + this.explicitPolicyRequired + "\n");
        sb.append("  Policy Mapping Inhibited: " + this.policyMappingInhibited + "\n");
        sb.append("  Any Policy Inhibited: " + this.anyPolicyInhibited + "\n");
        sb.append("  Policy Qualifiers Rejected: " + this.policyQualifiersRejected + "\n");
        sb.append("  Target Cert Constraints: " + String.valueOf(this.certSelector) + "\n");
        if (this.certPathCheckers != null) {
            sb.append("  Certification Path Checkers: ["
                + this.certPathCheckers.toString() + "]\n");
        }
        if (this.certStores != null) {
            sb.append("  CertStores: [" + this.certStores.toString() + "]\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
