package me.torciv.vlib.commands.exceptions;

/**
 * This can be used to immediately throw an error without returning a <code>CommandFinished</code>.
 * It'll display the specified error.
 */
public class CommandException extends Exception {

    private static final long serialVersionUID = 1L;

    public CommandException(String message) {
        super(message);
    }
}
