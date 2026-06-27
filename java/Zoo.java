// Lightweight intrinsics: Class.isInstance + String.equals + String.charAt.
//   a.getClass().isInstance(d)  → true (Dog is-a Animal)  → enters the if
//   "ab".equals("ab")           → true                    → enters the if
//   "A".charAt(0)               → 'A' = 65
// Returns 65.
class Zoo {
    static int run() {
        Animal a = new Animal();
        Dog d = new Dog();
        if (a.getClass().isInstance(d)) {
            if ("ab".equals("ab")) {
                return "A".charAt(0);
            }
        }
        return 0;
    }
}
