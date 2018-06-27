package me.torciv.vlib.commands.extra.language;

import me.torciv.vlib.commands.extra.language.debug.Debug;
import me.torciv.vlib.commands.extra.language.debug.EnglishDebug;
import me.torciv.vlib.commands.extra.language.debug.SpanishDebug;
import me.torciv.vlib.commands.extra.language.types.English;
import me.torciv.vlib.commands.extra.language.types.Spanish;

public class LangFactory {

    public static Language loadLang(LangType langType){
        switch (langType){
            case ENGLISH:
                return new English();
            case SPANISH:
                return new Spanish();
            default:
                return null;
        }
    }

    public static Debug loadDebug(LangType langType){
        switch (langType){
            case ENGLISH:
                return new EnglishDebug();
            case SPANISH:
                return new SpanishDebug();
            default:
                return null;
        }
    }

}
