package kr.daniel.danielsk;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.io.File;
import org.bukkit.event.Event;

public abstract class Pragma extends Effect {
   protected void execute(Event event) {
   }

   public String toString(Event event, boolean b) {
      return "pragma";
   }

   public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
      this.register(ScriptLoader.currentScript.getFile(), parseResult);
      return true;
   }

   protected abstract void register(File var1, SkriptParser.ParseResult var2);
}
