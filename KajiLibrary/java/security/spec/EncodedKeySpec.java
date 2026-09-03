package java.security.spec;

// La clave como un chorro de bytes en algun formato estandar, mas el nombre del algoritmo.
//
// Es el `KeySpec` menos transparente de todos —no expone ni un campo de la clave— y aun asi es el
// que mas se usa, porque es el unico que sirve para un algoritmo que la biblioteca no conoce: si
// se puede mover el DER de un lado al otro, no hace falta entenderlo.
//
// El arreglo se **copia** al entrar y al salir. No es paranoia de estilo: el que recibe una clave
// codificada no puede permitir que quien se la dio se la cambie por atras despues de la
// validacion, y el que la entrega no puede permitir que el receptor mute la copia interna.
public abstract class EncodedKeySpec implements KeySpec {

    private final byte[] encodedKey;

    // El nombre del algoritmo, o null si quien construyo la spec no lo sabia. Que sea opcional es
    // del contrato: un DER X.509 lleva el algoritmo adentro, y el llamador puede no haberlo leido.
    private final String algorithmName;

    public EncodedKeySpec(byte[] encodedKey) {
        this.encodedKey = copiar(encodedKey);
        this.algorithmName = null;
    }

    protected EncodedKeySpec(byte[] encodedKey, String algorithm) {
        if (algorithm == null) {
            throw new NullPointerException("algorithm name may not be null");
        }
        if (algorithm.isEmpty()) {
            throw new IllegalArgumentException("algorithm name may not be empty");
        }
        this.encodedKey = copiar(encodedKey);
        this.algorithmName = algorithm;
    }

    private static byte[] copiar(byte[] b) {
        if (b == null) {
            throw new NullPointerException("the encoded key must not be null");
        }
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    // El nombre del algoritmo, o null si no se dio.
    public String getAlgorithm() {
        return this.algorithmName;
    }

    // Una copia de los bytes codificados.
    public byte[] getEncoded() {
        byte[] c = new byte[this.encodedKey.length];
        System.arraycopy(this.encodedKey, 0, c, 0, this.encodedKey.length);
        return c;
    }

    // El nombre del formato de la codificacion: "X.509", "PKCS#8".
    public abstract String getFormat();
}
