package java.security;

// Lo que un proveedor tiene que escribir para **generar** parametros de algoritmo.
//
// Es la contraparte de `AlgorithmParametersSpi`: aquel lee y escribe parametros que ya existen,
// este los inventa. La diferencia importa porque generar parametros de dominio —los primos p y q de
// DSA, por ejemplo— es caro y probabilistico, mientras que decodificarlos no.
//
// Los dos `engineInit` reciben la fuente de azar, y esa dependencia es esencial y no incidental:
// generar un primo grande **es** elegir candidatos al azar hasta que uno pase la prueba de
// primalidad. Sin azar no hay nada que generar. Por eso el API no deja inicializar sin decir de
// donde sale.
public abstract class AlgorithmParameterGeneratorSpi {

    public AlgorithmParameterGeneratorSpi() {
    }

    /**
     * Inicializa por tamaño: cuantos bits tienen que tener los parametros.
     *
     * @param random de donde sale el azar. No es opcional -- ver la nota de la clase
     */
    protected abstract void engineInit(int size, SecureRandom random);

    /**
     * Inicializa con parametros concretos, cuando el tamaño no alcanza para describir lo que se
     * quiere.
     *
     * @throws java.security.InvalidAlgorithmParameterException si los parametros no le sirven a
     *     este generador
     */
    protected abstract void engineInit(java.security.spec.AlgorithmParameterSpec genParamSpec,
            SecureRandom random) throws InvalidAlgorithmParameterException;

    // Los parametros generados.
    protected abstract AlgorithmParameters engineGenerateParameters();
}
