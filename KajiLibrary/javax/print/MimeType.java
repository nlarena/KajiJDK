package javax.print;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * KajiLibrary's javax.print.MimeType -- un tipo MIME normalizado.
 *
 * <p>Es de acceso de paquete: existe solo para que {@link DocFlavor} tenga contra que comparar. No es
 * parte de la API publica y no hay que exponerla.
 *
 * <h2>Que normaliza, y que no</h2>
 *
 * <p>El punto de la clase es que {@code "Text/Plain; CharSet=Utf-8"} y {@code "text/plain;
 * charset=utf-8"} tienen que ser iguales. Para eso:
 *
 * <ul>
 *   <li>tipo y subtipo pasan a minusculas;
 *   <li>los <b>nombres</b> de parametro pasan a minusculas;
 *   <li>los parametros se ordenan alfabeticamente por nombre;
 *   <li>los <b>valores</b> se dejan como estan, con una excepcion: el de {@code charset}, que tambien
 *       pasa a minusculas.
 * </ul>
 *
 * <p>Esa excepcion no es un capricho. Un nombre de juego de caracteres es insensible a mayusculas por
 * definicion, mientras que el valor de un parametro cualquiera --{@code name="Informe Final"}-- puede
 * no serlo, y bajarlo a minusculas seria destruir informacion.
 */
class MimeType implements Serializable, Cloneable {

    private static final long serialVersionUID = -2785720609362367683L;

    /** El texto de entrada, tal cual. */
    private final String mimeType;

    /** Tipo, ya en minusculas. */
    private final String mediaType;

    /** Subtipo, ya en minusculas. */
    private final String mediaSubtype;

    /** Parametros, ordenados y normalizados. */
    private final TreeMap<String, String> parameterMap;

    /** La forma canonica, que es lo que se compara. */
    private final String canonical;

    /**
     * @throws NullPointerException si es null
     * @throws IllegalArgumentException si no es un tipo MIME valido
     */
    public MimeType(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        this.mimeType = s;
        this.parameterMap = new TreeMap<String, String>();
        Parser parser = new Parser(s);
        this.mediaType = parser.token().toLowerCase();
        parser.expect('/');
        this.mediaSubtype = parser.token().toLowerCase();
        while (parser.skipSemicolon()) {
            String name = parser.token().toLowerCase();
            parser.expect('=');
            String value = parser.value();
            if (name.equals("charset")) {
                value = value.toLowerCase();
            }
            this.parameterMap.put(name, value);
        }
        parser.expectEnd();
        this.canonical = build(this.mediaType, this.mediaSubtype, this.parameterMap);
    }

    /** La forma canonica: tipo, subtipo y parametros ordenados. Ver la nota de la clase. */
    public String getMimeType() {
        return this.canonical;
    }

    /** El tipo, en minusculas. */
    public String getMediaType() {
        return this.mediaType;
    }

    /** El subtipo, en minusculas. */
    public String getMediaSubtype() {
        return this.mediaSubtype;
    }

    /** Los parametros, de solo lectura. */
    public Map<String, String> getParameterMap() {
        return java.util.Collections.unmodifiableMap(this.parameterMap);
    }

    /** La forma canonica. */
    @Override
    public String toString() {
        return this.canonical;
    }

    /** Sobre la forma canonica, no sobre el texto de entrada. */
    @Override
    public int hashCode() {
        return this.canonical.hashCode();
    }

    /** Idem. Dos tipos escritos distinto pero equivalentes son iguales. */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MimeType)) {
            return false;
        }
        return this.canonical.equals(((MimeType) obj).canonical);
    }

    /** El texto tal como se paso, sin normalizar. */
    String getOriginal() {
        return this.mimeType;
    }

    /** Arma la forma canonica. */
    private static String build(String type, String subtype, TreeMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append('/').append(subtype);
        java.util.Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            sb.append("; ").append(e.getKey()).append("=\"").append(e.getValue()).append('"');
        }
        return sb.toString();
    }

    /**
     * El analizador de RFC 2045, reducido a lo que hace falta.
     *
     * <p>Un token es cualquier cosa que no sea espacio, control, ni uno de los separadores; un valor
     * puede ademas venir entre comillas, y ahi si acepta espacios y barras invertidas de escape.
     */
    private static final class Parser {

        /** Los separadores de RFC 2045; un token no puede contener ninguno. */
        private static final String TSPECIALS = "()<>@,;:/[]?=\\\"";

        private final String text;

        private int pos;

        Parser(String text) {
            this.text = text;
            this.pos = 0;
        }

        /** Se come los espacios. */
        private void skipSpace() {
            while (this.pos < this.text.length() && this.text.charAt(this.pos) <= ' ') {
                this.pos = this.pos + 1;
            }
        }

        /** Un token no vacio. */
        String token() {
            skipSpace();
            int start = this.pos;
            while (this.pos < this.text.length()) {
                char c = this.text.charAt(this.pos);
                if (c <= ' ' || c >= 127 || TSPECIALS.indexOf(c) >= 0) {
                    break;
                }
                this.pos = this.pos + 1;
            }
            if (this.pos == start) {
                throw new IllegalArgumentException();
            }
            return this.text.substring(start, this.pos);
        }

        /** Un token o una cadena entre comillas. */
        String value() {
            skipSpace();
            if (this.pos < this.text.length() && this.text.charAt(this.pos) == '"') {
                this.pos = this.pos + 1;
                StringBuilder sb = new StringBuilder();
                while (true) {
                    if (this.pos >= this.text.length()) {
                        throw new IllegalArgumentException();
                    }
                    char c = this.text.charAt(this.pos);
                    this.pos = this.pos + 1;
                    if (c == '"') {
                        return sb.toString();
                    }
                    if (c == '\\') {
                        if (this.pos >= this.text.length()) {
                            throw new IllegalArgumentException();
                        }
                        c = this.text.charAt(this.pos);
                        this.pos = this.pos + 1;
                    }
                    sb.append(c);
                }
            }
            return token();
        }

        /** Consume ese caracter o falla. */
        void expect(char c) {
            skipSpace();
            if (this.pos >= this.text.length() || this.text.charAt(this.pos) != c) {
                throw new IllegalArgumentException();
            }
            this.pos = this.pos + 1;
        }

        /** Consume un punto y coma si lo hay; dice si lo habia. */
        boolean skipSemicolon() {
            skipSpace();
            if (this.pos < this.text.length() && this.text.charAt(this.pos) == ';') {
                this.pos = this.pos + 1;
                return true;
            }
            return false;
        }

        /** Falla si sobro algo. */
        void expectEnd() {
            skipSpace();
            if (this.pos < this.text.length()) {
                throw new IllegalArgumentException();
            }
        }
    }
}
