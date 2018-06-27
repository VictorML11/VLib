package me.torciv.vlib.commands.extra.language.types;

import me.torciv.vlib.commands.extra.language.LangType;
import me.torciv.vlib.commands.extra.language.Language;
import me.torciv.vlib.commands.extra.language.Translator;

public class Spanish extends Language {

    public Spanish() {
        super(LangType.SPANISH, new Translator());
    }

}
