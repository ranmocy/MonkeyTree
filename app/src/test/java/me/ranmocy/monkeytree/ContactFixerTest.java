package me.ranmocy.monkeytree;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.text.Transliterator;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ContactFixerTest {

    private static final Map<String, String> testMap = ImmutableMap.<String, String>builder()
            .put("赵", "zhào")
            .put("钱", "qián")
            .put("孙", "sūn")
            .put("李", "lǐ")
            .put("陈", "chén")
            .put("盛", "shèng")
            .put("吴", "wú")
            .put("広", "guǎng")
            .put("单", "shàn")
            .put("任", "rén")
            .put("殷", "yīn")
            .put("肖", "xiào")
            .put("俞", "yú")
            .put("乐", "yuè")
            .put("华", "huà")
            .put("过", "guō")
            .put("纪", "jǐ")
            .put("燕", "yān")
            .put("种", "chóng")
            .put("解", "xiè")
            .put("查", "zhā")
            .put("曾", "zēng")
            .put("盖", "gě")
            .put("区", "ōu")
            .put("仇", "qiú")
            .put("阚", "kàn")
            .put("缪", "miào")
            .put("朴", "piáo")
            .put("么", "yāo")
            .build();

    private Transliterator transliterator;

    @Before
    public void setup() {
        transliterator = new ContactFixer.ChineseNameTransliterator();
    }

    @Ignore
    @Test
    public void Han2Latin_Names_results() throws Exception {
        for (Map.Entry<String, String> entry : testMap.entrySet()) {
            System.out.println(String.format(".put(\"%s\", \"%s\")",
                    entry.getKey(), transliterator.transliterate(entry.getKey())));
        }
    }

    @Test
    public void Han2Latin_Names() throws Exception {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : testMap.entrySet()) {
            String target = entry.getKey();
            String expected = entry.getValue();
            String actual = transliterator.transliterate(target);
            if (!expected.contentEquals(actual)) {
                builder.append(String.format(
                        "\nExpected '%s' to be '%s', but actually is '%s'.",
                        target, expected, actual));
            }
        }
        if (builder.length() > 0) {
            throw new RuntimeException(builder.toString());
        }
    }
}
