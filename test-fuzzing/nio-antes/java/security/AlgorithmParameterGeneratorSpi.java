package java.security;

// What a provider has to write in order to **generate** algorithm parameters.
//
// It is the counterpart of `AlgorithmParametersSpi`: that one reads and writes parameters that
// exist already, this one invents them. The difference matters because generating domain parameters
// —the primes p and q of DSA, for example— is expensive and probabilistic, while decoding them is
// not.
//
// Both `engineInit`s receive the source of randomness, and that dependency is essential and not
// incidental: generating a big prime **is** choosing candidates at random until one passes the
// primality test. Without randomness there is nothing to generate. That is why the API does not
// allow initialising without saying where it comes from.
public abstract class AlgorithmParameterGeneratorSpi {

    public AlgorithmParameterGeneratorSpi() {
    }

    /**
     * Initialises by size: how many bits the parameters have to have.
     *
     * @param random where the randomness comes from. It is not optional -- see the note of the
     *     class
     */
    protected abstract void engineInit(int size, SecureRandom random);

    /**
     * Initialises with concrete parameters, when the size is not enough to describe what is wanted.
     *
     * @throws java.security.InvalidAlgorithmParameterException if the parameters do not serve this
     *     generator
     */
    protected abstract void engineInit(java.security.spec.AlgorithmParameterSpec genParamSpec,
            SecureRandom random) throws InvalidAlgorithmParameterException;

    // The generated parameters.
    protected abstract AlgorithmParameters engineGenerateParameters();
}
