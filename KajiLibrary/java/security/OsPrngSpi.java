package java.security;

// El generador que registra `KajiProvider`: un pase directo al del sistema operativo.
//
// ===============================================================================================
// POR QUE UN PASE DIRECTO Y NO UN DRBG
// ===============================================================================================
//
// Un DRBG --el `SHA1PRNG` o el `Hash_DRBG` del JDK-- tiene estado interno: se siembra una vez y
// despues expande esa semilla con una funcion de hash. Escribir uno bien no es dificil, pero
// **elegir mal cualquier detalle no se nota**: la salida se ve igual de aleatoria con o sin el
// error, y nadie lo descubre hasta que alguien la ataca.
//
// Un pase directo no tiene esa clase de error posible. Cada byte que sale es un byte que dio
// `BCryptGenRandom` o `/dev/urandom`, y la seguridad es exactamente la del sistema -- que ademas es
// la que el sistema resiembra solo con las fuentes de entropia del hardware. Es lo mismo que hace
// el proveedor `SunMSCAPI` del JDK con su `Windows-PRNG`.
//
// El precio es velocidad: cada llamada baja al sistema. Para lo que hace falta en esta biblioteca
// --semillas, nonces, identificadores-- no importa.
//
// ===============================================================================================
// QUE HACE CON LA SEMILLA QUE LE DAN, Y POR QUE
// ===============================================================================================
//
// `engineSetSeed` **la ignora**, y hay que decirlo porque suena a que falta algo.
//
// El contrato de `setSeed` es que la semilla **agrega**, nunca reemplaza: llamarlo no puede dejar
// al generador mas predecible. Ignorarla lo cumple -- la salida sigue siendo la del sistema, que no
// depende de lo que el llamador pase. La alternativa seria mezclarla, y ahi habria que inventar la
// mezcla: derivar un flujo de la semilla y combinarlo con los bytes del sistema. Eso es diseñar una
// construccion criptografica para no ganar nada, porque los bytes del sistema ya son fuertes.
//
// `engineReseed` tampoco esta: no hay estado interno que resembrar. Quien lo llame recibe
// `UnsupportedOperationException`, que es la respuesta correcta y no un no-op silencioso.
final class OsPrngSpi extends SecureRandomSpi {

    private static final long serialVersionUID = 6812298296178204625L;

    /** Ver la nota de la clase: la semilla se acepta y se descarta. */
    @Override
    protected void engineSetSeed(byte[] seed) {
        if (seed == null) {
            throw new NullPointerException("seed is null");
        }
    }

    @Override
    protected void engineNextBytes(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        OsEntropy.fill(bytes);
    }

    /**
     * Entropia para sembrar a otro generador.
     *
     * <p>Sale de la misma fuente que {@code engineNextBytes}, y aca eso <b>si</b> es correcto: la
     * distincion entre salida y entropia existe porque un DRBG expande una semilla, y expandir no
     * agrega entropia. Un pase directo no expande nada, asi que las dos son la misma cosa.
     */
    @Override
    protected byte[] engineGenerateSeed(int numBytes) {
        if (numBytes < 0) {
            throw new IllegalArgumentException("numBytes cannot be negative");
        }
        return OsEntropy.bytes(numBytes);
    }

    @Override
    public String toString() {
        return "OS-PRNG";
    }
}
