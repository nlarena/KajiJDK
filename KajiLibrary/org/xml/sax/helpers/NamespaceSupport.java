package org.xml.sax.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// KajiLibrary's org.xml.sax.helpers.NamespaceSupport -- the stack that turns `foo:bar` into a
// (URI, local name) pair.
//
// XML namespaces have per-element scope: a prefix declared in <a> is seen inside <a> and
// disappears after </a>, and an inner element can redeclare the same prefix to mean something
// else. So the state is a stack of contexts, one per open element, and the caller is the one who
// drives it:
//
//     support.pushContext();                       // entering an element
//     for each attribute xmlns:p="u":
//         support.declarePrefix("p", "u");
//     support.processName(qName, parts, false);    // the name of the element itself
//     ... children ...
//     support.popContext();                        // leaving it
//
// The order matters: each declarePrefix of an element has to happen after its pushContext and
// before the first processName, because processName memoises its answers per context and a
// declaration that arrives later may not be seen by the names already resolved. Nothing enforces
// this -- the JDK once had a declsOK flag that threw IllegalStateException and left it commented
// out in the source it ships, so whoever declares late simply receives stale answers. This library
// does not add the check back: it would throw where the JDK returns, which is a louder lie than the
// silence.
//
// The rules that are easy to get wrong, all respected here:
//
//   - The default prefix ("") applies *only to elements*. An attribute with no prefix is in no
//     namespace, never in the default one, so processName("id", parts, true) gives a URI of ""
//     even with a default namespace in force. This is the most common misreading of the standard.
//   - "xml" comes predeclared, permanently, to http://www.w3.org/XML/1998/namespace. It is bound
//     before the caller has a chance to speak, and reset() puts it back.
//   - declarePrefix rejects "xml" and "xmlns" by returning false. It does not throw: rejecting them
//     is a normal result, and a parser is expected to treat that false as "that was not a
//     namespace declaration".
//   - An unbound prefix is not an error here, it is a null processName returns. Deciding what to do
//     with that is the caller's business.
//
// setNamespaceDeclUris(true) switches on the optional behaviour where the xmlns attributes are in
// turn reported inside a namespace (NSDECL) instead of being invisible. It can only be called with
// no element open, that is before the first pushContext or after its matching popContext;
// otherwise the contexts already built would not match the ones that come later.
//
// The implementation detail that makes this cheap: a context that declares nothing does not copy
// the tables of its parent, it shares them. The copy happens on the first declarePrefix, which for
// real documents is a small minority of the elements. That is why Context has both a `parent` and
// a `declSeen` flag, and why clear() takes the trouble of letting go of the references -- a
// context that was popped off the stack is reused on the next push at that depth, and keeping the
// old tables would keep them alive.
public class NamespaceSupport {

    ////////////////////////////////////////////////////////////////////
    // Constants
    ////////////////////////////////////////////////////////////////////

    // The URI bound to the prefix "xml", always, by the standard itself.
    public static final String XMLNS =
        "http://www.w3.org/XML/1998/namespace";

    // The URI of the xmlns attributes when setNamespaceDeclUris(true) is in force.
    public static final String NSDECL =
        "http://www.w3.org/xmlns/2000/";

    private static final Enumeration<String> EMPTY_ENUMERATION =
        Collections.enumeration(new ArrayList<String>());

    // Whether String.intern() works on the VM we are running on. The JDK's NamespaceSupport interns
    // every prefix, URI and name it keeps; that is not part of the SAX contract (nothing documents
    // these strings as interned) but it is the JDK's behaviour, and on a VM that can do it we do it
    // too. On one that cannot, canon() returns the string untouched: the maps go by equals(), so
    // every answer this class gives is identical in either case -- the only thing that changes is
    // the reference identity of the strings returned, and nothing here nor in ParserAdapter depends
    // on that.
    //
    // The note said that on KajiJDK's own VM intern() is declared but not implemented, so calling
    // it throws UnsatisfiedLinkError. It is implemented now (the VM's natives register
    // java/lang/String.intern), so the probe answers true and the interning path is the one that
    // runs. The probe stays for any other VM.
    //
    // It is tested once instead of catching the failure on every call, because this sits in the
    // middle of the hottest loop of namespace processing.
    private static final boolean CAN_INTERN = probeIntern();

    private static boolean probeIntern() {
        try {
            String s = "";
            return s.intern() != null;
        } catch (Throwable e) {
            return false;
        }
    }

    private static String canon(String s) {
        if (CAN_INTERN) {
            return s.intern();
        }
        return s;
    }

    ////////////////////////////////////////////////////////////////////
    // State
    ////////////////////////////////////////////////////////////////////

    private Context[] contexts;
    private Context currentContext;
    private int contextPos;
    private boolean namespaceDeclUris;

    // It starts reset, that is with a context that has the predeclared binding of "xml".
    public NamespaceSupport() {
        reset();
    }

    // Back to the initial state, ready for another document. It throws away all the contexts and
    // puts back the binding of "xml"; it also clears namespaceDeclUris.
    public void reset() {
        contexts = new Context[32];
        namespaceDeclUris = false;
        contextPos = 0;
        // Split in two. The note said `contexts[i] = currentContext = new Context()` miscompiles on
        // the house javac; see the comment in pushContext().
        currentContext = new Context();
        contexts[contextPos] = currentContext;
        currentContext.declarePrefix("xml", XMLNS);
    }

    ////////////////////////////////////////////////////////////////////
    // The stack
    ////////////////////////////////////////////////////////////////////

    // Entering an element.
    public void pushContext() {
        int max = contexts.length;

        contextPos++;

        // It grows if we run out of room. Deep documents are rare; 32 covers almost everything.
        if (contextPos >= max) {
            Context newContexts[] = new Context[max * 2];
            System.arraycopy(contexts, 0, newContexts, 0, max);
            contexts = newContexts;
        }

        // Reuse the Context object a previous sibling left at this depth, if there is one.
        currentContext = contexts[contextPos];
        if (currentContext == null) {
            // Split in two. The note said the house javac generated wrong bytecode for `array[i] =
            // instanceField = value` -- a badly built stack, with the aastore finding an int where
            // a reference has to be. Checked 2026-09-18: the frozen javac emits dup_x1/putfield/
            // aastore for exactly this shape and the JDK 25 verifier accepts it, so the bug is
            // gone. The split form is equivalent and is kept.
            currentContext = new Context();
            contexts[contextPos] = currentContext;
        }

        if (contextPos > 0) {
            currentContext.setParent(contexts[contextPos - 1]);
        }
    }

    // Leaving an element. The Context object stays in the array to be reused, but its tables are
    // let go so that nothing outlives the element.
    public void popContext() {
        contexts[contextPos].clear();

        contextPos--;
        if (contextPos < 0) {
            throw new EmptyStackException();
        }
        currentContext = contexts[contextPos];
    }

    ////////////////////////////////////////////////////////////////////
    // Declarations
    ////////////////////////////////////////////////////////////////////

    // It binds `prefix` to `uri` for the current element and its children. An empty prefix sets the
    // default namespace, and an empty uri turns it off (which is how xmlns="" works).
    //
    // It returns false, declaring nothing, for "xml" and "xmlns": the first is already bound and
    // cannot be rebound, the second is not a prefix at all.
    public boolean declarePrefix(String prefix, String uri) {
        if (prefix.equals("xml") || prefix.equals("xmlns")) {
            return false;
        } else {
            currentContext.declarePrefix(prefix, uri);
            return true;
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Resolution
    ////////////////////////////////////////////////////////////////////

    // It splits a qualified name into (URI, local name, qName) and writes the three into `parts`,
    // which has to have room for three. It returns `parts` when it works and null when the prefix
    // of the name is not bound.
    //
    // `isAttribute` is not cosmetic: an *element* name with no prefix takes the default namespace,
    // an *attribute* name with no prefix takes none. See the comment of the class.
    public String[] processName(String qName, String[] parts,
                                boolean isAttribute) {
        String[] myParts = currentContext.processName(qName, isAttribute);
        if (myParts == null) {
            return null;
        } else {
            parts[0] = myParts[0];
            parts[1] = myParts[1];
            parts[2] = myParts[2];
            return parts;
        }
    }

    // The URI this prefix means at this moment, or null if it means nothing. "" asks for the
    // default namespace.
    public String getURI(String prefix) {
        return currentContext.getURI(prefix);
    }

    // All the prefixes in scope at this moment, *except* the default one and except "xml". The
    // default one is left out because it is not a prefix and getURI("") already answers for it;
    // "xml" is left out only in the sense that it was declared in the root context and therefore it
    // does appear -- whoever cares about that filters it on their own.
    public Enumeration<String> getPrefixes() {
        return currentContext.getPrefixes();
    }

    // A prefix bound to this URI, or null. Which one, when several are bound to the same URI, is
    // not specified; the default prefix is never returned, because whoever asks this question wants
    // something they can put in front of a colon.
    public String getPrefix(String uri) {
        return currentContext.getPrefix(uri);
    }

    // All the prefixes bound to this URI, unlike the single answer of getPrefix.
    public Enumeration<String> getPrefixes(String uri) {
        List<String> prefixes = new ArrayList<String>();
        Enumeration<String> allPrefixes = getPrefixes();
        while (allPrefixes.hasMoreElements()) {
            String prefix = allPrefixes.nextElement();
            if (uri.equals(getURI(prefix))) {
                prefixes.add(prefix);
            }
        }
        return Collections.enumeration(prefixes);
    }

    // The prefixes declared *by this element itself*, not the inherited ones. This is what a parser
    // reports through startPrefixMapping/endPrefixMapping.
    public Enumeration<String> getDeclaredPrefixes() {
        return currentContext.getDeclaredPrefixes();
    }

    ////////////////////////////////////////////////////////////////////
    // The optional xmlns-inside-a-namespace mode
    ////////////////////////////////////////////////////////////////////

    // It can only be called between documents, that is with no element open. Changing it in the
    // middle of the analysis would make the contexts already built not match the ones that come
    // later, so it throws.
    public void setNamespaceDeclUris(boolean value) {
        if (contextPos != 0) {
            throw new IllegalStateException();
        }
        if (value == namespaceDeclUris) {
            return;
        }
        namespaceDeclUris = value;
        if (value) {
            currentContext.declarePrefix("xmlns", NSDECL);
        } else {
            // Switching it off has to let go of the xmlns binding, and the cheapest correct way is
            // a new root context with only "xml" inside. Split in two as in pushContext().
            currentContext = new Context();
            contexts[contextPos] = currentContext;
            currentContext.declarePrefix("xml", XMLNS);
        }
    }

    public boolean isNamespaceDeclUris() {
        return namespaceDeclUris;
    }

    ////////////////////////////////////////////////////////////////////
    // The bindings that belong to an element.
    ////////////////////////////////////////////////////////////////////

    // Inner and not static because processName has to consult the namespaceDeclUris flag of the
    // enclosing NamespaceSupport.
    //
    // The two name tables are memoisation caches: inside one same element, the same qName always
    // resolves to the same triple, and elements repeat attribute names all the time. They are
    // separate for elements and attributes precisely because the two resolve differently.
    final class Context {

        Map<String, String> prefixTable;
        Map<String, String> uriTable;
        Map<String, String[]> elementNameTable;
        Map<String, String[]> attributeNameTable;
        String defaultNS = null;
        boolean declSeen = false;

        private List<String> declarations = null;
        private Context parent = null;

        Context() {
            copyTables();
        }

        // Reuse this object for a new element under `parent`. It shares the tables of the parent
        // instead of copying them; the copy happens in declarePrefix, and only if it is ever
        // called.
        void setParent(Context parent) {
            this.parent = parent;
            declarations = null;
            prefixTable = parent.prefixTable;
            uriTable = parent.uriTable;
            elementNameTable = parent.elementNameTable;
            attributeNameTable = parent.attributeNameTable;
            defaultNS = parent.defaultNS;
            declSeen = false;
        }

        // Let go of everything on leaving. The object survives in the contexts array to be reused,
        // but it must not leave the tables of the popped element reachable.
        void clear() {
            parent = null;
            prefixTable = null;
            uriTable = null;
            elementNameTable = null;
            attributeNameTable = null;
            defaultNS = null;
        }

        void declarePrefix(String prefix, String uri) {
            // First declaration in this element: stop sharing the tables of the parent.
            if (!declSeen) {
                copyTables();
            }
            if (declarations == null) {
                declarations = new ArrayList<String>();
            }

            // Interned so that the cached triples of processName can be compared and shared
            // cheaply, and so that the strings handed to the caller are the canonical ones.
            prefix = canon(prefix);
            uri = canon(uri);
            if ("".equals(prefix)) {
                // xmlns="" turns off the default namespace instead of binding it to "".
                if ("".equals(uri)) {
                    defaultNS = null;
                } else {
                    defaultNS = uri;
                }
            } else {
                prefixTable.put(prefix, uri);
                uriTable.put(uri, prefix);
            }
            declarations.add(prefix);
        }

        String[] processName(String qName, boolean isAttribute) {
            Map<String, String[]> table;

            // The division between element and attribute, which is the whole reason for there being
            // two tables.
            if (isAttribute) {
                table = attributeNameTable;
            } else {
                table = elementNameTable;
            }

            String[] name = table.get(qName);
            if (name != null) {
                return name;
            }

            name = new String[3];
            name[2] = canon(qName);
            int index = qName.indexOf(':');

            // No colon: a name with no prefix.
            if (index == -1) {
                if (isAttribute) {
                    // An attribute with no prefix is in no namespace -- never in the default one.
                    // The only exception is the xmlns attribute itself, and only when the caller
                    // asked for the xmlns attributes to be given a URI.
                    //
                    // The `==` is not a slip nor an equals() in disguise: here the JDK compares by
                    // identity, so an "xmlns" that is not the interned literal --one built at run
                    // time, say-- falls to "" even with the functionality switched on. Checked
                    // against jdk-25.0.2: processName(new
                    // StringBuilder("xml").append("ns").toString(), p, true) answers "" there, and
                    // answers "" here. Written as equals() this library would not match the JDK on
                    // that input, so identity it is. Parsers pass this method names that came from
                    // the document, and a parser that interns them (something ParserAdapter does
                    // not need to do, because it filters out the xmlns attributes before even
                    // asking) is the only caller for which this branch fires.
                    if (qName == "xmlns" && namespaceDeclUris) {
                        name[0] = NSDECL;
                    } else {
                        name[0] = "";
                    }
                } else if (defaultNS == null) {
                    name[0] = "";
                } else {
                    name[0] = defaultNS;
                }
                name[1] = name[2];
            }

            // A name with a prefix.
            else {
                String prefix = qName.substring(0, index);
                String local = qName.substring(index + 1);
                String uri;
                if ("".equals(prefix)) {
                    uri = defaultNS;
                } else {
                    uri = prefixTable.get(prefix);
                }
                // An unbound prefix, or an xmlns:* used where an element name was expected, is a
                // null answer and not an exception: what to do with that is the caller's decision,
                // not ours.
                if (uri == null
                        || (!isAttribute && "xmlns".equals(prefix))) {
                    return null;
                }
                name[0] = uri;
                name[1] = canon(local);
            }

            table.put(name[2], name);
            return name;
        }

        String getURI(String prefix) {
            if ("".equals(prefix)) {
                return defaultNS;
            } else if (prefixTable == null) {
                return null;
            } else {
                return prefixTable.get(prefix);
            }
        }

        String getPrefix(String uri) {
            if (uriTable == null) {
                return null;
            } else {
                return uriTable.get(uri);
            }
        }

        Enumeration<String> getDeclaredPrefixes() {
            if (declarations == null) {
                return EMPTY_ENUMERATION;
            } else {
                return Collections.enumeration(declarations);
            }
        }

        Enumeration<String> getPrefixes() {
            if (prefixTable == null) {
                return EMPTY_ENUMERATION;
            } else {
                return Collections.enumeration(prefixTable.keySet());
            }
        }

        // Take private copies of what we were sharing with the parent. The two name caches are
        // *not* copied but start empty: they memoise answers that depend on the bindings, and the
        // bindings are about to change.
        private void copyTables() {
            if (prefixTable != null) {
                prefixTable = new HashMap<String, String>(prefixTable);
            } else {
                prefixTable = new HashMap<String, String>();
            }
            if (uriTable != null) {
                uriTable = new HashMap<String, String>(uriTable);
            } else {
                uriTable = new HashMap<String, String>();
            }
            elementNameTable = new HashMap<String, String[]>();
            attributeNameTable = new HashMap<String, String[]>();
            declSeen = true;
        }
    }
}
