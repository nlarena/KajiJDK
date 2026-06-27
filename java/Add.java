public class Add {
    public static int add(int a, int b) {
        return a + b;
    }

    public static int substract(int a, int b) {
        return a - b;
    }

    public static void main(String[] args) {
        int r = add(2, 3);
        int x = add(r, 1);
        System.out.println(x);
    }
}
