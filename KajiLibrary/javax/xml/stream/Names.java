package javax.xml.stream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * Como se escribe un {@link QName} en el documento, y que caracteres puede tener un nombre XML.
 *
 * <p>Lo primero no lo hace {@link QName#toString()}, que produce la notacion
 * <code>{uri}local</code> --util para mensajes, no para XML-- y ademas descarta el prefijo, que es
 * justamente lo que hace falta aca.
 *
 * <p>Lo segundo son las reglas de {@code Name} y {@code NameStartChar} de XML 1.0 quinta edicion,
 * recortadas al plano basico. Los nombres con caracteres suplementarios --pares subrogados-- no se
 * aceptan; es una limitacion real y vale mas que un nombre asi de raro sea rechazado a que pase
 * como valido y produzca un documento que otro parser no lee.
 */
final class Names {

    private Names() {
    }

    /** El nombre tal como va escrito: {@code prefijo:local}, o {@code local} si no hay prefijo. */
    static String written(QName q) {
        String p = q.getPrefix();
        if (p == null || p.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            return q.getLocalPart();
        }
        return p + ":" + q.getLocalPart();
    }

    /** Si el caracter puede empezar un nombre XML. */
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

    /** Si el caracter puede continuar un nombre XML. */
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
