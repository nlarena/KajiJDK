package javax.xml.crypto.dsig.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.ExcC14NParameterSpec -- which prefixes an exclusive
 * canonicalization drags along.
 *
 * <p><b>Exclusive</b> canonicalization exists so that a signed fragment keeps validating if it is
 * moved to another document. The inclusive one drags along all the namespace declarations that are
 * in the context, even if the fragment does not use them; the exclusive one drags only the ones it
 * uses. That is why moving the fragment does not change its canonical form, and the signature still
 * checks out.
 *
 * <p>This list is the exception to that rule: the prefixes that have to be dragged along
 * <b>anyway</b>, even if they are not used in names. It is needed when a prefix appears inside the
 * <b>content</b> --in an attribute, in an XPath value-- where canonicalization does not see it as a
 * use.
 *
 * <p>Forgetting one is the classic cause of a signature that validates where it was created and
 * fails at the recipient.
 *
 * <p>{@link #DEFAULT} names the default prefix, which has no name and therefore needs a marker.
 */
public final class ExcC14NParameterSpec implements C14NMethodParameterSpec {

    /** The default prefix, which has no name of its own. */
    public static final String DEFAULT = "#default";

    /** The prefixes to drag along; never null, and unmodifiable. */
    private final List<String> prefixList;

    /** Without extra prefixes: only what is used is dragged along. */
    public ExcC14NParameterSpec() {
        this.prefixList = Collections.emptyList();
    }

    /**
     * With the list of prefixes to drag along.
     *
     * <p>It is copied: the list passed can change later and this has to stay fixed.
     *
     * @throws NullPointerException if the list is null
     * @throws ClassCastException if some element is not a {@code String}
     */
    public ExcC14NParameterSpec(List<String> prefixList) {
        if (prefixList == null) {
            throw new NullPointerException("prefixList cannot be null");
        }
        List<String> copy = new ArrayList<String>();
        int i = 0;
        while (i < prefixList.size()) {
            Object p = prefixList.get(i);
            if (!(p instanceof String)) {
                throw new ClassCastException("not a String: " + p);
            }
            copy.add((String) p);
            i = i + 1;
        }
        this.prefixList = Collections.unmodifiableList(copy);
    }

    /** The prefixes to drag along. Unmodifiable. */
    public List<String> getPrefixList() {
        return this.prefixList;
    }
}
