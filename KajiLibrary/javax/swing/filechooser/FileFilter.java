package javax.swing.filechooser;

import java.io.File;

/**
 * Which files are shown to the user in a chooser.
 *
 * <h2>Why it is an abstract class and not the {@link java.io.FileFilter} that already exists</h2>
 *
 * <p>Because a second method is needed: {@link #getDescription}. A file chooser does not only
 * filter -- it shows a drop-down list with the available filters, and each one needs a text a
 * person can read, of the sort "Images (*.jpg, *.png)". The {@code java.io} interface only
 * knows how to say yes or no.
 *
 * <p>And it is a class and not an interface so that adding a method later does not break whoever
 * already extended it. The price is that a filter cannot inherit from anything else, which in
 * practice does not get in the way.
 */
public abstract class FileFilter {

    /** For the subclasses. */
    protected FileFilter() {
    }

    /**
     * Whether {@code f} is shown.
     *
     * <p>Directories <strong>almost always</strong> have to pass: filtering them out would leave
     * the user unable to navigate to where the files the filter does accept are. It is the
     * commonest mistake when writing one.
     */
    public abstract boolean accept(File f);

    /** The text shown in the filter list. */
    public abstract String getDescription();
}
