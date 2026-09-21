package java.security;

import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;

// The bridge between `Signature` and the `SignatureSpi` a provider brought.
//
// It is needed because of a historical oddity of the API: `Signature` **extends** `SignatureSpi`
// instead of containing it. That allowed an old provider to write a direct subclass of `Signature`,
// and it still allows it, but it leaves `getInstance` with no way of returning the provider's SPI:
// what it has to return is a `Signature`, not a `SignatureSpi`.
//
// The solution —the same as the JDK's— is this class: a concrete `Signature` that forwards each
// `engine*` method to the real SPI. The public methods of `Signature` are `final` and do the state
// checking already, so here only the forwarding is left.
//
// It is package-private on purpose: it is not part of the API and nobody outside the package names
// it.
final class SignatureDelegate extends Signature {

    private final SignatureSpi spi;

    SignatureDelegate(SignatureSpi spi, String algorithm) {
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

    // It can only be cloned if the SPI underneath allows it: copying the delegate without copying
    // the SPI would give two objects sharing the half-computed state of a signature.
    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this.spi instanceof Cloneable) {
            SignatureSpi copy = (SignatureSpi) this.spi.clone();
            SignatureDelegate d = new SignatureDelegate(copy, this.getAlgorithm());
            d.provider = this.provider;
            d.state = this.state;
            return d;
        }
        throw new CloneNotSupportedException();
    }
}
