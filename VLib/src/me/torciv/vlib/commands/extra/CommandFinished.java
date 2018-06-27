package me.torciv.vlib.commands.extra;

/**
 * A set of command errors.
 */
public enum CommandFinished {
    /**
     * Finished correctly
     */
    DONE(false, "Hecho"),
    /**
     * Command does not exist
     */
    COMMAND(true, "El comando no existe. Usa /%s para ayuda."),
    /**
     * Command does not exist
     */
    BADCOMMAND(true, "Error usa: /%s "),
    /**
     * Console not allowed to use
     */
    NOCONSOLE(true, "Este comando no puede ser ejecutado desde la consola."),
    /**
     * Player not allowed to use
     */
    NOPLAYER(true, "Este comando no puede ser ejecutador por jugadores."),
    /**
     * Player does not exist
     */
    EXISTPLAYER(true, "El jugador no existe"),
    /**
     * Incorrect permissions
     */
    PERMISSION(true, "Permisos insuficientes."),

    HOLDBLOCK(true, "You must be holding a block."),
    HOLDITEM(true, "You must be holding an item."),

    LONGSTRING(true, "String cannot be longer than %s!"),

    /**
     * Custom error
     */
    CUSTOM(true, "%s"),
    EXCEPTION(true, "An exception occured. Please contact a member of staff and tell them!");

    private boolean shouldPrint;
    private String errorString;
    private String extraString;

    CommandFinished(boolean par1ShouldPrint, String par1Error) {
        shouldPrint = par1ShouldPrint;
        errorString = par1Error;
    }

    public boolean shouldPrint() {
        return shouldPrint;
    }

    public String getErrorString() {
        if (extraString != null)
            return errorString.replace("%s", extraString);
        else
            return errorString;
    }

    public CommandFinished replace(String theString) {
        extraString = theString;
        return this;
    }
}
