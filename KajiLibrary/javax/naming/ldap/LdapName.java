package javax.naming.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.Name;

/**
 * Un nombre distinguido de LDAP: {@code cn=Juan,ou=Ventas,dc=ejemplo,dc=com}.
 *
 * <h2>El orden, que es lo primero que confunde</h2>
 *
 * <p>Un DN se <strong>escribe</strong> del mas especifico al mas general —la persona primero, el
 * dominio al final— y {@link Name} numera los componentes al reves: el indice {@code 0} es el
 * <em>menos</em> significativo, o sea el ultimo escrito.
 *
 * <p>No es un capricho de esta clase: es la convencion de {@link Name}, que existe para que
 * {@code /a/b/c} y {@code cn=x,dc=y} se puedan recorrer con la misma API pese a escribirse en
 * sentidos opuestos. La consecuencia practica es que {@code getRdn(0)} de
 * {@code "cn=Juan,dc=com"} devuelve {@code dc=com}, no {@code cn=Juan}.
 *
 * <h2>Es mutable, a diferencia de casi todo lo demas</h2>
 *
 * <p>{@link #add} y {@link #remove} cambian este objeto y devuelven {@code this}. Lo hereda de
 * {@link Name}, que se diseno asi antes de que la inmutabilidad fuera el reflejo por omision.
 * Consecuencia: <strong>no se comparte entre hilos</strong>, y guardarlo en un mapa despues de
 * haberlo modificado deja la clave rota.
 */
public class LdapName implements Name {

    private static final long serialVersionUID = -1595520034788997356L;

    /** Del menos significativo al mas; ver la nota de la clase sobre el orden. */
    private final List<Rdn> rdns;

    /**
     * Desde su forma en texto.
     *
     * @throws InvalidNameException si no es un DN valido
     */
    public LdapName(String name) throws InvalidNameException {
        this.rdns = parsear(name);
    }

    /**
     * Desde una lista de RDN, del menos significativo al mas.
     *
     * @throws NullPointerException si la lista es {@code null}
     */
    public LdapName(List<Rdn> rdns) {
        this.rdns = new ArrayList<Rdn>(rdns);
    }

    private LdapName(List<Rdn> rdns, boolean interno) {
        this.rdns = rdns;
    }

    /**
     * Parte el texto en RDN y los invierte.
     *
     * <p>La coma se busca <strong>fuera de comillas y sin contar las escapadas</strong>, por lo
     * mismo que en {@link Rdn}: un {@code cn=Perez\, Juan} es un solo componente, y partirlo por
     * cualquier coma produce un nombre distinto que igual parsea bien.
     */
    private static List<Rdn> parsear(String name) throws InvalidNameException {
        List<Rdn> out = new ArrayList<Rdn>();
        if (name == null) {
            throw new InvalidNameException("el nombre no puede ser null");
        }
        String s = name.trim();
        if (s.isEmpty()) {
            return out;
        }
        int desde = 0;
        boolean comillas = false;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length()) {
                out.add(new Rdn(s.substring(desde).trim()));
                break;
            }
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                comillas = !comillas;
            } else if ((c == ',' || c == ';') && !comillas) {
                out.add(new Rdn(s.substring(desde, i).trim()));
                desde = i + 1;
            }
        }
        if (comillas) {
            throw new InvalidNameException("comillas sin cerrar en: " + name);
        }
        // Del texto salen del mas significativo al menos; `Name` los quiere al reves.
        Collections.reverse(out);
        return out;
    }

    /** Cuantos componentes tiene. */
    public int size() {
        return this.rdns.size();
    }

    /** Si no tiene ninguno. */
    public boolean isEmpty() {
        return this.rdns.isEmpty();
    }

    /** Los componentes como texto, del menos significativo al mas. */
    public Enumeration<String> getAll() {
        List<String> out = new ArrayList<String>(this.rdns.size());
        for (int i = 0; i < this.rdns.size(); i++) {
            out.add(this.rdns.get(i).toString());
        }
        return Collections.enumeration(out);
    }

    /** El componente {@code posn} como texto. */
    public String get(int posn) {
        return this.rdns.get(posn).toString();
    }

    /** El componente {@code posn}. */
    public Rdn getRdn(int posn) {
        return this.rdns.get(posn);
    }

    /**
     * Los primeros {@code posn} componentes.
     *
     * <p>"Prefijo" en el orden de {@link Name}, o sea la parte <em>mas general</em> del nombre —
     * el sufijo en la escritura. Es la fuente clasica de confusion de esta API.
     */
    public Name getPrefix(int posn) {
        return new LdapName(new ArrayList<Rdn>(this.rdns.subList(0, posn)), true);
    }

    /** Los componentes desde {@code posn} en adelante. */
    public Name getSuffix(int posn) {
        return new LdapName(new ArrayList<Rdn>(this.rdns.subList(posn, this.rdns.size())), true);
    }

    /** Si {@code n} es prefijo de este nombre. */
    public boolean startsWith(Name n) {
        if (!(n instanceof LdapName)) {
            return false;
        }
        return startsWith(((LdapName) n).rdns);
    }

    /** Si esos RDN son prefijo de este nombre. */
    public boolean startsWith(List<Rdn> rdns) {
        if (rdns.size() > this.rdns.size()) {
            return false;
        }
        for (int i = 0; i < rdns.size(); i++) {
            if (!this.rdns.get(i).equals(rdns.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Si {@code n} es sufijo de este nombre. */
    public boolean endsWith(Name n) {
        if (!(n instanceof LdapName)) {
            return false;
        }
        return endsWith(((LdapName) n).rdns);
    }

    /** Si esos RDN son sufijo de este nombre. */
    public boolean endsWith(List<Rdn> rdns) {
        int d = this.rdns.size() - rdns.size();
        if (d < 0) {
            return false;
        }
        for (int i = 0; i < rdns.size(); i++) {
            if (!this.rdns.get(d + i).equals(rdns.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Agrega esos componentes al final. */
    public Name addAll(Name suffix) throws InvalidNameException {
        if (!(suffix instanceof LdapName)) {
            throw new InvalidNameException("no es un LdapName: " + String.valueOf(suffix));
        }
        return addAll(((LdapName) suffix).rdns);
    }

    /** Agrega esos componentes al final. */
    public Name addAll(List<Rdn> suffixRdns) {
        this.rdns.addAll(suffixRdns);
        return this;
    }

    /** Los inserta en esa posicion. */
    public Name addAll(int posn, Name n) throws InvalidNameException {
        if (!(n instanceof LdapName)) {
            throw new InvalidNameException("no es un LdapName: " + String.valueOf(n));
        }
        return addAll(posn, ((LdapName) n).rdns);
    }

    /** Los inserta en esa posicion. */
    public Name addAll(int posn, List<Rdn> rdns) {
        this.rdns.addAll(posn, rdns);
        return this;
    }

    /** Agrega un componente al final. */
    public Name add(String comp) throws InvalidNameException {
        this.rdns.add(new Rdn(comp));
        return this;
    }

    /** Agrega un componente al final. */
    public Name add(Rdn comp) {
        this.rdns.add(comp);
        return this;
    }

    /** Lo inserta en esa posicion. */
    public Name add(int posn, String comp) throws InvalidNameException {
        this.rdns.add(posn, new Rdn(comp));
        return this;
    }

    /** Lo inserta en esa posicion. */
    public Name add(int posn, Rdn comp) {
        this.rdns.add(posn, comp);
        return this;
    }

    /** Saca el componente {@code posn} y lo devuelve como texto. */
    public Object remove(int posn) throws InvalidNameException {
        return this.rdns.remove(posn).toString();
    }

    /** Los componentes, en una lista inmodificable. */
    public List<Rdn> getRdns() {
        return Collections.unmodifiableList(this.rdns);
    }

    /** Una copia independiente: modificarla no toca a esta. */
    public Object clone() {
        return new LdapName(new ArrayList<Rdn>(this.rdns), true);
    }

    /** La forma en texto, del mas significativo al menos — o sea al reves que los indices. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = this.rdns.size() - 1; i >= 0; i--) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(this.rdns.get(i).toString());
        }
        return sb.toString();
    }

    /** Componente por componente, sin distinguir mayusculas. */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LdapName)) {
            return false;
        }
        return compareTo(obj) == 0;
    }

    /**
     * Compara componente por componente, empezando por el <strong>mas significativo</strong>.
     *
     * <p>Ese orden es el que hace util al resultado: ordenar una lista de DN asi los agrupa por
     * subarbol, que es lo que uno espera ver.
     *
     * @throws ClassCastException si {@code obj} no es un {@link LdapName}
     */
    public int compareTo(Object obj) {
        if (!(obj instanceof LdapName)) {
            throw new ClassCastException("no es un LdapName: " + String.valueOf(obj));
        }
        LdapName o = (LdapName) obj;
        int i = this.rdns.size() - 1;
        int j = o.rdns.size() - 1;
        while (i >= 0 && j >= 0) {
            int c = this.rdns.get(i).compareTo(o.rdns.get(j));
            if (c != 0) {
                return c;
            }
            i--;
            j--;
        }
        return this.rdns.size() - o.rdns.size();
    }

    /** Sobre los componentes, coherente con {@link #equals}. */
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < this.rdns.size(); i++) {
            h = h + this.rdns.get(i).hashCode();
        }
        return h;
    }
}
