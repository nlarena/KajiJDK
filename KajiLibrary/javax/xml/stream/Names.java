package javax.xml.stream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * How a {@link QName} is written in the document, and which characters an XML name can have.
 *
 * <p>The first is not done by {@link QName#toString()}, which produces the <code>{uri}local</code>
 * notation --useful for messages, not for XML-- and also discards the prefix, which is precisely
 * what is needed here.
 *
 * <p>The second are the {@code Name} and {@code NameStartChar} rules of XML 1.0 fifth edition, cut
 * down to the basic plane. Names with supplementary characters --surrogate pairs-- are not
 * accepted; it is a real limitation and it is better for such an odd name to be rejected than to
 * pass as valid and produce a document another parser cannot read.
 */
final class Names {

    private Names() {
    }

    /** The name as it is written: {@code prefix:local}, or {@code local} if there is no prefix. */
    static String written(QName q) {
        String p = q.getPrefix();
        if (p == null || p.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            return q.getLocalPart();
        }
        return p + ":" + q.getLocalPart();
    }

    /** Whether the character can start an XML name. */
    static boolean isNameStart(char c) {
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        if (c == '_' || c == ':') {
            return true;
        }
        if (c >= 0xC0 && c <= 0xD6) {
            return true;
        }
        if (c >= 0xD8 && c <= 0xF6) {
            return true;
        }
        if (c >= 0xF8 && c <= 0x2FF) {
            return true;
        }
        if (c >= 0x370 && c <= 0x37D) {
            return true;
        }
        if (c >= 0x37F && c <= 0x1FFF) {
            return true;
        }
        if (c >= 0x200C && c <= 0x200D) {
            return true;
        }
        if (c >= 0x2070 && c <= 0x218F) {
            return true;
        }
        if (c >= 0x2C00 && c <= 0x2FEF) {
            return true;
        }
        if (c >= 0x3001 && c <= 0xD7FF) {
            return true;
        }
        if (c >= 0xF900 && c <= 0xFDCF) {
            return true;
        }
        if (c >= 0xFDF0 && c <= 0xFFFD) {
            return true;
        }
        return false;
    }

    /** Whether the character can continue an XML name. */
    static boolean isNamePart(char c) {
        if (isNameStart(c)) {
            return true;
        }
        if (c >= '0' && c <= '9') {
            return true;
        }
        if (c == '-' || c == '.' || c == 0xB7) {
            return true;
        }
        if (c >= 0x300 && c <= 0x36F) {
            return true;
        }
        if (c >= 0x203F && c <= 0x2040) {
            return true;
        }
        return false;
    }
}
