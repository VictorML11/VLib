package me.torciv.vlib.commands.extra.language;

public abstract class Language {

    private LangType langType;
    private Translator translator;

    public Language(LangType langType, Translator translator) {
        this.langType = langType;
        this.translator = translator;
    }

    public LangType getLangType() {
        return langType;
    }

    public void setLangType(LangType langType) {
        this.langType = langType;
    }

    public Translator getTranslator() {
        return translator;
    }

    public void setTranslator(Translator translator) {
        this.translator = translator;
    }

    
}
