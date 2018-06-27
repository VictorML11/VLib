package me.torciv.vlib.commands.extra.language.types;

import me.torciv.vlib.commands.extra.language.LangType;
import me.torciv.vlib.commands.extra.language.Language;
import me.torciv.vlib.commands.extra.language.Translator;

public class English extends Language {

    public English() {
        super(LangType.ENGLISH, new Translator());
    }



}
