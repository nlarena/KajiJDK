package javax.security.auth.spi;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

/**
 * KajiLibrary's javax.security.auth.spi.LoginModule -- un mecanismo de autenticacion enchufable.
 *
 * <h2>Las dos fases, que es lo unico dificil de esta interfaz</h2>
 *
 * <p>Autenticar no es un metodo sino <b>dos</b>: {@link #login()} y {@link #commit()}. La razon es
 * que una configuracion puede apilar varios modulos --contraseña, certificado, segundo factor-- y
 * exigir que pasen todos. Si el primero escribiera los principals en el Subject apenas termina, y el
 * tercero fallara, el Subject quedaria con una identidad a medias: autenticado por uno y rechazado
 * por otro.
 *
 * <p>Por eso {@code login()} solo verifica y guarda el resultado <b>adentro del modulo</b>, y recien
 * {@code commit()} --que corre cuando <b>todos</b> pasaron-- lo escribe en el Subject. Si alguno
 * fallo se llama a {@link #abort()} y cada modulo tira lo suyo.
 *
 * <h2>Lo que devuelve cada metodo</h2>
 *
 * <p>{@code true} quiere decir "este modulo hizo algo", {@code false} quiere decir "no me tocaba".
 * Un modulo que no aplica --por ejemplo uno de tarjeta inteligente en una maquina sin lector--
 * devuelve false en vez de lanzar, y la configuracion sigue con el siguiente.
 *
 * <p><b>Esta biblioteca no trae ningun modulo</b>: la interfaz esta para que uno que se escriba
 * encaje, igual que {@code X509Certificate} esta sin que haya ningun parser de certificados.
 */
public interface LoginModule {

    /**
     * Le da al modulo lo que necesita antes de empezar.
     *
     * @param subject         donde se van a escribir las identidades, en {@link #commit()}
     * @param callbackHandler por donde se le pregunta al usuario. Ver
     *     {@link javax.security.auth.callback.Callback} para por que el modulo no pregunta solo
     * @param sharedState     lo que los modulos de la misma pila se pasan entre si -- tipicamente la
     *     contraseña, para que el segundo no se la vuelva a pedir al usuario
     * @param options         la configuracion de este modulo en esta pila
     */
    void initialize(Subject subject, CallbackHandler callbackHandler,
        java.util.Map<String, ?> sharedState, java.util.Map<String, ?> options);

    /**
     * Verifica. <b>No</b> escribe en el Subject; ver la nota de la clase.
     *
     * @return si este modulo hizo algo
     * @throws LoginException si la autenticacion fallo
     */
    boolean login() throws LoginException;

    /**
     * Escribe en el Subject lo que {@link #login()} verifico. Corre solo si <b>toda</b> la pila paso.
     *
     * @return si este modulo hizo algo
     * @throws LoginException si no se pudo escribir
     */
    boolean commit() throws LoginException;

    /**
     * Tira lo que {@link #login()} habia verificado. Corre cuando algun otro modulo de la pila fallo.
     *
     * @return si este modulo hizo algo
     * @throws LoginException si no se pudo deshacer
     */
    boolean abort() throws LoginException;

    /**
     * Saca del Subject lo que este modulo le puso.
     *
     * @return si este modulo hizo algo
     * @throws LoginException si no se pudo
     */
    boolean logout() throws LoginException;
}
