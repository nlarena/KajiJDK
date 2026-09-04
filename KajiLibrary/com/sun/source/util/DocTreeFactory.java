package com.sun.source.util;

import java.util.List;

import javax.lang.model.element.Name;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.sun.source.doctree.*;

/**
 * Construye nodos de documentacion.
 *
 * <h2>Para que sirve construir un arbol de documentacion</h2>
 *
 * <p>Para reescribirlo. Una herramienta que hereda documentacion, que expande una plantilla o que
 * corrige un tag necesita <strong>producir</strong> nodos, no solo leerlos — y los nodos son
 * interfaces sin constructor publico, a proposito: cada compilador tiene su representacion. Esta
 * fabrica es la unica forma portable de crearlos.
 *
 * <h2>La posicion, que es lo unico con estado</h2>
 *
 * <p>{@link #at} fija en que posicion del fuente quedan los nodos que se creen despues, y devuelve
 * la misma fabrica. Importa porque un diagnostico sobre un nodo construido necesita apuntar a algun
 * lado; sin eso, un error sobre documentacion generada no tendria linea.
 */
public interface DocTreeFactory {

    AttributeTree newAttributeTree(Name a0, AttributeTree.ValueKind a1, List<? extends DocTree> a2);

    AuthorTree newAuthorTree(List<? extends DocTree> a0);

    /**
     * El nodo de {@code {@code}}. Devuelve un {@link LiteralTree} igual que
     * {@link #newLiteralTree}: los dos tags comparten interfaz y se distinguen por su {@code Kind}.
     */
    LiteralTree newCodeTree(TextTree a0);

    CommentTree newCommentTree(String a0);

    DeprecatedTree newDeprecatedTree(List<? extends DocTree> a0);

    DocCommentTree newDocCommentTree(List<? extends DocTree> a0, List<? extends DocTree> a1);

    DocCommentTree newDocCommentTree(List<? extends DocTree> a0, List<? extends DocTree> a1, List<? extends DocTree> a2, List<? extends DocTree> a3);

    DocRootTree newDocRootTree();

    DocTypeTree newDocTypeTree(String a0);

    EndElementTree newEndElementTree(Name a0);

    EntityTree newEntityTree(Name a0);

    ErroneousTree newErroneousTree(String a0, Diagnostic<JavaFileObject> a1);

    /** El nodo de un escape de Markdown. */
    EscapeTree newEscapeTree(char a0);

    /** El nodo de {@code @exception}; mismo tipo que {@link #newThrowsTree}. */
    ThrowsTree newExceptionTree(ReferenceTree a0, List<? extends DocTree> a1);

    HiddenTree newHiddenTree(List<? extends DocTree> a0);

    IdentifierTree newIdentifierTree(Name a0);

    IndexTree newIndexTree(DocTree a0, List<? extends DocTree> a1);

    InheritDocTree newInheritDocTree();

    default InheritDocTree newInheritDocTree(ReferenceTree a0) {
        throw new UnsupportedOperationException(
                "esta fabrica no soporta newInheritDocTree");
    }

    LinkTree newLinkTree(ReferenceTree a0, List<? extends DocTree> a1);

    /** El nodo de {@code {@linkplain}}; mismo tipo que {@link #newLinkTree}. */
    LinkTree newLinkPlainTree(ReferenceTree a0, List<? extends DocTree> a1);

    LiteralTree newLiteralTree(TextTree a0);

    ParamTree newParamTree(boolean a0, IdentifierTree a1, List<? extends DocTree> a2);

    ProvidesTree newProvidesTree(ReferenceTree a0, List<? extends DocTree> a1);

    /** Texto en un formato que este arbol no interpreta, con el {@code Kind} que dice cual. */
    RawTextTree newRawTextTree(DocTree.Kind a0, String a1);

    ReferenceTree newReferenceTree(String a0);

    ReturnTree newReturnTree(List<? extends DocTree> a0);

    default ReturnTree newReturnTree(boolean a0, List<? extends DocTree> a1) {
        throw new UnsupportedOperationException(
                "esta fabrica no soporta newReturnTree");
    }

    SeeTree newSeeTree(List<? extends DocTree> a0);

    SerialTree newSerialTree(List<? extends DocTree> a0);

    SerialDataTree newSerialDataTree(List<? extends DocTree> a0);

    SerialFieldTree newSerialFieldTree(IdentifierTree a0, ReferenceTree a1, List<? extends DocTree> a2);

    SinceTree newSinceTree(List<? extends DocTree> a0);

    SnippetTree newSnippetTree(List<? extends DocTree> a0, TextTree a1);

    SpecTree newSpecTree(TextTree a0, List<? extends DocTree> a1);

    StartElementTree newStartElementTree(Name a0, List<? extends DocTree> a1, boolean a2);

    default SummaryTree newSummaryTree(List<? extends DocTree> a0) {
        throw new UnsupportedOperationException(
                "esta fabrica no soporta newSummaryTree");
    }

    SystemPropertyTree newSystemPropertyTree(Name a0);

    TextTree newTextTree(String a0);

    ThrowsTree newThrowsTree(ReferenceTree a0, List<? extends DocTree> a1);

    UnknownBlockTagTree newUnknownBlockTagTree(Name a0, List<? extends DocTree> a1);

    UnknownInlineTagTree newUnknownInlineTagTree(Name a0, List<? extends DocTree> a1);

    UsesTree newUsesTree(ReferenceTree a0, List<? extends DocTree> a1);

    ValueTree newValueTree(ReferenceTree a0);

    default ValueTree newValueTree(TextTree a0, ReferenceTree a1) {
        throw new UnsupportedOperationException(
                "esta fabrica no soporta newValueTree");
    }

    VersionTree newVersionTree(List<? extends DocTree> a0);

    /**
     * Fija la posicion en el fuente de los nodos que se creen despues.
     *
     * <p>Devuelve la misma fabrica, no una nueva: es una perilla con estado, y encadenar
     * {@code at(p).newTextTree(s)} es la forma prevista de usarla.
     */
    com.sun.source.util.DocTreeFactory at(int a0);

    /** La primera oracion de esa lista, con el mismo criterio que usa javadoc. */
    List<DocTree> getFirstSentence(List<? extends DocTree> a0);
}
