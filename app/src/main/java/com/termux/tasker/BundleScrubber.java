package com.termux.tasker;

import android.content.Intent;
import android.os.Bundle;
/**
 * Helper class to scrub Bundles of invalid extras. This is a workaround for an Android bug:
 * <http://code.google.com/p/android/issues/detail?id=16006>.
 */
public final class BundleScrubber {

    /**
     * Scrubs Intents for private serializable subclasses in the Intent extras. If the Intent's extras contain
     * a private serializable subclass, the Bundle is cleared. The Bundle will not be set to null. If the
     * Bundle is null, has no extras, or the extras do not contain a private serializable subclass, the Bundle
     * is not mutated.
     *
     * @param intent The {@link Intent} to scrub. This parameter may be mutated if scrubbing is
     *               necessary. This parameter may be {@code null}.
     * @return Returns {@code true} if the Intent was scrubbed, otherwise returns {@code false} if
     * the {@link Intent} was not modified.
     */
    public static boolean scrub(final Intent intent) {
        return null != intent && scrub(intent.getExtras());
    }

    /**
     * Scrubs Bundles for private serializable subclasses in the extras. If the Bundle's extras contain a
     * private serializable subclass, the Bundle is cleared. If the Bundle is null, has no extras, or the
     * extras do not contain a private serializable subclass, the Bundle is not mutated.
     *
     * @param bundle The {@link Bundle} to scrub. This parameter may be mutated if scrubbing is necessary.
     *               This parameter may be {@code null}.
     * @return Returns {@code true} if the Bundle was scrubbed, otheriwse {@code false} if the
     * {@link Bundle} was not modified.
     */
    public static boolean scrub(final Bundle bundle) {
        if (null == bundle) return false;

        // Note: This is a hack to work around a private serializable classloader attack
        try {
            // If a private serializable exists, this will throw an exception.
            bundle.containsKey(null);
        } catch (final Exception e) {
            bundle.clear();
            return true;
        }

        return false;
    }
}