package javax.security.auth.login;

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.spi.LoginModule;

/**
 * KajiLibrary's javax.security.auth.login.LoginContext -- corre la cadena de autenticacion.
 *
 * <p>Le pide a la {@link Configuration} la lista de modulos de un nombre, los instancia y los corre
 * en dos fases. El resultado, si sale bien, es un {@link Subject} lleno de principales y
 * credenciales.
 *
 * <h2>Por que dos fases</h2>
 *
 * <p>Primero se llama a {@code login()} en los modulos, y recien despues a {@code commit()}. Ningun
 * modulo escribe en el sujeto durante la primera fase.
 *
 * <p>Es lo que evita un sujeto a medio llenar. Con una sola fase, una cadena de tres modulos donde
 * el tercero falla dejaria adentro del sujeto los principales de los dos primeros, y la aplicacion
 * recibiria un sujeto que parece autenticado y no lo esta. Con dos, o entran todos o no entra
 * ninguno: el fracaso llama a {@code abort()}.
 *
 * <h2>Como decide la cadena</h2>
 *
 * <p>Las reglas de {@link AppConfigurationEntry.LoginModuleControlFlag} se aplican asi:
 *
 * <ul>
 *   <li>un modulo que anda y es <b>SUFFICIENT</b> corta el recorrido, salvo que antes haya fallado
 *       alguno obligatorio -- en ese caso el login ya esta perdido y cortar solo escondaria el
 *       motivo;
 *   <li>un <b>REQUISITE</b> que falla corta ahi mismo;
 *   <li>un <b>REQUIRED</b> que falla se anota y la cadena <b>sigue</b>. Seguir cuesta tiempo y es a
 *       proposito: si cortara, el tiempo de respuesta diria cual de los modulos rechazo, que es
 *       justo lo que no conviene contarle a quien esta probando;
 *   <li>si al final no fallo ningun obligatorio pero tampoco anduvo ninguno, se lanza
 *       {@link LoginException}: una cadena entera de opcionales que se desentienden no es un login
 *       exitoso.
 * </ul>
 *
 * <p>El primer error obligatorio es el que se lanza, no el ultimo: es el que dice donde empezo el
 * problema.
 *
 * <h2>Cuatro recorridos con la misma forma</h2>
 *
 * <p>{@code login}, {@code commit}, {@code abort} y {@code logout} recorren la misma lista con las
 * mismas reglas, y se diferencian en <b>una</b> cosa: los dos primeros cortan ante un SUFFICIENT que
 * anda y los dos ultimos no. La asimetria es necesaria. Cortar en la ida es la definicion de
 * "suficiente"; cortar en la vuelta dejaria modulos con estado sin enterarse de que la cadena
 * termino.
 *
 * <p>De ahi sale un detalle que sorprende la primera vez: un modulo que nunca llego a correr
 * <b>igual se instancia</b> cuando hay que abortar o cerrar sesion. Es lo correcto -- el recorrido
 * de limpieza tiene que llegar a todos los configurados-- y es lo que hace el JDK.
 *
 * <h2>El estado compartido</h2>
 *
 * <p>Los modulos de una cadena comparten un mapa que sobrevive entre ellos. Para eso esta: el primer
 * modulo le pide la contrasena a la persona una vez y la deja ahi, y los que siguen la usan sin
 * volver a preguntar.
 */
public class LoginContext {

    /** La propiedad de seguridad con el manejador por omision. */
    private static final String DEFAULT_HANDLER = "auth.login.defaultCallbackHandler";

    private final String name;
    private final CallbackHandler callbackHandler;
    private final Configuration configuration;
    private final AppConfigurationEntry[] entries;

    /** Uno por entrada; null hasta que ese modulo hace falta. */
    private final LoginModule[] modules;

    /** Compartido entre los modulos de esta cadena; ver la nota de la clase. */
    private final Map<String, Object> sharedState = new HashMap<String, Object>();

    /** Null hasta el primer {@link #login}, salvo que lo haya dado quien llama. */
    private Subject subject;

    /** Si el sujeto lo trajo quien llama; decide que devuelve {@link #getSubject} al fallar. */
    private final boolean subjectProvided;

    /** Si la ultima cadena cerro bien. */
    private boolean loginSucceeded = false;

    /** Con un sujeto nuevo y sin manejador propio. */
    public LoginContext(String name) throws LoginException {
        this(name, null, null, null);
    }

    /** Sobre un sujeto ya existente. */
    public LoginContext(String name, Subject subject) throws LoginException {
        this(name, subject, null, null);
    }

    /** Con un manejador que sabe preguntarle a la persona. */
    public LoginContext(String name, CallbackHandler callbackHandler) throws LoginException {
        this(name, null, callbackHandler, null);
    }

    /** Las dos cosas. */
    public LoginContext(String name, Subject subject, CallbackHandler callbackHandler)
        throws LoginException {
        this(name, subject, callbackHandler, null);
    }

    /**
     * Todo explicito, incluida la configuracion.
     *
     * <p>Pasarla aca es la forma de no depender de la global: dos partes del mismo proceso pueden
     * autenticarse con reglas distintas.
     *
     * @throws LoginException si el nombre es null o no tiene modulos configurados
     */
    public LoginContext(String name, Subject subject, CallbackHandler callbackHandler,
                        Configuration config) throws LoginException {
        if (name == null) {
            throw new LoginException("Invalid null input: name");
        }
        this.name = name;
        this.subject = subject;
        this.subjectProvided = subject != null;
        this.callbackHandler = (callbackHandler == null) ? defaultHandler() : callbackHandler;
        this.configuration = (config == null) ? Configuration.getConfiguration() : config;
        AppConfigurationEntry[] found = this.configuration.getAppConfigurationEntry(name);
        if (found == null || found.length == 0) {
            throw new LoginException("No LoginModules configured for " + name);
        }
        this.entries = found;
        this.modules = new LoginModule[found.length];
    }

    /**
     * Corre la cadena: {@code login} en todos, despues {@code commit}.
     *
     * <p>Si cualquiera de las dos fases falla, se aborta la cadena entera y se lanza el <b>primer</b>
     * error; el que salga del aborto no tapa al original.
     *
     * @throws LoginException si la autenticacion no cierra
     */
    public void login() throws LoginException {
        this.loginSucceeded = false;
        if (this.subject == null) {
            this.subject = new Subject();
        }
        try {
            invoke(Phase.LOGIN);
            invoke(Phase.COMMIT);
            this.loginSucceeded = true;
        } catch (LoginException first) {
            try {
                invoke(Phase.ABORT);
            } catch (LoginException ignored) {
                // El error del aborto no aporta nada y taparia al que de verdad importa.
            }
            throw first;
        }
    }

    /**
     * Deshace la autenticacion.
     *
     * <p>Recorre <b>todos</b> los modulos configurados, incluidos los que nunca corrieron; ver la
     * nota de la clase.
     *
     * @throws LoginException si nunca hubo login, o si un modulo falla al salir
     */
    public void logout() throws LoginException {
        if (this.subject == null) {
            throw new LoginException("null subject - logout called before login");
        }
        invoke(Phase.LOGOUT);
        this.loginSucceeded = false;
    }

    /**
     * El sujeto autenticado.
     *
     * @return null si el login no cerro y el sujeto no lo trajo quien llama -- devolver un sujeto
     *     vacio invitaria a confundirlo con uno autenticado sin permisos
     */
    public Subject getSubject() {
        if (!this.loginSucceeded && !this.subjectProvided) {
            return null;
        }
        return this.subject;
    }

    /**
     * El recorrido, que es el mismo para las cuatro fases.
     *
     * <p>Las diferencias estan en {@link Phase}: que metodo se llama y si un SUFFICIENT que anda
     * corta.
     */
    private void invoke(Phase phase) throws LoginException {
        boolean anySucceeded = false;
        LoginException firstRequiredError = null;
        LoginException firstOtherError = null;
        int i = 0;
        while (i < this.entries.length) {
            AppConfigurationEntry entry = this.entries[i];
            AppConfigurationEntry.LoginModuleControlFlag flag = entry.getControlFlag();
            boolean status = false;
            LoginException failure = null;
            try {
                LoginModule module = moduleAt(i);
                status = phase.call(module);
            } catch (LoginException e) {
                failure = e;
            }
            if (failure == null && status) {
                anySucceeded = true;
                if (phase.stopsAtSufficient()
                        && flag == AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT
                        && firstRequiredError == null) {
                    return;
                }
            } else if (failure != null) {
                if (flag == AppConfigurationEntry.LoginModuleControlFlag.REQUISITE) {
                    if (firstRequiredError == null) {
                        firstRequiredError = failure;
                    }
                    break;
                }
                if (flag == AppConfigurationEntry.LoginModuleControlFlag.REQUIRED) {
                    if (firstRequiredError == null) {
                        firstRequiredError = failure;
                    }
                } else if (firstOtherError == null) {
                    firstOtherError = failure;
                }
            }
            i = i + 1;
        }
        if (firstRequiredError != null) {
            throw firstRequiredError;
        }
        if (!anySucceeded) {
            if (firstOtherError != null) {
                throw firstOtherError;
            }
            throw new LoginException("Login Failure: all modules ignored");
        }
    }

    /** El modulo de esa entrada, creandolo e inicializandolo la primera vez. */
    private LoginModule moduleAt(int index) throws LoginException {
        if (this.modules[index] != null) {
            return this.modules[index];
        }
        AppConfigurationEntry entry = this.entries[index];
        LoginModule made = instantiate(entry);
        made.initialize(this.subject, this.callbackHandler, this.sharedState, entry.getOptions());
        this.modules[index] = made;
        return made;
    }

    /** Carga e instancia un modulo por nombre de clase. */
    private LoginModule instantiate(AppConfigurationEntry entry) throws LoginException {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = LoginContext.class.getClassLoader();
            }
            Class<?> found = Class.forName(entry.getLoginModuleName(), true, loader);
            Object made = found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
            if (!(made instanceof LoginModule)) {
                throw new LoginException(
                    entry.getLoginModuleName() + " is not a javax.security.auth.spi.LoginModule");
            }
            return (LoginModule) made;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new LoginException(
                "Unable to instantiate LoginModule " + entry.getLoginModuleName() + ": " + e);
        }
    }

    /**
     * El manejador nombrado en la propiedad de seguridad, o null.
     *
     * <p>Null es valido: un modulo que no necesita preguntar nada --uno que lee un token del
     * ambiente, por ejemplo-- anda igual sin manejador.
     */
    private static CallbackHandler defaultHandler() {
        String className = null;
        try {
            className = java.security.Security.getProperty(DEFAULT_HANDLER);
        } catch (SecurityException e) {
            // Sin permiso para leerla: se sigue sin manejador.
        }
        if (className == null || className.length() == 0) {
            return null;
        }
        try {
            Class<?> found = Class.forName(className);
            Object made = found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
            return (CallbackHandler) made;
        } catch (Exception e) {
            // Un manejador mal configurado no puede tumbar el arranque de la autenticacion.
            return null;
        }
    }

    /** Las cuatro fases, con lo unico que las distingue. */
    private static final class Phase {

        /** Ida: verifica. */
        static final Phase LOGIN = new Phase(0, true);

        /** Ida: escribe en el sujeto. */
        static final Phase COMMIT = new Phase(1, true);

        /** Vuelta: deshace lo que se hubiera empezado. */
        static final Phase ABORT = new Phase(2, false);

        /** Vuelta: cierra la sesion. */
        static final Phase LOGOUT = new Phase(3, false);

        private final int which;
        private final boolean stopsAtSufficient;

        private Phase(int which, boolean stopsAtSufficient) {
            this.which = which;
            this.stopsAtSufficient = stopsAtSufficient;
        }

        /** Si un SUFFICIENT que anda corta el recorrido. Ver la nota de la clase. */
        boolean stopsAtSufficient() {
            return this.stopsAtSufficient;
        }

        /** Llama al metodo de esta fase. */
        boolean call(LoginModule module) throws LoginException {
            if (this.which == 0) {
                return module.login();
            }
            if (this.which == 1) {
                return module.commit();
            }
            if (this.which == 2) {
                return module.abort();
            }
            return module.logout();
        }
    }
}
