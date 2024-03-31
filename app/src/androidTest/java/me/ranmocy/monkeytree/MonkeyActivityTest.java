package me.ranmocy.monkeytree;

import static junit.framework.Assert.assertEquals;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

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
