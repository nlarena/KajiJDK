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
 * El modulo JAAS que autentica con un certificado guardado en un almacen de claves.
 *
 * <h2>Que significa autenticarse con un certificado</h2>
 *
 * <p>Que el usuario demuestra tener la clave privada que corresponde a un certificado. Este modulo
 * hace la parte facil: abre el almacen y saca la clave. La demostracion criptografica la hace
 * despues quien use la credencial que este modulo deja en el {@link Subject} — firmar algo con ella
 * es lo que prueba la identidad del otro lado.
 *
 * <p>Por eso el resultado no es un booleano sino una <strong>credencial privada</strong>: no
 * alcanza con saber quien dice ser, hace falta poder actuar como el.
 *
 * <h2>Las dos contrasenas</h2>
 *
 * <p>Una abre el almacen, otra abre la clave privada dentro de el. Son distintas a proposito: el
 * almacen puede tener las claves de varios usuarios, y la contrasena del almacen no deberia dar
 * acceso a la clave de nadie. Cuando no se da la segunda, se usa la primera — que es el caso comun
 * de un almacen de un solo usuario.
 *
 * <h2>Las opciones</h2>
 *
 * <ul>
 *   <li>{@code keyStoreURL} — de donde leer el almacen. {@code "NONE"} para los que no son un
 *       archivo, como un token criptografico.
 *   <li>{@code keyStoreType} — el tipo; por omision el del sistema.
 *   <li>{@code keyStoreProvider} — el proveedor, si se quiere uno en particular.
 *   <li>{@code keyStoreAlias} — el alias de la entrada; si falta, se pregunta.
 *   <li>{@code keyStorePasswordURL} — de donde leer la contrasena del almacen; si falta, se
 *       pregunta.
 *   <li>{@code privateKeyPasswordURL} — idem para la de la clave privada.
 *   <li>{@code protected} — {@code true} cuando el almacen pide la contrasena por su cuenta, por
 *       ejemplo un lector de tarjetas con teclado propio. Entonces no se pregunta nada.
 *   <li>{@code debug} — deja rastro por la salida estandar.
 * </ul>
 *
 * @since 1.4
 */
public class KeyStoreLoginModule implements LoginModule {

    private static final int TIPO_ALMACEN = 0;
    private static final int TIPO_CLAVE = 1;

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

    /** Para la configuracion de JAAS, que lo instancia por reflexion. */
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
     * Abre el almacen y saca el certificado y la clave privada del alias pedido.
     *
     * @return {@code true} si se pudo
     * @throws FailedLoginException si el alias no existe o la contrasena no abre
     * @throws LoginException si falta configuracion o el almacen no se pudo leer
     */
    public boolean login() throws LoginException {
        final String alias = alias();
        final char[] passAlmacen = contrasena(TIPO_ALMACEN, keyStorePasswordURL,
                "Keystore password: ");
        char[] passClave = contrasena(TIPO_CLAVE, privateKeyPasswordURL,
                "Private key password (optional): ");
        // Sin contrasena propia para la clave se usa la del almacen: es el caso de un almacen de
        // un solo usuario, donde tener dos secretos distintos no protegeria de nada.
        if (passClave == null || passClave.length == 0) {
            passClave = passAlmacen;
        }

        try {
            final KeyStore ks = abrir(passAlmacen);
            final Certificate[] cadena = ks.getCertificateChain(alias);
            if (cadena == null || cadena.length == 0) {
                throw new FailedLoginException(
                        "no hay una cadena de certificados para el alias " + alias);
            }
            final java.security.Key clave = ks.getKey(alias, passClave);
            if (!(clave instanceof PrivateKey)) {
                throw new FailedLoginException(
                        "la entrada " + alias + " no tiene una clave privada");
            }

            final List<Certificate> lista = new ArrayList<Certificate>(Arrays.asList(cadena));
            this.certP = CertificateFactory.getInstance("X.509").generateCertPath(lista);
            final X509Certificate hoja = (X509Certificate) cadena[0];
            this.principal = hoja.getSubjectX500Principal();
            this.privateCredential =
                    new X500PrivateCredential(hoja, (PrivateKey) clave, alias);
        } catch (final LoginException e) {
            throw e;
        } catch (final java.security.GeneralSecurityException e) {
            limpiar();
            final LoginException le = new LoginException("no se pudo leer el almacen de claves");
            le.initCause(e);
            throw le;
        } catch (final IOException e) {
            limpiar();
            // La contrasena equivocada de un almacen llega como IOException con causa
            // UnrecoverableKeyException, no como excepcion de seguridad. Distinguirla importa:
            // para el que llama es un fallo de autenticacion, no un problema de entrada/salida.
            final LoginException le = e.getCause() instanceof java.security.GeneralSecurityException
                    ? new FailedLoginException("la contrasena no abre el almacen")
                    : new LoginException("no se pudo leer el almacen de claves");
            le.initCause(e);
            throw le;
        } finally {
            if (passClave != passAlmacen) {
                borrar(passClave);
            }
            borrar(passAlmacen);
        }

        if (debug) {
            System.out.println("\t\t[KeyStoreLoginModule]: entro " + principal);
        }
        succeeded = true;
        return true;
    }

    private KeyStore abrir(final char[] pass)
            throws java.security.GeneralSecurityException, IOException {
        final KeyStore ks = keyStoreProvider == null
                ? KeyStore.getInstance(keyStoreType)
                : KeyStore.getInstance(keyStoreType, keyStoreProvider);
        if (keyStoreURL == null || "NONE".equals(keyStoreURL)) {
            // "NONE" es lo que se usa con un token criptografico: el almacen no es un archivo y
            // la carga sin flujo le dice al proveedor que se busque los datos solo.
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
                    "hace falta la opcion keyStoreAlias o un CallbackHandler para preguntarlo");
        }
        final NameCallback cb = new NameCallback("Keystore alias: ");
        preguntar(new Callback[] { cb });
        final String n = cb.getName();
        if (n == null || n.length() == 0) {
            throw new LoginException("no se dio un alias");
        }
        return n;
    }

    /**
     * La contrasena, de la URL configurada o preguntada.
     *
     * <p>Con {@code protected} en {@code true} no se pregunta ninguna de las dos: el almacen las
     * pide por su cuenta, y preguntarlas aca ademas seria pedirle al usuario que las escriba donde
     * no corresponde.
     */
    private char[] contrasena(final int tipo, final String url, final String prompt)
            throws LoginException {
        if (url != null) {
            return leerDeUrl(url);
        }
        if (protectedPath || callbackHandler == null) {
            return null;
        }
        final PasswordCallback cb = new PasswordCallback(prompt, false);
        preguntar(new Callback[] { cb });
        final char[] p = cb.getPassword();
        cb.clearPassword();
        return p;
    }

    private static char[] leerDeUrl(final String url) throws LoginException {
        try {
            final InputStream in = new URL(url).openStream();
            try {
                final java.io.BufferedReader r = new java.io.BufferedReader(
                        new java.io.InputStreamReader(in, "UTF-8"));
                final String linea = r.readLine();
                return linea == null ? new char[0] : linea.toCharArray();
            } finally {
                in.close();
            }
        } catch (final IOException e) {
            final LoginException le = new LoginException("no se pudo leer la contrasena de " + url);
            le.initCause(e);
            throw le;
        }
    }

    private void preguntar(final Callback[] cbs) throws LoginException {
        try {
            callbackHandler.handle(cbs);
        } catch (final IOException e) {
            final LoginException le = new LoginException("fallo el CallbackHandler");
            le.initCause(e);
            throw le;
        } catch (final UnsupportedCallbackException e) {
            final LoginException le =
                    new LoginException("el CallbackHandler no soporta " + e.getCallback());
            le.initCause(e);
            throw le;
        }
    }

    private static void borrar(final char[] p) {
        if (p != null) {
            // Sobrescribir y no confiar en el recolector: un arreglo de char con una contrasena
            // puede quedar en memoria hasta que alguien lo pise, y un volcado la mostraria.
            java.util.Arrays.fill(p, ' ');
        }
    }

    /**
     * Pone en el {@link Subject} el principal, la cadena de certificados y la clave privada.
     *
     * @return {@code true} si este modulo habia tenido exito en {@link #login}
     * @throws LoginException si el {@code Subject} es de solo lectura
     */
    public boolean commit() throws LoginException {
        if (!succeeded) {
            return false;
        }
        if (subject.isReadOnly()) {
            limpiar();
            throw new LoginException("Subject is ReadOnly");
        }
        if (!subject.getPrincipals().contains(principal)) {
            subject.getPrincipals().add(principal);
        }
        // La cadena va como credencial publica y la clave como privada: la cadena se le muestra a
        // cualquiera para que verifique, la clave no sale de aca.
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
     * Deshace lo que este modulo hizo, porque la autenticacion en conjunto fallo.
     *
     * @return {@code true} si este modulo habia tenido exito en {@link #login}
     * @throws LoginException si el {@code Subject} es de solo lectura
     */
    public boolean abort() throws LoginException {
        if (!succeeded) {
            return false;
        }
        if (!commitSucceeded) {
            succeeded = false;
            limpiar();
        } else {
            logout();
        }
        return true;
    }

    /**
     * Saca del {@link Subject} lo que este modulo habia puesto, y destruye la clave privada.
     *
     * @return {@code true} siempre
     * @throws LoginException si el {@code Subject} es de solo lectura
     */
    public boolean logout() throws LoginException {
        if (subject.isReadOnly()) {
            limpiar();
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
                // destroy() suelta las referencias de la credencial; no borra la clave, que
                // sigue viva si alguien mas la tiene. Igual hay que llamarlo: es lo que marca la
                // credencial como destruida para quien la consulte, y es lo que el JDK hace aca.
                privateCredential.destroy();
            } catch (final javax.security.auth.DestroyFailedException e) {
                final LoginException le =
                        new LoginException("no se pudo destruir la credencial privada");
                le.initCause(e);
                throw le;
            }
        }
        succeeded = false;
        commitSucceeded = false;
        limpiar();
        return true;
    }

    private void limpiar() {
        principal = null;
        certP = null;
        privateCredential = null;
    }
}
