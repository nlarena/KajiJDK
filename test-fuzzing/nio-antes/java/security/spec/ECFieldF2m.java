package java.security.spec;

import java.math.BigInteger;
import java.util.Arrays;

// The binary field GF(2^m): polynomials over GF(2) modulo an irreducible reduction polynomial.
//
// An element is a polynomial of degree < m, and reduction is done modulo another polynomial of
// degree exactly m. That polynomial can be given in three ways and all three describe the same
// thing:
//
//   - no polynomial: a "generic" field, with no fixed basis. It cannot be operated on, but it
//     serves to say what size the field is.
//   - as a `BigInteger`: bit i set means the term x^i is present.
//   - as the indices of the middle terms: for a trinomial x^m + x^k + 1 it is {k}, for a
//     pentanomial x^m + x^k3 + x^k2 + x^k1 + 1 it is {k3, k2, k1}.
//
// The last two are converted into each other in the constructor, so after construction both
// accessors answer, whichever was given. That is why `equals` compares only m and the indices: the
// `BigInteger` is redundant and comparing it too would be extra work for the same result.
//
// Only trinomials and pentanomials are accepted (bitCount 3 or 5), as the JDK specifies: standards
// always choose one of the two because reduction is cheap. This note added that accepting an
// arbitrary polynomial would open the door to a reducible one; nothing here checks irreducibility,
// and a trinomial or a pentanomial can be reducible too.
public class ECFieldF2m implements ECField {

    private final int m;

    // The reduction polynomial as bits, or null if the field was created without a basis.
    private final BigInteger rp;

    // The indices of the middle terms, in **descending** order. Null if there is no polynomial.
    private final int[] ks;

    // A field with no fixed basis: its size is known and nothing else.
    public ECFieldF2m(int m) {
        if (m <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        this.m = m;
        this.rp = null;
        this.ks = null;
    }

    public ECFieldF2m(int m, BigInteger rp) {
        if (m <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        // Without `rp` there is nothing to validate: blowing up here with an NPE is what the JDK
        // does.
        int count = rp.bitCount();
        // The constant term and the degree-m term have to be present: the first because without it
        // the polynomial is divisible by x —that is, reducible—, the second because it is what
        // fixes the degree.
        if (!rp.testBit(0) || !rp.testBit(m) || ((count != 3) && (count != 5))) {
            throw new IllegalArgumentException("rp does not represent a valid reduction polynomial");
        }
        this.m = m;
        this.rp = rp;
        // The two ends are removed and exactly the middle terms remain.
        BigInteger rest = rp.clearBit(0).clearBit(m);
        this.ks = new int[count - 2];
        // Filled from back to front because `getLowestSetBit` returns the indices from lowest to
        // highest and the contract asks for descending order.
        for (int i = this.ks.length - 1; i >= 0; i--) {
            int index = rest.getLowestSetBit();
            this.ks[i] = index;
            rest = rest.clearBit(index);
        }
    }

    public ECFieldF2m(int m, int[] ks) {
        if (m <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        int[] copy = new int[ks.length];
        System.arraycopy(ks, 0, copy, 0, ks.length);
        if ((copy.length != 1) && (copy.length != 3)) {
            throw new IllegalArgumentException("length of ks is neither 1 nor 3");
        }
        for (int i = 0; i < copy.length; i++) {
            // A middle term with index 0 or m would be one of the ends, which are implicit.
            if ((copy[i] < 1) || (copy[i] > m - 1)) {
                throw new IllegalArgumentException("ks[" + i + "] is out of range");
            }
            if ((i != 0) && (copy[i] >= copy[i - 1])) {
                throw new IllegalArgumentException("values in ks are not in descending order");
            }
        }
        this.m = m;
        this.ks = copy;
        BigInteger p = BigInteger.ONE.setBit(m);
        for (int i = 0; i < copy.length; i++) {
            p = p.setBit(copy[i]);
        }
        this.rp = p;
    }

    // In a binary field an element is exactly m bits, whatever the polynomial.
    @Override
    public int getFieldSize() {
        return this.m;
    }

    public int getM() {
        return this.m;
    }

    // The reduction polynomial, or null if the field was created without a basis.
    public BigInteger getReductionPolynomial() {
        return this.rp;
    }

    // A copy of the indices of the middle terms, or null if there is no polynomial.
    public int[] getMidTermsOfReductionPolynomial() {
        if (this.ks == null) {
            return null;
        }
        int[] c = new int[this.ks.length];
        System.arraycopy(this.ks, 0, c, 0, this.ks.length);
        return c;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ECFieldF2m) {
            ECFieldF2m other = (ECFieldF2m) obj;
            // No need to look at `rp`: it is a function of m and ks.
            return (this.m == other.m) && Arrays.equals(this.ks, other.ks);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (this.m * 31) + (this.rp == null ? 0 : this.rp.hashCode());
    }
}
