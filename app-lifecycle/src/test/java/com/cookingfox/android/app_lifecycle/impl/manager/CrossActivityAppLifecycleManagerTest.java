package com.cookingfox.android.app_lifecycle.impl.manager;

import android.app.Activity;

import com.cookingfox.android.app_lifecycle.api.listener.AppLifecycleEventListener;
import com.cookingfox.android.app_lifecycle.api.listener.AppLifecycleListenable;
import com.cookingfox.android.app_lifecycle.api.listener.AppLifecycleListener;
import com.cookingfox.android.app_lifecycle.api.listener.OnAppCreated;
import com.cookingfox.android.app_lifecycle.fixture.FirstActivity;
import com.cookingfox.android.app_lifecycle.fixture.SecondActivity;
import com.cookingfox.android.app_lifecycle.impl.listener.DefaultAppLifecycleListener;
import com.cookingfox.android.app_lifecycle.impl.listener.PersistentAppLifecycleListener;

import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.cookingfox.guava_preconditions.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link CrossActivityAppLifecycleManager}.
 */
public class CrossActivityAppLifecycleManagerTest {

    private CrossActivityAppLifecycleManager appLifecycleManager;

    //----------------------------------------------------------------------------------------------
    // SETUP
    //----------------------------------------------------------------------------------------------

    @Before
    public void setUp() throws Exception {
        appLifecycleManager = new CrossActivityAppLifecycleManager();
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: addListener
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void addListener_should_throw_if_null() throws Exception {
        appLifecycleManager.addListener(null);
    }

    @Test(expected = IllegalStateException.class)
    public void addListener_should_throw_if_already_added() throws Exception {
        final AppLifecycleListener listener = new DefaultAppLifecycleListener();

        appLifecycleManager.addListener(listener);
        appLifecycleManager.addListener(listener);
    }

    @Test
    public void addListener_should_support_multiple_listeners() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final int numListeners = 3;

        for (int i = 0; i < numListeners; i++) {
            appLifecycleManager.addListener(new DefaultAppLifecycleListener() {
                @Override
                public void onAppCreated(Class<?> origin) {
                    counter.incrementAndGet();
                }
            });
        }

        appLifecycleManager.onCreate(new FirstActivity());

        assertEquals(numListeners, counter.get());
    }

    @Test
    public void addListener_should_return_manager() throws Exception {
        AppLifecycleListenable listenable = appLifecycleManager.addListener(new DefaultAppLifecycleListener());

        assertSame(appLifecycleManager, listenable);
    }

    @Test
    public void addListener_should_add_listeners_in_reverse_order() throws Exception {
        final LinkedList<AppLifecycleEventListener> actualCalled = new LinkedList<AppLifecycleEventListener>();

        class TestListener implements OnAppCreated {
            @Override
            public void onAppCreated(Class<?> origin) {
                actualCalled.add(this);
            }
        }

        final TestListener first = new TestListener();
        final TestListener second = new TestListener();
        final TestListener third = new TestListener();

        appLifecycleManager.addListener(first);
        appLifecycleManager.addListener(second);
        appLifecycleManager.addListener(third);

        appLifecycleManager.onCreate(new FirstActivity());

        final LinkedList<AppLifecycleEventListener> expectedCalled = new LinkedList<AppLifecycleEventListener>();
        expectedCalled.add(third);
        expectedCalled.add(second);
        expectedCalled.add(first);

        assertEquals(expectedCalled, actualCalled);
    }

    @Test
    public void addListener_should_not_throw_concurrent_modification_exception() throws Exception {
        appLifecycleManager.addListener(new DefaultAppLifecycleListener());
        appLifecycleManager.addListener(new DefaultAppLifecycleListener() {
            @Override
            public void onAppCreated(Class<?> origin) {
                appLifecycleManager.addListener(new DefaultAppLifecycleListener());
                appLifecycleManager.addListener(new DefaultAppLifecycleListener());
            }
        });
        appLifecycleManager.addListener(new DefaultAppLifecycleListener());

        appLifecycleManager.onCreate(new FirstActivity());
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: removeListener
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void removeListener_should_throw_if_null() throws Exception {
        appLifecycleManager.removeListener(null);
    }

    @Test(expected = IllegalStateException.class)
    public void removeListener_should_throw_if_not_added() throws Exception {
        appLifecycleManager.removeListener(new DefaultAppLifecycleListener());
    }

    @Test
    public void removeListener_should_remove_listener() throws Exception {
        final DefaultAppLifecycleListener listener = new DefaultAppLifecycleListener();

        appLifecycleManager.addListener(listener);

        assertTrue(appLifecycleManager.listeners.contains(listener));

        appLifecycleManager.removeListener(listener);

        assertFalse(appLifecycleManager.listeners.contains(listener));
    }

    @Test
    public void removeListener_should_not_remove_persistent_listener() throws Exception {
        final PersistentAppLifecycleListener listener = new PersistentAppLifecycleListener();

        appLifecycleManager.addListener(listener);

        assertTrue(appLifecycleManager.listeners.contains(listener));

        appLifecycleManager.removeListener(listener);

        assertTrue(appLifecycleManager.listeners.contains(listener));
    }

    @Test
    public void removeListener_should_return_manager() throws Exception {
        AppLifecycleListenable listenable = appLifecycleManager.addListener(new DefaultAppLifecycleListener());

        assertSame(appLifecycleManager, listenable);
    }

    @Test
    public void removeListener_should_not_throw_concurrent_modification_exception() throws Exception {
        final DefaultAppLifecycleListener firstListener = new DefaultAppLifecycleListener();
        final DefaultAppLifecycleListener lastListener = new DefaultAppLifecycleListener();

        appLifecycleManager.addListener(firstListener);
        appLifecycleManager.addListener(new DefaultAppLifecycleListener() {
            @Override
            public void onAppCreated(Class<?> origin) {
                appLifecycleManager.removeListener(firstListener);
                appLifecycleManager.removeListener(lastListener);
            }
        });
        appLifecycleManager.addListener(lastListener);

        appLifecycleManager.onCreate(new FirstActivity());
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: dispose
    //----------------------------------------------------------------------------------------------

    @Test
    public void dispose_should_remove_listeners() throws Exception {
        assertTrue(appLifecycleManager.listeners.isEmpty());

        appLifecycleManager.addListener(new DefaultAppLifecycleListener());

        assertFalse(appLifecycleManager.listeners.isEmpty());

        appLifecycleManager.dispose();

        assertTrue(appLifecycleManager.listeners.isEmpty());
    }

    @Test
    public void dispose_should_reset_state() throws Exception {
        appLifecycleManager.onCreate(new FirstActivity());

        assertNotNull(appLifecycleManager.currentOrigin);
        assertNotNull(appLifecycleManager.lastEvent);

        appLifecycleManager.dispose();

        assertNull(appLifecycleManager.currentOrigin);
        assertNull(appLifecycleManager.lastEvent);
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: onCreate
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void onCreate_should_throw_if_origin_null() throws Exception {
        appLifecycleManager.onCreate(null);
    }

    @Test
    public void onCreate_should_call_listener_once() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final Activity targetOrigin = new FirstActivity();

        appLifecycleManager.addListener(new DefaultAppLifecycleListener() {
            @Override
            public void onAppCreated(Class<?> origin) {
                counter.incrementAndGet();
            }
        });

        appLifecycleManager.onCreate(targetOrigin);
        appLifecycleManager.onCreate(new SecondActivity());
        appLifecycleManager.onCreate(new FirstActivity());

        assertEquals(1, counter.get());
        assertEquals(targetOrigin.getClass(), appLifecycleManager.currentOrigin);
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: onStart
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void onStart_should_throw_if_origin_null() throws Exception {
        appLifecycleManager.onStart(null);
    }

    @Test
    public void onStart_should_only_call_listeners_if_same_origin() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final Activity targetOrigin = new FirstActivity();
        final Activity secondOrigin = new SecondActivity();

        appLifecycleManager.addListener(new DefaultAppLifecycleListener() {
            @Override
            public void onAppStarted(Class<?> origin) {
                counter.incrementAndGet();
            }
        });

        // these should not be called
        appLifecycleManager.onStart(secondOrigin);
        appLifecycleManager.onStart(secondOrigin);

        appLifecycleManager.onCreate(targetOrigin);
        appLifecycleManager.onStart(targetOrigin);
        appLifecycleManager.onStart(secondOrigin);

        assertEquals(1, counter.get());
    }

    @Test
    public void onStart_should_call_listeners_only_once() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final Activity targetOrigin = new FirstActivity();

        appLifecycleManager.addListener(new DefaultAppLifecycleListener() {
            @Override
            public void onAppStarted(Class<?> origin) {
                counter.incrementAndGet();
            }
        });

        appLifecycleManager.onCreate(targetOrigin);
        appLifecycleManager.onStart(targetOrigin);
        appLifecycleManager.onStart(targetOrigin);

        assertEquals(1, counter.get());
    }

    @Test
    public void onStart_should_not_set_origin_if_current_is_null() throws Exception {
        appLifecycleManager.onStart(new FirstActivity());

        assertNull(appLifecycleManager.currentOrigin);
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: onResume
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void onResume_should_throw_if_origin_null() throws Exception {
        appLifecycleManager.onResume(null);
    }

    @Test
    public void onResume_should_call_listeners_only_once() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final Activity targetOrigin = new FirstActivity();

        appLifecycleManager.addListener(new DefaultAppLifecycleListener() {
            @Override
            public void onAppResumed(Class<?> origin) {
                counter.incrementAndGet();
            }
        });

        appLifecycleManager.onCreate(targetOrigin);
        appLifecycleManager.onStart(targetOrigin);
        appLifecycleManager.onResume(targetOrigin);
        appLifecycleManager.onResume(targetOrigin);

        assertEquals(1, counter.get());
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: onPause
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void onPause_should_throw_if_origin_null() throws Exception {
        appLifecycleManager.onPause(null);
    }

    @Test
    public void onPause_should_call_listeners_only_once() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final Activity targetOrigin = new FirstActivity();

        appLifecycleManager.addListener(new DefaultAppLifecycleListener() {
            @Override
            public void onAppPaused(Class<?> origin) {
                counter.incrementAndGet();
            }
        });

        appLifecycleManager.onCreate(targetOrigin);
        appLifecycleManager.onStart(targetOrigin);
        appLifecycleManager.onResume(targetOrigin);
        appLifecycleManager.onPause(targetOrigin);
        appLifecycleManager.onPause(targetOrigin);

        assertEquals(1, counter.get());
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: onStop
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void onStop_should_throw_if_origin_null() throws Exception {
        appLifecycleManager.onStop(null);
    }

    @Test
    public void onStop_should_call_listeners_only_once() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final Activity targetOrigin = new FirstActivity();

        appLifecycleManager.addListener(new DefaultAppLifecycleListener() {
            @Override
            public void onAppStopped(Class<?> origin) {
                counter.incrementAndGet();
            }
        });

        appLifecycleManager.onCreate(targetOrigin);
        appLifecycleManager.onStart(targetOrigin);
        appLifecycleManager.onResume(targetOrigin);
        appLifecycleManager.onPause(targetOrigin);
        appLifecycleManager.onStop(targetOrigin);
        appLifecycleManager.onStop(targetOrigin);

        assertEquals(1, counter.get());
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: onFinish
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void onFinish_should_throw_if_origin_null() throws Exception {
        appLifecycleManager.onFinish(null);
    }

    @Test
    public void onFinish_should_call_listeners_only_once() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final Activity targetOrigin = new FirstActivity();

        appLifecycleManager.addListener(new DefaultAppLifecycleListener() {
            @Override
            public void onAppFinished(Class<?> origin) {
                counter.incrementAndGet();
            }
        });

        appLifecycleManager.onCreate(targetOrigin);
        appLifecycleManager.onStart(targetOrigin);
        appLifecycleManager.onResume(targetOrigin);
        appLifecycleManager.onPause(targetOrigin);
        appLifecycleManager.onStop(targetOrigin);
        appLifecycleManager.onFinish(targetOrigin);
        appLifecycleManager.onFinish(targetOrigin);

        assertEquals(1, counter.get());
    }

    @Test
    public void onFinish_should_reset_state() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final Activity targetOrigin = new FirstActivity();

        final AppLifecycleListener listener = new DefaultAppLifecycleListener() {
            @Override
            public void onAppFinished(Class<?> origin) {
                counter.incrementAndGet();
            }
        };

        appLifecycleManager.addListener(listener);

        appLifecycleManager.onCreate(targetOrigin);
        appLifecycleManager.onStart(targetOrigin);
        appLifecycleManager.onResume(targetOrigin);
        appLifecycleManager.onPause(targetOrigin);
        appLifecycleManager.onStop(targetOrigin);
        appLifecycleManager.onFinish(targetOrigin);
        appLifecycleManager.onFinish(targetOrigin);

        assertFalse(appLifecycleManager.listeners.contains(listener));
        assertNull(appLifecycleManager.currentOrigin);
        assertNull(appLifecycleManager.lastEvent);
    }

    //----------------------------------------------------------------------------------------------
    // FUNCTIONAL TESTS
    //----------------------------------------------------------------------------------------------

    @Test
    public void functional_navigating_between_activities() throws Exception {
        final List<TestOriginEvent> actualEvents = createTestListener();

        final FirstActivity firstActivity = new FirstActivity();
        final SecondActivity secondActivity = new SecondActivity();

        // launch: start first activity
        appLifecycleManager.onCreate(firstActivity); // NOTIFY: create
        appLifecycleManager.onStart(firstActivity); // NOTIFY: start
        appLifecycleManager.onResume(firstActivity); // NOTIFY: resume

        // start second activity
        appLifecycleManager.onPause(firstActivity); // NOTIFY: pause
        appLifecycleManager.onCreate(secondActivity);
        appLifecycleManager.onStart(secondActivity);
        appLifecycleManager.onResume(secondActivity); // NOTIFY: resume
        appLifecycleManager.onStop(firstActivity);

        // go back to first activity
        appLifecycleManager.onPause(secondActivity); // NOTIFY: pause
        appLifecycleManager.onCreate(firstActivity);
        appLifecycleManager.onStart(firstActivity);
        appLifecycleManager.onResume(firstActivity); // NOTIFY: resume
        appLifecycleManager.onStop(secondActivity);
        appLifecycleManager.onFinish(secondActivity);

        // go back to home screen (exit)
        appLifecycleManager.onPause(firstActivity); // NOTIFY: pause
        appLifecycleManager.onStop(firstActivity); // NOTIFY: stop
        appLifecycleManager.onFinish(firstActivity); // NOTIFY: finish

        final List<TestOriginEvent> expectedEvents = new LinkedList<TestOriginEvent>();
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.CREATE));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.START));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.RESUME));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.PAUSE));
        expectedEvents.add(new TestOriginEvent(SecondActivity.class, AppLifecycleEvent.RESUME));
        expectedEvents.add(new TestOriginEvent(SecondActivity.class, AppLifecycleEvent.PAUSE));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.RESUME));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.PAUSE));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.STOP));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.FINISH));

        assertEquals(expectedEvents, actualEvents);
    }

    @Test
    public void functional_bringing_app_to_background_and_foreground() throws Exception {
        final List<TestOriginEvent> actualEvents = createTestListener();

        final FirstActivity firstActivity = new FirstActivity();

        // launch: start first activity
        appLifecycleManager.onCreate(firstActivity);
        appLifecycleManager.onStart(firstActivity);
        appLifecycleManager.onResume(firstActivity);

        // go back to home screen (to background)
        appLifecycleManager.onPause(firstActivity);
        appLifecycleManager.onStop(firstActivity);

        // go back to app (to foreground)
        appLifecycleManager.onStart(firstActivity);
        appLifecycleManager.onResume(firstActivity);

        // go back to home screen (exit)
        appLifecycleManager.onPause(firstActivity);
        appLifecycleManager.onStop(firstActivity);
        appLifecycleManager.onFinish(firstActivity);

        final List<TestOriginEvent> expectedEvents = new LinkedList<TestOriginEvent>();
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.CREATE));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.START));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.RESUME));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.PAUSE));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.STOP));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.START));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.RESUME));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.PAUSE));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.STOP));
        expectedEvents.add(new TestOriginEvent(FirstActivity.class, AppLifecycleEvent.FINISH));

        assertEquals(expectedEvents, actualEvents);
    }

    //----------------------------------------------------------------------------------------------
    // HELPER METHODS
    //----------------------------------------------------------------------------------------------

    private List<TestOriginEvent> createTestListener() {
        final List<TestOriginEvent> actualEvents = new LinkedList<TestOriginEvent>();

        appLifecycleManager.addListener(new AppLifecycleListener() {
            @Override
            public void onAppCreated(Class<?> origin) {
                actualEvents.add(new TestOriginEvent(origin, AppLifecycleEvent.CREATE));
            }

            @Override
            public void onAppStarted(Class<?> origin) {
                actualEvents.add(new TestOriginEvent(origin, AppLifecycleEvent.START));
            }

            @Override
            public void onAppResumed(Class<?> origin) {
                actualEvents.add(new TestOriginEvent(origin, AppLifecycleEvent.RESUME));
            }

            @Override
            public void onAppPaused(Class<?> origin) {
                actualEvents.add(new TestOriginEvent(origin, AppLifecycleEvent.PAUSE));
            }

            @Override
            public void onAppStopped(Class<?> origin) {
                actualEvents.add(new TestOriginEvent(origin, AppLifecycleEvent.STOP));
            }

            @Override
            public void onAppFinished(Class<?> origin) {
                actualEvents.add(new TestOriginEvent(origin, AppLifecycleEvent.FINISH));
            }
        });

        return actualEvents;
    }

    //----------------------------------------------------------------------------------------------
    // HELPER CLASS: TestOriginEvent
    //----------------------------------------------------------------------------------------------

    static final class TestOriginEvent {
        final Class<?> origin;
        final AppLifecycleEvent event;

        public TestOriginEvent(Class<?> origin, AppLifecycleEvent event) {
            this.origin = checkNotNull(origin);
            this.event = checkNotNull(event);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TestOriginEvent && o.hashCode() == hashCode();
        }

        @Override
        public int hashCode() {
            int result = origin.hashCode();
            result = 31 * result + event.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "TestOriginEvent{" +
                    "origin=" + origin.getSimpleName() +
                    ", event=" + event +
                    '}';
        }
    }

}
