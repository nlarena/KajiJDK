package java.security;

import java.io.Serializable;
import java.net.URL;
import java.security.cert.Certificate;

// De donde vino el codigo: una URL y, opcionalmente, quien lo firmo.
//
// Es la mitad "quien sos" de una decision de politica. `Policy` mira un `CodeSource` y contesta
// que permisos le corresponden, y el `implies` de esta clase es lo que decide si una entrada de la
// politica —"todo lo que este debajo de file:/opt/app/-"— aplica a un codigo concreto.
//
// ===============================================================================================
// `implies` ES UNA DECISION DE SEGURIDAD, Y ACA ES DELIBERADAMENTE MAS ESTRICTO QUE EL JDK
// ===============================================================================================
//
// Un `implies` que devuelve `true` de mas concede permisos que la politica no queria conceder. Por
// eso, donde el JDK usa la logica de comodines de host de `SocketPermission` —que resuelve nombres
// y acepta patrones como `*.ejemplo.com`— aca solo se aceptan el host exacto y el comodin total
// `*`. La diferencia es siempre en la direccion segura: lo que aca da `false` y en el JDK daria
// `true` se traduce en un permiso **no** concedido, nunca al reves.
//
// Los certificados se guardan pero **no se validan**: esta clase no verifica ninguna firma, y no
// promete que quien figura como firmante haya firmado nada. Lo unico que hace `matchCerts` es
// comparar conjuntos. La verificacion real la haria quien construya el `CodeSource`, y en esta
// biblioteca no hay nadie que pueda hacerla.
public class CodeSource implements Serializable {

    // null significa "cualquier origen", y por eso implica a todos.
    private final URL location;

    // Los certificados de la cadena de firma, o null si el codigo no viene firmado.
    private final Certificate[] certs;

    private final CodeSigner[] signers;

    public CodeSource(URL url, Certificate[] certs) {
        this.location = url;
        this.certs = certs == null ? null : copiar(certs);
        this.signers = null;
    }

    public CodeSource(URL url, CodeSigner[] signers) {
        this.location = url;
        this.signers = signers == null ? null : copiarFirmantes(signers);
        this.certs = null;
    }

    private static Certificate[] copiar(Certificate[] a) {
        Certificate[] c = new Certificate[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    private static CodeSigner[] copiarFirmantes(CodeSigner[] a) {
        CodeSigner[] c = new CodeSigner[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    @Override
    public int hashCode() {
        return this.location == null ? 0 : this.location.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CodeSource)) {
            return false;
        }
        CodeSource cs = (CodeSource) obj;
        if (this.location == null) {
            if (cs.location != null) {
                return false;
            }
        } else if (!this.location.equals(cs.location)) {
            return false;
        }
        // Igualdad simetrica: cada uno tiene que tener todos los certificados del otro.
        return this.tieneTodos(cs.getCertificates()) && cs.tieneTodos(this.getCertificates());
    }

    public final URL getLocation() {
        return this.location;
    }

    // Los certificados de la firma, o null si no hay.
    //
    // Cuando el `CodeSource` se construyo con firmantes, se derivan de ellos: cada firmante aporta
    // los certificados de su cadena, en orden.
    public final Certificate[] getCertificates() {
        if (this.certs != null) {
            return copiar(this.certs);
        }
        if (this.signers == null) {
            return null;
        }
        java.util.ArrayList<Certificate> lista = new java.util.ArrayList<Certificate>();
        int i = 0;
        while (i < this.signers.length) {
            java.util.List<? extends Certificate> cs =
                this.signers[i].getSignerCertPath().getCertificates();
            int j = 0;
            while (j < cs.size()) {
                lista.add(cs.get(j));
                j = j + 1;
            }
            i = i + 1;
        }
        Certificate[] a = new Certificate[lista.size()];
        int k = 0;
        while (k < lista.size()) {
            a[k] = lista.get(k);
            k = k + 1;
        }
        return a;
    }

    // Los firmantes, o null si el `CodeSource` se construyo con certificados sueltos.
    //
    // A KajiLibrary subset: el JDK sabe **deducir** los firmantes a partir de un arreglo de
    // certificados, partiendo la lista en cadenas por emisor. Eso requiere leer el emisor y el
    // sujeto de cada X.509, y aca no hay parser de X.509. Devolver null es decir "no se", que es
    // la verdad; inventar un agrupamiento seria afirmar que ciertos certificados forman una cadena
    // sin haberlo comprobado.
    public final CodeSigner[] getCodeSigners() {
        if (this.signers == null) {
            return null;
        }
        return copiarFirmantes(this.signers);
    }

    // Si este `CodeSource` cubre al otro: mismos o menos requisitos de firma, y una ubicacion que
    // abarca la del otro.
    public boolean implies(CodeSource codesource) {
        if (codesource == null) {
            return false;
        }
        return this.matchCerts(codesource) && this.matchLocation(codesource);
    }

    // El otro tiene que traer **todos** los certificados que este exige. Traer de mas no molesta:
    // un codigo firmado por A y B satisface una politica que pide solo A.
    private boolean matchCerts(CodeSource that) {
        Certificate[] mios = this.getCertificates();
        if (mios == null || mios.length == 0) {
            return true;
        }
        return that.tieneTodos(mios);
    }

    private boolean tieneTodos(Certificate[] buscados) {
        if (buscados == null || buscados.length == 0) {
            return true;
        }
        Certificate[] mios = this.getCertificates();
        if (mios == null) {
            return false;
        }
        int i = 0;
        while (i < buscados.length) {
            boolean hallado = false;
            int j = 0;
            while (j < mios.length) {
                if (buscados[i].equals(mios[j])) {
                    hallado = true;
                    j = mios.length;
                } else {
                    j = j + 1;
                }
            }
            if (!hallado) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // La comparacion de ubicaciones. El grueso de la decision esta en el sufijo del path:
    //
    //     ".../-"   todo lo que cuelgue, a cualquier profundidad
    //     ".../*"   los archivos de ese directorio, sin bajar mas
    //     ".../"    lo que empiece con ese prefijo
    //     otro      igualdad exacta
    private boolean matchLocation(CodeSource that) {
        if (this.location == null) {
            return true;
        }
        URL otra = that.location;
        if (otra == null) {
            return false;
        }
        if (this.location.equals(otra)) {
            return true;
        }
        String p1 = this.location.getProtocol();
        String p2 = otra.getProtocol();
        if (p1 == null || p2 == null || !p1.equalsIgnoreCase(p2)) {
            return false;
        }
        if (!this.matchHost(this.location.getHost(), otra.getHost())) {
            return false;
        }
        int puerto = this.location.getPort();
        if (puerto != -1 && puerto != otra.getPort()) {
            return false;
        }
        String ref = this.location.getRef();
        if (ref != null && !ref.equals(otra.getRef())) {
            return false;
        }
        return this.matchFile(this.location.getFile(), otra.getFile());
    }

    // Solo host exacto o `*`. Ver la cabecera: el JDK acepta ademas patrones con comodin parcial
    // y equivalencias por DNS, y no soportarlos solo puede negar de mas.
    private boolean matchHost(String mio, String otro) {
        if (mio == null || mio.isEmpty()) {
            return true;
        }
        if (mio.equals("*")) {
            return true;
        }
        if (otro == null) {
            return false;
        }
        if (mio.equalsIgnoreCase(otro)) {
            return true;
        }
        // "" y "localhost" son la misma maquina en una URL `file:` o `http:` sin host.
        boolean mioLocal = mio.equalsIgnoreCase("localhost");
        boolean otroLocal = otro.isEmpty() || otro.equalsIgnoreCase("localhost");
        return mioLocal && otroLocal;
    }

    private boolean matchFile(String mio, String otro) {
        if (mio == null) {
            return otro == null;
        }
        if (otro == null) {
            return false;
        }
        if (mio.endsWith("/-")) {
            return otro.startsWith(mio.substring(0, mio.length() - 1));
        }
        if (mio.endsWith("/*")) {
            String prefijo = mio.substring(0, mio.length() - 1);
            if (!otro.startsWith(prefijo)) {
                return false;
            }
            // Sin bajar de directorio: lo que sigue al prefijo no puede tener otra barra.
            return otro.indexOf('/', prefijo.length()) < 0;
        }
        if (mio.endsWith("/")) {
            return otro.startsWith(mio);
        }
        // Un directorio escrito sin barra final tambien cubre a si mismo con ella.
        return mio.equals(otro) || (mio + "/").equals(otro);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("(");
        b.append(this.location);
        Certificate[] cs = this.getCertificates();
        if (cs == null || cs.length == 0) {
            b.append(" <no signer certificates>");
        } else {
            int i = 0;
            while (i < cs.length) {
                b.append("\n");
                b.append(cs[i].toString());
                i = i + 1;
            }
        }
        b.append(")");
        return b.toString();
    }
}
