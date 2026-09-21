package com.sun.security.auth;

import javax.security.auth.Subject;

/**
 * A principal that knows how to say whether it <em>implies</em> a whole subject.
 *
 * <h2>What "implies" means</h2>
 *
 * <p>That a policy written for this principal reaches that subject. The normal thing is for a
 * subject to have several principals -- user, groups, domain -- and for a policy written for
 * the group {@code admin} to reach everybody who has it among theirs.
 *
 * <p>Without this, comparing would be looking for exact equality against each principal of the
 * subject, and there would be no way of expressing a principal that represents a set.
 *
 * @deprecated the policy mechanism based on {@code Subject} fell into disuse along with the
 *     security manager.
 */
@Deprecated(since = "17", forRemoval = true)
public interface PrincipalComparator {

    /** Whether a policy written for this principal reaches {@code subject}. */
    boolean implies(Subject subject);
}
