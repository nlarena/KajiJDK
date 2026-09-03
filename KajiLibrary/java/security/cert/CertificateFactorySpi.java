package java.security.cert;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

// Lo que un proveedor tiene que escribir para saber leer certificados y CRLs de un stream.
//
// Los cuatro metodos de `CertPath` no son abstractos y los otros cuatro si. La asimetria es
// historica: los caminos de certificacion llegaron en Java 1.4, despues de que ya hubiera fabricas
// escritas, y hacerlos abstractos habria roto todas. Los que no se sobreescriben tiran
// `UnsupportedOperationException`, que es lo honesto: una fabrica que no sabe de caminos lo dice en
// vez de devolver algo vacio.
public abstract class CertificateFactorySpi {

    public CertificateFactorySpi() {
    }

    // Lee un certificado del stream.
    public abstract Certificate engineGenerateCertificate(InputStream inStream)
        throws CertificateException;

    public CertPath engineGenerateCertPath(InputStream inStream) throws CertificateException {
        throw new UnsupportedOperationException();
    }

    public CertPath engineGenerateCertPath(InputStream inStream, String encoding)
            throws CertificateException {
        throw new UnsupportedOperationException();
    }

    public CertPath engineGenerateCertPath(List<? extends Certificate> certificates)
            throws CertificateException {
        throw new UnsupportedOperationException();
    }

    // Las codificaciones de camino soportadas, con la preferida primero.
    public Iterator<String> engineGetCertPathEncodings() {
        throw new UnsupportedOperationException();
    }

    // Lee todos los certificados del stream. Puede devolver una coleccion vacia.
    public abstract Collection<? extends Certificate> engineGenerateCertificates(
        InputStream inStream) throws CertificateException;

    public abstract CRL engineGenerateCRL(InputStream inStream) throws CRLException;

    public abstract Collection<? extends CRL> engineGenerateCRLs(InputStream inStream)
        throws CRLException;
}
