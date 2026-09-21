package com.sun.security.auth.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;
import javax.security.auth.x500.X500PrivateCredential;

/**
 * The JAAS module that authenticates with a certificate kept in a key store.
 *
 * <h2>What authenticating with a certificate means</h2>
 *
 * <p>That the user proves it has the private key that corresponds to a certificate. This
 * module does the easy part: it opens the store and takes the key out. The cryptographic proof
 * is done afterwards by whoever uses the credential this module leaves in the
 * {@link Subject} -- signing something with it is what proves the identity to the other side.
 *
 * <p>That is why the result is not a boolean but a <strong>private credential</strong>: it is
 * not enough to know who it says it is, it has to be possible to act as it.
 *
 * <h2>The two passwords</h2>
 *
 * <p>One opens the store, the other opens the private key inside it. They are different on
 * purpose: the store may have the keys of several users, and the store's password should not
 * give access to anybody's key. When the second is not given, the first is used -- which is
 * the common case of a store of a single user.
 *
 * <h2>The options</h2>
 *
 * <ul>
 *   <li>{@code keyStoreURL} -- where to read the store from. {@code "NONE"} for those that
 *       are not a file, such as a cryptographic token.
 *   <li>{@code keyStoreType} -- the type; by default the system's.
 *   <li>{@code keyStoreProvider} -- the provider, if a particular one is wanted.
 *   <li>{@code keyStoreAlias} -- the entry's alias; if it is missing, it is asked for.
 *   <li>{@code keyStorePasswordURL} -- where to read the store's password from; if it is
 *       missing, it is asked for.
 *   <li>{@code privateKeyPasswordURL} -- the same for the private key's.
 *   <li>{@code protected} -- {@code true} when the store asks for the password on its own, for
 *       example a card reader with a keypad of its own. Then nothing is asked for.
 *   <li>{@code debug} -- it leaves a trace over the standard output.
 * </ul>
 *
 * @since 1.4
 */
public class KeyStoreLoginModule implements LoginModule {

    private static final int STORE_TYPE = 0;
    private static final int KEY_TYPE = 1;

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    private boolean debug;
    private boolean protectedPath;
    private String keyStoreURL;
    private String keyStoreType;
    private String keyStoreProvider;
    private String keyStoreAlias;
    private String keyStorePasswordURL;
    private String privateKeyPasswordURL;

    private X500Principal principal;
    private CertPath certP;
    private X500PrivateCredential privateCredential;

    private boolean succeeded;
    private boolean commitSucceeded;

    /** For the JAAS configuration, which instantiates it by reflection. */
    public KeyStoreLoginModule() {
    }

    /** {@inheritDoc} */
    public void initialize(final Subject subject, final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        this.protectedPath = "true".equalsIgnoreCase((String) options.get("protected"));
        this.keyStoreURL = (String) options.get("keyStoreURL");
        this.keyStoreType = (String) options.get("keyStoreType");
        this.keyStoreProvider = (String) options.get("keyStoreProvider");
        this.keyStoreAlias = (String) options.get("keyStoreAlias");
        this.keyStorePasswordURL = (String) options.get("keyStorePasswordURL");
        this.privateKeyPasswordURL = (String) options.get("privateKeyPasswordURL");

        if (keyStoreType == null) {
            keyStoreType = KeyStore.getDefaultType();
        }
    }

    /**
     * It opens the store and takes the certificate and the private key of the asked-for alias
     * out.
     *
     * @return {@code true} if it could be done
     * @throws FailedLoginException if the alias does not exist or the password does not open it
     * @throws LoginException if configuration is missing or the store could not be read
     */
    public boolean login() throws LoginException {
        final String alias = alias();
        final char[] storePassword = password(STORE_TYPE, keyStorePasswordURL,
                "Keystore password: ");
        char[] keyPassword = password(KEY_TYPE, privateKeyPasswordURL,
                "Private key password (optional): ");
        // With no password of its own for the key, the store's is used: it is the case of a
                // store of a single user, where having two different secrets would protect nothing.
        if (keyPassword == null || keyPassword.length == 0) {
            keyPassword = storePassword;
        }

        try {
            final KeyStore ks = open(storePassword);
            final Certificate[] chain = ks.getCertificateChain(alias);
            if (chain == null || chain.length == 0) {
                throw new FailedLoginException(
                        "a certificate chain is missing for the alias " + alias);
            }
            final java.security.Key key = ks.getKey(alias, keyPassword);
            if (!(key instanceof PrivateKey)) {
                throw new FailedLoginException(
                        "the entry " + alias + " does not have a private key");
            }

            final List<Certificate> list = new ArrayList<Certificate>(Arrays.asList(chain));
            this.certP = CertificateFactory.getInstance("X.509").generateCertPath(list);
            final X509Certificate leaf = (X509Certificate) chain[0];
            this.principal = leaf.getSubjectX500Principal();
            this.privateCredential =
                    new X500PrivateCredential(leaf, (PrivateKey) key, alias);
        } catch (final LoginException e) {
            throw e;
        } catch (final java.security.GeneralSecurityException e) {
            reset();
            final LoginException le = new LoginException("the key store could not be read");
            le.initCause(e);
            throw le;
        } catch (final IOException e) {
            reset();
            // The wrong password of a store arrives as an IOException with an
                        // UnrecoverableKeyException cause, not as a security exception. Telling it
                        // apart matters: for whoever calls it is an authentication failure, not an
                        // input/output problem.
            final LoginException le = e.getCause() instanceof java.security.GeneralSecurityException
                    ? new FailedLoginException("the password does not open the store")
                    : new LoginException("the key store could not be read");
            le.initCause(e);
            throw le;
        } finally {
            if (keyPassword != storePassword) {
                wipe(keyPassword);
            }
            wipe(storePassword);
        }

        if (debug) {
            System.out.println("\t\t[KeyStoreLoginModule]: entro " + principal);
        }
        succeeded = true;
        return true;
    }

    private KeyStore open(final char[] pass)
            throws java.security.GeneralSecurityException, IOException {
        final KeyStore ks = keyStoreProvider == null
                ? KeyStore.getInstance(keyStoreType)
                : KeyStore.getInstance(keyStoreType, keyStoreProvider);
        if (keyStoreURL == null || "NONE".equals(keyStoreURL)) {
            // "NONE" is what is used with a cryptographic token: the store is not a file and
                        // the load with no stream tells the provider to look for the data by
                        // itself.
            ks.load(null, protectedPath ? null : pass);
            return ks;
        }
        final InputStream in = new URL(keyStoreURL).openStream();
        try {
            ks.load(in, protectedPath ? null : pass);
        } finally {
            in.close();
        }
        return ks;
    }

    private String alias() throws LoginException {
        if (keyStoreAlias != null) {
            return keyStoreAlias;
        }
        if (callbackHandler == null) {
            throw new LoginException(
                    "the keyStoreAlias option or a CallbackHandler to ask for it is needed");
        }
        final NameCallback cb = new NameCallback("Keystore alias: ");
        ask(new Callback[] { cb });
        final String n = cb.getName();
        if (n == null || n.length() == 0) {
            throw new LoginException("an alias was not given");
        }
        return n;
    }

    /**
     * The password, from the configured URL or asked for.
     *
     * <p>With {@code protected} at {@code true} neither of the two is asked for: the store asks
     * for them on its own, and asking for them here as well would be asking the user to write
     * them where they do not belong.
     */
    private char[] password(final int type, final String url, final String prompt)
            throws LoginException {
        if (url != null) {
            return readFromUrl(url);
        }
        if (protectedPath || callbackHandler == null) {
            return null;
        }
        final PasswordCallback cb = new PasswordCallback(prompt, false);
        ask(new Callback[] { cb });
        final char[] p = cb.getPassword();
        cb.clearPassword();
        return p;
    }

    private static char[] readFromUrl(final String url) throws LoginException {
        try {
            final InputStream in = new URL(url).openStream();
            try {
                final java.io.BufferedReader r = new java.io.BufferedReader(
                        new java.io.InputStreamReader(in, "UTF-8"));
                final String line = r.readLine();
                return line == null ? new char[0] : line.toCharArray();
            } finally {
                in.close();
            }
        } catch (final IOException e) {
            final LoginException le =
                    new LoginException("the password could not be read from " + url);
            le.initCause(e);
            throw le;
        }
    }

    private void ask(final Callback[] cbs) throws LoginException {
        try {
            callbackHandler.handle(cbs);
        } catch (final IOException e) {
            final LoginException le = new LoginException("the CallbackHandler failed");
            le.initCause(e);
            throw le;
        } catch (final UnsupportedCallbackException e) {
            final LoginException le =
                    new LoginException("the CallbackHandler does not support " + e.getCallback());
            le.initCause(e);
            throw le;
        }
    }

    private static void wipe(final char[] p) {
        if (p != null) {
            // To overwrite and not to trust the collector: an array of char with a password may
                    // stay in memory until somebody treads on it, and a dump would show it.
            java.util.Arrays.fill(p, ' ');
        }
    }

    /**
     * It puts the principal, the certificate chain and the private key in the {@link Subject}.
     *
     * @return {@code true} if this module had had success in {@link #login}
     * @throws LoginException if the {@code Subject} is read-only
     */
    public boolean commit() throws LoginException {
        if (!succeeded) {
            return false;
        }
        if (subject.isReadOnly()) {
            reset();
            throw new LoginException("Subject is ReadOnly");
        }
        if (!subject.getPrincipals().contains(principal)) {
            subject.getPrincipals().add(principal);
        }
        // The chain goes as a public credential and the key as a private one: the chain is
                // shown to anybody so that they should verify, the key does not leave here.
        if (!subject.getPublicCredentials().contains(certP)) {
            subject.getPublicCredentials().add(certP);
        }
        if (!subject.getPrivateCredentials().contains(privateCredential)) {
            subject.getPrivateCredentials().add(privateCredential);
        }
        commitSucceeded = true;
        return true;
    }

    /**
     * It undoes what this module did, because the authentication as a whole failed.
     *
     * @return {@code true} if this module had had success in {@link #login}
     * @throws LoginException if the {@code Subject} is read-only
     */
    public boolean abort() throws LoginException {
        if (!succeeded) {
            return false;
        }
        if (!commitSucceeded) {
            succeeded = false;
            reset();
        } else {
            logout();
        }
        return true;
    }

    /**
     * It takes out of the {@link Subject} what this module had put in, and destroys the private
     * key.
     *
     * @return {@code true} always
     * @throws LoginException if the {@code Subject} is read-only
     */
    public boolean logout() throws LoginException {
        if (subject.isReadOnly()) {
            reset();
            throw new LoginException("Subject is ReadOnly");
        }
        if (principal != null) {
            subject.getPrincipals().remove(principal);
        }
        if (certP != null) {
            subject.getPublicCredentials().remove(certP);
        }
        if (privateCredential != null) {
            subject.getPrivateCredentials().remove(privateCredential);
            try {
                // destroy() releases the credential's references; it does not erase the key,
                                // which goes on living if somebody else has it. It has to be called
                                // all the same: it is what marks the credential as destroyed for
                                // whoever consults it, and it is what the JDK does here.
                privateCredential.destroy();
            } catch (final javax.security.auth.DestroyFailedException e) {
                final LoginException le =
                        new LoginException("the private credential could not be destroyed");
                le.initCause(e);
                throw le;
            }
        }
        succeeded = false;
        commitSucceeded = false;
        reset();
        return true;
    }

    private void reset() {
        principal = null;
        certP = null;
        privateCredential = null;
    }
}
