package com.sun.security.auth.module;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;

import com.sun.security.auth.UserPrincipal;

/**
 * El modulo JAAS que autentica contra un directorio LDAP.
 *
 * <h2>Como se autentica contra LDAP: atandose, no comparando</h2>
 *
 * <p>No se lee la contrasena del directorio para compararla — eso ni siquiera es posible, porque el
 * directorio devuelve el atributo cifrado o directamente no lo devuelve. Lo que se hace es
 * <strong>atarse</strong> (bind) al directorio con el nombre y la contrasena del usuario: si el
 * servidor acepta la atadura, la contrasena era buena; si la rechaza, no.
 *
 * <p>La consecuencia es que el algoritmo de cifrado de contrasenas es problema del servidor y no de
 * este codigo, y que la contrasena nunca se compara aca.
 *
 * <h2>Los dos modos</h2>
 *
 * <p><strong>Autenticacion con busqueda</strong>: primero una atadura con una cuenta de servicio
 * para <em>encontrar</em> el DN del usuario a partir de su nombre, y despues la atadura de verdad
 * con ese DN. Hace falta cuando el nombre que escribe el usuario no es su DN, que es lo habitual.
 *
 * <p><strong>Autenticacion directa</strong>: el DN se arma con una plantilla
 * ({@code userDNPattern}), sin busqueda previa. Es una atadura en vez de tres operaciones, pero
 * exige que todos los usuarios esten bajo la misma rama.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>El codigo de aca es el real y completo: arma el entorno JNDI, hace la busqueda si corresponde,
 * se ata con las credenciales del usuario y traduce el rechazo a
 * {@link FailedLoginException}. Lo que hace falta debajo es un <strong>proveedor JNDI de
 * LDAP</strong>, que es quien habla el protocolo; sin el, {@link InitialDirContext} falla con
 * {@link NamingException} y {@link #login} la envuelve en {@link LoginException} con la causa.
 *
 * @since 1.6
 */
public class LdapLoginModule implements LoginModule {

    private static final String FABRICA_LDAP = "com.sun.jndi.ldap.LdapCtxFactory";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    private boolean debug;
    private boolean useSSL = true;
    private String userProvider;
    private String userFilter;
    private String authIdentity;
    private String authzIdentity;
    private String userDNPattern;

    private String nombreUsuario;
    private UserPrincipal userPrincipal;
    private X500Principal dnPrincipal;
    private UserPrincipal authzPrincipal;

    private boolean succeeded;
    private boolean commitSucceeded;

    /** Para la configuracion de JAAS, que lo instancia por reflexion. */
    public LdapLoginModule() {
    }

    /** {@inheritDoc} */
    public void initialize(final Subject subject, final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        final String ssl = (String) options.get("useSSL");
        this.useSSL = ssl == null || "true".equalsIgnoreCase(ssl);
        this.userProvider = (String) options.get("userProvider");
        this.userFilter = (String) options.get("userFilter");
        this.authIdentity = (String) options.get("authIdentity");
        this.authzIdentity = (String) options.get("authzIdentity");
        this.userDNPattern = (String) options.get("userDNPattern");
    }

    /**
     * Pide nombre y contrasena, y se ata al directorio con ellos.
     *
     * @return {@code true} si la atadura fue aceptada
     * @throws FailedLoginException si el directorio rechazo las credenciales
     * @throws LoginException si falta configuracion o no se pudo hablar con el directorio
     */
    public boolean login() throws LoginException {
        if (userProvider == null) {
            throw new LoginException("falta la opcion userProvider");
        }
        if (userFilter == null && userDNPattern == null) {
            throw new LoginException("hace falta userFilter o userDNPattern");
        }

        final NameCallback nc = new NameCallback("Nombre de usuario: ");
        final PasswordCallback pc = new PasswordCallback("Contrasena: ", false);
        preguntar(new Callback[] { nc, pc });
        nombreUsuario = nc.getName();
        final char[] pass = pc.getPassword();
        pc.clearPassword();
        if (nombreUsuario == null || nombreUsuario.length() == 0 || pass == null) {
            throw new FailedLoginException("faltan el nombre o la contrasena");
        }

        try {
            final String dn = userDNPattern != null
                    ? userDNPattern.replace("{USERNAME}", nombreUsuario)
                    : buscarDN(nombreUsuario);
            atarse(dn, pass);

            userPrincipal = new UserPrincipal(nombreUsuario);
            dnPrincipal = new X500Principal(dn);
            if (authzIdentity != null) {
                authzPrincipal = new UserPrincipal(authzIdentity);
            }
        } catch (final NamingException e) {
            limpiar();
            final LoginException le =
                    new LoginException("no se pudo hablar con el directorio " + userProvider);
            le.initCause(e);
            throw le;
        } finally {
            Arrays.fill(pass, ' ');
        }

        if (debug) {
            System.out.println("\t\t[LdapLoginModule]: entro " + nombreUsuario);
        }
        succeeded = true;
        return true;
    }

    /**
     * El DN del usuario, buscandolo con una cuenta de servicio.
     *
     * <p>Hacen falta dos ataduras: esta, con la identidad de servicio o anonima, sirve solo para
     * <em>encontrar</em> al usuario. La que autentica es la de despues, con su DN y su contrasena.
     */
    private String buscarDN(final String usuario) throws NamingException, LoginException {
        final Hashtable<String, Object> env = entorno();
        if (authIdentity != null) {
            env.put(Context.SECURITY_PRINCIPAL, authIdentity);
        }
        final DirContext ctx = new InitialDirContext(env);
        try {
            final SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setReturningAttributes(new String[0]);
            sc.setCountLimit(2);
            final NamingEnumeration<SearchResult> r =
                    ctx.search("", userFilter.replace("{USERNAME}", usuario), sc);
            if (!r.hasMore()) {
                throw new FailedLoginException("no se encontro al usuario " + usuario);
            }
            final SearchResult primero = r.next();
            if (r.hasMore()) {
                // Dos coincidencias no es "elegir la primera": el filtro no identifica a nadie en
                // particular, y atarse con cualquiera de las dos seria autenticar al usuario
                // equivocado.
                throw new FailedLoginException(
                        "el filtro encontro mas de un usuario para " + usuario);
            }
            final String dn = primero.getNameInNamespace();
            return dn != null && dn.length() > 0 ? dn : primero.getName();
        } finally {
            ctx.close();
        }
    }

    /** La atadura que autentica: con el DN del usuario y su contrasena. */
    private void atarse(final String dn, final char[] pass) throws NamingException, LoginException {
        final Hashtable<String, Object> env = entorno();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, new String(pass));
        try {
            new InitialDirContext(env).close();
        } catch (final javax.naming.AuthenticationException e) {
            final FailedLoginException f = new FailedLoginException("credenciales rechazadas");
            f.initCause(e);
            throw f;
        }
    }

    private Hashtable<String, Object> entorno() {
        final Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, FABRICA_LDAP);
        env.put(Context.PROVIDER_URL, userProvider);
        if (useSSL) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        return env;
    }

    private void preguntar(final Callback[] cbs) throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("hace falta un CallbackHandler");
        }
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

    /**
     * Pone los principales en el {@link Subject}.
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
        agregar(userPrincipal);
        agregar(dnPrincipal);
        agregar(authzPrincipal);
        commitSucceeded = true;
        return true;
    }

    private void agregar(final java.security.Principal p) {
        if (p != null && !subject.getPrincipals().contains(p)) {
            subject.getPrincipals().add(p);
        }
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
     * Saca del {@link Subject} los principales que este modulo habia puesto.
     *
     * @return {@code true} siempre
     * @throws LoginException si el {@code Subject} es de solo lectura
     */
    public boolean logout() throws LoginException {
        if (subject.isReadOnly()) {
            limpiar();
            throw new LoginException("Subject is ReadOnly");
        }
        sacar(userPrincipal);
        sacar(dnPrincipal);
        sacar(authzPrincipal);
        succeeded = false;
        commitSucceeded = false;
        limpiar();
        return true;
    }

    private void sacar(final java.security.Principal p) {
        if (p != null) {
            subject.getPrincipals().remove(p);
        }
    }

    private void limpiar() {
        nombreUsuario = null;
        userPrincipal = null;
        dnPrincipal = null;
        authzPrincipal = null;
    }
}
