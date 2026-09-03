package org.w3c.dom.html;

/**
 * Un `<script>`.
 */
public interface HTMLScriptElement extends HTMLElement {

    /** El texto que se muestra. */
    String getText();

    /** Fija el texto que se muestra. */
    void setText(String text);

    /** El atributo `htmlFor`. */
    String getHtmlFor();

    /** Fija el atributo `htmlFor`. */
    void setHtmlFor(String htmlFor);

    /** El atributo `event`. */
    String getEvent();

    /** Fija el atributo `event`. */
    void setEvent(String event);

    /** La codificacion del destino. */
    String getCharset();

    /** Fija la codificacion del destino. */
    void setCharset(String charset);

    /** El atributo `defer`. */
    boolean getDefer();

    /** Fija el atributo `defer`. */
    void setDefer(boolean defer);

    /** El origen. */
    String getSrc();

    /** Fija el origen. */
    void setSrc(String src);

    /** El tipo del control. */
    String getType();

    /** Fija el tipo del control. */
    void setType(String type);
}
