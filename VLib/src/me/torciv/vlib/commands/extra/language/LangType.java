package me.torciv.vlib.commands.extra.language;

public enum LangType {

    SPANISH("Español"),
    ENGLISH("English");

    private String name;

    LangType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
