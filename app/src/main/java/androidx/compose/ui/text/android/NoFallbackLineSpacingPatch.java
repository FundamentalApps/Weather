package androidx.compose.ui.text.android;

import android.os.Build;
import android.text.StaticLayout;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class NoFallbackLineSpacingPatch {
    private static final String TAG = "NoFallbackTextPatch";

    private static volatile boolean attempted;
    private static volatile boolean installed;

    private NoFallbackLineSpacingPatch() {
    }

    public static synchronized boolean install() {
        if (installed) {
            return true;
        }
        if (attempted) {
            return false;
        }
        attempted = true;

        if (Build.VERSION.SDK_INT < 28) {
            return false;
        }

        try {
            Field delegateField = StaticLayoutFactory.class.getDeclaredField("delegate");
            delegateField.setAccessible(true);

            Object currentDelegate = delegateField.get(null);
            if (currentDelegate instanceof PatchedStaticLayoutFactory) {
                installed = true;
                return true;
            }
            if (!(currentDelegate instanceof StaticLayoutFactoryImpl)) {
                return false;
            }

            PatchedStaticLayoutFactory patchedDelegate =
                    new PatchedStaticLayoutFactory((StaticLayoutFactoryImpl) currentDelegate);

            if (!setStaticFinalField(delegateField, patchedDelegate)) {
                return false;
            }

            installed = delegateField.get(null) == patchedDelegate;
            return installed;
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to patch Compose fallback line spacing.", throwable);
            return false;
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    private static boolean setStaticFinalField(Field field, Object value) throws Exception {
        try {
            field.set(null, value);
            if (field.get(null) == value) {
                return true;
            }
        } catch (IllegalAccessException ignored) {
            // Try less polite paths below.
        }

        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, value);
            if (field.get(null) == value) {
                return true;
            }
        } catch (Throwable ignored) {
            // Android/Java versions differ here; Unsafe is the last resort.
        }

        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            Object base = unsafeClass.getMethod("staticFieldBase", Field.class).invoke(unsafe, field);
            long offset =
                    ((Number) unsafeClass.getMethod("staticFieldOffset", Field.class).invoke(unsafe, field))
                            .longValue();
            unsafeClass
                    .getMethod("putObjectVolatile", Object.class, long.class, Object.class)
                    .invoke(unsafe, base, offset, value);
            return field.get(null) == value;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static StaticLayoutParams withoutFallbackLineSpacing(StaticLayoutParams params) {
        return new StaticLayoutParams(
                params.getText(),
                params.getStart(),
                params.getEnd(),
                params.getPaint(),
                params.getWidth(),
                params.getTextDir(),
                params.getAlignment(),
                params.getMaxLines(),
                params.getEllipsize(),
                params.getEllipsizedWidth(),
                params.getLineSpacingMultiplier(),
                params.getLineSpacingExtra(),
                params.getJustificationMode(),
                params.getIncludePadding(),
                false,
                params.getBreakStrategy(),
                params.getLineBreakStyle(),
                params.getLineBreakWordStyle(),
                params.getHyphenationFrequency(),
                params.getLeftIndents(),
                params.getRightIndents()
        );
    }

    private static final class PatchedStaticLayoutFactory implements StaticLayoutFactoryImpl {
        private final StaticLayoutFactoryImpl delegate;

        private PatchedStaticLayoutFactory(StaticLayoutFactoryImpl delegate) {
            this.delegate = delegate;
        }

        @Override
        public StaticLayout create(StaticLayoutParams params) {
            return delegate.create(withoutFallbackLineSpacing(params));
        }

        @Override
        public boolean isFallbackLineSpacingEnabled(
                StaticLayout layout,
                boolean useFallbackLineSpacing
        ) {
            return delegate.isFallbackLineSpacingEnabled(layout, false);
        }
    }
}
