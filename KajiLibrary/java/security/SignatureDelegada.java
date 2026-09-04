package java.security;

import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;

// El puente entre `Signature` y el `SignatureSpi` que trajo un proveedor.
//
// Hace falta por una rareza historica del API: `Signature` **extiende** `SignatureSpi` en vez de
// contenerlo. Eso permitia que un proveedor antiguo escribiera una subclase de `Signature` directa,
// y sigue permitiendolo, pero deja a `getInstance` sin forma de devolver el SPI del proveedor: lo
// que tiene que devolver es una `Signature`, no un `SignatureSpi`.
//
// La solucion —la misma del JDK— es esta clase: una `Signature` concreta que reenvia cada metodo
// `engine*` al SPI real. Los metodos publicos de `Signature` son `final` y ya hacen la validacion
// de estado, asi que aca solo queda el reenvio.
//
// Es package-private a proposito: no forma parte del API y nadie fuera del paquete la nombra.
final class SignatureDelegada extends Signature {

    private final SignatureSpi spi;

    SignatureDelegada(SignatureSpi spi, String algorithm) {
        super(algorithm);
        this.spi = spi;
    }

    @Override
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        this.spi.engineInitVerify(publicKey);
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        this.spi.engineInitSign(privateKey);
    }

    @Override
    protected void engineUpdate(byte b) throws SignatureException {
        this.spi.engineUpdate(b);
    }

    @Override
    protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
        this.spi.engineUpdate(b, off, len);
    }

    @Override
    protected void engineUpdate(ByteBuffer input) {
        this.spi.engineUpdate(input);
    }

    @Override
    protected byte[] engineSign() throws SignatureException {
        return this.spi.engineSign();
    }

    @Override
    protected int engineSign(byte[] outbuf, int offset, int len) throws SignatureException {
        return this.spi.engineSign(outbuf, offset, len);
    }

    @Override
    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        return this.spi.engineVerify(sigBytes);
    }

    @Override
    protected boolean engineVerify(byte[] sigBytes, int offset, int length)
            throws SignatureException {
        return this.spi.engineVerify(sigBytes, offset, length);
    }

    @Override
    protected void engineSetParameter(String param, Object value)
            throws InvalidParameterException {
        this.spi.engineSetParameter(param, value);
    }

    @Override
    protected void engineSetParameter(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        this.spi.engineSetParameter(params);
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        return this.spi.engineGetParameters();
    }

    @Override
    protected Object engineGetParameter(String param) throws InvalidParameterException {
        return this.spi.engineGetParameter(param);
    }

    // Solo se puede clonar si el SPI de abajo se deja: copiar el delegado sin copiar el SPI daria
    // dos objetos compartiendo el estado a medio calcular de una firma.
    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this.spi instanceof Cloneable) {
            SignatureSpi copia = (SignatureSpi) this.spi.clone();
            SignatureDelegada d = new SignatureDelegada(copia, this.getAlgorithm());
            d.provider = this.provider;
            d.state = this.state;
            return d;
        }
        throw new CloneNotSupportedException();
    }
}
