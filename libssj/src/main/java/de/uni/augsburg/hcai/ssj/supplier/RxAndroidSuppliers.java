package de.uni.augsburg.hcai.ssj.supplier;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.annimon.stream.Stream;

import java.util.Locale;

import de.uni.augsburg.hcai.ssj.RxSsjEvent;
import de.uni.augsburg.hcai.ssj.exceptions.SensorNotFoundException;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;

/**
 * This enum provides all available suppliers for a device with an Android OS.
 *
 * @author Fabio Hellmann
 */
public enum RxAndroidSuppliers implements RxSsjSupplier {
    TYPE_ACCELEROMETER(Sensor.TYPE_ACCELEROMETER),
    @RequiresApi(api = Build.VERSION_CODES.O)
    TYPE_ACCELEROMETER_UNCALIBRATED(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED),
    TYPE_AMBIENT_TEMPERATURE(Sensor.TYPE_AMBIENT_TEMPERATURE),
    @RequiresApi(api = Build.VERSION_CODES.N)
    TYPE_DEVICE_PRIVATE_BASE(Sensor.TYPE_DEVICE_PRIVATE_BASE),
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    TYPE_GAME_ROTATION_VECTOR(Sensor.TYPE_GAME_ROTATION_VECTOR),
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    TYPE_GEOMAGNETIC_ROTATION_VECTOR(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR),
    TYPE_GRAVITY(Sensor.TYPE_GRAVITY),
    TYPE_GYROSCOPE(Sensor.TYPE_GYROSCOPE),
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    TYPE_GYROSCOPE_UNCALIBRATED(Sensor.TYPE_GYROSCOPE_UNCALIBRATED),
    @RequiresApi(api = Build.VERSION_CODES.N)
    TYPE_HEART_BEAT(Sensor.TYPE_HEART_BEAT),
    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    TYPE_HEART_RATE(Sensor.TYPE_HEART_RATE),
    TYPE_LIGHT(Sensor.TYPE_LIGHT),
    TYPE_LINEAR_ACCELERATION(Sensor.TYPE_LINEAR_ACCELERATION),
    @RequiresApi(api = Build.VERSION_CODES.O)
    TYPE_LOW_LATENCY_OFFBODY_DETECT(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT),
    TYPE_MAGNETIC_FIELD(Sensor.TYPE_MAGNETIC_FIELD),
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    TYPE_MAGNETIC_FIELD_UNCALIBRATED(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED),
    @RequiresApi(api = Build.VERSION_CODES.N)
    TYPE_MOTION_DETECT(Sensor.TYPE_MOTION_DETECT);

    private final int mSensorType;

    RxAndroidSuppliers(final int sensorType) {
        mSensorType = sensorType;
    }

    @Override
    public Flowable<RxSsjEvent> open(@NonNull Context context) throws Throwable {
        return observeSensor(context, mSensorType, SensorManager.SENSOR_DELAY_FASTEST, BackpressureStrategy.BUFFER);
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public int getId() {
        return ordinal();
    }

    /**
     * Returns RxJava Observable, which allows to monitor hardware sensors as a stream of
     * RxSensor.Event object.
     *
     * @param context            of the Android application.
     * @param sensorType         sensor type from Sensor class from Android SDK.
     * @param samplingPeriodInUs sampling period in microseconds, you can use predefined values from
     *                           SensorManager class with prefix SENSOR_DELAY.
     * @param strategy           BackpressureStrategy for RxJava 3 Flowable type.
     * @return RxJava Observable with RxSensorEvent.
     */
    private Flowable<RxSsjEvent> observeSensor(@NonNull final Context context, final int sensorType, final int samplingPeriodInUs, @NonNull final BackpressureStrategy strategy) {
        final SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(sensorType) == null) {
            return Flowable.error(new SensorNotFoundException(String.format(Locale.getDefault(), "Sensor with id = %d is not available on this device", sensorType)));
        }
        final Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        final SensorEventListenerWrapper wrapper = new SensorEventListenerWrapper();
        final SensorEventListener sensorEventListener = wrapper.create();
        sensorManager.registerListener(sensorEventListener, sensor, samplingPeriodInUs);
        return Flowable.create(wrapper::setEmitter, strategy)
                .doOnCancel(() -> sensorManager.unregisterListener(sensorEventListener));
    }

    private static class SensorEventListenerWrapper {
        private Emitter<RxSsjEvent> mEmitter;

        public void setEmitter(Emitter<RxSsjEvent> emitter) {
            this.mEmitter = emitter;
        }

        public SensorEventListener create() {
            return new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    final RxSsjEvent event = new RxSsjEvent(
                            getSsjSensorByAndroidSensor(sensorEvent.sensor),
                            sensorEvent.accuracy,
                            sensorEvent.values);
                    if (mEmitter != null) {
                        mEmitter.onNext(event);
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    final RxSsjEvent event = new RxSsjEvent(
                            getSsjSensorByAndroidSensor(sensor),
                            accuracy);
                    if (mEmitter != null) {
                        mEmitter.onNext(event);
                    }
                }
            };
        }

        private RxSsjSupplier getSsjSensorByAndroidSensor(@NonNull final Sensor sensor) {
            return Stream.of(RxAndroidSuppliers.values())
                    .filter(value -> value.mSensorType == sensor.getType())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("The sensor type is not available: " + sensor.getName()));
        }
    }
}
