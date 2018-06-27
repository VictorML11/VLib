package me.torciv.vlib.commands.extra;

/**
 * Used to define who is allowed to use a command.
 */
public enum CommandOnly {
    /**
     * Only players can use the command.
     */
    PLAYER,
    /**
     * Only op players can use the command. Can be overridden by a matched permission.
     */
    OP,
    /**
     * Only the console can use the command.
     */
    CONSOLE,
    /**
     * Anyone can use the command, given their permissions match.
     */
    ALL
}
