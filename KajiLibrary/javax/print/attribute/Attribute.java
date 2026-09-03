package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.Attribute -- lo que hace que un objeto sea un atributo de
 * impresion.
 *
 * <h2>La idea central del paquete: categoria y valor son cosas distintas</h2>
 *
 * <p>Un atributo de impresion es un **valor tipado** --"tres copias", "a dos caras", "papel A4"-- y
 * cada valor pertenece a una **categoria**, que es la pregunta que ese valor contesta. La categoria
 * no se guarda como un string ni como un numero: es un {@code Class}, el que devuelve
 * {@link #getCategory()}.
 *
 * <p>Eso es lo que gobierna todo el paquete. En un {@link AttributeSet} la clave es la categoria, no
 * la clase del objeto ni el objeto mismo: meter {@code new Copies(3)} donde ya habia
 * {@code new Copies(2)} **reemplaza**, porque las dos contestan la misma pregunta. Y por eso hay dos
 * metodos y no uno --sin la categoria explicita, una subclase de un atributo se archivaria bajo una
 * clave distinta que su padre y el conjunto tendria dos respuestas para la misma pregunta.
 *
 * <p>Lo normal es que la categoria sea la clase misma ({@code Copies.getCategory()} es
 * {@code Copies.class}). Las excepciones son justamente las que explican para que sirve la
 * indireccion: {@code MediaSizeName}, {@code MediaTray} y {@code MediaName} son tres clases
 * distintas y las tres reportan {@code Media.class}, porque "que papel" es **una** pregunta que se
 * puede contestar por tamano, por bandeja o por nombre, y un trabajo no puede tener las tres
 * respuestas a la vez.
 *
 * <p>{@link #getName()} devuelve el nombre del protocolo IPP (RFC 2911) --{@code "copies"},
 * {@code "sides"}-- y no cambia con el idioma: es para el cable, no para el usuario.
 */
public interface Attribute extends Serializable {

    /**
     * La pregunta que este valor contesta.
     *
     * <p>Es la clave bajo la que un {@link AttributeSet} lo archiva, y por eso importa que no
     * siempre sea {@code getClass()}: ver la nota sobre {@code Media} en la cabecera.
     */
    Class<? extends Attribute> getCategory();

    /** El nombre IPP de la categoria; no se traduce. */
    String getName();
}
