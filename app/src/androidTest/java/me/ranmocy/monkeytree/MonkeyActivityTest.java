package me.ranmocy.monkeytree;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

/**
 * Android test for {@link MonkeyActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class MonkeyActivityTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("me.ranmocy.monkeytree", appContext.getPackageName());
    }
}
