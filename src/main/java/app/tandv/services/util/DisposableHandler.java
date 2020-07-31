package app.tandv.services.util;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * The purpose of this class is to capture any disposable onto a composite disposable if available
 *
 * Normally the composite disposable would be fetched from a routing context
 *
 * @author vic on 2020-07-29
 */
public abstract class DisposableHandler {
    protected void dispose(CompositeDisposable compositeDisposable, Disposable toDispose) {
        if (compositeDisposable != null) {
            compositeDisposable.add(toDispose);
        }
    }
}
