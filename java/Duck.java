// Example showing the interfaces[] section populated: Duck implements two.
interface Flyable {
    void fly();
}

interface Swimmable {
    void swim();
}

public class Duck implements Flyable, Swimmable {
    public void fly() { }
    public void swim() { }
}
