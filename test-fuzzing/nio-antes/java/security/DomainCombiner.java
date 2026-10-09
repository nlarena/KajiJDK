package java.security;

// It knows how to combine two lists of protection domains into the one that is finally used.
//
// It exists for the case where the effective permissions are not simply the intersection of the
// call stack: the canonical example is JAAS, where to the domains of the code one has to add those
// of the authenticated subject. Without this hook, "who you are" could not influence "what you can
// do".
@Deprecated
public interface DomainCombiner {

    // Combines the domains of the current execution with the ones inherited from the context.
    ProtectionDomain[] combine(ProtectionDomain[] currentDomains,
                               ProtectionDomain[] assignedDomains);
}
