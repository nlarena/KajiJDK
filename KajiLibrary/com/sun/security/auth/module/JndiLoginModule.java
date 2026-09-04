package com.sun.security.auth.module;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
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

import com.sun.security.auth.UnixNumericGroupPrincipal;
import com.sun.security.auth.UnixNumericUserPrincipal;
import com.sun.security.auth.UnixPrincipal;

/**
 * El modulo JAAS que autentica contra un directorio estilo NIS o LDAP leido por JNDI.
 *
 * <h2>En que se diferencia de {@link LdapLoginModule}</h2>
 *
 * <p>En donde se verifica la contrasena. {@code LdapLoginModule} se <strong>ata</strong> al
 * directorio con las credenciales del usuario y deja que el servidor decida. Este modulo, en
 * cambio, <strong>lee</strong> el atributo {@code userPassword} y lo compara aca.
 *
 * <p>Esa diferencia no es de estilo: leer la contrasena cifrada obliga a que el cliente conozca el
 * algoritmo con el que se cifro, que en un mapa NIS clasico es {@code crypt(3)}. Atarse no obliga a
 * nada porque el algoritmo queda del lado del servidor. Por eso el de LDAP envejecio mejor.
 *
 * <h2>Las dos URL</h2>
 *
 * <p>{@link #USER_PROVIDER} apunta al mapa de usuarios y {@link #GROUP_PROVIDER} al de grupos.
 * Estan separadas porque en NIS son dos mapas distintos y pueden vivir en servidores distintos.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Todo lo que se ve aca es real: los callbacks, las dos consultas al directorio, la lectura de
 * los atributos {@code uid}, {@code gid} y {@code userPassword}, y el armado de los principales.
 * Faltan dos piezas debajo, y las dos fallan diciendo cual son:
 *
 * <ul>
 *   <li>un <strong>proveedor JNDI</strong> que hable con el directorio — sin el,
 *       {@link InitialDirContext} lanza {@link NamingException};
 *   <li><strong>{@code crypt(3)}</strong>, el cifrado de contrasenas de Unix. No esta escrito a
 *       proposito: es DES con tablas grandes, y una tabla mal transcripta produciria un modulo que
 *       compila, corre, y acepta o rechaza contrasenas equivocadas sin avisar. La comparacion
 *       lanza {@link LoginException} nombrando lo que falta, que es la unica forma segura de no
 *       tenerlo.
 * </ul>
 *
 * @since 1.4
 */
public class JndiLoginModule implements LoginModule {

    /** El nombre de la opcion con la URL del mapa de usuarios. */
    public final String USER_PROVIDER = "user.provider.url";

    /** El nombre de la opcion con la URL del mapa de grupos. */
    public final String GROUP_PROVIDER = "group.provider.url";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    private boolean debug;
    private String userProvider;
    private String groupProvider;

    private String nombreUsuario;
    private UnixPrincipal userPrincipal;
    private UnixNumericUserPrincipal uidPrincipal;
    private UnixNumericGroupPrincipal gidPrincipal;
    private final List<UnixNumericGroupPrincipal> gruposExtra =
            new LinkedList<UnixNumericGroupPrincipal>();

    private boolean succeeded;
    private boolean commitSucceeded;

    /** Para la configuracion de JAAS, que lo instancia por reflexion. */
    public JndiLoginModule() {
    }

    /** {@inheritDoc} */
    public void initialize(final Subject subject, final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        this.userProvider = (String) options.get(USER_PROVIDER);
        this.groupProvider = (String) options.get(GROUP_PROVIDER);
    }

    /**
     * Pide nombre y contrasena, busca al usuario en el directorio y verifica la contrasena.
     *
     * @return {@code true} si la contrasena era buena
     * @throws FailedLoginException si el usuario no esta o la contrasena no coincide
     * @throws LoginException si falta configuracion o no se pudo hablar con el directorio
     */
    public boolean login() throws LoginException {
        if (userProvider == null) {
            throw new LoginException("falta la opcion " + USER_PROVIDER);
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
            final Attributes attrs = buscarUsuario(nombreUsuario);
            final String cifrada = valor(attrs, "userPassword");
            if (cifrada == null) {
                throw new FailedLoginException(
                        "el directorio no devolvio la contrasena de " + nombreUsuario);
            }
            verificar(pass, cifrada);

            userPrincipal = new UnixPrincipal(nombreUsuario);
            final String uid = valor(attrs, "uidNumber");
            if (uid == null) {
                uidPrincipal = null;
            } else {
                uidPrincipal = new UnixNumericUserPrincipal(uid);
            }
            final String gid = valor(attrs, "gidNumber");
            if (gid != null) {
                gidPrincipal = new UnixNumericGroupPrincipal(gid, true);
            }
            if (groupProvider != null) {
                for (final String g : buscarGrupos(nombreUsuario)) {
                    gruposExtra.add(new UnixNumericGroupPrincipal(g, false));
                }
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
            System.out.println("\t\t[JndiLoginModule]: entro " + nombreUsuario);
        }
        succeeded = true;
        return true;
    }

    /**
     * Compara la contrasena escrita contra la cifrada que devolvio el directorio.
     *
     * @throws LoginException siempre, en esta biblioteca; ver la nota de la clase sobre
     *     {@code crypt(3)}
     */
    private static void verificar(final char[] escrita, final String cifrada)
            throws LoginException {
        throw new LoginException(
                "la verificacion necesita crypt(3), que esta biblioteca no implementa; una tabla "
                + "DES mal transcripta aceptaria o rechazaria contrasenas sin avisar, asi que "
                + "falta a proposito");
    }

    private Attributes buscarUsuario(final String usuario)
            throws NamingException, LoginException {
        final DirContext ctx = new InitialDirContext(entorno(userProvider));
        try {
            final SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setCountLimit(2);
            final NamingEnumeration<SearchResult> r = ctx.search("", "(uid=" + usuario + ")", sc);
            if (!r.hasMore()) {
                throw new FailedLoginException("no se encontro al usuario " + usuario);
            }
            final SearchResult primero = r.next();
            if (r.hasMore()) {
                throw new FailedLoginException("hay mas de una entrada para " + usuario);
            }
            return primero.getAttributes();
        } finally {
            ctx.close();
        }
    }

    private List<String> buscarGrupos(final String usuario) throws NamingException {
        final List<String> out = new LinkedList<String>();
        final DirContext ctx = new InitialDirContext(entorno(groupProvider));
        try {
            final SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            final NamingEnumeration<SearchResult> r =
                    ctx.search("", "(memberUid=" + usuario + ")", sc);
            while (r.hasMore()) {
                final String gid = valor(r.next().getAttributes(), "gidNumber");
                if (gid != null) {
                    out.add(gid);
                }
            }
            return out;
        } finally {
            ctx.close();
        }
    }

    private static Hashtable<String, Object> entorno(final String url) {
        final Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.PROVIDER_URL, url);
        return env;
    }

    /** El primer valor de un atributo, o {@code null} si no vino. */
    private static String valor(final Attributes attrs, final String nombre)
            throws NamingException {
        if (attrs == null) {
            return null;
        }
        final Attribute a = attrs.get(nombre);
        if (a == null || a.size() == 0) {
            return null;
        }
        final Object v = a.get();
        if (v instanceof byte[]) {
            // Un atributo binario, que es como viaja userPassword en varios servidores.
            return new String((byte[]) v, java.nio.charset.StandardCharsets.UTF_8);
        }
        return v == null ? null : v.toString();
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
        agregar(uidPrincipal);
        agregar(gidPrincipal);
        for (final UnixNumericGroupPrincipal g : gruposExtra) {
            agregar(g);
        }
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
        sacar(uidPrincipal);
        sacar(gidPrincipal);
        for (final UnixNumericGroupPrincipal g : gruposExtra) {
            sacar(g);
        }
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
        uidPrincipal = null;
        gidPrincipal = null;
        gruposExtra.clear();
    }
}
