package jdk.javadoc.doclet;

import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import com.sun.source.util.DocTrees;

/**
 * El modelo del codigo ya analizado, que es lo unico que un complemento recibe.
 *
 * <h2>Lo especificado y lo incluido</h2>
 *
 * <p>{@link #getSpecifiedElements} son los que se nombraron en la linea de comandos; se pidio
 * documentar un paquete, y ahi esta ese paquete. {@link #getIncludedElements} es lo que
 * efectivamente hay que documentar despues de aplicar los filtros de visibilidad: las clases de ese
 * paquete que pasan el corte, sus miembros publicos, y tambien lo que arrastraron por ser
 * alcanzable.
 *
 * <p>Los dos conjuntos casi nunca coinciden y confundirlos es el error clasico de un complemento
 * nuevo: recorrer lo especificado documenta de menos, no filtrar por lo incluido documenta lo
 * privado.
 *
 * <h2>Por que hay dos preguntas de pertenencia</h2>
 *
 * <p>{@link #isIncluded} dice si un elemento va a documentarse. {@link #isSelected} dice si pasa el
 * filtro de visibilidad, sin mirar si ademas es alcanzable. La diferencia importa para decidir si
 * enlazar: algo seleccionado pero no incluido existe y se ve, pero no va a tener pagina propia.
 *
 * <h2>Las tres utilidades prestadas</h2>
 *
 * <p>{@link #getDocTrees}, {@link #getElementUtils} y {@link #getTypeUtils} son las mismas que usa
 * un procesador de anotaciones. Un complemento no puede fabricarlas por su cuenta —dependen del
 * estado del compilador— y sin ellas no podria hacer nada mas que leer nombres.
 *
 * @since 9
 */
public interface DocletEnvironment {

    /**
     * Lo que se nombro en la linea de comandos.
     *
     * @return los elementos especificados
     */
    Set<? extends Element> getSpecifiedElements();

    /**
     * Lo que hay que documentar, ya filtrado.
     *
     * @return los elementos incluidos
     */
    Set<? extends Element> getIncludedElements();

    /**
     * Como llegar a los comentarios de documentacion y a sus arboles.
     *
     * @return las utilidades de documentacion
     */
    DocTrees getDocTrees();

    /**
     * Las utilidades sobre elementos del programa.
     *
     * @return las utilidades de elementos
     */
    Elements getElementUtils();

    /**
     * Las utilidades sobre tipos.
     *
     * @return las utilidades de tipos
     */
    Types getTypeUtils();

    /**
     * Si ese elemento va a documentarse.
     *
     * @param e el elemento
     * @return si esta incluido
     */
    boolean isIncluded(Element e);

    /**
     * Si ese elemento pasa el filtro de visibilidad, sin mirar la alcanzabilidad.
     *
     * @param e el elemento
     * @return si esta seleccionado
     */
    boolean isSelected(Element e);

    /**
     * De donde salen los archivos.
     *
     * <p>Lo necesita un complemento que quiera escribir su salida al lado de las clases, o leer un
     * recurso que acompana al codigo.
     *
     * @return el gestor de archivos
     */
    JavaFileManager getJavaFileManager();

    /**
     * La version del lenguaje con la que se analizo.
     *
     * @return la version
     */
    SourceVersion getSourceVersion();

    /**
     * Si se documentan las APIs de los modulos o todo su contenido.
     *
     * @return el modo
     */
    ModuleMode getModuleMode();

    /**
     * De donde salio ese tipo: de un fuente o de un {@code .class}.
     *
     * <p>Importa porque un tipo leido de un {@code .class} casi nunca trae comentarios, y un
     * complemento que no distinga va a informar como faltante una documentacion que nunca pudo
     * estar ahi.
     *
     * @param type el tipo
     * @return la clase de archivo del que salio
     */
    JavaFileObject.Kind getFileKind(TypeElement type);

    /** Cuanto de un modulo se documenta. */
    enum ModuleMode {
        /** Solo lo que el modulo exporta. */
        API,
        /** Todo lo que el modulo contiene. */
        ALL
    }
}
