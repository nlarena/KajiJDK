package java.lang.annotation;

/**
 * KajiLibrary's java.lang.annotation.Native — marca una constante que tiene que aparecer en un
 * header nativo.
 *
 * <p>No la lee la VM ni nadie en tiempo de ejecucion: es una senal para las herramientas que
 * generan headers C a partir de clases Java (historicamente {@code javah}, hoy {@code javac -h}).
 * Un {@code static final int} marcado asi aparece como {@code #define} en el {@code .h}, y asi el
 * codigo nativo deja de repetir a mano un numero que vive en Java.
 *
 * <p>De ahi salen sus dos meta-anotaciones, que no son decorativas:
 *
 * <ul>
 *   <li>{@code @Retention(SOURCE)} — la anotacion <strong>no llega al `.class`</strong>. Quien la
 *       consume es un procesador que ya tiene el codigo fuente delante, asi que guardarla en el
 *       archivo compilado seria peso muerto. Consecuencia practica: no hay forma de encontrarla por
 *       reflexion, y una prueba que la busque con {@code getAnnotation} tiene que esperar
 *       {@code null};
 *   <li>{@code @Target(FIELD)} — solo campos. Una constante es lo unico que un header puede
 *       reproducir; un metodo o una clase no tienen valor que copiar.
 * </ul>
 *
 * <p>Esta biblioteca no trae la herramienta que la consume, y aun asi el tipo hace falta: sin el,
 * el codigo de terceros que anota sus constantes con {@code @Native} no compila contra este JDK.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Native {
}
