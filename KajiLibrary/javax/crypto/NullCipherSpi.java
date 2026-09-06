package javax.crypto;

import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

// The implementation behind `NullCipher`: it copies the bytes as they are.
//
// Package-private on purpose, as in the JDK: it is not API, and the only thing that builds one is
// `NullCipher`. Making it public would add to `javax.crypto` a name the JDK does not have.
//
// Everything that is not copying answers what befits a cipher that does not encrypt: a block of one
// byte, an output the same size as the input, no parameters. The initialization vector is eight
// zeros and not `null`, which is what the JDK does; returning `null` would be more honest but would
// break anyone comparing it against theirs.
final class NullCipherSpi extends CipherSpi {

    private static final byte[] EMPTY_IV = new byte[8];

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
        return EMPTY_IV.clone();
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
