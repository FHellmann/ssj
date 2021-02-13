package de.uni.augsburg.hcai.ssj.transformer;

import com.annimon.stream.IntStream;

import org.reactivestreams.Publisher;

import de.uni.augsburg.hcai.ssj.RxSsjEvent;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;

/**
 * This enum provides all available transformers.
 *
 * @author Fabio Hellmann
 */
public enum RxSsjTransformers implements RxSsjTransformer {
    /**
     * Extract the minimum value.
     */
    MIN(values -> new float[]{
            (float) IntStream.range(0, values.length)
                    .mapToDouble(idx -> values[idx])
                    .min()
                    .orElseThrow(IllegalStateException::new)
    }),
    /**
     * Extract the maximum value.
     */
    MAX(values -> new float[]{
            (float) IntStream.range(0, values.length)
                    .mapToDouble(idx -> values[idx])
                    .max()
                    .orElseThrow(IllegalStateException::new)
    }),
    /**
     * Invert all values in the array.
     */
    INVERT(values -> {
        final float[] result = new float[values.length];
        IntStream.range(0, values.length).forEach(idx -> result[idx] = 1.0f - values[idx]);
        return result;
    }),
    /**
     * Calculate the average/mean value in the array.
     */
    AVERAGE_MEAN(values -> new float[]{
            (float) IntStream.range(0, values.length)
                    .mapToDouble(idx -> values[idx])
                    .sum() / values.length
    }),
    /**
     * Count the elements in the array.
     */
    COUNT(values -> new float[]{
            values.length
    }),
    /**
     * Computes common spectral features for heart rate.
     * <p>
     * Code adapted from SSI's QRSHRVspectral.cpp.
     */
    HEART_RATE_V_SPECTRAL(values -> {
        final float VLF = values[0];
        final float LF = values[1];
        final float HF = values[2];
        final float nVLF = (VLF * 100.0f) / (VLF + LF + HF);
        final float nLF = (LF * 100.0f) / (VLF + LF + HF);
        final float nHF = (HF * 100.0f) / (VLF + LF + HF);
        final float dLFHF = Math.abs(nLF - nHF);
        final float SMI = LF / (LF + HF);
        final float VMI = HF / (LF + HF);
        final float SVI = (Math.abs(HF) < 0.0001) ? 0 : LF / HF;
        return new float[]{
                VLF, LF, HF, nVLF, nLF, nHF, dLFHF, SMI, VMI, SVI
        };
    });

    private final Function<float[], float[]> mTransformer;
    private final RxSsjTransformer mTransformer2;

    RxSsjTransformers(@NonNull final Function<float[], float[]> transformer) {
        this.mTransformer = transformer;
        this.mTransformer2 = null;
    }

    RxSsjTransformers(@NonNull final RxSsjTransformer transformer) {
        this.mTransformer = null;
        this.mTransformer2 = transformer;
    }

    @Override
    public @NonNull Publisher<RxSsjEvent> apply(@NonNull Flowable<RxSsjEvent> upstream) {
        if (mTransformer != null) {
            return upstream.map(rxSensorEvent -> new RxSsjEvent(
                    rxSensorEvent,
                    mTransformer.apply(rxSensorEvent.getSensorValues()
                            .orElseThrow(() -> new IllegalStateException("Not yet implemented")))
            ));
        } else if (mTransformer2 != null) {
            return upstream.compose(mTransformer2);
        } else {
            throw new IllegalStateException("No transformer provided");
        }
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public int getId() {
        return ordinal();
    }
}
