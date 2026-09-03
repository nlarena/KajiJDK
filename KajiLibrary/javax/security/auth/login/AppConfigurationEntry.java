package javax.security.auth.login;

import java.util.Collections;
import java.util.Map;

/**
 * KajiLibrary's javax.security.auth.login.AppConfigurationEntry -- un modulo en la cadena de login.
 *
 * <p>Tres cosas: que modulo cargar, que tan obligatorio es, y con que opciones. Una aplicacion se
 * autentica contra una <b>lista</b> de estos, y las banderas de {@link LoginModuleControlFlag} son
 * lo que hace que la lista sea un programa y no una enumeracion.
 *
 * <h2>El mapa no se copia</h2>
 *
 * <p>{@link #getOptions} devuelve una vista <b>no modificable</b> del mapa que se paso, no una
 * copia. La diferencia se nota: quien construyo la entrada puede seguir cambiando su mapa y esos
 * cambios se ven desde aca. Es como esta especificado y por eso se replica, pero conviene pasarle un
 * mapa que uno no vaya a tocar mas.
 */
public class AppConfigurationEntry {

    private final String loginModuleName;
    private final LoginModuleControlFlag controlFlag;
    private final Map<String, ?> options;

    /**
     * @param loginModuleName el nombre completo de la clase del modulo
     * @param controlFlag que tan obligatorio es; ver {@link LoginModuleControlFlag}
     * @param options lo que se le pasa al modulo al inicializarlo
     * @throws IllegalArgumentException si el nombre es null o vacio, o si la bandera o las opciones
     *     son null
     */
    public AppConfigurationEntry(String loginModuleName, LoginModuleControlFlag controlFlag,
                                 Map<String, ?> options) {
        if (loginModuleName == null || loginModuleName.length() == 0) {
            throw new IllegalArgumentException("invalid null or empty LoginModule name");
        }
        if (controlFlag == null) {
            throw new IllegalArgumentException("invalid null ControlFlag");
        }
        if (options == null) {
            throw new IllegalArgumentException("invalid null options");
        }
        this.loginModuleName = loginModuleName;
        this.controlFlag = controlFlag;
        // Vista, no copia; ver la nota de la clase.
        this.options = Collections.unmodifiableMap(options);
    }

    /** El nombre de la clase del modulo. */
    public String getLoginModuleName() {
        return this.loginModuleName;
    }

    /** Que tan obligatorio es. */
    public LoginModuleControlFlag getControlFlag() {
        return this.controlFlag;
    }

    /** Las opciones, sin poder modificarlas. Ver la nota de la clase. */
    public Map<String, ?> getOptions() {
        return this.options;
    }

    /**
     * Que tan obligatorio es un modulo dentro de la cadena.
     *
     * <p>Las cuatro banderas responden dos preguntas independientes: si el modulo <b>tiene</b> que
     * andar para que el login cierre, y si su resultado <b>corta</b> el recorrido de la lista.
     *
     * <ul>
     *   <li><b>REQUIRED</b> -- tiene que andar; si falla, la cadena sigue igual. Seguir es a
     *       proposito: si se cortara, quien intenta entrar podria deducir <b>cual</b> modulo lo
     *       rechazo por lo rapido que vuelve la respuesta;
     *   <li><b>REQUISITE</b> -- tiene que andar y ademas corta al fallar. Se usa cuando los modulos
     *       que siguen no tienen sentido sin este;
     *   <li><b>SUFFICIENT</b> -- no hace falta que ande, pero si anda --y ningun obligatorio fallo
     *       antes-- alcanza y se corta ahi;
     *   <li><b>OPTIONAL</b> -- ni hace falta ni corta. Sirve para juntar datos en el sujeto sin que
     *       eso decida nada.
     * </ul>
     *
     * <p>No es un enum: la clase es de 1999, anterior a que Java tuviera enums, y cambiarla ahora
     * romperia la serializacion de cualquier configuracion guardada.
     */
    public static class LoginModuleControlFlag {

        private final String name;

        /** Tiene que andar; no corta. */
        public static final LoginModuleControlFlag REQUIRED =
            new LoginModuleControlFlag("required");

        /** Tiene que andar; corta al fallar. */
        public static final LoginModuleControlFlag REQUISITE =
            new LoginModuleControlFlag("requisite");

        /** No hace falta; corta al andar. */
        public static final LoginModuleControlFlag SUFFICIENT =
            new LoginModuleControlFlag("sufficient");

        /** Ni hace falta ni corta. */
        public static final LoginModuleControlFlag OPTIONAL =
            new LoginModuleControlFlag("optional");

        /** Privado: las cuatro constantes son las unicas que hay. */
        private LoginModuleControlFlag(String name) {
            this.name = name;
        }

        /** La forma que espera quien lee un registro: {@code "LoginModuleControlFlag: required"}. */
        public String toString() {
            return "LoginModuleControlFlag: " + this.name;
        }
    }
}
