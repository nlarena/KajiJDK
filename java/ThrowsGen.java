import java.io.IOException;
public class ThrowsGen {
    <T> T m() throws IOException, RuntimeException { return null; }
    void plain() throws Exception {}
}
