package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Set;

// What a provider has to write in order to offer a format of key store.
//
// Most of the methods are abstract because the SPI is from Java 1.2; the few that are not arrived
// afterwards —the typed entries of Java 5, `engineProbe` of Java 9— and have a base implementation
// so as not to break the providers that already existed.
//
// `engineGetEntry` and `engineSetEntry` deserve attention: their base implementation translates
// between the old model —three different methods depending on the type of entry— and the new,
// uniform one. It is compatibility, not a useful layer: a provider that knows how to tell its types
// apart does a better job by overriding them.
//
// A KajiLibrary subset: the `engineGetEntry`/`engineSetEntry` overload that would work with
// `KeyStore.SecretKeyEntry` cannot resolve that case because `javax.crypto.SecretKey` does not
// exist in this library; the translation code covers private keys and trusted certificates, which
// is what can be represented.
public abstract class KeyStoreSpi {

    public KeyStoreSpi() {
    }

    // The key associated with the alias, or null if there is none. The password protects **that
    // entry**, not the whole store: in PKCS#12 and in JKS each private key is encrypted separately.
    public abstract Key engineGetKey(String alias, char[] password)
        throws NoSuchAlgorithmException, UnrecoverableKeyException;

    // The chain of certificates of that key, from the subject towards the root, or null.
    public abstract Certificate[] engineGetCertificateChain(String alias);

    public abstract Certificate engineGetCertificate(String alias);

    public abstract Date engineGetCreationDate(String alias);

    // It saves a key with its chain. The chain is **compulsory** for a private key: a private key
    // without the certificate that publishes it is of no use at all, because nobody could verify
    // what it signs.
    public abstract void engineSetKeyEntry(String alias, Key key, char[] password,
                                           Certificate[] chain) throws KeyStoreException;

    // It saves a key that comes already protected in its final format. The store does not interpret
    // it: it is the road for moving a key between stores without deciphering it on the way.
    public abstract void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain)
        throws KeyStoreException;

    // It saves a **trusted** certificate. It is the most delicate operation of the store: what
    // comes in through here becomes a root, and one root too many can sign a certificate for any
    // name.
    public abstract void engineSetCertificateEntry(String alias, Certificate cert)
        throws KeyStoreException;

    public abstract void engineDeleteEntry(String alias) throws KeyStoreException;

    public abstract Enumeration<String> engineAliases();

    public abstract boolean engineContainsAlias(String alias);

    public abstract int engineSize();

    public abstract boolean engineIsKeyEntry(String alias);

    public abstract boolean engineIsCertificateEntry(String alias);

    // The alias of the first certificate that matches, or null. The comparison is by encoding.
    public abstract String engineGetCertificateAlias(Certificate cert);

    public abstract void engineStore(OutputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException;

    // Saving with parameters instead of with a loose password. A base that throws, because it
    // arrived afterwards.
    public void engineStore(KeyStore.LoadStoreParameter param)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        throw new UnsupportedOperationException();
    }

    // It loads the store. The password may be null: in that case the integrity of the file is **not
    // verified**, it is only read. It is legitimate when only the public certificates are of
    // interest, and it is a mistake when trust anchors are going to come out of there.
    public abstract void engineLoad(InputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException;

    public void engineLoad(KeyStore.LoadStoreParameter param)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        throw new UnsupportedOperationException();
    }

    // The attributes of the entry, or an empty set. The attributes are a thing of PKCS#12: friendly
    // name, local identifier.
    public Set<KeyStore.Entry.Attribute> engineGetAttributes(String alias) {
        return Collections.<KeyStore.Entry.Attribute>emptySet();
    }

    // It translates the old model into that of typed entries. A provider that tells its types apart
    // better than this overrides it.
    public KeyStore.Entry engineGetEntry(String alias, KeyStore.ProtectionParameter protParam)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException {
        if (!this.engineContainsAlias(alias)) {
            return null;
        }
        if (protParam == null) {
            if (this.engineIsCertificateEntry(alias)) {
                return new KeyStore.TrustedCertificateEntry(
                    this.engineGetCertificate(alias), this.engineGetAttributes(alias));
            }
            throw new UnrecoverableKeyException(
                "requested entry requires a password");
        }
        // A KajiLibrary subset: the JDK accepts here a `KeyStore.PasswordProtection`, which does
        // not exist in this library because it needs `javax.security.auth.Destroyable`. Without it
        // there is no way of getting the password out of a `ProtectionParameter`, so the only case
        // that can be attended is that of a trusted certificate, which carries no password.
        if (this.engineIsCertificateEntry(alias)) {
            return new KeyStore.TrustedCertificateEntry(
                this.engineGetCertificate(alias), this.engineGetAttributes(alias));
        }
        throw new UnsupportedOperationException(
            "protection parameters are not supported by this KeyStoreSpi");
    }

    public void engineSetEntry(String alias, KeyStore.Entry entry,
                               KeyStore.ProtectionParameter protParam) throws KeyStoreException {
        if (entry == null) {
            throw new KeyStoreException("invalid null input");
        }
        // A KajiLibrary subset: the JDK accepts here a `KeyStore.PasswordProtection` and takes the
        // password out of it. That class does not exist in this library (see `KeyStore`), so any
        // protection that is not null is rejected — which is the same thing the JDK does with a
        // protection of a type it does not know.
        if (protParam != null) {
            throw new KeyStoreException("unsupported protection parameter");
        }
        if (entry instanceof KeyStore.TrustedCertificateEntry) {
            KeyStore.TrustedCertificateEntry tce = (KeyStore.TrustedCertificateEntry) entry;
            this.engineSetCertificateEntry(alias, tce.getTrustedCertificate());
            return;
        }
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            // Without a password a private key cannot be saved: it would be left in the clear
            // inside the store. Failing is the only right thing.
            throw new KeyStoreException("non-null password required to create PrivateKeyEntry");
        }
        throw new KeyStoreException(
            "unsupported entry type: " + entry.getClass().getName());
    }

    public boolean engineEntryInstanceOf(String alias,
                                         Class<? extends KeyStore.Entry> entryClass) {
        if (entryClass == KeyStore.TrustedCertificateEntry.class) {
            return this.engineIsCertificateEntry(alias);
        }
        if (entryClass == KeyStore.PrivateKeyEntry.class) {
            return this.engineIsKeyEntry(alias) && this.engineGetCertificate(alias) != null;
        }
        return false;
    }

    // Whether this stream looks as if it were of this format. It serves so that
    // `KeyStore.getInstance(File, ...)` guesses the type without being told.
    //
    // It has to leave the stream **as it found it**: it is going to be passed to another provider
    // if this one says no.
    public boolean engineProbe(InputStream stream) throws IOException {
        return false;
    }
}
