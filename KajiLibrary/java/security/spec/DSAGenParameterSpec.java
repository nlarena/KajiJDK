package java.security.spec;

// Which DSA domain parameters to generate: the length of p, of q and of the seed.
//
// It validates **combinations** and not single values, and the reason is that FIPS 186-3 does not
// let the two lengths be chosen separately: only (1024, 160), (2048, 224), (2048, 256) and (3072,
// 256) are legal. (This note called it the only spec in the package that does so; `EllipticCurve`
// also checks its coefficients against its field.) The restriction is not red tape —the length of q
// sets the cost of the best generic attack on the discrete logarithm in the subgroup, and that of p
// the cost of the best sieve attack on the whole group— and choosing a small q with a large p gives
// a pair that looks strong and is not. Rejecting invalid combinations in the constructor is what
// keeps that mistake from being discovered only once keys have been issued.
public final class DSAGenParameterSpec implements AlgorithmParameterSpec {

    private final int primePLen;
    private final int subprimeQLen;
    private final int seedLen;

    // Without an explicit seed length, q's is used, which is the legal minimum.
    public DSAGenParameterSpec(int primePLen, int subprimeQLen) {
        this(primePLen, subprimeQLen, subprimeQLen);
    }

    public DSAGenParameterSpec(int primePLen, int subprimeQLen, int seedLen) {
        switch (primePLen) {
            case 1024:
                if (subprimeQLen != 160) {
                    throw new IllegalArgumentException(
                        "subprimeQLen must be 160 when primePLen=1024");
                }
                break;
            case 2048:
                if (subprimeQLen != 224 && subprimeQLen != 256) {
                    throw new IllegalArgumentException(
                        "subprimeQLen must be 224 or 256 when primePLen=2048");
                }
                break;
            case 3072:
                if (subprimeQLen != 256) {
                    throw new IllegalArgumentException(
                        "subprimeQLen must be 256 when primePLen=3072");
                }
                break;
            default:
                throw new IllegalArgumentException("primePLen must be 1024, 2048, or 3072");
        }
        // A seed shorter than q would put a ceiling on the entropy of the whole domain: it does not
        // matter how large p is if the process that generated it started from fewer bits.
        if (seedLen < subprimeQLen) {
            throw new IllegalArgumentException(
                "seedLen must be equal to or greater than subprimeQLen");
        }
        this.primePLen = primePLen;
        this.subprimeQLen = subprimeQLen;
        this.seedLen = seedLen;
    }

    public int getPrimePLength() {
        return this.primePLen;
    }

    public int getSubprimeQLength() {
        return this.subprimeQLen;
    }

    public int getSeedLength() {
        return this.seedLen;
    }
}
