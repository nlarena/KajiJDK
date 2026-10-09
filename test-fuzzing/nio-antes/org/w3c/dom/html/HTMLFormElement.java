package org.w3c.dom.html;

/**
 * A `<form>`.
 *
 * <p>`getElements` gives the controls of the form, not its children: an `<input>` put inside a
 * `<div>` that is in turn in the form appears all the same. The collection is live.
 *
 * <p>`submit()` submits the form **without firing the `onsubmit` event**, which is the observable
 * difference with pressing the button. `reset()` does fire `onreset`.
 */
public interface HTMLFormElement extends HTMLElement {

    /** The controls, in a live collection. */
    HTMLCollection getElements();

    /** The count. */
    int getLength();

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);

    /** The `acceptCharset` attribute. */
    String getAcceptCharset();

    /** It sets the `acceptCharset` attribute. */
    void setAcceptCharset(String acceptCharset);

    /** The `action` attribute. */
    String getAction();

    /** It sets the `action` attribute. */
    void setAction(String action);

    /** The `enctype` attribute. */
    String getEnctype();

    /** It sets the `enctype` attribute. */
    void setEnctype(String enctype);

    /** The `method` attribute. */
    String getMethod();

    /** It sets the `method` attribute. */
    void setMethod(String method);

    /** The target frame. */
    String getTarget();

    /** It sets the target frame. */
    void setTarget(String target);

    /** It submits the form, **without** firing `onsubmit`. */
    void submit();

    /** It returns the controls to their default values and fires `onreset`. */
    void reset();
}
