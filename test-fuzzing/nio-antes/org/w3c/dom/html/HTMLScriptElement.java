package org.w3c.dom.html;

/**
 * A `<script>`.
 */
public interface HTMLScriptElement extends HTMLElement {

    /** The text that is shown. */
    String getText();

    /** It sets the text that is shown. */
    void setText(String text);

    /** The `htmlFor` attribute. */
    String getHtmlFor();

    /** It sets the `htmlFor` attribute. */
    void setHtmlFor(String htmlFor);

    /** The `event` attribute. */
    String getEvent();

    /** It sets the `event` attribute. */
    void setEvent(String event);

    /** The encoding of the destination. */
    String getCharset();

    /** It sets the encoding of the destination. */
    void setCharset(String charset);

    /** The `defer` attribute. */
    boolean getDefer();

    /** It sets the `defer` attribute. */
    void setDefer(boolean defer);

    /** The source. */
    String getSrc();

    /** It sets the source. */
    void setSrc(String src);

    /** The type of the control. */
    String getType();

    /** It sets the type of the control. */
    void setType(String type);
}
