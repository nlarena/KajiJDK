package org.w3c.dom.html;

import org.w3c.dom.Element;

/**
 * What every element of an HTML document has, whatever its tag.
 *
 * <p>The five attributes here --`id`, `title`, `lang`, `dir`, `class`; the note said "four"-- are
 * the ones HTML 4 defines for any element, and that is why they live at the root of the hierarchy
 * and are not repeated in each subtype.
 *
 * <p>Careful with `getClassName`: the attribute is called `class` in the document and the method
 * `className` in the API. It is nobody's oversight --`class` is a reserved word of Java--. The note
 * said it is the only property of this package where the name of the method and that of the
 * attribute do not match; it is not: `htmlFor` (`for`), `httpEquiv` (`http-equiv`) and
 * `acceptCharset` (`accept-charset`) differ as well, for the same kind of reason.
 */
public interface HTMLElement extends org.w3c.dom.Element {

    /** The `id` attribute. */
    String getId();

    /** It sets the `id` attribute. */
    void setId(String id);

    /** The `title` attribute. */
    String getTitle();

    /** It sets the `title` attribute. */
    void setTitle(String title);

    /** The `lang` attribute. */
    String getLang();

    /** It sets the `lang` attribute. */
    void setLang(String lang);

    /** The `dir` attribute. */
    String getDir();

    /** It sets the `dir` attribute. */
    void setDir(String dir);

    /** The `class` attribute. */
    String getClassName();

    /** It sets the `class` attribute. */
    void setClassName(String className);
}
