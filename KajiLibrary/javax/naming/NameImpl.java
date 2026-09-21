package javax.naming;

import java.util.Enumeration;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;

/**
 * The naming machinery shared by `CompositeName` and `CompoundName`. It is not public.
 *
 * <h2>Why there is a single class for both</h2>
 *
 * <p>`CompositeName` and `CompoundName` do the same thing with different parameters: a list of
 * components, a parse and a string build governed by a syntax. `CompositeName` fixes that syntax
 * --slash, double quote, single quote, backslash, left to right-- and `CompoundName` receives it in
 * a `Properties`. Writing the logic twice would have guaranteed the two drifting apart; with it
 * here, `CompositeName` is literally `CompoundName` with this class's default syntax, which is
 * exactly what the initial values of the fields below describe.
 *
 * <h2>The three directions</h2>
 *
 * <p>`jndi.syntax.direction` is `left_to_right`, `right_to_left` or `flat`, and it is not cosmetic:
 *
 * <ul>
 *   <li>Left to right, component 0 is the leftmost one in the string.
 *   <li>Right to left --LDAP--, 0 is the **rightmost** one: in `cn=john,o=acme`, `get(0)` is
 *       `o=acme`. That is why parsing inserts at the front and `toString` walks backwards.
 *   <li>Flat has no separator: the whole string is a single component, and adding a second one
 *       fails with `InvalidNameException`.
 * </ul>
 *
 * <h2>The invariant in charge: `toString` must parse back</h2>
 *
 * <p>Everything odd about quoting and escaping comes from upholding that. If a component contains
 * the separator, `stringifyComp` **quotes** it when the syntax has quotes and **escapes** it when
 * not; if it contains a quote at the start it escapes it, because a quote at the start of a
 * component is what **opens** a quote when parsing; and if it contains a backslash before a
 * metacharacter it doubles it, because otherwise parsing would swallow it. In the other direction,
 * `extractComp` undoes exactly that.
 *
 * <p>The rule easiest to lose is the quoting one: a quote only counts if it opens **at the start**
 * of the component, and its close has to fall on a separator or at the end of the string. A quote
 * in the middle is an ordinary character; a quote that closes before the component ends is a
 * syntax error and not an odd component.
 *
 * <h2>Empty components</h2>
 *
 * <p>A trailing separator adds an empty component --`"a/"` is two components-- but only if what
 * comes before is not all empty; that is why `"/"` is **one** empty component and not two. And
 * conversely, a name whose components are all empty is printed with an extra separator, so that
 * `""` (zero components) and `{""}` (one empty) are not confused on the round trip.
 */
class NameImpl {

    private static final byte FLAT = 0;
    private static final byte LEFT_TO_RIGHT = 1;
    private static final byte RIGHT_TO_LEFT = 2;

    private Vector<String> components;

    // The initial values **are** `CompositeName`'s syntax: when the `Properties` is null none is
    // touched and this is what remains. Changing a default here changes `CompositeName`.
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
                // Trailing separator: there is an empty component after it. But only if something
                // that came before was not empty -- otherwise `"/"` would give two empties instead
                // of one, and `toString` could no longer tell `{""}` from `{"", ""}`.
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
        // The components come already split: they are neither parsed nor validated. It is the door
        // through which `getPrefix`/`getSuffix`/`clone` build names without going through the
        // syntax.
        while (comps.hasMoreElements()) {
            components.addElement(comps.nextElement());
        }
    }

    // ---- reading the syntax ---------------------------------------------------------------------

    private void recordNamingConvention(Properties p) {
        String dir = p.getProperty("jndi.syntax.direction", "flat");
        if (dir.equals("left_to_right")) {
            syntaxDirection = LEFT_TO_RIGHT;
        } else if (dir.equals("right_to_left")) {
            syntaxDirection = RIGHT_TO_LEFT;
        } else if (dir.equals("flat")) {
            syntaxDirection = FLAT;
        } else {
            // Deliberately unchecked: a syntax with a made-up direction is a programmer error, not
            // a misspelled name.
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
            // Flat separates nothing, and leaving it null is what makes `toString` add no
            // separators and `isSeparator` always say no.
            syntaxSeparator = null;
        }
        syntaxEscape = p.getProperty("jndi.syntax.escape");

        syntaxCaseInsensitive = getBoolean(p, "jndi.syntax.ignorecase");
        syntaxTrimBlanks = getBoolean(p, "jndi.syntax.trimblanks");

        // Giving only one of the two ends of a quote means it opens and closes the same, which is
        // the normal case (`"`); giving both allows asymmetric quotes like `<`...`>`.
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

        // LDAP's two: `,` between attributes of the same component and `=` between type and value.
        // The second is the only one parsing looks at, and only to let `cn="with,comma"` through.
        syntaxAvaSeparator = p.getProperty("jndi.syntax.separator.ava");
        syntaxTypevalSeparator = p.getProperty("jndi.syntax.separator.typeval");
    }

    private static boolean getBoolean(Properties p, String name) {
        String v = p.getProperty(name);
        return (v != null) && v.toLowerCase(Locale.ENGLISH).equals("true");
    }

    // ---- recognizing metacharacters -------------------------------------------------------------

    /**
     * `true` if `match` is not null and appears in `n` right at `i`. The null check is half the
     * point.
     */
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

    // ---- parsing --------------------------------------------------------------------------------

    /**
     * Extracts a component from `name` starting at `i`, puts it in `comps` and returns where it
     * stopped --on the separator, or at `len`.
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
                // Quote: it only counts if it opens at the **start** of the component. `start` is
                // what makes the quote in the middle of `a"b` a character and not a quote.
                beginQuote = one ? syntaxBeginQuote1 : syntaxBeginQuote2;
                endQuote = one ? syntaxEndQuote1 : syntaxEndQuote2;

                for (i += beginQuote.length();
                     (i < len) && !name.startsWith(endQuote, i);
                     i++) {
                    // Inside the quote the escape only means something if it covers the closing
                    // quote; before anything else it is copied as is.
                    if (isA(name, i, syntaxEscape) && isA(name, i + syntaxEscape.length(), endQuote)) {
                        i += syntaxEscape.length();
                    }
                    answer.append(name.charAt(i));
                }

                if (i >= len) {
                    throw new InvalidNameException(name + ": no close quote");
                }

                i += endQuote.length();

                // Closing the quote in the middle of the component is an error: otherwise `"a"b`
                // would be ambiguous -- the result could not be printed back.
                if (i == len || isSeparator(name, i)) {
                    break;
                }
                throw new InvalidNameException(name + ": close quote appears before end of component");

            } else if (isSeparator(name, i)) {
                break;

            } else if (isA(name, i, syntaxEscape)) {
                if (isMeta(name, i + syntaxEscape.length())) {
                    // The escape is consumed and the metacharacter that follows goes in as plain
                    // text.
                    i += syntaxEscape.length();
                } else if (i + syntaxEscape.length() >= len) {
                    // A dangling escape at the end cannot escape anything.
                    throw new InvalidNameException(
                        name + ": unescaped " + syntaxEscape + " at end of component");
                }
                // Before something that is not a meta, the escape is one more character: falls to
                // append.

            } else if (isA(name, i, syntaxTypevalSeparator)
                       && ((one = isA(name, i + syntaxTypevalSeparator.length(), syntaxBeginQuote1))
                           || isA(name, i + syntaxTypevalSeparator.length(), syntaxBeginQuote2))) {
                // The LDAP case `cn="Smith, John"`: the quote starts **after** the `=`, not at the
                // start of the component. It is consumed like a normal quote, but the quotes are
                // **kept** in the result: they are part of the attribute value.
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

        // Right to left, the first one read is the last component.
        if (syntaxDirection == RIGHT_TO_LEFT) {
            comps.insertElementAt(answer.toString(), 0);
        } else {
            comps.addElement(answer.toString());
        }
        return i;
    }

    // ---- building the string --------------------------------------------------------------------

    /** A component, escaped or quoted so that `extractComp` returns it unchanged. */
    private String stringifyComp(String comp) {
        int len = comp.length();
        boolean escapeSeparator = false;
        boolean escapeSeparator2 = false;
        String beginQuote = null;
        String endQuote = null;
        StringBuilder strbuf = new StringBuilder(len);

        // A separator inside the component is the only thing that forces doing something. Quoting
        // is preferred --it is more readable-- and it only escapes when the syntax has no quotes.
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
            // Quoted: inside a quote the only thing to cover is the closing quote.
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
            // Unquoted there are four things to cover, each one because parsing would read it as
            // something else: the opening quote (only at the start), an escape that would end up
            // next to a meta, the escape at the end, and the separator when there are no quotes.
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
        // All empty: without this extra separator, a name with one empty component would print the
        // same as the name with no components, and the round trip breaks.
        if (compsAllEmpty && (size >= 1) && (syntaxSeparator != null)) {
            answer.append(syntaxSeparator);
        }
        return answer.toString();
    }

    // ---- comparison -----------------------------------------------------------------------------
    //
    // The four that compare --`equals`, `compareTo`, `startsWith`, `endsWith`-- normalize the same
    // way and with **this** name's syntax, not the other's. It is asymmetric and it is part of the
    // contract: whoever asks sets the rules.

    private boolean compEquals(String a, String b) {
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
                    if (!compEquals(mycomps.nextElement(), comps.nextElement())) {
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

        // Common prefix: the shorter one wins. Returns the difference in lengths, not -1/1.
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
            // A sum and not the usual 31*h+x: it must agree with `equals`, and `equals` looks at
            // order no further than component by component. It is the real JDK's hash.
            hash += comp.hashCode();
        }
        return hash;
    }

    // ---- access ---------------------------------------------------------------------------------

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
     * `posn` is how many components the prefix being tested has.
     *
     * <p>The `catch` is not paranoia: the enumeration comes from the **other** name and may run out
     * of elements before ours if someone modified it midway. There the right answer is "it does
     * not start like that", not an exception.
     */
    public boolean startsWith(int posn, Enumeration<String> prefix) {
        if (posn < 0 || posn > size()) {
            return false;
        }
        try {
            Enumeration<String> mycomps = getPrefix(posn);
            while (mycomps.hasMoreElements()) {
                if (!compEquals(mycomps.nextElement(), prefix.nextElement())) {
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
                if (!compEquals(mycomps.nextElement(), suffix.nextElement())) {
                    return false;
                }
            }
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    // ---- modification ---------------------------------------------------------------------------
    //
    // The flat check is done **per component** and against the current size, so an empty flat
    // name accepts one and only the second fails; and an `addAll` of several onto an empty one adds
    // the first and fails on the second, leaving the name half modified. It is the JDK's way.

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

/** A view of a stretch `[start, lim)` of the vector, without copying it. */
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
