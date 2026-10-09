package java.beans;

// It tells "I am being edited in a tool" from "I am really running". A bean in design mode should
// not open connections nor start threads: it is being drawn, not used.
public interface DesignMode {

    // The name of the property fired when the mode changes.
    String PROPERTYNAME = "designTime";

    void setDesignTime(boolean designTime);

    boolean isDesignTime();
}
