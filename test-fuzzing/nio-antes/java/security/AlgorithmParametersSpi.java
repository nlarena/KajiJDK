package java.security;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

// The provider's face for a set of algorithm parameters.
//
// All of its methods are abstract, and that makes this class unable to lie: the implementation is
// written whole by whoever knows the algorithm. KajiLibrary brings none.
public abstract class AlgorithmParametersSpi {

    public AlgorithmParametersSpi() {
    }

    protected abstract void engineInit(AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException;

    protected abstract void engineInit(byte[] params) throws IOException;

    protected abstract void engineInit(byte[] params, String format) throws IOException;

    protected abstract <T extends AlgorithmParameterSpec> T engineGetParameterSpec(
        Class<T> paramSpec) throws InvalidParameterSpecException;

    protected abstract byte[] engineGetEncoded() throws IOException;

    protected abstract byte[] engineGetEncoded(String format) throws IOException;

    protected abstract String engineToString();
}
