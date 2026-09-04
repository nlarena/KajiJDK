package java.security;

// Lo que un proveedor tiene que escribir para generar pares de claves.
//
// Los dos `initialize` reciben la fuente de azar, y el API no deja generar sin ella. La razon
// conviene decirla entera: **generar un par de claves es
// exactamente la operacion que mas depende de la aleatoriedad**. Una clave RSA sale de dos primos
// elegidos al azar; una clave EC, de un escalar al azar. Con un generador predecible, las claves
// son predecibles, y una clave privada predecible no protege nada. Por eso el que recibe la fuente
// es **abstracto** y el que recibe parametros tiene un default que **rechaza**: un proveedor que no
// sepa manejar parametros tiene que decirlo, no ignorarlos y generar otra cosa.
public abstract class KeyPairGeneratorSpi {

    public KeyPairGeneratorSpi() {
    }

    /**
     * Inicializa por tamaño de clave.
     *
     * @param random de donde sale el azar. Ver la nota de la clase: no es un detalle
     */
    public abstract void initialize(int keysize, SecureRandom random);

    /**
     * Inicializa con parametros concretos -- una curva, un grupo -- cuando el tamaño no alcanza.
     *
     * <p>El default <b>rechaza</b>. Un proveedor que no entienda los parametros tiene que decirlo:
     * ignorarlos y generar con los suyos daria una clave que no es la que se pidio, y quien la
     * reciba no tiene como notarlo.
     *
     * @throws InvalidAlgorithmParameterException siempre, salvo que el proveedor lo sobrescriba
     */
    public void initialize(java.security.spec.AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException();
    }

    // Genera el par. Se puede llamar varias veces y cada una da un par distinto.
    public abstract KeyPair generateKeyPair();
}
