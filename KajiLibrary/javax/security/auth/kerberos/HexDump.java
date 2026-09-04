package javax.security.auth.kerberos;

/**
 * El volcado hexadecimal clasico, en el formato que usa el JDK para los tickets.
 *
 * <p>Cada linea son dieciseis bytes: el desplazamiento en cuatro digitos, dos grupos de ocho bytes
 * separados por tres espacios, y a la derecha los mismos bytes como texto, con un punto por cada uno
 * que no sea imprimible. La ultima linea se rellena con espacios para que el texto quede alineado.
 */
final class HexDump {

    /** Los digitos hexadecimales, en mayusculas como los escribe el JDK. */
    private static final char[] DIGITS = "0123456789ABCDEF".toCharArray();

    private HexDump() {
    }

    /** El volcado, con un salto de linea al final de cada linea. */
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

    /** Dos digitos. */
    private static void appendHex(StringBuilder text, int value) {
        text.append(DIGITS[(value >> 4) & 0xF]).append(DIGITS[value & 0xF]);
    }
}
