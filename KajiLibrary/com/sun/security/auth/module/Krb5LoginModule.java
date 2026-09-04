package com.sun.security.auth.module;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * El modulo JAAS que autentica contra Kerberos.
 *
 * <h2>Que hace distinto a Kerberos</h2>
 *
 * <p>Que la contrasena no viaja. El cliente le pide al centro de distribucion de claves un
 * <strong>ticket</strong> para conceder tickets, y la respuesta viene cifrada con una clave
 * derivada de la contrasena del usuario. Si el cliente puede descifrarla, sabia la contrasena — y
 * el servidor nunca la vio pasar.
 *
 * <p>De ahi sale la otra propiedad, la que hace que valga la pena: con ese ticket el usuario obtiene
 * tickets para cada servicio sin volver a escribir nada. Es el inicio de sesion unico, y no es un
 * agregado sino una consecuencia directa del diseno.
 *
 * <h2>Que deja en el {@link Subject}</h2>
 *
 * <p>Un {@link KerberosPrincipal} con el nombre completo ({@code usuario@REINO}) y, como credencial
 * <strong>privada</strong>, el {@link KerberosTicket}. La division es la de siempre: el principal
 * dice quien es, el ticket es lo que permite actuar.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Este modulo no esta implementado, y a diferencia de sus companeros de paquete no le falta un
 * paso sino <strong>todo el trabajo</strong>: hablar Kerberos es implementar el protocolo — el
 * intercambio con el centro de distribucion, la codificacion ASN.1 de los mensajes, las funciones
 * de derivacion de clave de cada tipo de cifrado, la cache de credenciales y el analisis del
 * archivo de configuracion del reino. En el JDK eso vive en {@code sun.security.krb5}, que son
 * decenas de clases y no es API publica.
 *
 * <p>Por eso {@link #login} lanza {@link LoginException} diciendo esto mismo, y los otros tres
 * contestan lo que el contrato pide de un modulo que no autentico. La alternativa —una maquina de
 * estados que devuelva {@code true} sin haber autenticado a nadie— seria un modulo que compila,
 * corre y deja pasar a cualquiera.
 *
 * @since 1.4
 */
public class Krb5LoginModule implements LoginModule {

    private static final String NO_HAY =
            "Krb5LoginModule necesita una implementacion del protocolo Kerberos (intercambio con "
            + "el KDC, ASN.1, derivacion de claves y cache de credenciales), que esta biblioteca "
            + "no tiene";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    /** Para la configuracion de JAAS, que lo instancia por reflexion. */
    public Krb5LoginModule() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Guarda lo que recibe. No falla: la firma no permite avisar, y fallar aca impediria que
     * una configuracion con varios modulos llegara siquiera a inicializar los otros.
     */
    public void initialize(final Subject subject, final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
    }

    /**
     * {@inheritDoc}
     *
     * @throws LoginException siempre; ver la nota de la clase
     */
    public boolean login() throws LoginException {
        throw new LoginException(NO_HAY);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Devuelve {@code false}, que es lo que el contrato de JAAS pide de un modulo cuyo
     * {@link #login} no tuvo exito. En el flujo normal ni siquiera se llama —un {@code login} que
     * falla lleva a {@link #abort}— pero un llamador directo tiene que recibir la respuesta del
     * contrato y no una excepcion.
     *
     * @return {@code false}
     */
    public boolean commit() throws LoginException {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Devuelve {@code false} en lugar de fallar: {@link #login} nunca tuvo exito, y el contrato
     * de JAAS dice que un modulo que no autentico contesta {@code false} al abortar. Hacerlo fallar
     * romperia el aborto de toda la configuracion por culpa de un modulo que no hizo nada.
     *
     * @return {@code false}
     */
    public boolean abort() throws LoginException {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Devuelve {@code false} por la misma razon que {@link #abort}: no hay nada que sacar del
     * {@link Subject} porque este modulo nunca puso nada.
     *
     * @return {@code false}
     */
    public boolean logout() throws LoginException {
        return false;
    }
}
