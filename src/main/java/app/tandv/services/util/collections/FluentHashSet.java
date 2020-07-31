package app.tandv.services.util.collections;

import java.util.HashSet;
import java.util.function.Consumer;

/**
 * To provide a fluent API to the regular hash set
 * @author vic on 2020-07-22
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class FluentHashSet<T> extends HashSet<T> {

    public FluentHashSet<T> thenAdd(T t) {
        return this.thenAdd(t, aBoolean -> {});
    }

    public FluentHashSet<T> thenAdd(T t, Consumer<Boolean> withResult){
        withResult.accept(super.add(t));
        return this;
    }

    public FluentHashSet<T> thenClear() {
        super.clear();
        return this;
    }
}
