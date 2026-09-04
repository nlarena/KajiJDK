import jdk.internal.io.Fs;

public class Probe {
    public static int run() {
        String tmp = System.getProperty("java.io.tmpdir");
        System.out.println("tmpdir=[" + tmp + "]");
        System.out.println("user.dir=[" + System.getProperty("user.dir") + "]");
        String f = tmp + "/kaji-probe.txt";
        System.out.println("write=" + Fs.writeAllBytes(f, new byte[] {65, 66}, false));
        System.out.println("canonical=[" + Fs.canonical(f) + "]");
        System.out.println("canonical-dir=[" + Fs.canonical(tmp) + "]");
        System.out.println("mtime=" + Fs.mtime(f));
        System.out.println("setMtime=" + Fs.setMtime(f, 1000000000L));
        System.out.println("mtime2=" + Fs.mtime(f));
        System.out.println("canonical-missing=[" + Fs.canonical(tmp + "/no-existe-nunca") + "]");
        System.out.println("mtime-missing=" + Fs.mtime(tmp + "/no-existe-nunca"));
        return 0;
    }
}
