// Covarianza de arrays: pasar un Dog[] donde se espera un Animal[].
class ArrCov {
    static int firstSound(Animal[] animals) {
        return animals[0].sound();        // aaload Animal, invokevirtual sound()
    }
    static int run() {
        Dog[] dogs = new Dog[1];
        dogs[0] = new Dog();
        return firstSound(dogs);          // Dog[] ⊑ Animal[] (covarianza) → Dog.sound() = 2
    }
}
