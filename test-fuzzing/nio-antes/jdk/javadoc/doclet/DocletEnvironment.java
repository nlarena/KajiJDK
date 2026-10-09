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
 * The model of the already analysed code, which is the only thing a plug-in receives.
 *
 * <h2>The specified and the included</h2>
 *
 * <p>{@link #getSpecifiedElements} are the ones that were named on the command line; a package was
 * asked to be documented, and there is that package. {@link #getIncludedElements} is what really
 * has to be documented after applying the visibility filters: the classes of that package that pass
 * the cut, their public members, and also what they dragged along by being reachable.
 *
 * <p>The two sets almost never coincide and confusing them is the classic mistake of a new plug-in:
 * walking the specified ones documents too little, not filtering by the included ones documents the
 * private.
 *
 * <h2>Why there are two questions of membership</h2>
 *
 * <p>{@link #isIncluded} says whether an element is going to be documented. {@link #isSelected}
 * says whether it passes the visibility filter, without looking at whether it is also reachable.
 * The difference matters for deciding whether to link: something selected but not included exists
 * and is seen, but it is not going to have a page of its own.
 *
 * <h2>The three borrowed utilities</h2>
 *
 * <p>{@link #getDocTrees}, {@link #getElementUtils} and {@link #getTypeUtils} are the same ones an
 * annotation processor uses. A plug-in cannot make them on its own --they depend on the state of
 * the compiler-- and without them it could do nothing more than read names.
 *
 * @since 9
 */
public interface DocletEnvironment {

    /**
     * What was named on the command line.
     *
     * @return the specified elements
     */
    Set<? extends Element> getSpecifiedElements();

    /**
     * What has to be documented, already filtered.
     *
     * @return the included elements
     */
    Set<? extends Element> getIncludedElements();

    /**
     * How to reach the documentation comments and their trees.
     *
     * @return the documentation utilities
     */
    DocTrees getDocTrees();

    /**
     * The utilities over elements of the program.
     *
     * @return the element utilities
     */
    Elements getElementUtils();

    /**
     * The utilities over types.
     *
     * @return the type utilities
     */
    Types getTypeUtils();

    /**
     * Whether that element is going to be documented.
     *
     * @param e the element
     * @return whether it is included
     */
    boolean isIncluded(Element e);

    /**
     * Whether that element passes the visibility filter, without looking at reachability.
     *
     * @param e the element
     * @return whether it is selected
     */
    boolean isSelected(Element e);

    /**
     * Where the files come from.
     *
     * <p>A plug-in that wants to write its output next to the classes, or to read a resource that
     * accompanies the code, needs it.
     *
     * @return the file manager
     */
    JavaFileManager getJavaFileManager();

    /**
     * The version of the language it was analysed with.
     *
     * @return the version
     */
    SourceVersion getSourceVersion();

    /**
     * Whether the APIs of the modules are documented or all of their contents.
     *
     * @return the mode
     */
    ModuleMode getModuleMode();

    /**
     * Where that type came from: from a source or from a {@code .class}.
     *
     * <p>It matters because a type read from a {@code .class} almost never brings comments, and a
     * plug-in that does not tell them apart is going to report as missing a documentation that
     * could never have been there.
     *
     * @param type the type
     * @return the kind of file it came from
     */
    JavaFileObject.Kind getFileKind(TypeElement type);

    /** How much of a module is documented. */
    enum ModuleMode {
        /** Only what the module exports. */
        API,
        /** Everything the module contains. */
        ALL
    }
}
