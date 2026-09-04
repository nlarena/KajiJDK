package jdk.javadoc.doclet;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;

/**
 * Lo que javadoc ejecuta: recibe un modelo del codigo ya analizado y produce lo que quiera.
 *
 * <h2>Por que la documentacion es un complemento y no parte de la herramienta</h2>
 *
 * <p>Porque analizar el codigo y generar HTML son dos trabajos que no tienen por que ir juntos. El
 * primero es caro y dificil —hay que compilar de verdad para saber que tipo tiene cada cosa— y el
 * segundo es una decision de formato. Separandolos, cualquiera puede aprovechar el analisis para
 * generar otra cosa: un indice, un JSON, una comparacion de API entre dos versiones.
 *
 * <p>El HTML que genera javadoc por omision es, en esta arquitectura, un complemento mas
 * ({@link StandardDoclet}) y no un privilegio de la herramienta.
 *
 * <h2>El orden de las llamadas</h2>
 *
 * <p>Primero {@link #init}, con el idioma y por donde informar. Despues
 * {@link #getSupportedOptions} y {@link #getSupportedSourceVersion}, que javadoc consulta
 * <strong>antes</strong> de procesar la linea de comandos —tiene que saber que opciones aceptar—.
 * Recien entonces {@link #run}, una sola vez, con el modelo entero.
 *
 * @since 9
 */
public interface Doclet {

    /**
     * El primer aviso: con que idioma y por donde informar.
     *
     * @param locale el idioma para los mensajes, o {@code null} si no hay preferencia
     * @param reporter por donde emitir diagnosticos
     */
    void init(Locale locale, Reporter reporter);

    /**
     * El nombre para los mensajes de la herramienta.
     *
     * @return el nombre
     */
    String getName();

    /**
     * Las opciones de linea de comandos que este complemento entiende.
     *
     * <p>Se consulta antes de procesar los argumentos: javadoc no puede decidir si {@code -foo} es
     * un error sin preguntar primero.
     *
     * @return las opciones, posiblemente vacio
     */
    Set<? extends Option> getSupportedOptions();

    /**
     * La version del lenguaje que este complemento soporta.
     *
     * @return la version
     */
    SourceVersion getSupportedSourceVersion();

    /**
     * Hace el trabajo, una sola vez, con el modelo completo.
     *
     * @param environment el modelo del codigo analizado
     * @return si termino bien
     */
    boolean run(DocletEnvironment environment);

    /**
     * Una opcion de linea de comandos que el complemento agrega.
     *
     * <p>Cada opcion se describe a si misma —cuantos argumentos toma, como se llama, que hace— y
     * ademas sabe procesarse. Es lo que le permite a javadoc validar opciones que no conoce y
     * mostrarlas en la ayuda sin saber nada de ellas.
     */
    interface Option {

        /**
         * Cuantos argumentos toma despues del nombre.
         *
         * @return la cantidad, cero si es una bandera
         */
        int getArgumentCount();

        /**
         * Que hace, para la ayuda.
         *
         * @return la descripcion
         */
        String getDescription();

        /**
         * Cuan visible es en la ayuda.
         *
         * @return la clase de opcion
         */
        Kind getKind();

        /**
         * Todas las formas de escribirla, la preferida primero.
         *
         * <p>Es una lista y no un nombre porque una misma opcion suele tener forma larga y corta, y
         * porque javadoc necesita reconocerlas todas.
         *
         * @return los nombres
         */
        List<String> getNames();

        /**
         * Como se escriben los argumentos en la ayuda, por ejemplo {@code "<directorio>"}.
         *
         * @return la plantilla de argumentos
         */
        String getParameters();

        /**
         * Procesa una aparicion de la opcion.
         *
         * @param option el nombre tal como aparecio
         * @param arguments los argumentos, tantos como dijo {@link #getArgumentCount}
         * @return si la opcion se acepto
         */
        boolean process(String option, List<String> arguments);

        /** Cuan visible es una opcion en la ayuda. */
        enum Kind {
            /** Se muestra solo con la ayuda extendida. */
            EXTENDED,
            /** Se muestra en la ayuda comun. */
            STANDARD,
            /** No se muestra. */
            OTHER
        }
    }
}
