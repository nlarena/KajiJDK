package java.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

// A store of keys and certificates: alias -> material.
//
// ===============================================================================================
// THE TWO DIFFERENT THINGS IT KEEPS
// ===============================================================================================
//
// A `KeyStore` mixes two roles that are better not confused, because they have opposite security
// implications:
//
//   - **private keys** with their chain: secret material, protected by a password. It leaks and its
//     owner can be impersonated.
//   - **trusted certificates**: public material and **not protected by a password**. There is
//     nothing secret inside, but what goes in there becomes a root: one CA too many in a truststore
//     can issue a valid certificate for any name. Adding a trusted entry is more dangerous than
//     leaking a key, although it looks like the opposite.
//
// That is why `setCertificateEntry` does not ask for a password and `setKeyEntry` does: it is not
// an asymmetry of convenience, it is that they protect different things.
//
// `load(stream, null)` is the other place where security is lost without noticing: with a null
// password the store is read **without verifying its integrity**. It is legitimate when one only
// wants to look at certificates; it is a hole if the trust anchors come from there, because anybody
// who can write the file can add a root.
//
// ===============================================================================================
// A KajiLibrary subset
// ===============================================================================================
//
// **There is no registered `KeyStore` provider**, so the overloads of `getInstance` always throw
// `KeyStoreException`. Reading a JKS or a PKCS#12 asks for decryption —PBES2, RC2, 3DES— and
// verification of a MAC, and none of that is implemented; a reader that ignored the MAC would be
// handing over material nobody authenticated. The whole structure is there: whoever brings a
// `KeyStoreSpi` obtains a `KeyStore` that works.
//
// Two nested types are missing, both for dependencies this library does not have:
//
//   - `SecretKeyEntry`, because `javax.crypto.SecretKey` does not exist.
//   - `PasswordProtection` and `CallbackHandlerProtection`, because they need
//     `javax.security.auth.Destroyable` and `javax.security.auth.callback.CallbackHandler`.
//
// The absence of `PasswordProtection` drags in the three `Builder.newInstance`s that work with
// files: their first step is to check that the protection is of one of those two classes, so
// without them they could never succeed. They are left out instead of being declared so that they
// always fail.
public class KeyStore {

    // An entry of the store. The interface is the modern model —uniform, typed— against the loose
    // methods of Java 1.2, which are still there out of compatibility.
    public interface Entry {

        // The attributes of the entry. Empty by default: the attributes are a thing of PKCS#12 and
        // arrived in Java 8, long after this interface.
        default Set<Attribute> getAttributes() {
            return Collections.<Attribute>emptySet();
        }

        // An attribute with a name and a value. In PKCS#12 the name is an OID.
        interface Attribute {

            String getName();

            String getValue();
        }
    }

    // How an entry is protected when reading or writing it. It is a marker interface: each form of
    // protection —a password, a dialogue that asks for it— is a separate class.
    public interface ProtectionParameter {
    }

    // Where to load the whole store from or where to save it to, with its protection.
    public interface LoadStoreParameter {

        // The protection of the integrity of the store, or null if there is none.
        ProtectionParameter getProtectionParameter();
    }

    // A private key with its chain of certificates.
    //
    // The chain is not optional and the constructor enforces it: a private key without the
    // certificate that publishes its public key is of no use to anybody, because there would be no
    // way of verifying what it signs. It is also checked that the public key of the first
    // certificate is of the same algorithm as the private one —not that they are the pair, because
    // that would cost a public key operation, but that they are not of different families—.
    public static final class PrivateKeyEntry implements Entry {

        private final PrivateKey privKey;
        private final Certificate[] chain;
        private final Set<Attribute> attributes;

        public PrivateKeyEntry(PrivateKey privateKey, Certificate[] chain) {
            this(privateKey, chain, Collections.<Attribute>emptySet());
        }

        public PrivateKeyEntry(PrivateKey privateKey, Certificate[] chain,
                               Set<Attribute> attributes) {
            if (privateKey == null || chain == null || attributes == null) {
                throw new NullPointerException("invalid null input");
            }
            if (chain.length == 0) {
                throw new IllegalArgumentException("invalid zero-length input chain");
            }
            Certificate[] copy = new Certificate[chain.length];
            System.arraycopy(chain, 0, copy, 0, chain.length);
            // Every certificate of the chain has to be of the same type: a chain that mixed formats
            // could not be validated from end to end.
            String type = copy[0].getType();
            int i = 1;
            while (i < copy.length) {
                if (!type.equals(copy[i].getType())) {
                    throw new IllegalArgumentException(
                        "chain does not contain certificates of the same type");
                }
                i = i + 1;
            }
            if (!privateKey.getAlgorithm().equals(copy[0].getPublicKey().getAlgorithm())) {
                throw new IllegalArgumentException(
                    "private key algorithm does not match algorithm of public key in end entity "
                    + "certificate (at index 0)");
            }
            this.privKey = privateKey;
            this.chain = copy;
            this.attributes = Collections.unmodifiableSet(new HashSet<Attribute>(attributes));
        }

        public PrivateKey getPrivateKey() {
            return this.privKey;
        }

        // A copy of the chain, from the subject towards the root.
        public Certificate[] getCertificateChain() {
            Certificate[] c = new Certificate[this.chain.length];
            System.arraycopy(this.chain, 0, c, 0, this.chain.length);
            return c;
        }

        // The certificate of the key itself: the first of the chain.
        public Certificate getCertificate() {
            return this.chain[0];
        }

        @Override
        public Set<Attribute> getAttributes() {
            return this.attributes;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Private key entry and certificate chain with "
                + this.chain.length + " elements:\r\n");
            int i = 0;
            while (i < this.chain.length) {
                sb.append(this.chain[i].toString());
                sb.append("\r\n");
                i = i + 1;
            }
            return sb.toString();
        }
    }

    // A certificate that is trusted. With no password, because there is nothing secret to protect
    // —what has to be protected is the **integrity** of the store, so that nobody adds one—.
    public static final class TrustedCertificateEntry implements Entry {

        private final Certificate cert;
        private final Set<Attribute> attributes;

        public TrustedCertificateEntry(Certificate trustedCert) {
            this(trustedCert, Collections.<Attribute>emptySet());
        }

        public TrustedCertificateEntry(Certificate trustedCert, Set<Attribute> attributes) {
            if (trustedCert == null || attributes == null) {
                throw new NullPointerException("invalid null input");
            }
            this.cert = trustedCert;
            this.attributes = Collections.unmodifiableSet(new HashSet<Attribute>(attributes));
        }

        public Certificate getTrustedCertificate() {
            return this.cert;
        }

        @Override
        public Set<Attribute> getAttributes() {
            return this.attributes;
        }

        @Override
        public String toString() {
            return "Trusted certificate entry:\r\n" + this.cert.toString();
        }
    }

    // A lazy factory of stores: it opens nothing until it is asked.
    //
    // It exists for the case where the password is not known yet when the system is configured
    // —somebody has to be asked for it— and so as not to have the store open longer than necessary.
    public abstract static class Builder {

        protected Builder() {
        }

        // The store, loaded. Each call may return the same object.
        public abstract KeyStore getKeyStore() throws KeyStoreException;

        // What to protect the entry of that alias with.
        public abstract ProtectionParameter getProtectionParameter(String alias)
            throws KeyStoreException;

        // A builder over an **already loaded** store: the protection is the same for every alias.
        public static Builder newInstance(final KeyStore keyStore,
                                          final ProtectionParameter protectionParameter) {
            if (keyStore == null || protectionParameter == null) {
                throw new NullPointerException();
            }
            return new Builder() {

                @Override
                public KeyStore getKeyStore() {
                    return keyStore;
                }

                @Override
                public ProtectionParameter getProtectionParameter(String alias) {
                    if (alias == null) {
                        throw new NullPointerException();
                    }
                    return protectionParameter;
                }
            };
        }

        // A builder that creates the store only when it is asked. It serves for stores that do not
        // come from a file —a card, an HSM—, where `load` reads nothing but may need a credential.
        public static Builder newInstance(final String type, final Provider provider,
                                          final ProtectionParameter protection) {
            if (type == null || protection == null) {
                throw new NullPointerException();
            }
            return new Builder() {

                private KeyStore ks;

                @Override
                public synchronized KeyStore getKeyStore() throws KeyStoreException {
                    if (this.ks != null) {
                        return this.ks;
                    }
                    KeyStore fresh;
                    if (provider == null) {
                        fresh = KeyStore.getInstance(type);
                    } else {
                        fresh = KeyStore.getInstance(type, provider);
                    }
                    try {
                        fresh.load(new SimpleParameters(protection));
                    } catch (Exception e) {
                        throw new KeyStoreException("KeyStore instantiation failed", e);
                    }
                    this.ks = fresh;
                    return fresh;
                }

                @Override
                public ProtectionParameter getProtectionParameter(String alias)
                        throws KeyStoreException {
                    if (alias == null) {
                        throw new NullPointerException();
                    }
                    // The creation is forced first so that a loading error comes out here and not
                    // later, with a protection in hand already and a store that was never opened.
                    this.getKeyStore();
                    return protection;
                }
            };
        }
    }

    // The minimal `LoadStoreParameter`: it only carries the protection. Package-private because in
    // the JDK it is internal too.
    static final class SimpleParameters implements LoadStoreParameter {

        private final ProtectionParameter protection;

        SimpleParameters(ProtectionParameter protection) {
            this.protection = protection;
        }

        public ProtectionParameter getProtectionParameter() {
            return this.protection;
        }
    }

    private final KeyStoreSpi keyStoreSpi;
    private final Provider provider;
    private final String type;
    // A store that is not loaded answers nothing: it is what separates "just created" from
    // "empty".
    private boolean initialized = false;

    protected KeyStore(KeyStoreSpi keyStoreSpi, Provider provider, String type) {
        this.keyStoreSpi = keyStoreSpi;
        this.provider = provider;
        this.type = type;
    }

    public static KeyStore getInstance(String type) throws KeyStoreException {
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("KeyStore", type);
            if (s != null) {
                return build(s, type);
            }
            i = i + 1;
        }
        throw new KeyStoreException(type + " not found");
    }

    public static KeyStore getInstance(String type, String provider)
            throws KeyStoreException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(type, p);
    }

    public static KeyStore getInstance(String type, Provider provider) throws KeyStoreException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider.Service s = provider.getService("KeyStore", type);
        if (s == null) {
            throw new KeyStoreException(
                "no such type: " + type + " for provider " + provider.getName());
        }
        return build(s, type);
    }

    private static KeyStore build(Provider.Service s, String type) throws KeyStoreException {
        Object o;
        try {
            o = s.newInstance(null);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(e.getMessage(), e);
        }
        if (!(o instanceof KeyStoreSpi)) {
            throw new KeyStoreException(
                "class configured for KeyStore is not a KeyStoreSpi: " + s.getClassName());
        }
        return new KeyStore((KeyStoreSpi) o, s.getProvider(), type);
    }

    // It opens a store guessing its type from the contents of the file.
    //
    // A KajiLibrary subset: the detection is done by asking each provider with `engineProbe`, and
    // since there is none registered, this always ends in `KeyStoreException`. The logic is left
    // written because it is the only non-trivial part and does not depend on knowing any format.
    public static final KeyStore getInstance(File file, char[] password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        if (file == null) {
            throw new NullPointerException();
        }
        throw new KeyStoreException("Unable to determine KeyStore type: no KeyStore provider "
            + "is registered in this library");
    }

    public static final KeyStore getInstance(File file, LoadStoreParameter param)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        if (file == null) {
            throw new NullPointerException();
        }
        throw new KeyStoreException("Unable to determine KeyStore type: no KeyStore provider "
            + "is registered in this library");
    }

    // The default type, from the security property `keystore.type`. "pkcs12" if it is not set.
    public static final String getDefaultType() {
        String t = Security.getProperty("keystore.type");
        if (t == null) {
            return "pkcs12";
        }
        return t;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getType() {
        return this.type;
    }

    // The attributes of that entry, or an empty set.
    public final Set<Entry.Attribute> getAttributes(String alias) throws KeyStoreException {
        this.requireLoaded();
        return this.keyStoreSpi.engineGetAttributes(alias);
    }

    // The key of the alias, or null if there is none with that name.
    public final Key getKey(String alias, char[] password)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        this.requireLoaded();
        return this.keyStoreSpi.engineGetKey(alias, password);
    }

    // The chain of that key, or null. From the subject towards the root.
    public final Certificate[] getCertificateChain(String alias) throws KeyStoreException {
        this.requireLoaded();
        return this.keyStoreSpi.engineGetCertificateChain(alias);
    }

    // The certificate of the alias. If the alias is of a key, it returns the first of its chain.
    public final Certificate getCertificate(String alias) throws KeyStoreException {
        this.requireLoaded();
        return this.keyStoreSpi.engineGetCertificate(alias);
    }

    public final Date getCreationDate(String alias) throws KeyStoreException {
        this.requireLoaded();
        return this.keyStoreSpi.engineGetCreationDate(alias);
    }

    // It saves a key. If it is private, the chain is compulsory.
    //
    // The missing chain is `IllegalArgumentException` and not `KeyStoreException`, although the
    // method declares the second: it is an error of the caller —they left out an argument— and not
    // a problem of the store. It surprises, but it is what the JDK does.
    public final void setKeyEntry(String alias, Key key, char[] password, Certificate[] chain)
            throws KeyStoreException {
        this.requireLoaded();
        if (key instanceof PrivateKey && (chain == null || chain.length == 0)) {
            throw new IllegalArgumentException(
                "Private key must be accompanied by certificate chain");
        }
        this.keyStoreSpi.engineSetKeyEntry(alias, key, password, chain);
    }

    // It saves a key that is already protected in its final format, without deciphering it.
    public final void setKeyEntry(String alias, byte[] key, Certificate[] chain)
            throws KeyStoreException {
        this.requireLoaded();
        this.keyStoreSpi.engineSetKeyEntry(alias, key, chain);
    }

    // It marks a certificate as trusted. See the note of the class: this creates a root.
    //
    // It does not validate that the alias and the certificate are non-null: the facade only checks
    // that the store is loaded and delegates. It is what the JDK does, and it makes sense because
    // whether it is legal to keep a null depends on the format underneath, not on this class.
    public final void setCertificateEntry(String alias, Certificate cert)
            throws KeyStoreException {
        this.requireLoaded();
        this.keyStoreSpi.engineSetCertificateEntry(alias, cert);
    }

    public final void deleteEntry(String alias) throws KeyStoreException {
        this.requireLoaded();
        this.keyStoreSpi.engineDeleteEntry(alias);
    }

    public final Enumeration<String> aliases() throws KeyStoreException {
        this.requireLoaded();
        return this.keyStoreSpi.engineAliases();
    }

    public final boolean containsAlias(String alias) throws KeyStoreException {
        this.requireLoaded();
        return this.keyStoreSpi.engineContainsAlias(alias);
    }

    public final int size() throws KeyStoreException {
        this.requireLoaded();
        return this.keyStoreSpi.engineSize();
    }

    public final boolean isKeyEntry(String alias) throws KeyStoreException {
        this.requireLoaded();
        return this.keyStoreSpi.engineIsKeyEntry(alias);
    }

    public final boolean isCertificateEntry(String alias) throws KeyStoreException {
        this.requireLoaded();
        return this.keyStoreSpi.engineIsCertificateEntry(alias);
    }

    // The alias of the first certificate that matches, or null. The comparison is by encoding, not
    // by identity.
    public final String getCertificateAlias(Certificate cert) throws KeyStoreException {
        this.requireLoaded();
        return this.keyStoreSpi.engineGetCertificateAlias(cert);
    }

    // It writes the store and **protects its integrity** with the password.
    public final void store(OutputStream stream, char[] password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        this.requireLoaded();
        this.keyStoreSpi.engineStore(stream, password);
    }

    public final void store(LoadStoreParameter param)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        this.requireLoaded();
        this.keyStoreSpi.engineStore(param);
    }

    // It loads the store. **It has to be called before anything else**, even in order to create an
    // empty one: there a null stream is passed.
    //
    // With a null password the integrity is not verified. See the note of the class.
    public final void load(InputStream stream, char[] password)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        this.keyStoreSpi.engineLoad(stream, password);
        this.initialized = true;
    }

    public final void load(LoadStoreParameter param)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        this.keyStoreSpi.engineLoad(param);
        this.initialized = true;
    }

    // The entry as a typed object, which is the modern model.
    public final Entry getEntry(String alias, ProtectionParameter protParam)
            throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
        if (alias == null) {
            throw new NullPointerException("invalid null input");
        }
        this.requireLoaded();
        return this.keyStoreSpi.engineGetEntry(alias, protParam);
    }

    public final void setEntry(String alias, Entry entry, ProtectionParameter protParam)
            throws KeyStoreException {
        if (alias == null || entry == null) {
            throw new NullPointerException("invalid null input");
        }
        this.requireLoaded();
        this.keyStoreSpi.engineSetEntry(alias, entry, protParam);
    }

    // Whether the entry is of that type. Asking this is cheaper than taking it out, because the
    // password is not needed.
    public final boolean entryInstanceOf(String alias, Class<? extends Entry> entryClass)
            throws KeyStoreException {
        if (alias == null || entryClass == null) {
            throw new NullPointerException("invalid null input");
        }
        this.requireLoaded();
        return this.keyStoreSpi.engineEntryInstanceOf(alias, entryClass);
    }

    // A store that is not loaded cannot answer: it is not that it is empty, it is that it is not
    // known. Failing here is the only right thing —returning null or zero would pass off as "it is
    // not there" something that might be—.
    private void requireLoaded() throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
    }
}
