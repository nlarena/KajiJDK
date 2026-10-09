package javax.management.openmbean;

/**
 * What a class implements when it wants to choose how it is converted to {@link CompositeData}.
 *
 * <p>Without this, the conversion of an object to open data is deduced by the MXBean framework from
 * the getters. A class that implements this interface takes control: it is given the
 * {@link CompositeType} the framework computed and it returns the value.
 *
 * <p>The case that justifies it is a class whose useful state does not match its getters --for
 * example one that wants to expose a derived field and hide three internal ones.
 */
public interface CompositeDataView {

    /** This object as a composite value of that type. */
    CompositeData toCompositeData(CompositeType ct);
}
