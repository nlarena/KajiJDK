package javax.naming.ldap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

/**
 * Un componente de un nombre distinguido: {@code cn=Juan}, y a veces mas de un par a la vez.
 *
 * <h2>Por que puede tener varios pares</h2>
 *
 * <p>Un RDN <em>multivaluado</em> —{@code cn=Juan+ou=Ventas}— existe para cuando un solo atributo no
 * alcanza para distinguir dos entradas hermanas. Es raro, y es la razon de que esta clase tenga
 * {@link #size} y {@link #toAttributes} en vez de ser un simple par tipo/valor.
 *
 * <p>{@link #getType} y {@link #getValue} devuelven <strong>uno</strong> de ellos, no todos; con
 * varios pares hay que ir por {@link #toAttributes}.
 *
 * <h2>El escape, que es donde estan los errores</h2>
 *
 * <p>Un valor puede contener los caracteres que la sintaxis usa como separadores: {@code ,}, {@code +},
 * {@code =}, {@code "}, {@code \}, {@code <}, {@code >}, {@code ;}. Escribir un nombre sin escaparlos
 * produce algo que parsea distinto de lo que se quiso decir — y como parsea <em>bien</em>, el error
 * es silencioso.
 *
 * <p>{@link #escapeValue} y {@link #unescapeValue} son inversas, y son estaticas justamente para
 * poder usarlas al armar un nombre a mano.
 *
 * <p>El espacio tiene una regla propia que sorprende: solo se escapa al principio y al final, porque
 * en el medio no es ambiguo. Lo mismo el {@code #}, que solo significa algo como primer caracter —
 * marca un valor en hexadecimal.
 */
public class Rdn implements Serializable, Comparable<Object> {

    private static final long serialVersionUID = -5994465067210009656L;

    private static final String ESCAPADOS = ",=+<>#;\"\\";

    private final List<String> tipos = new ArrayList<String>();
    private final List<Object> valores = new ArrayList<Object>();

    /**
     * Desde un conjunto de atributos; cada uno aporta un par.
     *
     * @throws InvalidNameException si el conjunto esta vacio, o si un atributo no tiene valor
     */
    public Rdn(Attributes attrSet) throws InvalidNameException {
        if (attrSet == null || attrSet.size() == 0) {
            throw new InvalidNameException("un RDN necesita al menos un atributo");
        }
        try {
            NamingEnumeration<? extends Attribute> e = attrSet.getAll();
            while (e.hasMore()) {
                Attribute a = e.next();
                if (a.size() == 0) {
                    throw new InvalidNameException("el atributo " + a.getID() + " no tiene valor");
                }
                this.tipos.add(a.getID());
                this.valores.add(a.get());
            }
        } catch (InvalidNameException e) {
            throw e;
        } catch (Exception e) {
            InvalidNameException x = new InvalidNameException("no se pudo leer los atributos");
            x.initCause(e);
            throw x;
        }
        ordenar();
    }

    /**
     * Desde su forma en texto: {@code "cn=Juan"} o {@code "cn=Juan+ou=Ventas"}.
     *
     * @throws InvalidNameException si no es un RDN valido
     */
    public Rdn(String rdnString) throws InvalidNameException {
        parsear(rdnString);
        ordenar();
    }

    /** Una copia. */
    public Rdn(Rdn rdn) {
        this.tipos.addAll(rdn.tipos);
        this.valores.addAll(rdn.valores);
    }

    /**
     * Con un solo par.
     *
     * @throws InvalidNameException si el tipo esta vacio
     */
    public Rdn(String type, Object value) throws InvalidNameException {
        if (type == null || type.isEmpty()) {
            throw new InvalidNameException("el tipo no puede estar vacio");
        }
        if (value == null) {
            throw new InvalidNameException("el valor no puede ser null");
        }
        this.tipos.add(type);
        this.valores.add(value);
    }

    /**
     * Los pares se guardan ordenados por tipo, sin distinguir mayusculas.
     *
     * <p>No es cosmetico: {@code cn=a+ou=b} y {@code ou=b+cn=a} son <strong>el mismo</strong> RDN
     * segun el RFC, y sin un orden canonico ni {@code equals} ni {@code compareTo} podrian decirlo.
     */
    private void ordenar() {
        for (int i = 1; i < this.tipos.size(); i++) {
            for (int j = i; j > 0; j--) {
                if (this.tipos.get(j).compareToIgnoreCase(this.tipos.get(j - 1)) < 0) {
                    Collections.swap(this.tipos, j, j - 1);
                    Collections.swap(this.valores, j, j - 1);
                } else {
                    break;
                }
            }
        }
    }

    private void parsear(String s) throws InvalidNameException {
        if (s == null) {
            throw new InvalidNameException("el RDN no puede ser null");
        }
        int i = 0;
        int n = s.length();
        while (true) {
            int igual = buscarFuera(s, i, '=');
            if (igual < 0) {
                throw new InvalidNameException("falta el '=' en: " + s);
            }
            String tipo = s.substring(i, igual).trim();
            if (tipo.isEmpty()) {
                throw new InvalidNameException("tipo vacio en: " + s);
            }
            int mas = buscarFuera(s, igual + 1, '+');
            int fin = mas < 0 ? n : mas;
            String valor = s.substring(igual + 1, fin);
            this.tipos.add(tipo);
            this.valores.add(unescapeValue(valor));
            if (mas < 0) {
                return;
            }
            i = mas + 1;
        }
    }

    /**
     * Busca {@code c} fuera de comillas y sin contar los escapados.
     *
     * <p>Un {@code buscar} ingenuo partiria {@code cn=a\+b} en dos pares, que es exactamente el
     * error silencioso que el escape existe para evitar.
     */
    private static int buscarFuera(String s, int desde, char c) {
        boolean comillas = false;
        for (int i = desde; i < s.length(); i++) {
            char d = s.charAt(i);
            if (d == '\\') {
                i++;
            } else if (d == '"') {
                comillas = !comillas;
            } else if (d == c && !comillas) {
                return i;
            }
        }
        return -1;
    }

    /** Uno de los valores. Con varios pares, cual no esta especificado. */
    public Object getValue() {
        return this.valores.get(0);
    }

    /** Uno de los tipos. */
    public String getType() {
        return this.tipos.get(0);
    }

    /** La forma en texto, con los valores escapados. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.tipos.size(); i++) {
            if (i > 0) {
                sb.append('+');
            }
            sb.append(this.tipos.get(i)).append('=').append(escapeValue(this.valores.get(i)));
        }
        return sb.toString();
    }

    /**
     * Compara sin distinguir mayusculas, tipo por tipo y despues valor por valor.
     *
     * @throws ClassCastException si {@code obj} no es un {@link Rdn}
     */
    public int compareTo(Object obj) {
        if (!(obj instanceof Rdn)) {
            throw new ClassCastException("no es un Rdn: " + String.valueOf(obj));
        }
        Rdn o = (Rdn) obj;
        int n = Math.min(this.tipos.size(), o.tipos.size());
        for (int i = 0; i < n; i++) {
            int c = this.tipos.get(i).compareToIgnoreCase(o.tipos.get(i));
            if (c != 0) {
                return c;
            }
            c = String.valueOf(this.valores.get(i))
                    .compareToIgnoreCase(String.valueOf(o.valores.get(i)));
            if (c != 0) {
                return c;
            }
        }
        return this.tipos.size() - o.tipos.size();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Rdn)) {
            return false;
        }
        return compareTo(obj) == 0;
    }

    /** Sobre los tipos y valores en minuscula, coherente con {@link #equals}. */
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < this.tipos.size(); i++) {
            h = h + this.tipos.get(i).toLowerCase(Locale.ENGLISH).hashCode()
                    + String.valueOf(this.valores.get(i)).toLowerCase(Locale.ENGLISH).hashCode();
        }
        return h;
    }

    /** Los pares como conjunto de atributos. Es la forma de ver todos cuando hay varios. */
    public Attributes toAttributes() {
        // Sin distinguir mayusculas en los identificadores, que es como funciona LDAP.
        BasicAttributes attrs = new BasicAttributes(true);
        for (int i = 0; i < this.tipos.size(); i++) {
            attrs.put(this.tipos.get(i), this.valores.get(i));
        }
        return attrs;
    }

    /** Cuantos pares tiene; casi siempre uno. */
    public int size() {
        return this.tipos.size();
    }

    /**
     * Escapa un valor para que se pueda escribir en un nombre.
     *
     * <p>Un {@code byte[]} se escribe como {@code #} seguido de hexadecimal, que es la forma que el
     * RFC define para lo que no es texto.
     *
     * @throws IllegalArgumentException si el valor no es una cadena ni un arreglo de bytes
     */
    public static String escapeValue(Object val) {
        if (val instanceof byte[]) {
            byte[] b = (byte[]) val;
            StringBuilder sb = new StringBuilder(1 + b.length * 2);
            sb.append('#');
            for (int i = 0; i < b.length; i++) {
                int v = b[i] & 0xFF;
                if (v < 16) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        }
        if (!(val instanceof String)) {
            throw new IllegalArgumentException(
                    "solo se puede escapar una cadena o un byte[]: " + String.valueOf(val));
        }
        String s = (String) val;
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // El espacio solo es ambiguo en los extremos; el `#` solo como primer caracter.
            boolean bordeEspacio = c == ' ' && (i == 0 || i == s.length() - 1);
            boolean primerNumeral = c == '#' && i == 0;
            if (ESCAPADOS.indexOf(c) >= 0 || bordeEspacio || primerNumeral) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * La inversa de {@link #escapeValue}.
     *
     * <p>Devuelve un {@code byte[]} cuando el valor empieza con {@code #}, y una {@link String} en
     * cualquier otro caso — de ahi que el tipo de retorno sea {@link Object}.
     *
     * @throws IllegalArgumentException si el texto no es un valor valido
     */
    public static Object unescapeValue(String val) {
        String s = val.trim();
        if (s.isEmpty()) {
            return "";
        }
        if (s.charAt(0) == '#') {
            String hex = s.substring(1);
            if (hex.length() % 2 != 0) {
                throw new IllegalArgumentException("el hexadecimal tiene largo impar: " + s);
            }
            byte[] out = new byte[hex.length() / 2];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return out;
        }
        StringBuilder sb = new StringBuilder(s.length());
        boolean comillas = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                if (i + 1 >= s.length()) {
                    throw new IllegalArgumentException("barra al final de: " + val);
                }
                char d = s.charAt(++i);
                // Un `\` puede escapar un caracter o introducir un par hexadecimal.
                if (esHex(d) && i + 1 < s.length() && esHex(s.charAt(i + 1))) {
                    sb.append((char) Integer.parseInt(s.substring(i, i + 2), 16));
                    i++;
                } else {
                    sb.append(d);
                }
            } else if (c == '"') {
                comillas = !comillas;
            } else {
                sb.append(c);
            }
        }
        if (comillas) {
            throw new IllegalArgumentException("comillas sin cerrar en: " + val);
        }
        return sb.toString();
    }

    private static boolean esHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
