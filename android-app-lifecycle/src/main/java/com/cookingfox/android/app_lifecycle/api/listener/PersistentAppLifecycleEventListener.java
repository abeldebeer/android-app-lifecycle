package com.cookingfox.android.app_lifecycle.api.listener;

import com.cookingfox.android.app_lifecycle.api.AppLifecycleListenable;

/**
 * If this interface is implemented, the listener will not be removed by
 * {@link AppLifecycleListenable#removeListener(AppLifecycleEventListener)}.
 */
public interface PersistentAppLifecycleEventListener {
}
