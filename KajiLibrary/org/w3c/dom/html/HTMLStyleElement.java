package org.w3c.dom.html;

/**
 * Un `<style>`.
 */
public interface HTMLStyleElement extends HTMLElement {

    /** Si esta deshabilitado. */
    boolean getDisabled();

    /** Fija si esta deshabilitado. */
    void setDisabled(boolean disabled);

    /** El atributo `media`. */
    String getMedia();

    /** Fija el atributo `media`. */
    void setMedia(String media);

    /** El tipo del control. */
    String getType();

    /** Fija el tipo del control. */
    void setType(String type);
}
