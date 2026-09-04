package com.sun.security.auth.module;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.sun.security.auth.UnixNumericGroupPrincipal;
import com.sun.security.auth.UnixNumericUserPrincipal;
import com.sun.security.auth.UnixPrincipal;

/**
 * El modulo JAAS que toma la identidad que el sistema Unix ya establecio.
 *
 * <h2>Que no hace: preguntar una contrasena</h2>
 *
 * <p>Este modulo no autentica a nadie. El usuario ya se autentico cuando entro al sistema, y lo que
 * hace este modulo es <strong>importar</strong> ese hecho al {@link Subject}: lee quien es el
 * dueno del proceso y agrega los principales que lo representan.
 *
 * <p>Por eso {@link #login} no usa el {@code CallbackHandler}. Si falla no es porque la contrasena
 * este mal, sino porque no se pudo averiguar quien es el usuario.
 *
 * <h2>Las dos fases, y por que no es una sola</h2>
 *
 * <p>JAAS separa {@link #login} de {@link #commit} porque una configuracion tiene varios modulos y
 * el resultado depende de todos. Primero corre el {@code login} de cada uno, y solo si el conjunto
 * es aceptable corre el {@code commit} de cada uno. Asi el {@code Subject} nunca queda a medio
 * llenar: o entran los principales de todos los modulos que debian entrar, o no entra ninguno.
 *
 * <p>{@link #abort} es la otra rama: alguien fallo, y este modulo tiene que deshacer. Por eso
 * distingue si ya habia hecho {@code commit} — si no lo hizo, alcanza con olvidar; si lo hizo, hay
 * que sacar del {@code Subject} lo que puso, y para eso llama a {@link #logout}.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>La maquina de estados de arriba esta completa y es la del JDK. Lo que falla es el unico paso
 * que necesita al sistema operativo: construir el {@link UnixSystem}, que sale de {@code getuid} y
 * companeras. {@link #login} lo convierte en {@link FailedLoginException} con la causa encadenada,
 * que es lo que el contrato pide — y es ademas el resultado correcto, porque no poder establecer
 * quien es el usuario tiene que ser un fallo de autenticacion y no un exito silencioso.
 *
 * @since 1.4
 */
public class UnixLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    private boolean debug;

    private UnixSystem ss;
    private UnixPrincipal userPrincipal;
    private UnixNumericUserPrincipal uidPrincipal;
    private UnixNumericGroupPrincipal gidPrincipal;
    private final List<UnixNumericGroupPrincipal> gruposExtra =
            new LinkedList<UnixNumericGroupPrincipal>();

    private boolean succeeded;
    private boolean commitSucceeded;

    /** Para la configuracion de JAAS, que lo instancia por reflexion. */
    public UnixLoginModule() {
    }

    /** {@inheritDoc} */
    public void initialize(final Subject subject, final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
    }

    /**
     * Averigua quien es el usuario del proceso y arma sus principales.
     *
     * <p>Todavia no los pone en el {@link Subject}: eso es {@link #commit}.
     *
     * @return {@code true} si se pudo
     * @throws FailedLoginException si no se pudo averiguar quien es el usuario
     */
    public boolean login() throws LoginException {
        try {
            ss = new UnixSystem();
        } catch (final UnsupportedOperationException e) {
            succeeded = false;
            final FailedLoginException f =
                    new FailedLoginException("Unable to get UNIX information");
            f.initCause(e);
            throw f;
        }

        userPrincipal = new UnixPrincipal(ss.getUsername());
        uidPrincipal = new UnixNumericUserPrincipal(ss.getUid());
        // gid 0 es el grupo root; el JDK no lo agrega como principal principal, y esta clase
        // reproduce esa decision.
        if (ss.getGid() != 0) {
            gidPrincipal = new UnixNumericGroupPrincipal(ss.getGid(), true);
        }
        final long[] grupos = ss.getGroups();
        if (grupos != null) {
            for (int i = 0; i < grupos.length; i++) {
                gruposExtra.add(new UnixNumericGroupPrincipal(grupos[i], false));
            }
        }
        if (debug) {
            System.out.println("\t\t[UnixLoginModule]: usuario " + ss.getUsername());
        }
        succeeded = true;
        return true;
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
        // El contains es necesario: el Subject puede traer principales de otro modulo o de una
        // autenticacion anterior, y agregar dos veces el mismo dejaria duplicados que despues
        // habria que sacar dos veces en el logout.
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
            // Nunca llego a tocar el Subject: alcanza con olvidar lo que averiguo.
            succeeded = false;
            limpiar();
        } else {
            // Ya habia puesto los principales; sacarlos es exactamente lo que hace logout.
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
        ss = null;
        userPrincipal = null;
        uidPrincipal = null;
        gidPrincipal = null;
        gruposExtra.clear();
    }
}
