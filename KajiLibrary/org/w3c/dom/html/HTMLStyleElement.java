package org.w3c.dom.html;

/**
 * A `<style>`.
 */
public interface HTMLStyleElement extends HTMLElement {

    /** Whether it is disabled. */
    boolean getDisabled();

    /** It sets whether it is disabled. */
    void setDisabled(boolean disabled);

    /** The `media` attribute. */
    String getMedia();

    /** It sets the `media` attribute. */
    void setMedia(String media);

    /** The type of the control. */
    String getType();

    /** It sets the type of the control. */
    void setType(String type);
}
