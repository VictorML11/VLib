package me.torciv.vlib.commands;

import me.torciv.vlib.commands.arguments.AbstractArg;
import me.torciv.vlib.commands.extra.CommandOnly;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * Attach to a function to denote it as a command. You must register the class it is in as a command class before it will be used.
 */
public @interface Cmd {
    /**
     * Takes in any amount of subcommands. The command handler always chooses the best-fit command.
     * <br><br>
     * Example:
     * <br>
     * cmd = "sub1 sub2 sub3 sub4 sub5"
     */
    String cmd() default "";

    /**
     * The arguments for the command. Required arguments must be enclosed in <>'s
     * <br><br>
     * Example:
     * <br>
     * args = "&lt;arg1&gt; &lt;arg2&gt; [arg3]"
     * <br>
     * Argument 1 and 2 are required; the third is optional.
     */
    String args() default "";

    /**
     * Specifies the type that an argument should be. Default is Arg.ArgString.
     * <br><br>
     * Example:
     * <br>
     * argTypes = { Arg.ArgInteger, Arg.ArgString, Arg.ArgPlayer }
     * <ul>
     * <li>Argument 1 <i>must</i> be an integer.</li>
     * <li>Argument 2 <i>must</i> be a string(anything).</li>
     * <li>Argument 3 <i>must</i> be an online player.</li>
     * </ul>
     */
    Class<? extends AbstractArg<?>>[] argTypes() default {};

    /**
     * Argument description
     * argDesc = {"Intruduce un numero", "Introduce un testo en comillas"}
     */
    String[] argDesc() default {};

    /**
     * The text to show next to a command when a user does /cmd help.
     */
    String help() default "Default help thingy... :(";

    /**
     * The text to show next to a command when a user does /cmd help &lt;command&gt;.
     * <br><br>
     * Use to give more information about the command.
     */
    String longhelp() default "";

    /**
     * Should the command be shown in help at all?
     */
    boolean showInHelp() default true;

    /**
     * Specifies if the command should be restricted to CONSOLE, OP, or PLAYERS. Otherwise, ALL.
     */
    CommandOnly only() default CommandOnly.ALL;

    /**
     * The permission node to use.
     * <br><br>
     * Example:
     * <br>
     * new CommandManager(plugin, "Test", "testnode", "test");
     * <br>
     * permission = "edit"
     * <br>
     * This setup would require a player to have testnode.edit to use the command.
     */
    String permission() default "";
}
