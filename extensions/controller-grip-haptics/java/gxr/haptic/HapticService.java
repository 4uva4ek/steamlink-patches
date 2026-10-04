package gxr.haptic;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

/**
 * Runs in a Shizuku user service process (shell identity) and relays vibrations to the controller HAL,
 * which is not reachable from an application process.
 */
public class HapticService extends Binder {
    static final String DESCRIPTOR = "gxr.haptic.IHapticService";
    static final int TRANSACTION_VIBRATE = 1;
    static final int TRANSACTION_STOP = 2;
    // Reserved by Shizuku: asks the user service to exit.
    private static final int TRANSACTION_DESTROY = 16777115;

    private static final String TAG = "GxrHapticMain";
    private static final String HAL_SERVICE = "vendor.samsung.hardware.secxrcontroller.ISecXRController/default";
    private static final String HAL_DESCRIPTOR = "vendor.samsung.hardware.secxrcontroller.ISecXRController";
    private static final int HAL_PERFORM_HAPTIC = 21;
    private static final int HAL_STOP_HAPTIC = 22;
    private static final int HAL_HAPTIC_INFO_SIZE = 28;

    private IBinder hal;

    public HapticService() {
        attachInterface(null, DESCRIPTOR);
        Log.i(TAG, "user service started, hal=" + (hal() != null));
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        switch (code) {
            case TRANSACTION_VIBRATE: {
                data.enforceInterface(DESCRIPTOR);
                final int device = data.readInt();
                final int vibrator = data.readInt();
                final int durationMs = data.readInt();
                final float frequency = data.readFloat();
                final float amplitude = data.readFloat();
                vibrate(device, vibrator, durationMs, frequency, amplitude);
                return true;
            }
            case TRANSACTION_STOP: {
                data.enforceInterface(DESCRIPTOR);
                stop(data.readInt());
                return true;
            }
            case TRANSACTION_DESTROY:
                System.exit(0);
                return true;
            default:
                return super.onTransact(code, data, reply, flags);
        }
    }

    private synchronized IBinder hal() {
        if (hal != null && hal.isBinderAlive()) return hal;
        try {
            hal = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("checkService", String.class)
                .invoke(null, HAL_SERVICE);
        } catch (Throwable error) {
            Log.w(TAG, "controller HAL lookup failed", error);
            hal = null;
        }
        return hal;
    }

    private void vibrate(int device, int vibrator, int durationMs, float frequency, float amplitude) {
        final Parcel data = Parcel.obtain();
        data.writeInterfaceToken(HAL_DESCRIPTOR);
        data.writeInt(device);
        data.writeInt(1);
        data.writeInt(HAL_HAPTIC_INFO_SIZE);
        data.writeInt(vibrator);
        data.writeInt(durationMs);
        data.writeFloat(frequency);
        data.writeFloat(amplitude);
        data.writeInt(0);
        data.writeInt(0);
        call(HAL_PERFORM_HAPTIC, data);
    }

    private void stop(int device) {
        final Parcel data = Parcel.obtain();
        data.writeInterfaceToken(HAL_DESCRIPTOR);
        data.writeInt(device);
        call(HAL_STOP_HAPTIC, data);
    }

    private void call(int code, Parcel data) {
        final Parcel reply = Parcel.obtain();
        try {
            final IBinder target = hal();
            if (target != null) target.transact(code, data, reply, 0);
        } catch (Throwable error) {
            Log.w(TAG, "controller HAL call failed", error);
        } finally {
            reply.recycle();
            data.recycle();
        }
    }
}
