package javax.security.auth.kerberos;

/**
 * Los numeros de tipo de cifrado de Kerberos y como se llaman.
 *
 * <p>Son los de la RFC 3961 y sus sucesoras; el numero es lo que viaja en el protocolo y el nombre es
 * lo que devuelve {@code getAlgorithm()}. Un tipo que no este en la tabla se llama {@code "unknown"},
 * el cero {@code "none"} y los negativos {@code "private"}, que es lo que el JDK contesta.
 */
final class EncryptionTypes {

    /** Los pares numero y nombre, en el orden de la norma. */
    private static final int[] NUMBERS = { 1, 3, 16, 17, 18, 19, 20, 23 };

    /** El nombre de cada numero de {@link #NUMBERS}. */
    private static final String[] NAMES = {
        "des-cbc-crc", "des-cbc-md5", "des3-cbc-sha1-kd", "aes128-cts-hmac-sha1-96",
        "aes256-cts-hmac-sha1-96", "aes128-cts-hmac-sha256-128", "aes256-cts-hmac-sha384-192",
        "rc4-hmac",
    };

    private EncryptionTypes() {
    }

    /** Como se llama ese tipo. Ver la nota de la clase. */
    static String algorithmName(int keyType) {
        if (keyType == 0) {
            return "none";
        }
        if (keyType < 0) {
            return "private";
        }
        int i = 0;
        while (i < NUMBERS.length) {
            if (NUMBERS[i] == keyType) {
                return NAMES[i];
            }
            i = i + 1;
        }
        return "unknown";
    }
}
