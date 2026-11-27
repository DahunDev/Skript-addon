package kr.daniel.danielsk;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.permissions.PermissionAttachment;

public class EffRunCmdWithPerm extends Effect {
    private Expression<Player> players;
    private Expression<String> commands;
    private Expression<String> permissions;

    @Override
    protected void execute(Event e) {
        String[] commandValues = commands.getArray(e);
        String[] permissionValues = permissions.getArray(e);
		// Stage 2 (Runtime): Block execution if the lengths of dynamic list variables or expressions differ.
        if (commandValues.length != permissionValues.length) {
            DanielSK.getInstance().getLogger().log(Level.WARNING,
                    "Command count (" + commandValues.length + ") does not match permission count ("
                            + permissionValues.length + "); skipping execution.");
            return;
        }

        if (commandValues.length == 0) {
            return;
        }

        Player[] targetPlayers = players.getArray(e);
        if (targetPlayers.length == 0) {
            return;
        }

        for (int i = 0; i < commandValues.length; i++) {
            String command = commandValues[i];
            String permission = permissionValues[i];

            if (command == null || permission == null) {
                continue;
            }

            if (command.startsWith("/")) {
                command = command.substring(1);
            }

            for (Player player : targetPlayers) {
                if (player == null) {
                    continue;
                }

                if (player.hasPermission(permission)) {
                    Skript.dispatchCommand(player, command);
                    continue;
                }

                DanielSK plugin = DanielSK.getInstance();
                PermissionAttachment attachment = player.addAttachment(plugin);

                try {
                    attachment.setPermission(permission, true);
                    Bukkit.dispatchCommand(player, command);
                } catch (RuntimeException ex) {
                    plugin.getLogger().log(Level.WARNING,
                            "Temporary-permission command failed", ex);
                    throw ex;
                } finally {
                    player.removeAttachment(attachment);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern,
                        Kleenean delayed, SkriptParser.ParseResult parser) {
        players = (Expression<Player>) expressions[0];
        commands = (Expression<String>) expressions[1];
        permissions = (Expression<String>) expressions[2];

		// Step 1 (Parse time): Reject static cardinality mismatches (one singular, one plural) at load time.
        if (commands.isSingle() != permissions.isSingle()) {
            Skript.error("The number of commands must match the number of permissions.");
            return false;
        }

        return true;
    }

    @Override
    public String toString(Event e, boolean debug) {
        return getClass().getName();
    }
}