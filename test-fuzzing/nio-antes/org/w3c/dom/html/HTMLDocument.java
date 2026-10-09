package org.w3c.dom.html;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * An HTML document: the root, plus the shortcuts HTML adds over a `Document`.
 *
 * <p>The collections it returns --`getImages`, `getLinks`, `getForms`, `getAnchors`,
 * `getApplets`-- are **live**: they reflect the document at the moment they are queried, not a
 * snapshot of when they were asked for. Adding an `<img>` changes what `getImages().getLength()`
 * answers without calling it again. It is the same rule as in `NodeList` and it is what makes
 * keeping a collection cheap and at the same time dangerous if one iterates while modifying.
 *
 * <p>`open`, `close`, `write` and `writeln` are the interface of the browsers' `document.write`:
 * writing over an already loaded document **replaces** it instead of adding to it. They are in the
 * API because DOM Level 1 HTML defines them; what they do depends on the implementation.
 */
public interface HTMLDocument extends Document {

    /** The `title` attribute. */
    String getTitle();

    /** It sets the `title` attribute. */
    void setTitle(String title);

    /** The URI this document was reached from, or the empty string. */
    String getReferrer();

    /** The domain of the server that served the document, or the empty string. */
    String getDomain();

    /** The complete URI of the document. */
    String getURL();

    /** The `<body>`, or the `<frameset>` if the document has frames. */
    HTMLElement getBody();

    /** It sets the `<body>`, or the `<frameset>` if the document has frames. */
    void setBody(HTMLElement body);

    /** The `<img>`s of the document, in a live collection. */
    HTMLCollection getImages();

    /** The `<applet>`s and the `<object>`s that are applets, in a live collection. */
    HTMLCollection getApplets();

    /** The `<a>`s and `<area>`s with an `href`, in a live collection. */
    HTMLCollection getLinks();

    /** The `<form>`s of the document, in a live collection. */
    HTMLCollection getForms();

    /** The `<a>`s with a `name`, in a live collection. */
    HTMLCollection getAnchors();

    /** The cookies of the document, in the format of the `Cookie` header. */
    String getCookie();

    /** It sets the cookies of the document, in the format of the `Cookie` header. */
    void setCookie(String cookie);

    /** It opens the document for writing. Whatever was there is discarded. */
    void open();

    /** It closes the stream opened by {@link #open}. */
    void close();

    /** It writes that text into the open document. */
    void write(String text);

    /** Like {@link #write}, plus an end of line. */
    void writeln(String text);

    /** The elements whose `name` is `elementName`. */
    NodeList getElementsByName(String elementName);
}
