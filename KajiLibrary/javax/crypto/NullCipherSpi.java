package javax.crypto;

import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

// La implementacion detras de `NullCipher`: copia los bytes tal cual.
//
// De paquete a proposito, igual que en el JDK: no es API, y lo unico que la construye es
// `NullCipher`. Hacerla publica agregaria a `javax.crypto` un nombre que el JDK no tiene.
//
// Todo lo que no sea copiar contesta lo que corresponde a un cifrado que no cifra: bloque de un
// byte, salida del mismo tamano que la entrada, sin parametros. El vector de inicializacion son
// ocho ceros y no `null`, que es lo que hace el JDK; devolver `null` seria mas honesto pero
// rompería a quien lo compare contra el suyo.
final class NullCipherSpi extends CipherSpi {

    private static final byte[] IV_VACIO = new byte[8];

    NullCipherSpi() {
    }

    @Override
    protected void engineSetMode(String mode) {
    }

    @Override
    protected void engineSetPadding(String padding) {
    }

    @Override
    protected int engineGetBlockSize() {
        return 1;
    }

    @Override
    protected int engineGetOutputSize(int inputLen) {
        return inputLen;
    }

    @Override
    protected byte[] engineGetIV() {
        return IV_VACIO.clone();
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    @Override
    protected void engineInit(int opmode, Key key, SecureRandom random) {
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params,
            SecureRandom random) {
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameters params,
            SecureRandom random) {
    }

    @Override
    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        if (input == null) {
            return null;
        }
        final byte[] out = new byte[inputLen];
        System.arraycopy(input, inputOffset, out, 0, inputLen);
        return out;
    }

    @Override
    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output,
            int outputOffset) throws ShortBufferException {
        if (input == null) {
            return 0;
        }
        if (output.length - outputOffset < inputLen) {
            throw new ShortBufferException("Output buffer too small");
        }
        System.arraycopy(input, inputOffset, output, outputOffset, inputLen);
        return inputLen;
    }

    @Override
    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) {
        return engineUpdate(input, inputOffset, inputLen);
    }

    @Override
    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output,
            int outputOffset) throws ShortBufferException {
        return engineUpdate(input, inputOffset, inputLen, output, outputOffset);
    }
}
