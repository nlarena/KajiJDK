package com.sun.security.auth.module;

import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.sun.security.auth.NTDomainPrincipal;
import com.sun.security.auth.NTNumericCredential;
import com.sun.security.auth.NTSidDomainPrincipal;
import com.sun.security.auth.NTSidGroupPrincipal;
import com.sun.security.auth.NTSidPrimaryGroupPrincipal;
import com.sun.security.auth.NTSidUserPrincipal;
import com.sun.security.auth.NTUserPrincipal;

/**
 * El modulo JAAS que toma la identidad que Windows ya establecio.
 *
 * <h2>Por que agrega tantos principales</h2>
 *
 * <p>Porque nombre y SID no son intercambiables y hacen falta los dos. El {@link NTUserPrincipal}
 * lleva el nombre, que es lo que un humano lee y lo que sirve para un mensaje; el
 * {@link NTSidUserPrincipal} lleva el SID, que es lo unico que se puede comparar contra una lista
 * de control de acceso de Windows.
 *
 * <p>Una politica de seguridad escrita contra el nombre es fragil —el nombre se puede reasignar— y
 * una escrita contra el SID es ilegible. Poniendo los dos, cada uso elige el que le sirve.
 *
 * <h2>La credencial privada</h2>
 *
 * <p>Ademas de los principales, {@link #commit} agrega un {@link NTNumericCredential} con el token
 * de suplantacion. Es lo que permite que codigo Java que corre bajo este {@code Subject} le pida a
 * Windows que actue como ese usuario. Va como credencial <strong>privada</strong> y no como
 * principal justamente porque es una capacidad y no una identidad: quien la tiene puede actuar,
 * quien tiene el principal solo puede ser reconocido.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>La maquina de estados de JAAS esta completa y es la del JDK. El unico paso que falla es
 * construir el {@link NTSystem}, que viene de la API de Windows; {@link #login} lo convierte en
 * {@link FailedLoginException} con la causa encadenada.
 *
 * @since 1.4
 */
public class NTLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    private boolean debug;
    private boolean debugNative;

    private NTSystem ntSystem;
    private NTUserPrincipal userPrincipal;
    private NTSidUserPrincipal userSid;
    private NTDomainPrincipal domainPrincipal;
    private NTSidDomainPrincipal domainSid;
    private NTSidPrimaryGroupPrincipal primaryGroupSid;
    private final List<NTSidGroupPrincipal> gruposSid = new LinkedList<NTSidGroupPrincipal>();
    private NTNumericCredential credencial;

    private boolean succeeded;
    private boolean commitSucceeded;

    /** Para la configuracion de JAAS, que lo instancia por reflexion. */
    public NTLoginModule() {
    }

    /** {@inheritDoc} */
    public void initialize(final Subject subject, final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        this.debugNative = "true".equalsIgnoreCase((String) options.get("debugNative"));
        // debugNative implica debug: pedir el detalle de la capa nativa sin el resto del rastro
        // daria lineas sueltas sin contexto.
        if (debugNative) {
            this.debug = true;
        }
    }

    /**
     * Averigua quien es el usuario de Windows y arma sus principales.
     *
     * @return {@code true} si se pudo
     * @throws FailedLoginException si no se pudo averiguar quien es el usuario
     */
    public boolean login() throws LoginException {
        try {
            ntSystem = new NTSystem();
        } catch (final UnsupportedOperationException e) {
            succeeded = false;
            final FailedLoginException f =
                    new FailedLoginException("Failed in attempt to import "
                            + "the underlying NT system identity information");
            f.initCause(e);
            throw f;
        }

        if (ntSystem.getName() != null) {
            userPrincipal = new NTUserPrincipal(ntSystem.getName());
        }
        if (ntSystem.getUserSID() != null) {
            userSid = new NTSidUserPrincipal(ntSystem.getUserSID());
        }
        if (ntSystem.getDomain() != null) {
            domainPrincipal = new NTDomainPrincipal(ntSystem.getDomain());
        }
        if (ntSystem.getDomainSID() != null) {
            domainSid = new NTSidDomainPrincipal(ntSystem.getDomainSID());
        }
        if (ntSystem.getPrimaryGroupID() != null) {
            primaryGroupSid = new NTSidPrimaryGroupPrincipal(ntSystem.getPrimaryGroupID());
        }
        final String[] grupos = ntSystem.getGroupIDs();
        if (grupos != null) {
            for (int i = 0; i < grupos.length; i++) {
                if (grupos[i] != null) {
                    gruposSid.add(new NTSidGroupPrincipal(grupos[i]));
                }
            }
        }
        credencial = new NTNumericCredential(ntSystem.getImpersonationToken());
        succeeded = true;
        return true;
    }

    /**
     * Pone los principales y la credencial en el {@link Subject}.
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
        agregar(userSid);
        agregar(domainPrincipal);
        agregar(domainSid);
        agregar(primaryGroupSid);
        for (final NTSidGroupPrincipal g : gruposSid) {
            agregar(g);
        }
        if (credencial != null
                && !subject.getPrivateCredentials().contains(credencial)) {
            subject.getPrivateCredentials().add(credencial);
        }
        commitSucceeded = true;
        return true;
    }

    private void agregar(final Principal p) {
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
     * Saca del {@link Subject} lo que este modulo habia puesto.
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
        sacar(userSid);
        sacar(domainPrincipal);
        sacar(domainSid);
        sacar(primaryGroupSid);
        for (final NTSidGroupPrincipal g : gruposSid) {
            sacar(g);
        }
        if (credencial != null) {
            subject.getPrivateCredentials().remove(credencial);
        }
        succeeded = false;
        commitSucceeded = false;
        limpiar();
        return true;
    }

    private void sacar(final Principal p) {
        if (p != null) {
            subject.getPrincipals().remove(p);
        }
    }

    private void limpiar() {
        ntSystem = null;
        userPrincipal = null;
        userSid = null;
        domainPrincipal = null;
        domainSid = null;
        primaryGroupSid = null;
        gruposSid.clear();
        credencial = null;
    }
}
