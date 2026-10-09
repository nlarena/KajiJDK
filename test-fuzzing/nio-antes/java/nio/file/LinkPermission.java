package java.nio.file;

import java.security.BasicPermission;

// The permission to create links: `"hard"` for hard ones, `"symbolic"` for symbolic ones.
//
// Only those two names, and that is why the constructor validates them: `BasicPermission` accepts
// wildcards like `"*"`, and here a wildcard would give a permission wider than either of the two
// that exist.
//
// KajiJDK creates no links --there is no native-- so nobody consults it; the type is here because
// the spec's exception signatures name it and because code that builds one has to compile.
public final class LinkPermission extends BasicPermission {

    private static final long serialVersionUID = -1441492453772213220L;

    private void validate(String name) {
        if (!name.equals("hard") && !name.equals("symbolic")) {
            throw new IllegalArgumentException("name: " + name);
        }
    }

    /**
     * @param name `"hard"` or `"symbolic"`
     * @throws IllegalArgumentException if it is anything else
     */
    public LinkPermission(String name) {
        super(name);
        this.validate(name);
    }

    /**
     * The same as the other; `actions` has to be empty or `null`.
     *
     * <p>This permission has no actions: the name says everything. The overload exists because the
     * security policy machinery builds permissions reflectively with two arguments always.
     */
    public LinkPermission(String name, String actions) {
        super(name);
        this.validate(name);
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("actions: " + actions);
        }
    }
}
