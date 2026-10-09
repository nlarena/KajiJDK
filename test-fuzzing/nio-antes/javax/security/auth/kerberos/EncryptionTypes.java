package javax.security.auth.kerberos;

/**
 * Kerberos encryption type numbers and what they are called.
 *
 * <p>They are those of RFC 3961 and its successors; the number is what travels in the protocol and
 * the name is what {@code getAlgorithm()} returns. A type not in the table is called
 * {@code "unknown"}, zero {@code "none"} and the negatives {@code "private"}, which is what the
 * JDK answers.
 */
final class EncryptionTypes {

    /** The number and name pairs, in the standard's order. */
    private static final int[] NUMBERS = { 1, 3, 16, 17, 18, 19, 20, 23 };

    /** The name of each number of {@link #NUMBERS}. */
    private static final String[] NAMES = {
        "des-cbc-crc", "des-cbc-md5", "des3-cbc-sha1-kd", "aes128-cts-hmac-sha1-96",
        "aes256-cts-hmac-sha1-96", "aes128-cts-hmac-sha256-128", "aes256-cts-hmac-sha384-192",
        "rc4-hmac",
    };

    private EncryptionTypes() {
    }

    /** What that type is called. See the class note. */
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
