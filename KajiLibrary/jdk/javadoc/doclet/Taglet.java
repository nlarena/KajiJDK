package jdk.javadoc.doclet;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;

/**
 * Una etiqueta de documentacion propia, que se suma a las que javadoc ya entiende.
 *
 * <h2>De bloque o en linea</h2>
 *
 * <p>Una etiqueta de bloque ocupa su propio parrafo y se escribe {@code @nombre ...}; una en linea
 * va dentro del texto y se escribe {@code {@nombre ...}}. Es la unica distincion estructural, y por
 * eso {@link #isBlockTag} viene por omision como la negacion de {@link #isInlineTag} — una etiqueta
 * es una cosa o la otra, nunca las dos.
 *
 * <h2>Donde puede aparecer</h2>
 *
 * <p>{@link #getAllowedLocations} devuelve un conjunto y no un lugar: {@code @since} tiene sentido
 * en casi todos lados, {@code @return} solo en un metodo. javadoc usa esa respuesta para avisar
 * cuando una etiqueta aparece donde no corresponde, que es un error que de otro modo pasaria como
 * texto suelto.
 *
 * <h2>Como se traduce</h2>
 *
 * <p>{@link #toString(List, Element)} recibe los arboles del comentario ya analizados, no el texto
 * crudo. Eso significa que la etiqueta no tiene que analizar nada: recibe la estructura y devuelve
 * lo que corresponda en el formato de salida. El {@link Element} es el que estaba documentado, y
 * sirve para lo que depende del contexto — un {@code @implNote} puede escribirse distinto en una
 * interfaz que en una clase.
 *
 * @since 9
 */
public interface Taglet {

    /**
     * Donde puede aparecer esta etiqueta.
     *
     * @return los lugares permitidos
     */
    Set<Location> getAllowedLocations();

    /**
     * Si va dentro del texto, entre llaves.
     *
     * @return si es en linea
     */
    boolean isInlineTag();

    /**
     * Si ocupa su propio parrafo.
     *
     * <p>Por omision es lo contrario de {@link #isInlineTag}: no hay una tercera forma.
     *
     * @return si es de bloque
     */
    default boolean isBlockTag() {
        return !isInlineTag();
    }

    /**
     * El nombre, sin la arroba.
     *
     * @return el nombre
     */
    String getName();

    /**
     * El aviso de arranque, con el modelo y el complemento que la hospeda.
     *
     * <p>Por omision no hace nada: una etiqueta que solo mira sus propios argumentos no necesita
     * enterarse de nada mas, y obligarla a escribir un metodo vacio seria ruido.
     *
     * @param env el modelo del codigo analizado
     * @param doclet el complemento que la va a usar
     */
    default void init(DocletEnvironment env, Doclet doclet) {
    }

    /**
     * La salida que produce esta etiqueta.
     *
     * @param tags las apariciones de la etiqueta, ya analizadas
     * @param element el elemento que estaba siendo documentado
     * @return el texto a insertar, en el formato de salida del complemento
     */
    String toString(List<? extends DocTree> tags, Element element);

    /** Los lugares donde una etiqueta puede aparecer. */
    enum Location {
        /** En el archivo de resumen general. */
        OVERVIEW,
        /** En la documentacion de un modulo. */
        MODULE,
        /** En la de un paquete. */
        PACKAGE,
        /** En la de una clase o interfaz. */
        TYPE,
        /** En la de un constructor. */
        CONSTRUCTOR,
        /** En la de un metodo. */
        METHOD,
        /** En la de un campo. */
        FIELD
    }
}
