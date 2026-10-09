package java.security;

import java.util.Set;

// A rule about which algorithms can be used and for what.
//
// It is what allows one to say "in this process, no MD5 for signing" without touching the code that
// signs. The three overloads are not redundant: an algorithm can be forbidden by name, a concrete
// key —for being short, for example, even though the algorithm is permitted— or the combination of
// the two with parameters. A realistic policy needs all three, because "RSA is fine" and "512-bit
// RSA is fine" are different assertions.
//
// The `Set<CryptoPrimitive>` is the "for what": the same algorithm can be permitted for encrypting
// and forbidden for signing.
//
// KajiLibrary brings no implementation, and it is not an omission: a list of forbidden algorithms
// is a decision of policy, not of the library. Whoever has one writes it.
public interface AlgorithmConstraints {

    // Whether the algorithm is permitted for those primitives, with those parameters.
    boolean permits(Set<CryptoPrimitive> primitives, String algorithm,
                    AlgorithmParameters parameters);

    // Whether that key is permitted for those primitives.
    boolean permits(Set<CryptoPrimitive> primitives, Key key);

    boolean permits(Set<CryptoPrimitive> primitives, String algorithm, Key key,
                    AlgorithmParameters parameters);
}
