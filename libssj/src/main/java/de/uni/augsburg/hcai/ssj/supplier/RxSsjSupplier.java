package de.uni.augsburg.hcai.ssj.supplier;

import android.content.Context;

import androidx.annotation.NonNull;

import de.uni.augsburg.hcai.ssj.RxSsjEvent;
import io.reactivex.rxjava3.core.Flowable;

/**
 * An interface for all sensor supplier classes.
 *
 * @author Fabio Hellmann
 */
public interface RxSsjSupplier {
    /**
     * Open a new supplier channel.
     *
     * @param context of the Android application.
     * @return the opened channel.
     * @throws Throwable if an error occures during opening channel.
     */
    Flowable<RxSsjEvent> open(@NonNull final Context context) throws Throwable;

    /**
     * Get the name of the supplier.
     *
     * @return the name.
     */
    String getName();

    /**
     * Get the id of the supplier.
     *
     * @return the id.
     */
    int getId();
}
