package kr.daniel.danielsk;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import org.bukkit.event.Event;

@Name("Safely Execute Thread")
@Description({"Makes the next line of code run on a different thread. The code following it will be delayed until the thread finishes."})
@Patterns({"$ thread"})
public class EffOptionThread extends Pragma {
   private static Method walkMethod;

   static {
      try {
         walkMethod = TriggerItem.class.getDeclaredMethod("walk", Event.class);
         walkMethod.setAccessible(true);
      } catch (NoSuchMethodException e) {
         e.printStackTrace();
      }

   }

   protected TriggerItem walk(final Event e) {
      final DanielSK plugin = DanielSK.getInstance();
      final TriggerItem next = this.getNext();
      if (next == null) {
         return null;
      }
      try {
         walkNext(next, e);
      } catch (RejectedExecutionException ex) {
         plugin.getLogger().log(Level.WARNING, "Async Skript item rejected: executor saturated or stopping");
      }
      return null;
   }

   private void walkNext(final TriggerItem next, final Event e) {
      DanielSK.getInstance().startAsync(() -> {
         EffOptionThread.walkMethod.invoke(next, e);
         // Preserve original continuation selection (not the reflective return value).
         return next.getNext();
      }, continuation -> TriggerItem.walk(continuation, e));
   }

   protected void register(File executingScript, SkriptParser.ParseResult parseResult) {
   }
}
