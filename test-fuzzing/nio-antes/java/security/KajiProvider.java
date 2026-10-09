package java.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// The only provider KajiLibrary registers as stock.
//
// It offers **digests and a random generator**, and only what can really be fulfilled: the digests
// are written from scratch in this library and checked against the JDK and against the vectors of
// the specifications, and the generator is a direct pass to the operating system's. There is no
// `Signature`, no `Cipher`, no `KeyPairGenerator`: registering a service that cannot be fulfilled
// would be worse than not having it, because `getInstance` would return an object that afterwards
// does not do what it promises.
//
// The generator is called `OS-PRNG` and not `SHA1PRNG` or `DRBG` on purpose. Those two names
// designate concrete constructions, and returning something else under that name would be lying
// about which algorithm is running. `new SecureRandom()` --which is how almost everybody asks for
// it-- names none and therefore works.
//
// It is registered **only if the system can give entropy**. A `SecureRandom` that exists and cannot
// hand over bytes is worse than its absence: the caller finds out at the worst moment.
//
// Package-private on purpose: it is not API of the JDK, and making it public would add to
// `java.security` a name the JDK does not have.
final class KajiProvider extends Provider {

    KajiProvider() {
        super("Kaji", "1.0", "Kaji digest provider (MD5, SHA-1, SHA-2 family)");

        this.register("MD5", "java.security.DigestMD5", new String[] {"1.2.840.113549.2.5"});
        this.register("SHA-1", "java.security.DigestSHA1", new String[] {"SHA", "SHA1"});
        this.register("SHA-224", "java.security.DigestSHA2", new String[] {"SHA224"});
        this.register("SHA-256", "java.security.DigestSHA2", new String[] {"SHA256"});
        this.register("SHA-384", "java.security.DigestSHA5", new String[] {"SHA384"});
        this.register("SHA-512", "java.security.DigestSHA5", new String[] {"SHA512"});

        if (OsEntropy.available()) {
            this.putService(new OsPrngService(this));
        }
    }

    // The service of the generator. It builds directly for the same reason as `DigestService`: the
    // class is an implementation detail of `java.security` and is not reachable by reflection.
    private static final class OsPrngService extends Provider.Service {

        OsPrngService(Provider p) {
            super(p, "SecureRandom", "OS-PRNG", "java.security.OsPrngSpi",
                new ArrayList<String>(), new HashMap<String, String>());
        }

        @Override
        public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
            if (constructorParameter != null) {
                // The parameters are only understood by a DRBG, and this is not one. They are
                // rejected instead of being ignored: whoever passes them is asking for a
                // configuration that is not going to be applied.
                throw new NoSuchAlgorithmException(
                    "OS-PRNG does not accept SecureRandomParameters");
            }
            return new OsPrngSpi();
        }
    }

    private void register(String algorithmName, String className, String[] alias) {
        List<String> list = new ArrayList<String>();
        int i = 0;
        while (i < alias.length) {
            list.add(alias[i]);
            i = i + 1;
        }
        this.putService(new DigestService(this, algorithmName, className, list));
    }

    // It instantiates the digests **without reflection**.
    //
    // The base implementation of `Provider.Service` does `Class.forName(className).newInstance()`,
    // and that works for an external provider whose classes are public. The ones here are not —they
    // are an implementation detail of `java.security`— so this service builds directly. The
    // `getClassName()` goes on telling the truth: it is the real name of the class that is going to
    // be instantiated, although from outside it cannot be reached by reflection.
    private static final class DigestService extends Provider.Service {

        private final String algorithmName;

        DigestService(Provider p, String algorithmName, String className, List<String> alias) {
            super(p, "MessageDigest", algorithmName, className, alias, new HashMap<String, String>());
            this.algorithmName = algorithmName;
        }

        @Override
        public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
            if (constructorParameter != null) {
                throw new InvalidParameterException(
                    "constructorParameter not used with MessageDigest engines");
            }
            if (this.algorithmName.equals("MD5")) {
                return new DigestMD5();
            }
            if (this.algorithmName.equals("SHA-1")) {
                return new DigestSHA1();
            }
            if (this.algorithmName.equals("SHA-224")) {
                return DigestSHA2.sha224();
            }
            if (this.algorithmName.equals("SHA-256")) {
                return DigestSHA2.sha256();
            }
            if (this.algorithmName.equals("SHA-384")) {
                return DigestSHA5.sha384();
            }
            if (this.algorithmName.equals("SHA-512")) {
                return DigestSHA5.sha512();
            }
            throw new NoSuchAlgorithmException(this.algorithmName);
        }
    }
}
