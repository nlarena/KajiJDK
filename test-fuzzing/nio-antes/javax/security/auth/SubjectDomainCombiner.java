package javax.security.auth;

import java.security.DomainCombiner;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.Set;

/**
 * KajiLibrary's javax.security.auth.SubjectDomainCombiner -- attaches a {@link Subject}'s
 * identities to the protection domains of the stack.
 *
 * <p>A {@code ProtectionDomain} says where the code came from and what it can do. This combiner
 * adds the other half of the question: <b>on whose behalf</b> it is running. Without it, a policy
 * can only decide by code origin; with it, it can say "this jar can read that file <i>only if</i>
 * john is running it".
 *
 * <p>The work is a walk: for each domain of the current stack a new one is built with the same
 * origin, the same loader and the same permissions, plus the Subject's principals; then the domains
 * already assigned are appended.
 *
 * <p>With one exception that has to be reproduced because it is observable: a domain with <b>static
 * permissions</b> --the one built with the permission collection already closed-- is returned <b>as
 * it is</b>, the same instance. The reason is not saving an object: a static domain says "these
 * permissions and nothing more, forever", so adding identities to it would change no answer -- its
 * {@code implies} no longer consults the policy --. The dynamic ones, on the other hand, are
 * rebuilt, because there the identities do enter the query.
 *
 * <p>A note on what it serves today: the security manager can no longer be enabled, so nothing in
 * the library calls this combiner. The class exists because its form is part of the API and because
 * the computation it does is pure -- it does not depend on there being a manager -- and can be
 * looked at and tested.
 */
public class SubjectDomainCombiner implements DomainCombiner {

    private final Subject subject;

    public SubjectDomainCombiner(Subject subject) {
        this.subject = subject;
    }

    /** The Subject whose identities are attached. The same instance that was passed. */
    public Subject getSubject() {
        return this.subject;
    }

    /**
     * The stack's domains with the Subject's identities on top, followed by the ones already
     * assigned.
     *
     * <p>If there are no current domains it returns the assigned ones <b>as they are</b> --null
     * included--: there is nothing to attach the identities to, and building an empty array would
     * be saying something different from "there was nothing".
     */
    public ProtectionDomain[] combine(ProtectionDomain[] currentDomains,
            ProtectionDomain[] assignedDomains) {
        if (currentDomains == null || currentDomains.length == 0) {
            return assignedDomains;
        }
        Principal[] principalsOf = principalsOf();
        int assignedCount = assignedDomains == null ? 0 : assignedDomains.length;
        ProtectionDomain[] out =
            new ProtectionDomain[currentDomains.length + assignedCount];
        int i = 0;
        while (i < currentDomains.length) {
            ProtectionDomain d = currentDomains[i];
            // See the class note: having identities on top changes nothing for the static one.
            out[i] = d.staticPermissionsOnly() ? d
                : new ProtectionDomain(d.getCodeSource(), d.getPermissions(),
                    d.getClassLoader(), principalsOf);
            i = i + 1;
        }
        int j = 0;
        while (j < assignedCount) {
            out[currentDomains.length + j] = assignedDomains[j];
            j = j + 1;
        }
        return out;
    }

    private Principal[] principalsOf() {
        if (this.subject == null) {
            return new Principal[0];
        }
        Set<Principal> ps = this.subject.getPrincipals();
        Principal[] arr = new Principal[ps.size()];
        int i = 0;
        java.util.Iterator<Principal> it = ps.iterator();
        while (it.hasNext()) {
            arr[i] = it.next();
            i = i + 1;
        }
        return arr;
    }
}
