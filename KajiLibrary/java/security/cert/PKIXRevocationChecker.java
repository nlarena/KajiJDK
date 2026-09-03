package java.security.cert;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// La configuracion de como se comprueba la revocacion: OCSP, CRLs, y que hacer cuando no se puede
// averiguar.
//
// ===============================================================================================
// SOFT_FAIL ES LA OPCION QUE HAY QUE ENTENDER
// ===============================================================================================
//
// Por default, si no se puede averiguar el estado de revocacion —el respondedor OCSP no contesta,
// la CRL no se pudo bajar— la validacion **falla**. Con `SOFT_FAIL` no falla: sigue como si el
// certificado no estuviera revocado, y el problema queda anotado en
// `getSoftFailExceptions()`.
//
// Eso convierte la comprobacion de revocacion en algo que un atacante puede desactivar: quien pueda
// bloquear el trafico al respondedor logra que un certificado robado y ya revocado pase. Se usa
// igual porque la alternativa —caerse cada vez que un OCSP tiene un mal dia— es peor para la
// disponibilidad, pero la eleccion tiene que ser consciente. Que las excepciones queden guardadas y
// no se pierdan es lo que permite al menos enterarse.
//
// El orden tambien importa: por default se intenta OCSP primero y CRL despues; `PREFER_CRLS` lo da
// vuelta y `NO_FALLBACK` deja solo el primero. Con `NO_FALLBACK` mas `SOFT_FAIL`, un solo punto
// caido alcanza para que no se compruebe nada.
//
// Esta clase es abstracta y no comprueba nada por si misma: guarda la configuracion y deja el
// unico metodo que hace trabajo —`getSoftFailExceptions()`— sin implementar. Esta biblioteca no
// trae ningun proveedor que la implemente: no hay cliente OCSP ni descarga de CRLs, y ninguna de
// las dos cosas se puede escribir sin `java.net` y sin verificacion de firmas.
public abstract class PKIXRevocationChecker extends PKIXCertPathChecker {

    // Las cuatro perillas. Ver arriba: `SOFT_FAIL` es la unica que cambia si la validacion puede
    // fallar o no.
    public enum Option {

        // Solo comprobar el certificado final, no los intermedios. Mas barato y mas debil.
        ONLY_END_ENTITY,

        // Intentar CRLs antes que OCSP.
        PREFER_CRLS,

        // No intentar el segundo mecanismo si el primero no anduvo.
        NO_FALLBACK,

        // No fallar cuando no se puede averiguar el estado. Ver la nota de la clase.
        SOFT_FAIL
    }

    private URI ocspResponder;
    private X509Certificate ocspResponderCert;
    private List<Extension> ocspExtensions = Collections.<Extension>emptyList();
    private Map<X509Certificate, byte[]> ocspResponses = Collections.emptyMap();
    private Set<Option> options = Collections.emptySet();

    protected PKIXRevocationChecker() {
    }

    // A que respondedor OCSP preguntar. Null significa usar el que diga la extension AIA de cada
    // certificado, que es lo normal: fijarlo aca solo tiene sentido con un respondedor propio.
    public void setOcspResponder(URI uri) {
        this.ocspResponder = uri;
    }

    public URI getOcspResponder() {
        return this.ocspResponder;
    }

    // Con que certificado verificar la firma de las respuestas OCSP. Null deja que se use el
    // mecanismo del RFC, donde el emisor delega en un respondedor. Una respuesta OCSP **firmada**
    // es lo unico que la hace confiable: sin verificar la firma, la respuesta es lo que diga la red.
    public void setOcspResponderCert(X509Certificate cert) {
        this.ocspResponderCert = cert;
    }

    public X509Certificate getOcspResponderCert() {
        return this.ocspResponderCert;
    }

    // Extensiones a mandar en la consulta OCSP. La que importa es el nonce: liga la respuesta a
    // esta consulta y evita que se reproduzca una vieja —de cuando el certificado todavia no estaba
    // revocado—.
    public void setOcspExtensions(List<Extension> extensions) {
        if (extensions == null) {
            this.ocspExtensions = Collections.<Extension>emptyList();
        } else {
            this.ocspExtensions = Collections.unmodifiableList(
                new ArrayList<Extension>(extensions));
        }
    }

    public List<Extension> getOcspExtensions() {
        return this.ocspExtensions;
    }

    // Respuestas OCSP ya obtenidas, para no volver a consultar. Es el mecanismo del "stapling" de
    // TLS: el servidor adjunta una respuesta reciente y el cliente no tiene que hablar con nadie
    // mas. Se siguen verificando: venir de aca no las hace confiables.
    public void setOcspResponses(Map<X509Certificate, byte[]> responses) {
        if (responses == null) {
            this.ocspResponses = Collections.emptyMap();
        } else {
            Map<X509Certificate, byte[]> copyOf = new HashMap<X509Certificate, byte[]>();
            Iterator<Map.Entry<X509Certificate, byte[]>> it = responses.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<X509Certificate, byte[]> e = it.next();
                byte[] v = e.getValue();
                byte[] c = new byte[v.length];
                System.arraycopy(v, 0, c, 0, v.length);
                copyOf.put(e.getKey(), c);
            }
            this.ocspResponses = copyOf;
        }
    }

    // Copia profunda: los arreglos son mutables y quien recibe el mapa no puede alterar el estado
    // del checker.
    public Map<X509Certificate, byte[]> getOcspResponses() {
        Map<X509Certificate, byte[]> copyOf = new HashMap<X509Certificate, byte[]>();
        Iterator<Map.Entry<X509Certificate, byte[]>> it = this.ocspResponses.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<X509Certificate, byte[]> e = it.next();
            byte[] v = e.getValue();
            byte[] c = new byte[v.length];
            System.arraycopy(v, 0, c, 0, v.length);
            copyOf.put(e.getKey(), c);
        }
        return copyOf;
    }

    public void setOptions(Set<Option> options) {
        if (options == null) {
            this.options = Collections.emptySet();
        } else {
            this.options = Collections.unmodifiableSet(EnumSet.copyOf(options));
        }
    }

    public Set<Option> getOptions() {
        return this.options;
    }

    // Los problemas que `SOFT_FAIL` dejo pasar, en orden. Vacio si no hubo ninguno —o si `SOFT_FAIL`
    // no estaba puesto, porque ahi habrian hecho fallar la validacion—. **Revisarlo no es opcional
    // si se activo `SOFT_FAIL`**: es el unico lugar donde queda registro de que no se comprobo nada.
    public abstract List<CertPathValidatorException> getSoftFailExceptions();

    // Copia superficial mas los contenedores. El tipo de retorno es covariante para que quien
    // clona no tenga que castear.
    @Override
    public PKIXRevocationChecker clone() {
        PKIXRevocationChecker copyOf = (PKIXRevocationChecker) super.clone();
        copyOf.ocspExtensions = new ArrayList<Extension>(this.ocspExtensions);
        copyOf.ocspResponses = new HashMap<X509Certificate, byte[]>(this.ocspResponses);
        return copyOf;
    }
}
