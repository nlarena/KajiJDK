public class props {
    public static void main(String[] a) {
        System.out.println("user.home=" + System.getProperty("user.home"));
        System.out.println("java.io.tmpdir=" + System.getProperty("java.io.tmpdir"));
        System.out.println("sep=" + System.getProperty("file.separator"));
    }
}
