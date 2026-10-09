import java.util.Enumeration;
import java.util.Properties;

import javax.naming.Binding;
import javax.naming.BinaryRefAddr;
import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

/**
 * Behaviour test of javax.naming, written to run **the same** in this VM and in the real JDK.
 *
 * <p>Each check has an index. {@code run()} returns -1 if they all passed, or the index of the
 * first one that failed: a single int is enough to compare the two VMs without depending on the
 * console output matching character by character.
 *
 * <p>The bulk is in `CompoundName` and `CompositeName`, which are the only two classes of the
 * package with real logic. What is pursued there is a single invariant --that `toString()` parse
 * back-- and its three uncomfortable consequences: the quoting, the escaping, and the empty
 * components. The rest of the classes are containers and are tested by their equality and their
 * string form.
 *
 * <p>None of this touches the network or the disk: with no installed provider, an `InitialContext`
 * fails straight away, and that is precisely one of the cases.
 */
public class JndiTest {

    // ---- example syntaxes ---------------------------------------------------------------------------

    /** LDAP: comma, from right to left, with quotes and backslash. */
    private static Properties ldap() {
        Properties p = new Properties();
        p.put("jndi.syntax.direction", "right_to_left");
        p.put("jndi.syntax.separator", ",");
        p.put("jndi.syntax.escape", "\\");
        p.put("jndi.syntax.beginquote", "\"");
        return p;
    }

    /** File system style: slash, from left to right. */
    private static Properties fileStyle() {
        Properties p = new Properties();
        p.put("jndi.syntax.direction", "left_to_right");
        p.put("jndi.syntax.separator", "/");
        p.put("jndi.syntax.escape", "\\");
        p.put("jndi.syntax.beginquote", "\"");
        return p;
    }

    /** Flat: the whole string is one component. */
    private static Properties flat() {
        Properties p = new Properties();
        p.put("jndi.syntax.direction", "flat");
        return p;
    }

    /** From left to right, ignoring case and trimming blanks. */
    private static Properties laxStyle() {
        Properties p = new Properties();
        p.put("jndi.syntax.direction", "left_to_right");
        p.put("jndi.syntax.separator", "/");
        p.put("jndi.syntax.ignorecase", "true");
        p.put("jndi.syntax.trimblanks", "true");
        return p;
    }

    private static boolean same(Name n, String[] expected) {
        if (n.size() != expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (!n.get(i).equals(expected[i])) {
                return false;
            }
        }
        return true;
    }

    public static int run() {
        int i = 0;
        try {
            // ---- CompositeName: splitting and counting ---------------------------------------------

            if (!same(new CompositeName("a/b/c"), new String[] {"a", "b", "c"})) return i; i++;   // 0
            if (new CompositeName("").size() != 0) return i; i++;                                  // 1
            if (!new CompositeName().isEmpty()) return i; i++;                                     // 2

            // The empty ones, which is where almost everybody gets it wrong: "/" is ONE empty
            // component, not two.
            if (!same(new CompositeName("/"), new String[] {""})) return i; i++;                   // 3
            if (!same(new CompositeName("a/"), new String[] {"a", ""})) return i; i++;             // 4
            if (!same(new CompositeName("/a"), new String[] {"", "a"})) return i; i++;             // 5
            if (!same(new CompositeName("a//b"), new String[] {"a", "", "b"})) return i; i++;      // 6
            if (!same(new CompositeName("//"), new String[] {"", ""})) return i; i++;              // 7

            // ---- CompositeName: the round-trip invariant -------------------------------------------

            String[] cases = {"", "/", "a", "a/b/c", "a/", "/a", "a//b", "//",
                              "\"a/b\"", "a\\/b", "x y/z"};
            for (int k = 0; k < cases.length; k++) {
                CompositeName n = new CompositeName(cases[k]);
                CompositeName roundTrip = new CompositeName(n.toString());
                if (!n.equals(roundTrip)) return i;
                if (n.size() != roundTrip.size()) return i;
            }
            i++;                                                                                   // 8

            // The empty one and the one with an empty component are NOT printed the same: if they
            // were, the round trip above could not tell them apart.
            if (new CompositeName("").toString().equals(new CompositeName("/").toString())) return i; i++; // 9

            // ---- CompositeName: quoting and escaping ------------------------------------------------

            if (!same(new CompositeName("\"a/b\""), new String[] {"a/b"})) return i; i++;         // 10
            if (!same(new CompositeName("a\\/b"), new String[] {"a/b"})) return i; i++;           // 11
            if (!same(new CompositeName("'a/b'"), new String[] {"a/b"})) return i; i++;           // 12
            // A quote in the middle is an ordinary character: it only opens a quotation at the
            // start.
            if (!same(new CompositeName("a\"b"), new String[] {"a\"b"})) return i; i++;           // 13

            // A component with a separator inside has to come out quoted or escaped, and come
            // back.
            CompositeName withSlash = new CompositeName();
            withSlash.add("a/b");
            if (withSlash.size() != 1) return i; i++;                                               // 14
            if (!same(new CompositeName(withSlash.toString()), new String[] {"a/b"})) return i; i++; // 15

            // ---- CompositeName: prefixes, suffixes and membership -----------------------------------

            CompositeName abc = new CompositeName("a/b/c");
            if (!abc.getPrefix(0).isEmpty()) return i; i++;                                        // 16
            if (!same(abc.getPrefix(2), new String[] {"a", "b"})) return i; i++;                   // 17
            if (!same(abc.getPrefix(3), new String[] {"a", "b", "c"})) return i; i++;              // 18
            if (!same(abc.getSuffix(1), new String[] {"b", "c"})) return i; i++;                   // 19
            if (!abc.getSuffix(3).isEmpty()) return i; i++;                                        // 20

            if (!abc.startsWith(new CompositeName("a/b"))) return i; i++;                          // 21
            if (!abc.startsWith(new CompositeName(""))) return i; i++;                             // 22
            if (abc.startsWith(new CompositeName("b"))) return i; i++;                             // 23
            if (!abc.endsWith(new CompositeName("b/c"))) return i; i++;                            // 24
            if (abc.endsWith(new CompositeName("a"))) return i; i++;                               // 25
            if (!abc.endsWith(new CompositeName(""))) return i; i++;                               // 26
            // A name longer than this one cannot be a prefix or a suffix.
            if (abc.startsWith(new CompositeName("a/b/c/d"))) return i; i++;                       // 27

            // A CompoundName is never a prefix of a CompositeName, whatever they have inside.
            if (abc.startsWith(new CompoundName("a/b", fileStyle()))) return i; i++;                 // 28

            // ---- CompositeName: mutacion ------------------------------------------------------------

            CompositeName m = new CompositeName("a/b");
            // They return `this` already modified: the identity is part of the contract.
            if (m.add("c") != m) return i; i++;                                                    // 29
            if (!same(m, new String[] {"a", "b", "c"})) return i; i++;                             // 30
            m.add(0, "z");
            if (!same(m, new String[] {"z", "a", "b", "c"})) return i; i++;                        // 31
            Object removed = m.remove(1);
            if (!"a".equals(removed)) return i; i++;                                                // 32
            if (!same(m, new String[] {"z", "b", "c"})) return i; i++;                             // 33
            m.addAll(new CompositeName("p/q"));
            if (!same(m, new String[] {"z", "b", "c", "p", "q"})) return i; i++;                   // 34
            m.addAll(1, new CompositeName("w"));
            if (!same(m, new String[] {"z", "w", "b", "c", "p", "q"})) return i; i++;              // 35

            // Sticking on something that is not composite is InvalidNameException, not
            // ClassCastException.
            try {
                new CompositeName("a").addAll(new CompoundName("b", fileStyle()));
                return i;                                                                          // 36
            } catch (InvalidNameException expectedOne) {
                i++;
            }

            // ---- CompositeName: equality, order and clone --------------------------------------------

            if (!new CompositeName("a/b").equals(new CompositeName("a/b"))) return i; i++;         // 37
            if (new CompositeName("a/b").equals(new CompositeName("A/b"))) return i; i++;          // 38
            if (new CompositeName("a/b").hashCode() != new CompositeName("a/b").hashCode()) return i; i++; // 39
            // The same components but another type: they are not equal.
            if (new CompositeName("a/b").equals(new CompoundName("a/b", fileStyle()))) return i; i++; // 40
            if (new CompositeName("a").compareTo(new CompositeName("a")) != 0) return i; i++;      // 41
            if (new CompositeName("a").compareTo(new CompositeName("b")) >= 0) return i; i++;      // 42
            // A common prefix: the shorter one wins.
            if (new CompositeName("a").compareTo(new CompositeName("a/b")) >= 0) return i; i++;    // 43
            try {
                new CompositeName("a").compareTo(new CompoundName("a", fileStyle()));
                return i;                                                                          // 44
            } catch (ClassCastException expectedOne) {
                i++;
            }

            // The clone is independent: mutating the copy does not touch the original.
            CompositeName orig = new CompositeName("a/b");
            CompositeName copy = (CompositeName) orig.clone();
            copy.add("c");
            if (orig.size() != 2) return i; i++;                                                   // 45
            if (copy.size() != 3) return i; i++;                                                  // 46

            // getAll walks in order.
            Enumeration<String> e = new CompositeName("a/b/c").getAll();
            StringBuilder sb = new StringBuilder();
            while (e.hasMoreElements()) sb.append(e.nextElement());
            if (!"abc".equals(sb.toString())) return i; i++;                                       // 47

            // ---- CompoundName: from left to right ------------------------------------------------------

            if (!same(new CompoundName("a/b/c", fileStyle()), new String[] {"a", "b", "c"})) return i; i++; // 48
            if (!"a/b/c".equals(new CompoundName("a/b/c", fileStyle()).toString())) return i; i++;   // 49

            // ---- CompoundName: from right to left, which is what surprises -----------------------------

            CompoundName dn = new CompoundName("cn=juan,o=acme", ldap());
            // Component 0 is the most significant one, that is, the one FURTHEST RIGHT.
            if (!same(dn, new String[] {"o=acme", "cn=juan"})) return i; i++;                     // 50
            if (!"cn=juan,o=acme".equals(dn.toString())) return i; i++;                            // 51
            // The prefix is the most significant ones, which on the string side are on the right.
            if (!"o=acme".equals(dn.getPrefix(1).toString())) return i; i++;                       // 52
            if (!"cn=juan".equals(dn.getSuffix(1).toString())) return i; i++;                      // 53

            CompoundName dn2 = (CompoundName) dn.clone();
            dn2.add("c=ar");
            if (!same(dn2, new String[] {"o=acme", "cn=juan", "c=ar"})) return i; i++;            // 54
            if (!"c=ar,cn=juan,o=acme".equals(dn2.toString())) return i; i++;                      // 55

            dn2.add(0, "dc=raiz");
            if (!"c=ar,cn=juan,o=acme,dc=raiz".equals(dn2.toString())) return i; i++;              // 56

            if (!dn.startsWith(new CompoundName("o=acme", ldap()))) return i; i++;                 // 57
            if (!dn.endsWith(new CompoundName("cn=juan", ldap()))) return i; i++;                  // 58
            if (dn.startsWith(new CompoundName("cn=juan", ldap()))) return i; i++;                 // 59

            // The round trip has to hold from right to left as well.
            if (!dn2.equals(new CompoundName(dn2.toString(), ldap()))) return i; i++;              // 60

            // ---- CompoundName: quoting and escaping with the given syntax -------------------------------

            if (!same(new CompoundName("\"a,b\"", ldap()), new String[] {"a,b"})) return i; i++;  // 61
            if (!same(new CompoundName("a\\,b", ldap()), new String[] {"a,b"})) return i; i++;    // 62

            // A component with the separator inside comes out quoted or escaped, and comes back
            // whole.
            CompoundName withComma = new CompoundName("x", ldap());
            withComma.add("a,b");
            if (withComma.size() != 2) return i; i++;                                                // 63
            CompoundName roundTripComma = new CompoundName(withComma.toString(), ldap());
            if (!roundTripComma.equals(withComma)) return i; i++;                                        // 64
            if (!same(roundTripComma, new String[] {"x", "a,b"})) return i; i++;                      // 65

            // And one that starts with the quote: the quote is escaped, because at the start it
            // opens a quotation.
            CompoundName withQuote = new CompoundName("", ldap());
            CompoundName cc = new CompoundName("z", ldap());
            cc.add("\"raro");
            if (!same(new CompoundName(cc.toString(), ldap()), new String[] {"z", "\"raro"})) return i; i++; // 66

            // And one with a backslash, which if it is not doubled gets eaten by the parsing.
            CompoundName cb = new CompoundName("z", ldap());
            cb.add("a\\b");
            if (!same(new CompoundName(cb.toString(), ldap()), new String[] {"z", "a\\b"})) return i; i++;   // 67

            // ---- CompoundName: flat ------------------------------------------------------------------

            CompoundName pl = new CompoundName("a/b/c", flat());
            if (!same(pl, new String[] {"a/b/c"})) return i; i++;                                 // 68
            if (!"a/b/c".equals(pl.toString())) return i; i++;                                     // 69
            // A flat name cannot have two components.
            try {
                pl.add("x");
                return i;                                                                          // 70
            } catch (InvalidNameException expectedOne) {
                i++;
            }
            // But an empty one does accept the first.
            CompoundName pv = new CompoundName("", flat());
            pv.add("unico");
            if (!same(pv, new String[] {"unico"})) return i; i++;                                 // 71

            // ---- CompoundName: ignoring case and trimming blanks -----------------------------------------

            CompoundName lax = new CompoundName("A/B", laxStyle());
            if (!lax.equals(new CompoundName("a/b", laxStyle()))) return i; i++;                       // 72
            if (lax.hashCode() != new CompoundName("a/b", laxStyle()).hashCode()) return i; i++;       // 73
            if (!new CompoundName(" a / b ", laxStyle()).equals(new CompoundName("a/b", laxStyle()))) return i; i++; // 74
            if (new CompoundName("A/B", laxStyle()).compareTo(new CompoundName("a/b", laxStyle())) != 0) return i; i++; // 75
            // And the asymmetry of the contract: the syntax that rules is that of whoever asks.
            if (!lax.startsWith(new CompoundName("a", laxStyle()))) return i; i++;                     // 76

            // ---- CompoundName: compulsory syntax and types -----------------------------------------------

            try {
                new CompoundName("a", null);
                return i;                                                                          // 77
            } catch (NullPointerException expectedOne) {
                i++;
            }
            try {
                new CompoundName("a", fileStyle()).addAll(new CompositeName("b"));
                return i;                                                                          // 78
            } catch (InvalidNameException expectedOne) {
                i++;
            }
            if (new CompoundName("a", fileStyle()).startsWith(new CompositeName("a"))) return i; i++; // 79

            // The clone shares the syntax, so it goes on being comparable with the original.
            CompoundName cl = (CompoundName) new CompoundName("a/b", fileStyle()).clone();
            if (!cl.equals(new CompoundName("a/b", fileStyle()))) return i; i++;                     // 80

            // ---- NameClassPair and Binding ---------------------------------------------------------------

            NameClassPair ncp = new NameClassPair("juan", "java.lang.String");
            if (!"juan".equals(ncp.getName())) return i; i++;                                      // 81
            if (!"java.lang.String".equals(ncp.getClassName())) return i; i++;                     // 82
            if (!ncp.isRelative()) return i; i++;                                                  // 83
            if (!"juan: java.lang.String".equals(ncp.toString())) return i; i++;                   // 84

            NameClassPair abs = new NameClassPair("ldap://h/x", "java.lang.Object", false);
            if (abs.isRelative()) return i; i++;                                                   // 85
            if (!abs.toString().startsWith("(not relative)")) return i; i++;                       // 86

            // The absolute name is optional: without setting it, it throws instead of returning
            // null.
            try {
                ncp.getNameInNamespace();
                return i;                                                                          // 87
            } catch (UnsupportedOperationException expectedOne) {
                i++;
            }
            ncp.setNameInNamespace("ou=gente,o=acme");
            if (!"ou=gente,o=acme".equals(ncp.getNameInNamespace())) return i; i++;                // 88

            // Binding deduces the class name from the object when it was not declared.
            Binding b = new Binding("x", "hola");
            if (!"java.lang.String".equals(b.getClassName())) return i; i++;                       // 89
            if (!"hola".equals(b.getObject())) return i; i++;                                      // 90
            // But the declared one wins over the deduced one.
            Binding b2 = new Binding("x", "com.ejemplo.Falso", "hola");
            if (!"com.ejemplo.Falso".equals(b2.getClassName())) return i; i++;                     // 91
            // And with neither of the two things it is null, not an exception.
            if (new Binding("x", null).getClassName() != null) return i; i++;                      // 92
            if (!"x: java.lang.String:hola".equals(b.toString())) return i; i++;                   // 93

            // ---- RefAddr and its two forms ---------------------------------------------------------------

            StringRefAddr sa = new StringRefAddr("URL", "ldap://h/");
            if (!"URL".equals(sa.getType())) return i; i++;                                        // 94
            if (!"ldap://h/".equals(sa.getContent())) return i; i++;                               // 95
            if (!sa.equals(new StringRefAddr("URL", "ldap://h/"))) return i; i++;                  // 96
            if (sa.equals(new StringRefAddr("OTRO", "ldap://h/"))) return i; i++;                  // 97
            if (sa.hashCode() != new StringRefAddr("URL", "ldap://h/").hashCode()) return i; i++;  // 98
            // Null contents: the hash is that of the type and it does not blow up.
            if (new StringRefAddr("URL", null).hashCode() != "URL".hashCode()) return i; i++;      // 99
            if (new StringRefAddr("URL", null).equals(sa)) return i; i++;                          // 100

            // The binary one compares byte by byte, not by identity of the array.
            byte[] bytes = {1, 2, 3};
            BinaryRefAddr ba = new BinaryRefAddr("bin", bytes);
            if (!ba.equals(new BinaryRefAddr("bin", new byte[] {1, 2, 3}))) return i; i++;         // 101
            if (ba.equals(new BinaryRefAddr("bin", new byte[] {1, 2}))) return i; i++;             // 102
            if (ba.hashCode() != new BinaryRefAddr("bin", new byte[] {1, 2, 3}).hashCode()) return i; i++; // 103
            // And the constructor copies: changing the array of whoever called does not change the
            // address.
            bytes[0] = 9;
            if (!ba.equals(new BinaryRefAddr("bin", new byte[] {1, 2, 3}))) return i; i++;         // 104
            // The range one copies only the stretch asked for.
            BinaryRefAddr br = new BinaryRefAddr("bin", new byte[] {0, 1, 2, 3, 4}, 1, 3);
            if (!br.equals(new BinaryRefAddr("bin", new byte[] {1, 2, 3}))) return i; i++;         // 105
            // A binary one and a text one are never equal even if they share a type.
            if (ba.equals(new StringRefAddr("bin", "123"))) return i; i++;                         // 106

            // ---- Reference ------------------------------------------------------------------------------

            Reference r = new Reference("com.ejemplo.Ds", "com.ejemplo.DsFactory", null);
            if (!"com.ejemplo.Ds".equals(r.getClassName())) return i; i++;                         // 107
            if (!"com.ejemplo.DsFactory".equals(r.getFactoryClassName())) return i; i++;           // 108
            if (r.getFactoryClassLocation() != null) return i; i++;                                // 109
            if (r.size() != 0) return i; i++;                                                      // 110

            r.add(new StringRefAddr("URL", "uno"));
            r.add(new StringRefAddr("URL", "dos"));
            r.add(new StringRefAddr("user", "juan"));
            if (r.size() != 3) return i; i++;                                                      // 111
            // get(String) returns the FIRST of that type: the order is the preference.
            if (!"uno".equals(r.get("URL").getContent())) return i; i++;                           // 112
            if (!"juan".equals(r.get("user").getContent())) return i; i++;                         // 113
            if (r.get("nada") != null) return i; i++;                                              // 114
            if (!"dos".equals(r.get(1).getContent())) return i; i++;                               // 115

            r.add(0, new StringRefAddr("URL", "cero"));
            if (!"cero".equals(r.get("URL").getContent())) return i; i++;                          // 116
            Object quitada = r.remove(0);
            if (!(quitada instanceof RefAddr)) return i; i++;                                      // 117
            if (!"uno".equals(r.get("URL").getContent())) return i; i++;                           // 118

            // The equality looks at class and addresses IN ORDER, and on purpose ignores the
            // factory.
            Reference r2 = new Reference("com.ejemplo.Ds", "OTRA.Fabrica", "http://x/");
            r2.add(new StringRefAddr("URL", "uno"));
            r2.add(new StringRefAddr("URL", "dos"));
            r2.add(new StringRefAddr("user", "juan"));
            if (!r.equals(r2)) return i; i++;                                                      // 119
            if (r.hashCode() != r2.hashCode()) return i; i++;                                      // 120
            // But the order does count.
            Reference r3 = new Reference("com.ejemplo.Ds");
            r3.add(new StringRefAddr("URL", "dos"));
            r3.add(new StringRefAddr("URL", "uno"));
            r3.add(new StringRefAddr("user", "juan"));
            if (r.equals(r3)) return i; i++;                                                       // 121
            // And so does the class.
            if (r.equals(new Reference("otra.Clase"))) return i; i++;                              // 122

            // The clone has a list of its own: adding an address to it does not touch the
            // original.
            Reference rc = (Reference) r.clone();
            if (!rc.equals(r)) return i; i++;                                                      // 123
            rc.add(new StringRefAddr("extra", "x"));
            if (r.size() != 3) return i; i++;                                                      // 124
            if (rc.size() != 4) return i; i++;                                                     // 125
            // And it keeps the factory, which equals does not look at but clone does copy.
            if (!"com.ejemplo.DsFactory".equals(rc.getFactoryClassName())) return i; i++;          // 126

            r.clear();
            if (r.size() != 0) return i; i++;                                                      // 127

            // ---- LinkRef --------------------------------------------------------------------------------

            LinkRef lr = new LinkRef("a/b");
            if (!"a/b".equals(lr.getLinkName())) return i; i++;                                    // 128
            if (!"javax.naming.LinkRef".equals(lr.getClassName())) return i; i++;                  // 129
            if (lr.size() != 1) return i; i++;                                                     // 130
            if (!"LinkAddress".equals(lr.get(0).getType())) return i; i++;                         // 131
            // The Name constructor uses the string form of the name.
            if (!"a/b".equals(new LinkRef(new CompositeName("a/b")).getLinkName())) return i; i++; // 132
            // If the address is taken away it stops being a link, and it says so instead of
            // throwing an NPE.
            LinkRef broken = new LinkRef("a/b");
            broken.clear();
            try {
                broken.getLinkName();
                return i;                                                                          // 133
            } catch (NamingException expectedOne) {
                i++;
            }

            // ---- NamingException: the state it accumulates -----------------------------------------------

            NamingException ne = new NamingException("fallo");
            if (!"fallo".equals(ne.getExplanation())) return i; i++;                               // 134
            if (ne.getResolvedName() != null) return i; i++;                                       // 135

            // The name setters CLONE: the name is mutable and the exception has travelled
            // already.
            CompositeName resolved = new CompositeName("a/b");
            ne.setResolvedName(resolved);
            resolved.add("c");
            if (ne.getResolvedName().size() != 2) return i; i++;                                   // 136

            // appendRemainingComponent accumulates while the exception goes up.
            NamingException ne2 = new NamingException("x");
            ne2.appendRemainingComponent("c");
            ne2.appendRemainingComponent("d");
            if (!same(ne2.getRemainingName(), new String[] {"c", "d"})) return i; i++;            // 137
            ne2.appendRemainingName(new CompositeName("e/f"));
            if (!same(ne2.getRemainingName(), new String[] {"c", "d", "e", "f"})) return i; i++;  // 138

            // getCause and getRootCause are two names for the same thing.
            NamingException ne3 = new NamingException("y");
            Exception cause = new IllegalStateException("causa");
            ne3.setRootCause(cause);
            if (ne3.getRootCause() != cause) return i; i++;                                        // 139
            if (ne3.getCause() != cause) return i; i++;                                            // 140
            // But the asymmetry is real: setRootCause does not touch the cause of Throwable.
            NamingException ne4 = new NamingException("z");
            ne4.initCause(cause);
            if (ne4.getRootCause() != cause) return i; i++;                                        // 141
            // And one caused by itself is not linked, so that printing it does not hang.
            NamingException ne5 = new NamingException("w");
            ne5.setRootCause(ne5);
            if (ne5.getRootCause() != null) return i; i++;                                         // 142

            // ---- The exception hierarchy, which is half the package ---------------------------------------

            if (!(new NoInitialContextException() instanceof NamingException)) return i; i++;      // 143
            if (!(new javax.naming.NameNotFoundException() instanceof NamingException)) return i; i++; // 144
            if (!(new javax.naming.NoPermissionException()
                    instanceof javax.naming.NamingSecurityException)) return i; i++;               // 145
            if (!(new javax.naming.SizeLimitExceededException()
                    instanceof javax.naming.LimitExceededException)) return i; i++;                // 146
            if (!(new javax.naming.MalformedLinkException()
                    instanceof javax.naming.LinkException)) return i; i++;                         // 147
            if (!(new javax.naming.CannotProceedException() instanceof NamingException)) return i; i++; // 148

            // LinkException carries its own pair of names, apart from those of the context.
            javax.naming.LinkException le = new javax.naming.LinkException("link");
            le.setLinkResolvedName(new CompositeName("a"));
            le.setLinkRemainingName(new CompositeName("b/c"));
            if (!same(le.getLinkResolvedName(), new String[] {"a"})) return i; i++;               // 149
            if (!same(le.getLinkRemainingName(), new String[] {"b", "c"})) return i; i++;         // 150
            if (le.getResolvedName() != null) return i; i++;                                       // 151

            // ---- InitialContext with no provider ---------------------------------------------------------
            //
            // With `java.naming.factory.initial` not set, EVERY operation fails with
            // NoInitialContextException. It is not a hole of this implementation: it is what the
            // real JDK does, and it is declared in the signature.

            InitialContext ic = new InitialContext();
            try {
                ic.lookup("cualquiera");
                return i;                                                                          // 152
            } catch (NoInitialContextException expectedOne) {
                i++;
            }
            try {
                ic.bind("x", "y");
                return i;                                                                          // 153
            } catch (NoInitialContextException expectedOne) {
                i++;
            }
            try {
                ic.list(new CompositeName("x"));
                return i;                                                                          // 154
            } catch (NoInitialContextException expectedOne) {
                i++;
            }
            try {
                ic.getEnvironment();
                return i;                                                                          // 155
            } catch (NoInitialContextException expectedOne) {
                i++;
            }
            try {
                InitialContext.doLookup("x");
                return i;                                                                          // 156
            } catch (NoInitialContextException expectedOne) {
                i++;
            }

            // composeName does work, because it needs no provider: the initial context is the
            // origin.
            if (!"a/b".equals(ic.composeName("a/b", ""))) return i; i++;                           // 157
            Name composite = ic.composeName(new CompositeName("a/b"), new CompositeName(""));
            if (!same(composite, new String[] {"a", "b"})) return i; i++;                         // 158

            // Closing with no provider does not fail, and closing twice does not either.
            ic.close();
            ic.close();
            i++;                                                                                   // 159

            // Naming a factory that does not exist fails in the constructor, not three calls
            // later.
            java.util.Hashtable<Object, Object> env = new java.util.Hashtable<Object, Object>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "no.existe.Fabrica");
            try {
                new InitialContext(env);
                return i;                                                                          // 160
            } catch (NamingException expectedOne) {
                i++;
            }

            // The constants of Context are the real keys of the environment.
            if (!"java.naming.factory.initial".equals(Context.INITIAL_CONTEXT_FACTORY)) return i; i++; // 161
            if (!"java.naming.provider.url".equals(Context.PROVIDER_URL)) return i; i++;           // 162

            return -1;
        } catch (NamingException ex) {
            return i;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
