package de.uni.augsburg.hcai.ssj.supplier.helper;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Locale;

import de.uni.augsburg.hcai.ssj.RxSsjEvent;
import de.uni.augsburg.hcai.ssj.exceptions.SensorNotFoundException;
import de.uni.augsburg.hcai.ssj.supplier.RxSsjSupplier;
import de.uni.augsburg.hcai.ssj.supplier.RxSsjSuppliers;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;

/**
 * A factory class to observe sensors.
 *
 * @author Fabio Hellmann
 */
public class RxAndroidSensorHelper {
    private RxAndroidSensorHelper() {
        // Factory ctr.
    }

    /**
     * Returns RxJava Observable, which allows to monitor hardware sensors as a stream of
     * RxSensor.Event object. Sampling period is set to SensorManager.SENSOR_DELAY_FASTEST.
     *
     * @param sensorType sensor type from Sensor class from Android SDK.
     * @return RxJava Observable with RxSensor.Event.
     */
    public static Flowable<RxSsjEvent> observeSensor(@NonNull final Context context, final int sensorType) {
        return observeSensor(context, sensorType, SensorManager.SENSOR_DELAY_FASTEST);
    }

    /**
     * Returns RxJava Observable, which allows to monitor hardware sensors as a stream of
     * RxSensor.Event object.
     *
     * @param sensorType         sensor type from Sensor class from Android SDK.
     * @param samplingPeriodInUs sampling period in microseconds, you can use predefined values from
     *                           SensorManager class with prefix SENSOR_DELAY.
     * @return RxJava Observable with RxSensor.Event.
     */
    public static Flowable<RxSsjEvent> observeSensor(@NonNull final Context context, final int sensorType, final int samplingPeriodInUs) {
        return observeSensor(context, sensorType, samplingPeriodInUs, BackpressureStrategy.BUFFER);
    }

    /**
     * Returns RxJava Observable, which allows to monitor hardware sensors as a stream of
     * RxSensor.Event object.
     *
     * @param sensorType         sensor type from Sensor class from Android SDK.
     * @param samplingPeriodInUs sampling period in microseconds, you can use predefined values from
     *                           SensorManager class with prefix SENSOR_DELAY.
     * @param strategy           BackpressureStrategy for RxJava 3 Flowable type.
     * @return RxJava Observable with RxSensorEvent.
     */
    public static Flowable<RxSsjEvent> observeSensor(@NonNull final Context context, final int sensorType, final int samplingPeriodInUs, @NonNull final BackpressureStrategy strategy) {
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
            final RxSsjSupplier supplier;
            switch (sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    supplier = RxSsjSuppliers.ANDROID_ACCELEROMETER;
                    break;
                case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        supplier = RxSsjSuppliers.ANDROID_ACCELEROMETER_UNCALIBRATED;
                    }
                    break;
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    supplier = RxSsjSuppliers.ANDROID_AMBIENT_TEMPERATURE;
                    break;
                case Sensor.TYPE_DEVICE_PRIVATE_BASE:
                    supplier = RxSsjSuppliers.ANDROID_DEVICE_PRIVATE_BASE;
                    break;
                case Sensor.TYPE_GAME_ROTATION_VECTOR:
                    supplier = RxSsjSuppliers.ANDROID_GAME_ROTATION_VECTOR;
                    break;
                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    supplier = RxSsjSuppliers.ANDROID_GEOMAGNETIC_ROTATION_VECTOR;
                    break;
                case Sensor.TYPE_GRAVITY:
                    supplier = RxSsjSuppliers.ANDROID_GRAVITY;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    supplier = RxSsjSuppliers.ANDROID_GYROSCOPE;
                    break;
                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    supplier = RxSsjSuppliers.ANDROID_GYROSCOPE_UNCALIBRATED;
                    break;
                case Sensor.TYPE_HEART_BEAT:
                    supplier = RxSsjSuppliers.ANDROID_HEART_BEAT;
                    break;
                case Sensor.TYPE_HEART_RATE:
                    supplier = RxSsjSuppliers.ANDROID_HEART_RATE;
                    break;
                case Sensor.TYPE_LIGHT:
                    supplier = RxSsjSuppliers.ANDROID_LIGHT;
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    supplier = RxSsjSuppliers.ANDROID_LINEAR_ACCELERATION;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown sensor: " + sensor.getName());
            }
            return supplier;
        }
    }
}
