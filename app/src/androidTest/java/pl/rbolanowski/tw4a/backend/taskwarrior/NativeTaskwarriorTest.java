package pl.rbolanowski.tw4a.backend.taskwarrior;

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pl.rbolanowski.tw4a.test.AndroidTestCase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pl.rbolanowski.tw4a.backend.taskwarrior.NativeTaskwarrior.*;

@RunWith(AndroidJUnit4.class)
public class NativeTaskwarriorTest extends AndroidTestCase {

    private static final Pattern UUID_PATTERN = Pattern.compile("\"uuid\":\"(.*?)\"");

    @BeforeClass public static void configureBackend() throws Exception {
        new TaskwarriorBackendFactory(getTargetContext()).newConfigurator().configure();
    }

    private NativeTaskwarrior mTaskwarrior;

    @Before public void setupTaskwarrior() {
        mTaskwarrior = new NativeTaskwarrior(getTargetContext(), TaskwarriorBackendFactory.getSpec());
    }

    @After public void clearDataDirectory() {
        mTaskwarrior.clear();
    }

    @Test public void putsTasks() {
        NativeTaskwarrior.Output outputs[] = new NativeTaskwarrior.Output[] {
            mTaskwarrior.put("first"),
            mTaskwarrior.put("second, and quite long, lol"),
        };
        assertTrue(matchPattern(Pattern.compile("Created task 1"), outputs[0].stdout));
        assertTrue(matchPattern(Pattern.compile("Created task 2"), outputs[1].stdout));
    }

    @Test public void exportsData() throws Exception{
        mTaskwarrior.put("some task");
        mTaskwarrior.put("other task");
        String[] output = mTaskwarrior.export().stdout.split("\n");
        assertTrue(matchPattern(Pattern.compile("\"description\":\"some task\""), output[0]));
        assertTrue(matchPattern(Pattern.compile("\"description\":\"other task\""), output[1]));
    }

    @Test public void updateingNotExistingTaskDoesNothing() throws Exception {
        Output output = mTaskwarrior.modify("any", "any", TaskStatus.Pending);
        assertTrue(output.stdout.isEmpty());
        assertTrue(mTaskwarrior.export().stdout.isEmpty());
    }

    @Test public void updatesDescription() throws Exception {
        mTaskwarrior.put("first");
        String uuid = findUuid(mTaskwarrior.export().stdout);
        assertNotNull(uuid);

        mTaskwarrior.modify(uuid, "changed to really long long text", null);

        String stdout = mTaskwarrior.export().stdout;
        assertTrue(isOneTask(stdout));
        assertTrue(matchPattern(Pattern.compile("\"description\":\"changed to really long long text\""), stdout));
    }

    @Test public void updatesStatus() throws Exception {
        mTaskwarrior.put("first");
        String uuid = findUuid(mTaskwarrior.export().stdout);
        assertNotNull(uuid);

        mTaskwarrior.modify(uuid, null, TaskStatus.Completed);

        String stdout = mTaskwarrior.export().stdout;
        assertTrue(isOneTask(stdout));
        assertTrue(matchPattern(Pattern.compile("\"status\":\"completed\""), stdout));
    }

    private static String findUuid(String value) {
        Matcher matcher = UUID_PATTERN.matcher(value);
        if (!matcher.find()) return null;
        return matcher.group(1);
    }

    private static boolean isOneTask(String value) {
        return value.split("\n").length == 1;
    }

    private static boolean matchPattern(Pattern pattern, String value) {
        return pattern.matcher(value).find();
    }

}
