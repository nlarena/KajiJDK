package org.xml.sax;

import org.xml.sax.XMLReader;

// KajiLibrary's org.xml.sax.XMLFilter -- an XMLReader that takes its events from another
// XMLReader instead of from a document.
//
// The trick is that a filter is *at the same time* the reader its client talks to and the handler
// its parent talks to. Calling parse() on the filter configures the parent to report to the filter
// and then starts the parent; each event arrives at the filter, which may discard it, rewrite it
// or let it through before handing it to the handler of the client. Filters chain, so a pipeline
// is nothing more than filters whose parent is the previous filter.
//
// helpers.XMLFilterImpl is the base class that lets everything through: one extends it and
// overrides the few events one cares about.
public interface XMLFilter extends XMLReader {

    // The reader this filter takes its events from.
    void setParent(XMLReader parent);

    // The parent reader, or null if none was set.
    XMLReader getParent();
}
