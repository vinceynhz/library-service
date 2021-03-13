package app.tandv.services.util.collections;

/**
 * @author vic on 2020-10-11
 */
public class Pair<A,B> {
    private final A one;
    private final B two;

    public Pair(final A one, final B two){
        this.one = one;
        this.two = two;
    }

    public A getOne() {
        return one;
    }

    public B getTwo() {
        return two;
    }
}
