package java.security;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

// La cara del proveedor para un juego de parametros de algoritmo.
//
// Todos sus metodos son abstractos, y eso hace que esta clase no pueda mentir: la implementacion
// la escribe entera quien conozca el algoritmo. KajiLibrary no trae ninguna.
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
