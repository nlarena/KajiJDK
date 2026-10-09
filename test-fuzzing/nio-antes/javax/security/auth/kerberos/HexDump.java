package javax.security.auth.kerberos;

/**
 * The classic hexadecimal dump, in the format the JDK uses for tickets.
 *
 * <p>Each line is sixteen bytes: the offset in four digits, two groups of eight bytes separated by
 * three spaces, and on the right the same bytes as text, with a dot for each one that is not
 * printable. The last line is padded with spaces so that the text stays aligned.
 */
final class HexDump {

    /** The hexadecimal digits, in upper case as the JDK writes them. */
    private static final char[] DIGITS = "0123456789ABCDEF".toCharArray();

    private HexDump() {
    }

    /** The dump, with a line break at the end of each line. */
    static String dump(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        int offset = 0;
        while (offset < bytes.length) {
            int count = Math.min(16, bytes.length - offset);
            appendHex(text, (offset >> 8) & 0xFF);
            appendHex(text, offset & 0xFF);
            text.append(": ");
            int i = 0;
            while (i < 16) {
                if (i < count) {
                    appendHex(text, bytes[offset + i] & 0xFF);
                } else {
                    text.append("  ");
                }
                if (i == 7) {
                    text.append("   ");
                } else if (i < 15) {
                    text.append(' ');
                }
                i = i + 1;
            }
            text.append("  ");
            i = 0;
            while (i < count) {
                int value = bytes[offset + i] & 0xFF;
                text.append(value >= 32 && value < 127 ? (char) value : '.');
                i = i + 1;
            }
            text.append('\n');
            offset = offset + count;
        }
        return text.toString();
    }

    /** Two digits. */
    private static void appendHex(StringBuilder text, int value) {
        text.append(DIGITS[(value >> 4) & 0xF]).append(DIGITS[value & 0xF]);
    }
}
