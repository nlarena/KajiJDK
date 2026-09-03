package java.security.cert;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;

// Un certificado: una clave publica atada a una identidad por la firma de un tercero.
//
// ===============================================================================================
// POR QUE ESTA CLASE PUEDE SER HONESTA SIN SABER CRIPTOGRAFIA
// ===============================================================================================
//
// Todo lo que decide si un certificado es de fiar —`verify`— es **abstracto**. Esta clase no lo
// implementa ni podria: no sabe de que formato es el certificado ni con que algoritmo esta
// firmado. Lo que si define es la parte estructural: el tipo, la igualdad por codificacion, y el
// contrato de que `verify` lanza si la firma no valida.
//
// Ese contrato merece leerse dos veces porque es al reves del de `Signature.verify`: aca **no hay
// valor de retorno**. Una verificacion que sale bien vuelve sin decir nada, y una que sale mal
// lanza. El que escribe `try { c.verify(k); } catch (Exception e) {}` no esta manejando el error:
// esta aceptando cualquier certificado.
//
// ===============================================================================================
// A KajiLibrary subset
// ===============================================================================================
//
// **No hay ninguna subclase.** `X509Certificate` y las fabricas (`CertificateFactory`) no estan:
// parsear un X.509 es leer ASN.1/DER y verificar su firma es RSA o ECDSA, y nada de eso esta
// implementado en esta biblioteca. Esta clase existe porque es el tipo que nombran `CodeSource`,
// `CodeSigner`, `CertPath` y `UnresolvedPermission`, y porque su parte estructural se puede
// escribir entera sin mentir.
//
// La igualdad se define **por la codificacion**, no por identidad ni por campos: dos objetos
// distintos que codifican los mismos bytes son el mismo certificado. Es lo unico correcto —el
// certificado es sus bytes— y es lo que hace que comparar cadenas de certificados funcione entre
// implementaciones distintas.
public abstract class Certificate implements Serializable {

    private final String type;

    protected Certificate(String type) {
        this.type = type;
    }

    // El tipo: "X.509".
    public final String getType() {
        return this.type;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Certificate)) {
            return false;
        }
        try {
            byte[] a = this.getEncoded();
            byte[] b = ((Certificate) other).getEncoded();
            if (a == null || b == null || a.length != b.length) {
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
        } catch (CertificateEncodingException e) {
            // Un certificado que no se puede codificar no se puede comparar. Decir "distinto" es
            // mas seguro que decir "igual": lo peor que pasa es que se rechace algo valido.
            return false;
        }
    }

    @Override
    public int hashCode() {
        try {
            byte[] a = this.getEncoded();
            int h = 0;
            int i = 0;
            while (i < a.length) {
                h = h * 31 + a[i];
                i = i + 1;
            }
            return h;
        } catch (CertificateEncodingException e) {
            return 0;
        }
    }

    // La forma codificada del certificado.
    public abstract byte[] getEncoded() throws CertificateEncodingException;

    // Verifica la firma del certificado con `key`. **No devuelve nada: si no lanza, valido.**
    public abstract void verify(PublicKey key)
        throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    public abstract void verify(PublicKey key, String sigProvider)
        throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    // La variante que recibe un `Provider` ya resuelto.
    //
    // Tira `UnsupportedOperationException` y **asi es en el JDK**: se agrego en Java 8 con una
    // implementacion base que no hace nada, para no romper a las subclases que ya existian. Una
    // que quiera soportarla la sobreescribe. Copiar el comportamiento es lo correcto — inventar
    // una verificacion aca seria justamente el agujero.
    public void verify(PublicKey key, Provider sigProvider)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
                   SignatureException {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract String toString();

    // La clave publica que este certificado certifica.
    public abstract PublicKey getPublicKey();

    // Serializa el certificado por su tipo y su codificacion, no por sus campos.
    //
    // A KajiLibrary subset: en el JDK devuelve un `CertificateRep`, una clase interna que al
    // deserializar reconstruye el certificado con una `CertificateFactory`. Aca no hay fabricas,
    // asi que no hay forma de volver: se lanza en vez de escribir algo que despues no se pueda
    // leer.
    protected Object writeReplace() throws ObjectStreamException {
        throw new java.io.NotSerializableException(
            "java.security.cert.Certificate: no CertificateFactory available to restore it");
    }
}
