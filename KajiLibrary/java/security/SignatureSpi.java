package java.security;

import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;

// Lo que un proveedor tiene que escribir para ofrecer un algoritmo de firma.
//
// El ciclo es siempre el mismo: inicializar con una clave, alimentar datos con `engineUpdate`, y
// cerrar con `engineSign` o `engineVerify`. Que sea un objeto con estado y no una funcion suelta es
// lo que permite firmar algo que no entra en memoria.
//
// ===============================================================================================
// EL NONCE, QUE ES LO QUE MAS IMPORTA DE ESTA CLASE
// ===============================================================================================
//
// Hay que decirlo porque no es menor: la firma de DSA y
// la de ECDSA necesitan un nonce impredecible por firma, y sin fuente de aleatoriedad no hay forma
// de producirlo. Un nonce repetido en ECDSA **revela la clave privada** con dos firmas y un poco de
// aritmetica; es como se rompio la consola de Sony en 2010. Un proveedor de firma para esta
// biblioteca tendria que usar un esquema deterministico —RFC 6979, o Ed25519, que lo es por
// diseño— o traerse su propia fuente.
public abstract class SignatureSpi {

    /**
     * La fuente de azar que puso la aplicacion, o null si no puso ninguna.
     *
     * <p>Protegido y mutable porque asi lo declara el JDK: el SPI lo lee al firmar. Un proveedor
     * que lo encuentre en null y necesite un nonce **no debe inventarse uno**; tiene que pedirle
     * uno a {@code new SecureRandom()}, que es el del sistema operativo.
     */
    protected SecureRandom appRandom = null;

    public SignatureSpi() {
    }

    // Prepara para verificar con esta clave publica.
    protected abstract void engineInitVerify(PublicKey publicKey) throws InvalidKeyException;

    // Prepara para firmar con esta clave privada.
    protected abstract void engineInitSign(PrivateKey privateKey) throws InvalidKeyException;

    /**
     * Idem, diciendo de donde sale el azar.
     *
     * <p>El default guarda la fuente en {@link #appRandom} y reenvia al de arriba, que es lo que
     * hace el JDK. Un proveedor que necesite el nonce -- DSA, ECDSA -- tiene que sobrescribirlo o
     * leer el campo; el que firma de forma deterministica -- Ed25519, RFC 6979 -- puede ignorarlo.
     */
    protected void engineInitSign(PrivateKey privateKey, SecureRandom random)
            throws InvalidKeyException {
        this.appRandom = random;
        this.engineInitSign(privateKey);
    }

    protected abstract void engineUpdate(byte b) throws SignatureException;

    protected abstract void engineUpdate(byte[] b, int off, int len) throws SignatureException;

    // Alimenta desde un `ByteBuffer`. La implementacion base lo resuelve delegando en la version de
    // arreglos: si el buffer tiene arreglo de respaldo lo usa directo, y si no lo copia de a
    // pedazos para no reservar memoria proporcional al mensaje.
    //
    // Consume el buffer entero: al volver, `position` quedo en `limit`. Es del contrato y sorprende
    // a quien espera poder releerlo.
    protected void engineUpdate(ByteBuffer input) {
        if (!input.hasRemaining()) {
            return;
        }
        try {
            if (input.hasArray()) {
                byte[] b = input.array();
                int ofs = input.arrayOffset();
                int pos = input.position();
                int lim = input.limit();
                this.engineUpdate(b, ofs + pos, lim - pos);
                input.position(lim);
            } else {
                int len = input.remaining();
                byte[] b = new byte[len < 4096 ? len : 4096];
                while (len > 0) {
                    int trozo = len < b.length ? len : b.length;
                    input.get(b, 0, trozo);
                    this.engineUpdate(b, 0, trozo);
                    len = len - trozo;
                }
            }
        } catch (SignatureException e) {
            // `engineUpdate(ByteBuffer)` no declara `SignatureException`, asi que la unica salida es
            // envolverla. Es lo que hace el JDK: un fallo aca es del proveedor, no del llamador.
            throw new ProviderException("update() failed", e);
        }
    }

    protected abstract byte[] engineSign() throws SignatureException;

    // Firma dejando el resultado en un buffer dado. La implementacion base firma normal y copia.
    //
    // **No trunca**: si el buffer no alcanza para la firma entera, lanza. Devolver una firma parcial
    // seria devolver algo que parece una firma y no verifica nunca.
    protected int engineSign(byte[] outbuf, int offset, int len) throws SignatureException {
        byte[] sig = this.engineSign();
        if (len < sig.length) {
            throw new SignatureException("partial signatures not returned");
        }
        if (outbuf.length - offset < sig.length) {
            throw new SignatureException(
                "insufficient space in the output buffer to store the signature");
        }
        System.arraycopy(sig, 0, outbuf, offset, sig.length);
        return sig.length;
    }

    // Verifica la firma. **Aca si hay valor de retorno**, al reves que en `Certificate.verify`:
    // false significa "no valida" y no es un error.
    protected abstract boolean engineVerify(byte[] sigBytes) throws SignatureException;

    protected boolean engineVerify(byte[] sigBytes, int offset, int length)
            throws SignatureException {
        byte[] copia = new byte[length];
        System.arraycopy(sigBytes, offset, copia, 0, length);
        return this.engineVerify(copia);
    }

    // La forma vieja de pasar parametros, por nombre. Esta desaconsejada desde el JDK 1.2 porque los
    // nombres nunca se estandarizaron: cada proveedor entendia los suyos.
    protected abstract void engineSetParameter(String param, Object value)
        throws InvalidParameterException;

    // La forma buena de pasar parametros. Tira `UnsupportedOperationException` por default porque
    // llego despues que el SPI y no se podia obligar a los proveedores que ya existian.
    protected void engineSetParameter(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException();
    }

    // Los parametros efectivos, incluidos los que el proveedor haya elegido por default. Tira por
    // default, por lo mismo que el setter.
    protected AlgorithmParameters engineGetParameters() {
        throw new UnsupportedOperationException();
    }

    protected abstract Object engineGetParameter(String param) throws InvalidParameterException;

    // Solo clona si la subclase declara `Cloneable`. Que el chequeo sea explicito y no automatico
    // importa: un objeto de firma tiene estado a medio calcular, y copiarlo sin que el proveedor lo
    // haya pensado daria dos objetos compartiendo el buffer interno.
    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
