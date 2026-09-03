package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.Callback -- una peticion de datos, sin decir como se
 * piden.
 *
 * <p>La interfaz esta <b>vacia</b>, y eso es todo su diseño. Un modulo de login necesita el nombre y
 * la clave del usuario, pero no puede saber si el programa que lo usa es una aplicacion de escritorio,
 * un servidor sin terminal o una prueba automatizada. En vez de elegir por el, arma un
 * {@code NameCallback} y un {@code PasswordCallback}, se los pasa a un {@link CallbackHandler} que
 * le dio el programa, y lee las respuestas.
 *
 * <p>De ahi sale la unica regla que importa: <b>el que pregunta no elige el medio</b>. Un modulo que
 * escribiera en {@code System.console()} directamente seria inutil en un servidor; uno que arme
 * callbacks funciona en los tres casos sin cambiar una linea.
 */
public interface Callback {
}
