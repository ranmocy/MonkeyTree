package me.ranmocy.monkeytree;

import androidx.annotation.VisibleForTesting;

import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.Transliterator;

import java.util.HashMap;
import java.util.Map;

/**
 * Container class contains all {@link Transliterator}s.
 */
final class Transliterators {
    private static final Transliterator HAN_TO_PINYIN = Transliterator.getInstance("Han-Latin/Names");
    static final Transliterator PINYIN_TO_ASCII = Transliterator.getInstance("Latin-Ascii");
    static final Transliterator ANY_TO_NULL = new NullTransliterator();
    static final Transliterator HAN_TO_PINYIN_NAME = new ChineseNameTransliterator();

    /**
     * NullTransliterator will change any string into empty string.
     */
    private static final class NullTransliterator extends Transliterator {

        private NullTransliterator() {
            super("Any-Null", null /*filter*/);
        }

        @Override
        protected void handleTransliterate(Replaceable text, Position pos, boolean incremental) {
            text.replace(pos.start, pos.limit, "");
            pos.start = text.length();
            pos.limit = text.length();
        }
    }

    /**
     * A {@link Transliterator} wrapper of "Han-Latin/Names" with enhancement of Chinese names.
     */
    @VisibleForTesting
    private static final class ChineseNameTransliterator extends Transliterator {

        private static final Map<String, String> ENHANCEMENTS = getEnhancements();

        private static Map<String, String> getEnhancements() {
            HashMap<String, String> map = new HashMap<>();
            map.put("阚", "kàn");
            map.put("缪", "miào");
            map.put("朴", "piáo");
            map.put("么", "yāo");
            map.put("肖", "xiāo");
            return map;
        }

        private ChineseNameTransliterator() {
            super("Han-Latin/NamesEnhanced", HAN_TO_PINYIN.getFilter());
        }

        @Override
        protected void handleTransliterate(Replaceable text, Position pos, boolean incremental) {
            String source = text.toString();
            String target = ENHANCEMENTS.containsKey(source)
                    ? ENHANCEMENTS.get(source)
                    : HAN_TO_PINYIN.transliterate(source);

            text.replace(pos.start, pos.limit, target);
            pos.start = text.length();
            pos.limit = text.length();
        }
    }

    private Transliterators() {}
}
