package java.security.spec;

import java.math.BigInteger;

// Que clave RSA generar: cuantos bits el modulo y con que exponente publico.
//
// Los dos valores predefinidos son los primos de Fermat F0 = 3 y F4 = 65537. Ser primo y tener
// pocos bits prendidos es lo que los hace utiles: el cifrado publico es una exponenciacion por e, y
// con e = 65537 = 2^16 + 1 son diecisiete pasos. F0 = 3 es aun mas barato pero se desaconseja: con
// e chico y sin padding correcto, un mensaje corto cifrado a tres destinatarios distintos se
// recupera sin factorizar nada (el ataque de Hastad). F4 es el default de todo el mundo.
public class RSAKeyGenParameterSpec implements AlgorithmParameterSpec {

    public static final BigInteger F0 = BigInteger.valueOf(3);
    public static final BigInteger F4 = BigInteger.valueOf(65537);

    private final int keysize;
    private final BigInteger publicExponent;
    private final AlgorithmParameterSpec keyParams;

    public RSAKeyGenParameterSpec(int keysize, BigInteger publicExponent) {
        this(keysize, publicExponent, null);
    }

    public RSAKeyGenParameterSpec(int keysize, BigInteger publicExponent,
                                  AlgorithmParameterSpec keyParams) {
        this.keysize = keysize;
        this.publicExponent = publicExponent;
        this.keyParams = keyParams;
    }

    // El tamaño del modulo en bits.
    public int getKeysize() {
        return this.keysize;
    }

    public BigInteger getPublicExponent() {
        return this.publicExponent;
    }

    // Parametros que quedan pegados a la clave generada; para RSASSA-PSS, la `PSSParameterSpec`.
    public AlgorithmParameterSpec getKeyParams() {
        return this.keyParams;
    }
}
