// Marcado transitivo por ARRAY: el Animal cuelga solo de arr[1].
class Arr {
    static int run() {
        Animal[] arr = new Animal[3];
        arr[1] = new Animal();   // alcanzable solo vía el elemento del array
        return arr[1].legs;      // 4
    }
}
