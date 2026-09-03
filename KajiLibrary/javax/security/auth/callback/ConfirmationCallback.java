package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.ConfirmationCallback -- pide una confirmacion.
 *
 * <p>Tiene dos modos y conviene distinguirlos antes de leer nada mas, porque las mismas constantes
 * significan cosas distintas en cada uno:
 *
 * <ul>
 *   <li><b>Con {@code optionType}</b> -- el juego de botones lo elige el que pregunta entre cuatro
 *       predefinidos, y la respuesta es una de las constantes {@link #YES}, {@link #NO},
 *       {@link #CANCEL} u {@link #OK}. Cual de ellas es valida depende del juego: pedir
 *       {@link #OK_CANCEL_OPTION} y contestar {@code YES} es un error.
 *   <li><b>Con opciones propias</b> -- el que pregunta pasa los textos y la respuesta es un
 *       <b>indice</b> en ese arreglo. En este modo {@link #getOptionType()} devuelve
 *       {@link #UNSPECIFIED_OPTION}, que es como se distinguen los dos desde afuera.
 * </ul>
 *
 * <h2>Dos detalles que sorprenden</h2>
 *
 * <ol>
 *   <li>{@link #getSelectedIndex()} <b>no lanza</b> si nadie contesto todavia: devuelve 0. Y 0 es
 *       {@link #YES} y tambien el primer indice de una lista propia, asi que es una respuesta
 *       valida. Quien necesite distinguir "no contestaron" de "dijeron que si" tiene que llevar la
 *       cuenta por su lado; es del JDK y no hay forma de rodearlo desde el API.
 *   <li>El default se valida <b>contra el juego elegido</b>. Es lo unico que impide construir un
 *       callback que pide "si o no" y sugiere "cancelar".
 * </ol>
 */
public class ConfirmationCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = -9095656433782481624L;

    /** No hay juego predefinido: las opciones son propias. Ver la nota de la clase. */
    public static final int UNSPECIFIED_OPTION = -1;

    /** Si / No. */
    public static final int YES_NO_OPTION = 0;

    /** Si / No / Cancelar. */
    public static final int YES_NO_CANCEL_OPTION = 1;

    /** Aceptar / Cancelar. */
    public static final int OK_CANCEL_OPTION = 2;

    /** La respuesta "si". Vale 0, que tambien es el primer indice de una lista propia. */
    public static final int YES = 0;

    /** La respuesta "no". */
    public static final int NO = 1;

    /** La respuesta "cancelar". */
    public static final int CANCEL = 2;

    /** La respuesta "aceptar". */
    public static final int OK = 3;

    /** Gravedad: informativo. */
    public static final int INFORMATION = 0;

    /** Gravedad: advertencia. */
    public static final int WARNING = 1;

    /** Gravedad: error. */
    public static final int ERROR = 2;

    private final String prompt;
    private final int messageType;
    private final int optionType;
    private final String[] options;
    private final int defaultOption;
    private int selection;

    /** Con juego predefinido y sin texto de pregunta. */
    public ConfirmationCallback(int messageType, int optionType, int defaultOption) {
        this(null, false, messageType, optionType, null, defaultOption, false);
    }

    /** Con opciones propias y sin texto de pregunta. */
    public ConfirmationCallback(int messageType, String[] options, int defaultOption) {
        this(null, false, messageType, UNSPECIFIED_OPTION, options, defaultOption, true);
    }

    /** Con juego predefinido y texto de pregunta. */
    public ConfirmationCallback(String prompt, int messageType, int optionType,
            int defaultOption) {
        this(prompt, true, messageType, optionType, null, defaultOption, false);
    }

    /** Con opciones propias y texto de pregunta. */
    public ConfirmationCallback(String prompt, int messageType, String[] options,
            int defaultOption) {
        this(prompt, true, messageType, UNSPECIFIED_OPTION, options, defaultOption, true);
    }

    private ConfirmationCallback(String prompt, boolean hasPrompt, int messageType, int optionType,
            String[] options, int defaultOption, boolean ownOptions) {
        // El prompt es opcional para el objeto pero **no** para el constructor que lo pide: los dos
        // que lo llevan exigen un texto de verdad, y los otros dos pasan null a proposito. Sin esta
        // distincion, pasarle null al que lo pide se aceptaria en silencio y el usuario veria una
        // confirmacion sin pregunta.
        if (hasPrompt && (prompt == null || prompt.length() == 0)) {
            throw new IllegalArgumentException("Invalid prompt");
        }
        if (messageType != INFORMATION && messageType != WARNING && messageType != ERROR) {
            throw new IllegalArgumentException("Invalid msgType");
        }
        if (ownOptions) {
            if (options == null || options.length == 0 || defaultOption < 0
                    || defaultOption >= options.length) {
                throw new IllegalArgumentException("Invalid options and/or default option");
            }
            int i = 0;
            while (i < options.length) {
                if (options[i] == null || options[i].length() == 0) {
                    throw new IllegalArgumentException("Invalid option value");
                }
                i = i + 1;
            }
            this.options = copy(options);
        } else {
            if (optionType != YES_NO_OPTION && optionType != YES_NO_CANCEL_OPTION
                    && optionType != OK_CANCEL_OPTION) {
                throw new IllegalArgumentException("Invalid optionType");
            }
            // El default tiene que ser una respuesta que ESE juego de botones pueda dar. Es lo
            // unico que impide pedir "si o no" y sugerir "cancelar".
            if (!validDefault(optionType, defaultOption)) {
                throw new IllegalArgumentException("Invalid default option");
            }
            this.options = null;
        }
        this.prompt = prompt;
        this.messageType = messageType;
        this.optionType = optionType;
        this.defaultOption = defaultOption;
    }

    private static boolean validDefault(int optionType, int defaultOption) {
        if (optionType == YES_NO_OPTION) {
            return defaultOption == YES || defaultOption == NO;
        }
        if (optionType == YES_NO_CANCEL_OPTION) {
            return defaultOption == YES || defaultOption == NO || defaultOption == CANCEL;
        }
        return defaultOption == OK || defaultOption == CANCEL;
    }

    /** El texto de la pregunta, o null si no se dio. */
    public String getPrompt() {
        return this.prompt;
    }

    public int getMessageType() {
        return this.messageType;
    }

    /** El juego de botones, o {@link #UNSPECIFIED_OPTION} si las opciones son propias. */
    public int getOptionType() {
        return this.optionType;
    }

    /** Las opciones propias, o null si se uso un juego predefinido. Copia. */
    public String[] getOptions() {
        return this.options == null ? null : copy(this.options);
    }

    public int getDefaultOption() {
        return this.defaultOption;
    }

    /** Contesta. Es una de las constantes, o un indice si las opciones son propias. */
    public void setSelectedIndex(int selection) {
        this.selection = selection;
    }

    /** Lo contestado. <b>0 si nadie contesto todavia</b>; ver la nota de la clase. */
    public int getSelectedIndex() {
        return this.selection;
    }

    private static String[] copy(String[] a) {
        String[] c = new String[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }
}
