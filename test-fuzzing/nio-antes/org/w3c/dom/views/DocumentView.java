package org.w3c.dom.views;

/**
 * KajiLibrary's org.w3c.dom.views.DocumentView -- a document that knows which its main view is.
 *
 * <p>The {@code Document} implements it. It is the inverse half of {@link AbstractView}: from the
 * view one reaches the document and from the document its default view.
 *
 * <p>"Default" means the one that is used when nobody says which: in a browser, the window. A
 * document may have many views and this interface names only one, because it is the only one that
 * can be chosen without knowing the medium.
 */
public interface DocumentView {

    /** The default view of this document. */
    AbstractView getDefaultView();
}
