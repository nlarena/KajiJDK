package javax.xml.crypto;

import java.security.Key;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;

/**
 * KajiLibrary's javax.xml.crypto.KeySelector -- decides which key signs or validates.
 *
 * <p>It receives the document's {@link KeyInfo}, what it is wanted for, and the algorithm, and
 * returns the key. It is <b>the</b> security decision of the whole XML signature, and that is why
 * the API leaves it in the hands of whoever uses the library instead of settling it by itself.
 *
 * <h2>The KeyInfo is not a source of trust</h2>
 *
 * <p>The most tempting argument is the {@code KeyInfo}: it comes with the key inside, or with a
 * certificate, and using it makes the signature validate. And it proves nothing -- whoever signed
 * wrote it, so a forged signature brings its own key and validates perfectly.
 *
 * <p>A correct selector uses the {@code KeyInfo} as a <b>hint</b> --to choose which of the keys one
 * already knows corresponds-- and never as a source. {@link #singletonKeySelector} is the extreme
 * and safest case: always the same key, ignoring whatever the document says.
 *
 * <p>{@link Purpose} tells the four uses apart. It matters because a key can serve for one and not
 * for another, and because validating with a key meant for signing is a configuration error worth
 * detecting.
 */
public abstract class KeySelector {

    /** For the subclasses. */
    protected KeySelector() {
    }

    /**
     * The key for that operation.
     *
     * @param keyInfo what the document says; see the class note
     * @param purpose what it is wanted for
     * @param method the algorithm that is going to use it
     * @throws KeySelectorException if none can be chosen
     */
    public abstract KeySelectorResult select(KeyInfo keyInfo, Purpose purpose,
                                             AlgorithmMethod method, XMLCryptoContext context)
        throws KeySelectorException;

    /**
     * A selector that always returns that key.
     *
     * <p>It ignores the {@code KeyInfo} completely, which is precisely what makes it safe: the key
     * is chosen by whoever validates and not by the document.
     *
     * @throws NullPointerException if the key is null
     */
    public static KeySelector singletonKeySelector(Key key) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        return new SingletonKeySelector(key);
    }

    /**
     * What the key is wanted for.
     *
     * <p>It is not an enum; it is four constants with a private constructor, the hand-made enum
     * pattern of that time. (The note said the reason is that the class is from 2005; Java 5
     * already had enums in 2004, but JSR 105 was also meant to run on J2SE 1.4, which had none.)
     */
    public static class Purpose {

        /** To sign. */
        public static final Purpose SIGN = new Purpose("sign");

        /** To validate a signature. */
        public static final Purpose VERIFY = new Purpose("verify");

        /** To encrypt. */
        public static final Purpose ENCRYPT = new Purpose("encrypt");

        /** To decrypt. */
        public static final Purpose DECRYPT = new Purpose("decrypt");

        private final String name;

        private Purpose(String name) {
            this.name = name;
        }

        /** The purpose's name. */
        public String toString() {
            return this.name;
        }
    }

    /** The one {@link KeySelector#singletonKeySelector} returns. */
    private static final class SingletonKeySelector extends KeySelector {

        private final Key key;

        SingletonKeySelector(Key key) {
            this.key = key;
        }

        /** Always the same one, looking at nothing. */
        public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method,
                                        XMLCryptoContext context) {
            return new SingletonResult(this.key);
        }
    }

    /** The result of {@link SingletonKeySelector}. */
    private static final class SingletonResult implements KeySelectorResult {

        private final Key key;

        SingletonResult(Key key) {
            this.key = key;
        }

        public Key getKey() {
            return this.key;
        }
    }
}
