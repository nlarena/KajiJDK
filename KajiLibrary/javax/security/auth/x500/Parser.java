package javax.security.auth.x500;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KajiLibrary's javax.security.auth.x500.Parser -- lee un nombre distinguido escrito en RFC 2253.
 *
 * <h2>La gramatica, que es mas chica de lo que parece</h2>
 *
 * <p>Un nombre son pasos separados por comas; cada paso son pares separados por `+`; cada par es
 * `type=value`. Todo lo demas es como se escribe un valor, que tiene **tres** formas y hay que
 * distinguirlas antes de hacer nada:
 *
 * <ul>
 *   <li>**Hexadecimal**: empieza con `#` y es el DER del valor, crudo. Se usa para tipos que no se
 *       pueden escribir como texto.
 *   <li>**Entre comillas**: adentro de las comillas casi todo es literal, incluidas las comas.
 *   <li>**Con escapes**: la forma normal. Una barra invertida vuelve literal al caracter siguiente, y
 *       ademas `\\XX` con dos digitos hexadecimales mete un byte crudo.
 * </ul>
 *
 * <p>El caso que hace falta pensar --y donde casi todos los parsers se equivocan-- son los **espacios
 * de los bordes**: un espacio al principio o al final de un valor **no cuenta** salvo que este
 * escapado. `CN= Juan ` es `Juan`, y `CN=\\ Juan` es `" Juan"`. Adentro del valor los espacios se
 * respetan tal cual.
 */
final class Parser {

    private Parser() {
    }

    /**
     * Los pasos del nombre, en el orden en que se escribieron: del mas particular al mas general.
     *
     * @throws IllegalArgumentException si el nombre no parsea
     */
    static X500Principal.Rdn[] parse(String name, Map<String, String> words) {
        List<X500Principal.Rdn> rdns = new ArrayList<X500Principal.Rdn>();
        // Un nombre vacio es un DN vacio y es **valido**: designa la raiz del directorio. No es un
        // error, y tratarlo como tal rompe los certificados que lo usan.
        if (name.trim().length() == 0) {
            return new X500Principal.Rdn[0];
        }
        List<String> trozos = partirNivelSuperior(name, ',');
        int i = 0;
        while (i < trozos.size()) {
            rdns.add(oneRdn(trozos.get(i), words));
            i = i + 1;
        }
        return rdns.toArray(new X500Principal.Rdn[rdns.size()]);
    }

    // Un paso: uno o mas `type=value` unidos por `+`.
    private static X500Principal.Rdn oneRdn(String text, Map<String, String> words) {
        List<String> pairs = partirNivelSuperior(text, '+');
        String[] types = new String[pairs.size()];
        String[] values = new String[pairs.size()];
        int i = 0;
        while (i < pairs.size()) {
            String pair = pairs.get(i);
            int igual = posicionDelIgual(pair);
            if (igual < 0) {
                throw new IllegalArgumentException("falta el `=` en: " + pair);
            }
            String type = pair.substring(0, igual).trim();
            String value = pair.substring(igual + 1, pair.length());
            types[i] = aOid(type, words);
            values[i] = AttrValue.read(value);
            i = i + 1;
        }
        return new X500Principal.Rdn(types, values);
    }

    // El primer `=` que no este adentro de comillas ni escapado. Buscarlo con `indexOf` estaria mal:
    // un valor puede contener `=` y de hecho es comun en los correos.
    private static int posicionDelIgual(String s) {
        boolean comillas = false;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                i = i + 2;
                continue;
            }
            if (c == '"') {
                comillas = !comillas;
            } else if (c == '=' && !comillas) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /**
     * Parte por ese separador, **respetando** comillas y escapes.
     *
     * <p>Es lo que impide que `CN=Perez, Juan` --con la coma adentro de un valor citado-- se lea como
     * dos pasos. Un `split` comun no puede hacer esto, y por eso no se usa.
     */
    private static List<String> partirNivelSuperior(String s, char separator) {
        List<String> out = new ArrayList<String>();
        StringBuilder actual = new StringBuilder();
        boolean comillas = false;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                actual.append(c).append(s.charAt(i + 1));
                i = i + 2;
                continue;
            }
            if (c == '"') {
                comillas = !comillas;
                actual.append(c);
            } else if (c == separator && !comillas) {
                out.add(actual.toString());
                actual = new StringBuilder();
            } else {
                actual.append(c);
            }
            i = i + 1;
        }
        out.add(actual.toString());
        return out;
    }

    // El tipo, siempre como OID en numeros: una palabra clave conocida, una del mapa, o ya un OID.
    private static String aOid(String type, Map<String, String> words) {
        if (type.length() == 0) {
            throw new IllegalArgumentException("tipo vacio");
        }
        String known = X500Principal.oidForWord(type);
        if (known != null) {
            return known;
        }
        // El mapa del llamador va despues de los conocidos: no puede redefinir `CN`.
        String fromMap = lookupIgnoringCase(words, type);
        if (fromMap != null) {
            validateOid(fromMap);
            return fromMap;
        }
        String cleanCert = type;
        // `OID.1.2.3` es la forma larga de escribir un OID; RFC 1779 la usa siempre.
        if (cleanCert.length() > 4 && cleanCert.substring(0, 4).equalsIgnoreCase("OID.")) {
            cleanCert = cleanCert.substring(4, cleanCert.length());
        }
        validateOid(cleanCert);
        return cleanCert;
    }

    private static String lookupIgnoringCase(Map<String, String> m, String clave) {
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(clave)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Que el OID tenga forma de OID.
     *
     * <p>Se valida al parsear y no al usar, y por eso el mensaje puede nombrar lo que estaba mal
     * escrito. Un OID mal formado que pasa el parseo reaparece mucho despues como un nombre que no
     * matchea con nada, y ahi ya no se sabe de donde salio.
     */
    static void validateOid(String oid) {
        if (oid == null || oid.length() == 0) {
            throw new IllegalArgumentException("OID vacio");
        }
        int arcos = 0;
        int i = 0;
        while (i < oid.length()) {
            int end = i;
            while (end < oid.length() && oid.charAt(end) != '.') {
                end = end + 1;
            }
            if (end == i) {
                throw new IllegalArgumentException("OID mal formado: " + oid);
            }
            int k = i;
            while (k < end) {
                if (oid.charAt(k) < '0' || oid.charAt(k) > '9') {
                    throw new IllegalArgumentException("OID mal formado: " + oid);
                }
                k = k + 1;
            }
            arcos = arcos + 1;
            i = end + 1;
            if (i == oid.length()) {
                throw new IllegalArgumentException("el OID termina en punto: " + oid);
            }
        }
        // Menos de dos arcos no es un OID: el primero elige la autoridad y el segundo la rama.
        if (arcos < 2) {
            throw new IllegalArgumentException("un OID necesita al menos dos arcos: " + oid);
        }
        int first = Integer.parseInt(oid.substring(0, oid.indexOf('.')));
        if (first > 2) {
            throw new IllegalArgumentException("el primer arco de un OID es 0, 1 o 2: " + oid);
        }
    }
}
