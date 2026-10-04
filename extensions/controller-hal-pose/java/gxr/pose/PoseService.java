package gxr.pose;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

/**
 * Runs in a Shizuku user service process (shell identity) and reads controller poses from the
 * controller HAL, which is not reachable from an application process.
 */
public class PoseService extends Binder {
    static final String DESCRIPTOR = "gxr.pose.IPoseService";
    static final int TRANSACTION_POSES = 1;
    // Reserved by Shizuku: asks the user service to exit.
    private static final int TRANSACTION_DESTROY = 16777115;

    private static final String TAG = "GxrHalPose";
    private static final String HAL_SERVICE = "vendor.samsung.hardware.secxrcontroller.ISecXRController/default";
    private static final String HAL_DESCRIPTOR = "vendor.samsung.hardware.secxrcontroller.ISecXRController";
    private static final int HAL_GET_POSE_AT_TIMESTAMP = 18;
    private static final int HAL_GET_DUAL_POSE_AT_TIMESTAMP = 19;
    private static final int HAL_POSE_REQUEST_SIZE = 20;
    private static final int CONTROLLERS = 2;
    // A PoseInfo in a HAL reply: the non-null marker and the parcelable.
    private static final int POSE_WORDS = 34;
    // Within it: the position and the tracking state (2 = tracked).
    private static final int POSE_POSITION = 17;
    private static final int POSE_STATE = 33;
    private static final int STATE_TRACKED = 2;
    // The two poses of a dual reply are told apart only once the controllers are this far apart.
    private static final float MIN_SEPARATION = 0.05f;

    private static final int ORDER_UNKNOWN = 0;
    private static final int ORDER_LEFT_FIRST = 1;
    private static final int ORDER_RIGHT_FIRST = 2;

    private IBinder hal;
    // Which controller the first pose of a dual reply belongs to. The system service makes the same
    // call once per display frame; here it is checked once against a single-controller read.
    private int dualOrder = ORDER_UNKNOWN;
    private final int[] single = new int[1 + POSE_WORDS];
    private final int[] first = new int[POSE_WORDS];
    private final int[] second = new int[POSE_WORDS];

    public PoseService() {
        attachInterface(null, DESCRIPTOR);
        Log.i(TAG, "user service started, hal=" + (hal() != null));
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        switch (code) {
            case TRANSACTION_POSES: {
                data.enforceInterface(DESCRIPTOR);
                final long timeNs = data.readLong();
                reply.writeNoException();
                poses(timeNs, reply);
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

    /**
     * Appends, for the left and then the right controller, 1, the HAL's result and its PoseInfo
     * words, or 0 when the HAL gave no pose.
     */
    private synchronized void poses(long timeNs, Parcel out) {
        if (dualOrder != ORDER_UNKNOWN) {
            final int result = readDual(timeNs);
            if (result != Integer.MIN_VALUE) {
                final boolean leftFirst = dualOrder == ORDER_LEFT_FIRST;
                write(out, result, leftFirst ? first : second);
                write(out, result, leftFirst ? second : first);
                return;
            }
        } else {
            learnDualOrder(timeNs);
        }
        for (int device = 0; device < CONTROLLERS; ++device) {
            if (readSingle(device, timeNs)) {
                out.writeInt(1);
                for (int word : single) out.writeInt(word);
            } else {
                out.writeInt(0);
            }
        }
    }

    private static void write(Parcel out, int result, int[] pose) {
        out.writeInt(1);
        out.writeInt(result);
        for (int word : pose) out.writeInt(word);
    }

    /** Compares a dual reply with the left controller's own pose for the same time. */
    private void learnDualOrder(long timeNs) {
        if (readDual(timeNs) == Integer.MIN_VALUE || !readSingle(0, timeNs)) return;
        if (first[POSE_STATE] != STATE_TRACKED || second[POSE_STATE] != STATE_TRACKED) return;
        if (distance(first, 0, second, 0) < MIN_SEPARATION) return;
        // The single reply carries the result word before the PoseInfo.
        final boolean leftFirst = distance(single, 1, first, 0) < distance(single, 1, second, 0);
        dualOrder = leftFirst ? ORDER_LEFT_FIRST : ORDER_RIGHT_FIRST;
        Log.i(TAG, "dual pose reply: " + (leftFirst ? "left" : "right") + " controller first");
    }

    private static float distance(int[] a, int aOffset, int[] b, int bOffset) {
        float sum = 0.0f;
        for (int axis = 0; axis < 3; ++axis) {
            final float delta = Float.intBitsToFloat(a[aOffset + POSE_POSITION + axis])
                - Float.intBitsToFloat(b[bOffset + POSE_POSITION + axis]);
            sum += delta * delta;
        }
        return (float) Math.sqrt(sum);
    }

    /** Fills {@link #first} and {@link #second}; returns the HAL's result, or MIN_VALUE on failure. */
    private int readDual(long timeNs) {
        final Parcel data = Parcel.obtain();
        final Parcel reply = Parcel.obtain();
        try {
            final IBinder target = hal();
            if (target == null) return Integer.MIN_VALUE;
            data.writeInterfaceToken(HAL_DESCRIPTOR);
            writeRequest(data, timeNs);
            target.transact(HAL_GET_DUAL_POSE_AT_TIMESTAMP, data, reply, 0);
            reply.readException();
            if (reply.dataAvail() < (1 + 2 * POSE_WORDS) * 4) return Integer.MIN_VALUE;
            final int result = reply.readInt();
            for (int word = 0; word < POSE_WORDS; ++word) first[word] = reply.readInt();
            for (int word = 0; word < POSE_WORDS; ++word) second[word] = reply.readInt();
            return result;
        } catch (Throwable error) {
            Log.w(TAG, "controller pose read failed", error);
            return Integer.MIN_VALUE;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    /** Fills {@link #single} with the HAL's result and one controller's PoseInfo. */
    private boolean readSingle(int device, long timeNs) {
        final Parcel data = Parcel.obtain();
        final Parcel reply = Parcel.obtain();
        try {
            final IBinder target = hal();
            if (target == null) return false;
            data.writeInterfaceToken(HAL_DESCRIPTOR);
            data.writeInt(device);
            writeRequest(data, timeNs);
            target.transact(HAL_GET_POSE_AT_TIMESTAMP, data, reply, 0);
            reply.readException();
            if (reply.dataAvail() < single.length * 4) return false;
            for (int word = 0; word < single.length; ++word) single[word] = reply.readInt();
            return true;
        } catch (Throwable error) {
            Log.w(TAG, "controller pose read failed", error);
            return false;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private static void writeRequest(Parcel data, long timeNs) {
        data.writeInt(1);
        data.writeInt(HAL_POSE_REQUEST_SIZE);
        data.writeLong(timeNs);
        data.writeLong(timeNs);
    }
}
