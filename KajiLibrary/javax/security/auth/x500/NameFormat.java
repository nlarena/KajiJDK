package javax.security.auth.x500;

import java.util.Map;

/**
 * KajiLibrary's javax.security.auth.x500.Formato -- escribe un nombre en los tres formatos.
 *
 * <p>Los tres recorren los mismos pasos en el mismo orden y difieren en cuatro decisiones: como se
 * nombra el tipo, como se escribe el valor, que separa un paso del siguiente, y que separa dos pares
 * dentro de un paso. Estan juntos por eso: separarlos daria tres recorridos que se pueden
 * desincronizar, y un nombre que se escribe distinto segun el formato deja de ser el mismo nombre.
 *
 * <h2>Las tres reglas que no son obvias</h2>
 *
 * <p>Todas salidas de preguntarle al JDK 25, no de leer el RFC -- que en los tres casos deja lugar a
 * mas de una lectura:
 *
 * <ol>
 *   <li><b>Un tipo sin palabra clave fuerza el valor a hexadecimal</b> en RFC 2253 y en canonico:
 *       `1.2.3.4=#1304616c676f` y no `1.2.3.4=algo`. La razon es que sin palabra clave tampoco hay
 *       una forma de texto acordada para el valor, asi que se escribe el DER crudo. Si el llamador
 *       pasa un diccionario que **si** nombra ese OID, vuelve a escribirse como texto.
 *   <li><b>RFC 1779 cita en vez de escapar</b>: `CN="Perez, Juan"` donde RFC 2253 pone
 *       `CN=Perez\, Juan`. Y cita tambien un valor con **dos espacios seguidos**, que es el caso que
 *       se olvida.
 *   <li><b>RFC 1779 separa los pares de un paso con ` + `</b>, con espacios, mientras que RFC 2253
 *       usa `+` pelado.
 * </ol>
 */
final class NameFormat {

    private NameFormat() {
    }

    /**
     * @param viejo    RFC 1779: cita en vez de escapar, `OID.x.y`, `, ` y ` + ` como separadores
     * @param canonico todo en minusculas, espacios colapsados, sin traducciones
     */
    static String write(X500Principal.Rdn[] rdns, Map<String, String> oidMap,
            boolean legacy, boolean canonical) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < rdns.length) {
            if (i > 0) {
                out.append(legacy ? ", " : ",");
            }
            oneRdn(out, rdns[i], oidMap, legacy, canonical);
            i = i + 1;
        }
        return out.toString();
    }

    private static void oneRdn(StringBuilder out, X500Principal.Rdn rdn, Map<String, String> oidMap,
            boolean legacy, boolean canonical) {
        int i = 0;
        while (i < rdn.types.length) {
            if (i > 0) {
                out.append(legacy ? " + " : "+");
            }
            String word = wordFor(rdn.types[i], oidMap, canonical);
            out.append(typeLabel(rdn.types[i], word, legacy, canonical));
            out.append('=');
            out.append(valueText(rdn.values[i], word != null, legacy, canonical));
            i = i + 1;
        }
    }

    // La palabra clave de ese OID, o `null` si no tiene ninguna. El diccionario del llamador gana
    // sobre las del estandar: para eso lo pasa. En canonico no hay diccionario -- ver
    // `X500Principal.getName`, que rechaza uno no vacio.
    private static String wordFor(String oid, Map<String, String> oidMap, boolean canonical) {
        if (!canonical) {
            String own = oidMap.get(oid);
            if (own != null) {
                return own;
            }
        }
        return X500Principal.wordForOid(oid);
    }

    private static String typeLabel(String oid, String word, boolean legacy,
            boolean canonical) {
        if (word == null) {
            // Sin palabra clave se escribe el OID. RFC 1779 le pone el prefijo `OID.`; RFC 2253 no,
            // y el canonico tampoco -- ahi el OID pelado es justamente la forma estable.
            return legacy ? ("OID." + oid) : oid;
        }
        return canonical ? word.toLowerCase() : word;
    }

    private static String valueText(String value, boolean hasWord, boolean legacy,
            boolean canonical) {
        // Sin palabra clave y sin RFC 1779: el valor va en hexadecimal. Ver la regla 1 de arriba.
        if (!hasWord && !legacy) {
            return "#" + Der.toHex(Der.writeValue(value)).toLowerCase();
        }
        if (canonical) {
            return AttrValue.canonical(value);
        }
        if (legacy) {
            return quotedLegacy(value);
        }
        return AttrValue.write(value);
    }

    // Lo que RFC 1779 cita. Un valor con caracteres de sintaxis, con espacios en los bordes, o con
    // dos espacios seguidos, va entre comillas; adentro solo hay que escapar la comilla y la barra.
    private static String quotedLegacy(String value) {
        if (!needsQuotes(value)) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
            i = i + 1;
        }
        sb.append('"');
        return sb.toString();
    }

    private static boolean needsQuotes(String v) {
        if (v.length() == 0) {
            return false;
        }
        if (v.charAt(0) == ' ' || v.charAt(v.length() - 1) == ' ' || v.charAt(0) == '#') {
            return true;
        }
        int i = 0;
        while (i < v.length()) {
            char c = v.charAt(i);
            if (",+=\"<>;\\".indexOf(c) >= 0) {
                return true;
            }
            if (c == ' ' && i + 1 < v.length() && v.charAt(i + 1) == ' ') {
                return true;
            }
            i = i + 1;
        }
        return false;
    }
}
