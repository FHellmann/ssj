package de.uni.augsburg.hcai.ssj;

import com.annimon.stream.Optional;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import de.uni.augsburg.hcai.ssj.supplier.RxSsjSupplier;

/**
 * @author Fabio Hellmann
 */
public class RxSsjEvent {
    private final RxSsjSupplier mOrigin;
    private final float[] mValues;
    private final int mAccuracy;

    /**
     * Copy ctr to replace old values with new ones.
     *
     * @param event     of the original.
     * @param newValues to be set.
     */
    public RxSsjEvent(final RxSsjEvent event, final float... newValues) {
        this(event.mOrigin, event.mAccuracy, newValues);
    }

    /**
     * Ctr.
     *
     * @param origin   of the supply chain.
     * @param accuracy of the supplier - if available otherwise -1.
     * @param values   provided by the supplier.
     */
    public RxSsjEvent(final RxSsjSupplier origin, final int accuracy, final float... values) {
        this.mOrigin = origin;
        this.mValues = values;
        this.mAccuracy = accuracy;
    }

    public String getSensorName() {
        return mOrigin.getName();
    }

    public int getSensorId() {
        return mOrigin.getId();
    }

    public Optional<float[]> getSensorValues() {
        return Optional.ofNullable(mValues);
    }

    public int getAccuracy() {
        return mAccuracy;
    }

    public boolean hasAccuracyChanged() {
        return mAccuracy != -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RxSsjEvent)) {
            return false;
        }
        final RxSsjEvent that = (RxSsjEvent) o;
        return mAccuracy == that.mAccuracy &&
                mOrigin.getId() == that.mOrigin.getId() &&
                Arrays.equals(mValues, that.mValues);
    }

    @Override
    public int hashCode() {
        int result = Arrays.deepHashCode(new Object[]{mOrigin.getId(), mAccuracy});
        result = 31 * result + Arrays.hashCode(mValues);
        return result;
    }

    @Override
    public @NotNull String toString() {
        return "RxSensorEvent{" +
                "SensorName=" + mOrigin.getName() +
                ", Accuracy=" + mAccuracy +
                ", Values=" + Arrays.toString(mValues) +
                '}';
    }
}
