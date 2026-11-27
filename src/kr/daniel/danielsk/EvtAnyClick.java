package kr.daniel.danielsk;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class EvtAnyClick extends SkriptEvent {

    @Override
    public String toString(Event e, boolean debug) {
        return "click";
    }

    @Override
    public boolean check(Event e) {
        return e instanceof PlayerInteractEntityEvent;
    }

    @Override
    public boolean init(
            Literal<?>[] args,
            int matchedPattern,
            SkriptParser.ParseResult parser) {
        return true;
    }
}