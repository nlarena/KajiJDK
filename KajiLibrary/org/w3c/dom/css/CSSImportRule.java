package org.w3c.dom.css;

import org.w3c.dom.stylesheets.MediaList;

/**
 * An `@import`: another sheet brought into this one.
 *
 * <p>`getStyleSheet` may return null and there are several legitimate reasons: the sheet has not
 * been downloaded yet, it could not be downloaded, or the medium of the `@import` does not apply to
 * the current medium. None is an error, and that is why there is no exception.
 */
public interface CSSImportRule extends CSSRule {

    /** The URI of the imported sheet. */
    String getHref();

    /** The media the import applies to. Empty means all. */
    MediaList getMedia();

    /** The imported sheet, or null. See the note of the class. */
    CSSStyleSheet getStyleSheet();
}
