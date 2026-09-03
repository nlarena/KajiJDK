package javax.naming;

import java.util.Enumeration;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;

/**
 * La maquinaria de nombres que comparten `CompositeName` y `CompoundName`. No es publica.
 *
 * <h2>Por que existe una sola clase para las dos</h2>
 *
 * <p>`CompositeName` y `CompoundName` hacen lo mismo con distintos parametros: una lista de
 * componentes, un parseo y un armado de cadena gobernados por una sintaxis. `CompositeName` fija
 * esa sintaxis --barra, comilla doble, comilla simple, contrabarra, de izquierda a derecha-- y
 * `CompoundName` la recibe en un `Properties`. Poner la logica dos veces habria garantizado que
 * las dos se separaran; poniendola aca, `CompositeName` es literalmente `CompoundName` con la
 * sintaxis por default de esta clase, que es exactamente lo que los valores iniciales de los
 * campos de abajo describen.
 *
 * <h2>Las tres direcciones</h2>
 *
 * <p>`jndi.syntax.direction` vale `left_to_right`, `right_to_left` o `flat`, y no es cosmetico:
 *
 * <ul>
 *   <li>De izquierda a derecha el componente 0 es el que esta mas a la izquierda de la cadena.
 *   <li>De derecha a izquierda --LDAP-- el 0 es el de **mas a la derecha**: en `cn=juan,o=acme`,
 *       `get(0)` es `o=acme`. Por eso el parseo inserta al frente y `toString` recorre al reves.
 *   <li>Plano no tiene separador: la cadena entera es un solo componente, y agregar un segundo
 *       falla con `InvalidNameException`.
 * </ul>
 *
 * <h2>La invariante que manda: `toString` tiene que volver a parsearse</h2>
 *
 * <p>Todo lo raro del citado y el escape sale de sostener eso. Si un componente contiene el
 * separador, `stringifyComp` lo **cita** cuando la sintaxis tiene comillas y lo **escapa** cuando
 * no; si contiene una comilla al principio la escapa, porque una comilla al principio de
 * componente es lo que **abre** una cita al parsear; y si contiene una contrabarra delante de un
 * metacaracter la duplica, porque si no el parseo se la comeria. En el otro sentido,
 * `extractComp` deshace exactamente eso.
 *
 * <p>La regla mas facil de perder es la de la cita: una cita solo cuenta si abre **al principio**
 * del componente, y su cierre tiene que caer en un separador o en el fin de la cadena. Una comilla
 * en el medio es un caracter comun; una cita que cierra antes de que termine el componente es un
 * error de sintaxis y no un componente raro.
 *
 * <h2>Componentes vacios</h2>
 *
 * <p>Un separador al final agrega un componente vacio --`"a/"` son dos componentes-- pero solo si
 * lo que hay antes no es todo vacio; por eso `"/"` es **un** componente vacio y no dos. Y al
 * revez, un nombre cuyos componentes son todos vacios se imprime con un separador de mas, para
 * que `""` (cero componentes) y `{""}` (uno vacio) no se confundan al ida y vuelta.
 */
class NameImpl {

    private static final byte FLAT = 0;
    private static final byte LEFT_TO_RIGHT = 1;
    private static final byte RIGHT_TO_LEFT = 2;

    private Vector<String> components;

    // Los valores iniciales **son** la sintaxis de `CompositeName`: cuando el `Properties` es null
    // no se toca ninguno y queda esto. Cambiar un default de aca cambia `CompositeName`.
    private byte syntaxDirection = LEFT_TO_RIGHT;
    private String syntaxSeparator = "/";
    private String syntaxSeparator2 = null;
    private boolean syntaxCaseInsensitive = false;
    private boolean syntaxTrimBlanks = false;
    private String syntaxEscape = "\\";
    private String syntaxBeginQuote1 = "\"";
    private String syntaxEndQuote1 = "\"";
    private String syntaxBeginQuote2 = "'";
    private String syntaxEndQuote2 = "'";
    private String syntaxAvaSeparator = null;
    private String syntaxTypevalSeparator = null;

    NameImpl(Properties syntax) {
        if (syntax != null) {
            recordNamingConvention(syntax);
        }
        components = new Vector<String>();
    }

    NameImpl(Properties syntax, String n) throws InvalidNameException {
        this(syntax);

        boolean rToL = (syntaxDirection == RIGHT_TO_LEFT);
        boolean compsAllEmpty = true;
        int len = n.length();

        for (int i = 0; i < len; ) {
            i = extractComp(n, i, len, components);

            String comp = rToL ? components.firstElement() : components.lastElement();
            if (comp.length() >= 1) {
                compsAllEmpty = false;
            }

            if (i < len) {
                i = skipSeparator(n, i);
                // Separador final: hay un componente vacio despues. Pero solo si algo de lo que
                // vino antes no era vacio -- si no, `"/"` daria dos vacios en vez de uno, y
                // `toString` ya no podria distinguir `{""}` de `{"", ""}`.
                if ((i == len) && !compsAllEmpty) {
                    if (rToL) {
                        components.insertElementAt("", 0);
                    } else {
                        components.addElement("");
                    }
                }
            }
        }
    }

    NameImpl(Properties syntax, Enumeration<String> comps) {
        this(syntax);
        // Los componentes vienen ya partidos: no se parsean ni se validan. Es la puerta por la que
        // `getPrefix`/`getSuffix`/`clone` arman nombres sin volver a pasar por la sintaxis.
        while (comps.hasMoreElements()) {
            components.addElement(comps.nextElement());
        }
    }

    // ---- lectura de la sintaxis ---------------------------------------------------------------------

    private void recordNamingConvention(Properties p) {
        String dir = p.getProperty("jndi.syntax.direction", "flat");
        if (dir.equals("left_to_right")) {
            syntaxDirection = LEFT_TO_RIGHT;
        } else if (dir.equals("right_to_left")) {
            syntaxDirection = RIGHT_TO_LEFT;
        } else if (dir.equals("flat")) {
            syntaxDirection = FLAT;
        } else {
            // No chequeada a proposito: una sintaxis con una direccion inventada es un error del
            // programador, no un nombre mal escrito.
            throw new IllegalArgumentException(dir +
                " is not a valid value for the jndi.syntax.direction property");
        }

        if (syntaxDirection != FLAT) {
            syntaxSeparator = p.getProperty("jndi.syntax.separator");
            syntaxSeparator2 = p.getProperty("jndi.syntax.separator2");
            if (syntaxSeparator == null) {
                throw new IllegalArgumentException(
                    "jndi.syntax.separator property required for non-flat syntax");
            }
        } else {
            // Plano no separa nada, y que quede en null es lo que hace que `toString` no meta
            // separadores y que `isSeparator` diga siempre que no.
            syntaxSeparator = null;
        }
        syntaxEscape = p.getProperty("jndi.syntax.escape");

        syntaxCaseInsensitive = getBoolean(p, "jndi.syntax.ignorecase");
        syntaxTrimBlanks = getBoolean(p, "jndi.syntax.trimblanks");

        // Dar solo una de las dos puntas de una cita significa que abre y cierra igual, que es el
        // caso normal (`"`); poner las dos permite citas asimetricas del estilo `<`...`>`.
        syntaxBeginQuote1 = p.getProperty("jndi.syntax.beginquote");
        syntaxEndQuote1 = p.getProperty("jndi.syntax.endquote");
        if (syntaxEndQuote1 == null && syntaxBeginQuote1 != null) {
            syntaxEndQuote1 = syntaxBeginQuote1;
        } else if (syntaxBeginQuote1 == null && syntaxEndQuote1 != null) {
            syntaxBeginQuote1 = syntaxEndQuote1;
        }
        syntaxBeginQuote2 = p.getProperty("jndi.syntax.beginquote2");
        syntaxEndQuote2 = p.getProperty("jndi.syntax.endquote2");
        if (syntaxEndQuote2 == null && syntaxBeginQuote2 != null) {
            syntaxEndQuote2 = syntaxBeginQuote2;
        } else if (syntaxBeginQuote2 == null && syntaxEndQuote2 != null) {
            syntaxBeginQuote2 = syntaxEndQuote2;
        }

        // Las dos de LDAP: `,` entre atributos de un mismo componente y `=` entre tipo y valor.
        // La segunda es la unica que el parseo mira, y solo para dejar pasar `cn="con,coma"`.
        syntaxAvaSeparator = p.getProperty("jndi.syntax.separator.ava");
        syntaxTypevalSeparator = p.getProperty("jndi.syntax.separator.typeval");
    }

    private static boolean getBoolean(Properties p, String name) {
        String v = p.getProperty(name);
        return (v != null) && v.toLowerCase(Locale.ENGLISH).equals("true");
    }

    // ---- reconocimiento de metacaracteres -----------------------------------------------------------

    /** `true` si `match` no es null y aparece en `n` justo en `i`. El null-check es la mitad del punto. */
    private boolean isA(String n, int i, String match) {
        return (match != null && n.startsWith(match, i));
    }

    private boolean isMeta(String n, int i) {
        return isA(n, i, syntaxEscape)
            || isA(n, i, syntaxBeginQuote1)
            || isA(n, i, syntaxBeginQuote2)
            || isSeparator(n, i);
    }

    private boolean isSeparator(String n, int i) {
        return isA(n, i, syntaxSeparator) || isA(n, i, syntaxSeparator2);
    }

    private int skipSeparator(String name, int i) {
        if (isA(name, i, syntaxSeparator)) {
            i += syntaxSeparator.length();
        } else if (isA(name, i, syntaxSeparator2)) {
            i += syntaxSeparator2.length();
        }
        return i;
    }

    // ---- parseo -------------------------------------------------------------------------------------

    /**
     * Saca un componente de `name` empezando en `i`, lo mete en `comps` y devuelve donde quedo
     * --parado en el separador, o en `len`--.
     */
    private int extractComp(String name, int i, int len, Vector<String> comps)
            throws InvalidNameException {
        String beginQuote;
        String endQuote;
        boolean start = true;
        boolean one = false;
        StringBuilder answer = new StringBuilder(len);

        while (i < len) {

            if (start && ((one = isA(name, i, syntaxBeginQuote1))
                          || isA(name, i, syntaxBeginQuote2))) {
                // Cita: solo cuenta si abre al **principio** del componente. `start` es lo que
                // hace que la comilla del medio de `a"b` sea un caracter y no una cita.
                beginQuote = one ? syntaxBeginQuote1 : syntaxBeginQuote2;
                endQuote = one ? syntaxEndQuote1 : syntaxEndQuote2;

                for (i += beginQuote.length();
                     (i < len) && !name.startsWith(endQuote, i);
                     i++) {
                    // Adentro de la cita el escape solo significa algo si esta tapando la comilla
                    // de cierre; delante de cualquier otra cosa se copia tal cual.
                    if (isA(name, i, syntaxEscape) && isA(name, i + syntaxEscape.length(), endQuote)) {
                        i += syntaxEscape.length();
                    }
                    answer.append(name.charAt(i));
                }

                if (i >= len) {
                    throw new InvalidNameException(name + ": no close quote");
                }

                i += endQuote.length();

                // Cerrar la cita en el medio del componente es error: si no, `"a"b` seria
                // ambiguo -- el resultado no se podria volver a imprimir.
                if (i == len || isSeparator(name, i)) {
                    break;
                }
                throw new InvalidNameException(name + ": close quote appears before end of component");

            } else if (isSeparator(name, i)) {
                break;

            } else if (isA(name, i, syntaxEscape)) {
                if (isMeta(name, i + syntaxEscape.length())) {
                    // El escape se consume y el metacaracter que sigue entra como texto comun.
                    i += syntaxEscape.length();
                } else if (i + syntaxEscape.length() >= len) {
                    // Un escape colgando al final no puede escapar nada.
                    throw new InvalidNameException(
                        name + ": unescaped " + syntaxEscape + " at end of component");
                }
                // Delante de algo que no es meta, el escape es un caracter mas: cae al append.

            } else if (isA(name, i, syntaxTypevalSeparator)
                       && ((one = isA(name, i + syntaxTypevalSeparator.length(), syntaxBeginQuote1))
                           || isA(name, i + syntaxTypevalSeparator.length(), syntaxBeginQuote2))) {
                // El caso LDAP `cn="Perez, Juan"`: la cita arranca **despues** del `=`, no al
                // principio del componente. Se consume igual que una cita normal, pero las
                // comillas se **conservan** en el resultado: son parte del valor del atributo.
                beginQuote = one ? syntaxBeginQuote1 : syntaxBeginQuote2;
                endQuote = one ? syntaxEndQuote1 : syntaxEndQuote2;

                i += syntaxTypevalSeparator.length();
                answer.append(syntaxTypevalSeparator).append(beginQuote);

                for (i += beginQuote.length();
                     (i < len) && !name.startsWith(endQuote, i);
                     i++) {
                    if (isA(name, i, syntaxEscape) && isA(name, i + syntaxEscape.length(), endQuote)) {
                        i += syntaxEscape.length();
                    }
                    answer.append(name.charAt(i));
                }

                if (i >= len) {
                    throw new InvalidNameException(name + ": typeval no close quote");
                }

                i += endQuote.length();
                answer.append(endQuote);

                if (i == len || isSeparator(name, i)) {
                    break;
                }
                throw new InvalidNameException(
                    name.substring(i) + ": typeval close quote appears before end of component");
            }

            answer.append(name.charAt(i++));
            start = false;
        }

        // De derecha a izquierda el primero que se lee es el ultimo componente.
        if (syntaxDirection == RIGHT_TO_LEFT) {
            comps.insertElementAt(answer.toString(), 0);
        } else {
            comps.addElement(answer.toString());
        }
        return i;
    }

    // ---- armado de la cadena ------------------------------------------------------------------------

    /** Un componente, escapado o citado de manera que `extractComp` lo devuelva igual. */
    private String stringifyComp(String comp) {
        int len = comp.length();
        boolean escapeSeparator = false;
        boolean escapeSeparator2 = false;
        String beginQuote = null;
        String endQuote = null;
        StringBuilder strbuf = new StringBuilder(len);

        // Un separador adentro del componente es lo unico que obliga a hacer algo. Se prefiere
        // citar --es mas legible-- y solo se escapa cuando la sintaxis no tiene comillas.
        if (syntaxSeparator != null && comp.contains(syntaxSeparator)) {
            if (syntaxBeginQuote1 != null) {
                beginQuote = syntaxBeginQuote1;
                endQuote = syntaxEndQuote1;
            } else if (syntaxBeginQuote2 != null) {
                beginQuote = syntaxBeginQuote2;
                endQuote = syntaxEndQuote2;
            } else if (syntaxEscape != null) {
                escapeSeparator = true;
            }
        }
        if (syntaxSeparator2 != null && comp.contains(syntaxSeparator2)) {
            if (syntaxBeginQuote1 != null) {
                if (beginQuote == null) {
                    beginQuote = syntaxBeginQuote1;
                    endQuote = syntaxEndQuote1;
                }
            } else if (syntaxBeginQuote2 != null) {
                if (beginQuote == null) {
                    beginQuote = syntaxBeginQuote2;
                    endQuote = syntaxEndQuote2;
                }
            } else if (syntaxEscape != null) {
                escapeSeparator2 = true;
            }
        }

        if (beginQuote != null) {
            // Citado: adentro de una cita lo unico que hay que tapar es la comilla de cierre.
            strbuf.append(beginQuote);
            for (int i = 0; i < len; ) {
                if (comp.startsWith(endQuote, i)) {
                    strbuf.append(syntaxEscape).append(endQuote);
                    i += endQuote.length();
                } else {
                    strbuf.append(comp.charAt(i++));
                }
            }
            strbuf.append(endQuote);

        } else {
            // Sin citar hay cuatro cosas que tapar, y cada una porque el parseo la leeria como
            // otra cosa: la comilla que abre (solo si esta al principio), el escape que quedaria
            // pegado a un meta, el escape al final, y el separador cuando no hay comillas.
            boolean start = true;
            for (int i = 0; i < len; ) {
                if (start && isA(comp, i, syntaxBeginQuote1)) {
                    strbuf.append(syntaxEscape).append(syntaxBeginQuote1);
                    i += syntaxBeginQuote1.length();
                } else if (start && isA(comp, i, syntaxBeginQuote2)) {
                    strbuf.append(syntaxEscape).append(syntaxBeginQuote2);
                    i += syntaxBeginQuote2.length();
                } else if (isA(comp, i, syntaxEscape)) {
                    if (i + syntaxEscape.length() >= len) {
                        strbuf.append(syntaxEscape);
                    } else if (isMeta(comp, i + syntaxEscape.length())) {
                        strbuf.append(syntaxEscape);
                    }
                    strbuf.append(syntaxEscape);
                    i += syntaxEscape.length();
                } else if (escapeSeparator && comp.startsWith(syntaxSeparator, i)) {
                    strbuf.append(syntaxEscape).append(syntaxSeparator);
                    i += syntaxSeparator.length();
                } else if (escapeSeparator2 && comp.startsWith(syntaxSeparator2, i)) {
                    strbuf.append(syntaxEscape).append(syntaxSeparator2);
                    i += syntaxSeparator2.length();
                } else {
                    strbuf.append(comp.charAt(i++));
                }
                start = false;
            }
        }
        return strbuf.toString();
    }

    @Override
    public String toString() {
        StringBuilder answer = new StringBuilder();
        String comp;
        boolean compsAllEmpty = true;
        int size = components.size();

        for (int i = 0; i < size; i++) {
            if (syntaxDirection == RIGHT_TO_LEFT) {
                comp = stringifyComp(components.elementAt(size - 1 - i));
            } else {
                comp = stringifyComp(components.elementAt(i));
            }
            if ((i != 0) && (syntaxSeparator != null)) {
                answer.append(syntaxSeparator);
            }
            if (comp.length() >= 1) {
                compsAllEmpty = false;
            }
            answer.append(comp);
        }
        // Todo vacio: sin este separador de mas, un nombre de un componente vacio se imprimiria
        // igual que el nombre sin componentes, y el ida y vuelta se rompe.
        if (compsAllEmpty && (size >= 1) && (syntaxSeparator != null)) {
            answer.append(syntaxSeparator);
        }
        return answer.toString();
    }

    // ---- comparacion --------------------------------------------------------------------------------
    //
    // Las cuatro que comparan --`equals`, `compareTo`, `startsWith`, `endsWith`-- normalizan igual
    // y con la sintaxis **de este** nombre, no la del otro. Es asimetrico y es del contrato: el
    // que pregunta pone las reglas.

    private boolean igual(String a, String b) {
        if (syntaxTrimBlanks) {
            a = a.trim();
            b = b.trim();
        }
        return syntaxCaseInsensitive ? a.equalsIgnoreCase(b) : a.equals(b);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NameImpl) {
            NameImpl target = (NameImpl) obj;
            if (target.size() == this.size()) {
                Enumeration<String> mycomps = getAll();
                Enumeration<String> comps = target.getAll();
                while (mycomps.hasMoreElements()) {
                    if (!igual(mycomps.nextElement(), comps.nextElement())) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public int compareTo(NameImpl obj) {
        if (this == obj) {
            return 0;
        }

        int len1 = size();
        int len2 = obj.size();
        int n = Math.min(len1, len2);

        int index1 = 0;
        int index2 = 0;

        while (n-- != 0) {
            String comp1 = get(index1++);
            String comp2 = obj.get(index2++);

            if (syntaxTrimBlanks) {
                comp1 = comp1.trim();
                comp2 = comp2.trim();
            }

            int local = syntaxCaseInsensitive
                ? comp1.compareToIgnoreCase(comp2)
                : comp1.compareTo(comp2);

            if (local != 0) {
                return local;
            }
        }

        // Prefijo comun: gana el mas corto. Devuelve la diferencia de largos, no -1/1.
        return len1 - len2;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (Enumeration<String> e = getAll(); e.hasMoreElements(); ) {
            String comp = e.nextElement();
            if (syntaxTrimBlanks) {
                comp = comp.trim();
            }
            if (syntaxCaseInsensitive) {
                comp = comp.toLowerCase(Locale.ENGLISH);
            }
            // Suma y no el 31*h+x de siempre: tiene que dar igual que `equals`, y `equals` no
            // mira el orden mas alla de componente a componente. Es el hash del JDK real.
            hash += comp.hashCode();
        }
        return hash;
    }

    // ---- acceso -------------------------------------------------------------------------------------

    public int size() {
        return components.size();
    }

    public boolean isEmpty() {
        return components.isEmpty();
    }

    public Enumeration<String> getAll() {
        return components.elements();
    }

    public String get(int posn) {
        return components.elementAt(posn);
    }

    public Enumeration<String> getPrefix(int posn) {
        if (posn < 0 || posn > size()) {
            throw new ArrayIndexOutOfBoundsException(posn);
        }
        return new NameImplEnumerator(components, 0, posn);
    }

    public Enumeration<String> getSuffix(int posn) {
        int cnt = size();
        if (posn < 0 || posn > cnt) {
            throw new ArrayIndexOutOfBoundsException(posn);
        }
        return new NameImplEnumerator(components, posn, cnt);
    }

    /**
     * `posn` es cuantos componentes tiene el prefijo que se prueba.
     *
     * <p>El `catch` no es paranoia: la enumeracion viene del **otro** nombre y puede quedarse sin
     * elementos antes que la nuestra si alguien lo modifico en el medio. Ahi la respuesta correcta
     * es "no empieza asi", no una excepcion.
     */
    public boolean startsWith(int posn, Enumeration<String> prefix) {
        if (posn < 0 || posn > size()) {
            return false;
        }
        try {
            Enumeration<String> mycomps = getPrefix(posn);
            while (mycomps.hasMoreElements()) {
                if (!igual(mycomps.nextElement(), prefix.nextElement())) {
                    return false;
                }
            }
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    public boolean endsWith(int posn, Enumeration<String> suffix) {
        int startIndex = size() - posn;
        if (startIndex < 0 || startIndex > size()) {
            return false;
        }
        try {
            Enumeration<String> mycomps = getSuffix(startIndex);
            while (mycomps.hasMoreElements()) {
                if (!igual(mycomps.nextElement(), suffix.nextElement())) {
                    return false;
                }
            }
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    // ---- modificacion -------------------------------------------------------------------------------
    //
    // El chequeo de plano se hace **por componente** y mirando el tamano actual, asi que un nombre
    // plano vacio acepta uno y recien el segundo falla; y un `addAll` de varios sobre uno vacio
    // agrega el primero y falla en el segundo, dejando el nombre modificado a medias. Es del JDK.

    public boolean addAll(Enumeration<String> comps) throws InvalidNameException {
        boolean added = false;
        while (comps.hasMoreElements()) {
            try {
                String comp = comps.nextElement();
                if (size() > 0 && syntaxDirection == FLAT) {
                    throw new InvalidNameException("A flat name can only have a single component");
                }
                components.addElement(comp);
                added = true;
            } catch (NoSuchElementException e) {
                break;
            }
        }
        return added;
    }

    public boolean addAll(int posn, Enumeration<String> comps) throws InvalidNameException {
        boolean added = false;
        for (int i = posn; comps.hasMoreElements(); i++) {
            try {
                String comp = comps.nextElement();
                if (size() > 0 && syntaxDirection == FLAT) {
                    throw new InvalidNameException("A flat name can only have a single component");
                }
                components.insertElementAt(comp, i);
                added = true;
            } catch (NoSuchElementException e) {
                break;
            }
        }
        return added;
    }

    public void add(String comp) throws InvalidNameException {
        if (size() > 0 && syntaxDirection == FLAT) {
            throw new InvalidNameException("A flat name can only have a single component");
        }
        components.addElement(comp);
    }

    public void add(int posn, String comp) throws InvalidNameException {
        if (size() > 0 && syntaxDirection == FLAT) {
            throw new InvalidNameException("A flat name can only zero or one component");
        }
        components.insertElementAt(comp, posn);
    }

    public Object remove(int posn) {
        Object r = components.elementAt(posn);
        components.removeElementAt(posn);
        return r;
    }
}

/** Una vista de un tramo `[start, lim)` del vector, sin copiarlo. */
final class NameImplEnumerator implements Enumeration<String> {

    Vector<String> vector;
    int count;
    int limit;

    NameImplEnumerator(Vector<String> v, int start, int lim) {
        vector = v;
        count = start;
        limit = lim;
    }

    public boolean hasMoreElements() {
        return count < limit;
    }

    public String nextElement() {
        if (count < limit) {
            return vector.elementAt(count++);
        }
        throw new NoSuchElementException("NameImplEnumerator");
    }
}
