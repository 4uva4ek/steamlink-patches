package gxr.shizuku;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuProvider;

/**
 * The application's one Shizuku provider. Binds a Shizuku user service for every feature whose
 * classes are in the application and hands each binder to that feature's bridge class.
 * Without Shizuku, or without its permission, nothing is bound.
 */
public class ShizukuBridge extends ShizukuProvider {
    private static final String TAG = "GxrShizuku";
    private static final int PERMISSION_REQUEST = 7301;

    /** A user service and the class with {@code public static void setBinder(IBinder)} that takes its binder. */
    private static final class Feature {
        final String service;
        final String bridge;
        final String processSuffix;
        final int version;
        boolean bound;
        Method setBinder;

        Feature(String service, String bridge, String processSuffix, int version) {
            this.service = service;
            this.bridge = bridge;
            this.processSuffix = processSuffix;
            this.version = version;
        }
    }

    // A changed service needs a higher version, or Shizuku keeps the running one.
    private static final Feature[] FEATURES = {
        new Feature("gxr.haptic.HapticService", "gxr.haptic.HapticBridge", "haptic", 3),
        new Feature("gxr.pose.PoseService", "gxr.pose.PoseBridge", "pose", 2),
    };

    @Override
    public boolean onCreate() {
        final boolean result = super.onCreate();
        try {
            Shizuku.addBinderReceivedListenerSticky(this::onShizukuAvailable);
            Shizuku.addBinderDeadListener(() -> {
                for (Feature feature : FEATURES) {
                    feature.bound = false;
                    setBinder(feature, null);
                }
            });
            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                if (requestCode != PERMISSION_REQUEST) return;
                if (grantResult == PackageManager.PERMISSION_GRANTED) bindAll();
                else Log.i(TAG, "Shizuku permission denied");
            });
        } catch (Throwable error) {
            Log.w(TAG, "Shizuku setup failed", error);
        }
        return result;
    }

    private void onShizukuAvailable() {
        try {
            if (Shizuku.isPreV11()) return;
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) bindAll();
            else Shizuku.requestPermission(PERMISSION_REQUEST);
        } catch (Throwable error) {
            Log.w(TAG, "Shizuku permission check failed", error);
        }
    }

    private void bindAll() {
        for (Feature feature : FEATURES) bind(feature);
    }

    private void bind(final Feature feature) {
        if (feature.bound) return;
        try {
            feature.setBinder = Class.forName(feature.bridge).getMethod("setBinder", IBinder.class);
            Class.forName(feature.service);
        } catch (ClassNotFoundException | NoSuchMethodException absent) {
            // The patch that brings this feature was not applied.
            return;
        }
        try {
            final ComponentName component = new ComponentName(getContext().getPackageName(), feature.service);
            Shizuku.bindUserService(
                new Shizuku.UserServiceArgs(component)
                    .daemon(false)
                    .processNameSuffix(feature.processSuffix)
                    .debuggable(false)
                    .version(feature.version),
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder binder) {
                        Log.i(TAG, feature.processSuffix + " user service connected");
                        setBinder(feature, binder);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        Log.i(TAG, feature.processSuffix + " user service disconnected");
                        feature.bound = false;
                        setBinder(feature, null);
                    }
                });
            feature.bound = true;
        } catch (Throwable error) {
            Log.w(TAG, feature.processSuffix + " user service bind failed", error);
        }
    }

    private static void setBinder(Feature feature, IBinder binder) {
        if (feature.setBinder == null) return;
        try {
            feature.setBinder.invoke(null, binder);
        } catch (Throwable error) {
            Log.w(TAG, feature.processSuffix + " bridge call failed", error);
        }
    }
}
