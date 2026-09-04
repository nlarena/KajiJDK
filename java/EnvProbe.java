import java.io.File;
public class EnvProbe {
    public static void main(String[] a) throws Exception {
        System.out.println("TEMP=" + System.getenv("TEMP"));
        System.out.println("TMP=" + System.getenv("TMP"));
        try { File f = File.createTempFile("kj", ".log"); System.out.println("tmp=" + f.getPath() + " exists=" + f.exists()); f.delete(); }
        catch (Throwable t) { System.out.println("createTempFile: " + t.getClass().getName() + " " + t.getMessage()); }
        System.out.println("cwd=" + System.getProperty("user.dir"));
    }
}
