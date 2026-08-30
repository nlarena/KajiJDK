import java.util.ListResourceBundle;

public class Msgs extends ListResourceBundle {
    protected Object[][] getContents() {
        return new Object[][] {
            { "greet", "hello" },
            { "bye", "goodbye" },
        };
    }
}
