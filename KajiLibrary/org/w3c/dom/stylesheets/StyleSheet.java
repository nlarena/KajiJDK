package org.w3c.dom.stylesheets;

import org.w3c.dom.Node;

/**
 * A style sheet, of whatever language.
 *
 * <p>It is the **language-independent** part: `type`, whether it is disabled, where it came from
 * and which media it is for. What the sheet says inside is not here -- the extension of each
 * language adds that, and in the case of CSS it is {@link org.w3c.dom.css.CSSStyleSheet}, which
 * adds the rules.
 *
 * <p>`getOwnerNode` and `getParentStyleSheet` are mutually exclusive: a sheet is either linked from
 * the document --and then it has an owner node-- or imported from another sheet --and then it has a
 * parent sheet--. The one that does not apply returns null.
 */
public interface StyleSheet {

    /** The language of the sheet, for example `"text/css"`. */
    String getType();

    /** Whether it is disabled. A disabled sheet does not affect the document. */
    boolean getDisabled();

    /** It enables or disables it. */
    void setDisabled(boolean disabled);

    /** The node that links it --a `<link>` or a `<style>`--, or null if it came imported. */
    Node getOwnerNode();

    /** The sheet that imported it, or null if it is linked from the document. */
    StyleSheet getParentStyleSheet();

    /** The URI it came from, or null if it is written in the document. */
    String getHref();

    /** The title whoever linked it gave it, or null. */
    String getTitle();

    /** The media it applies to. Empty means all. */
    MediaList getMedia();
}
