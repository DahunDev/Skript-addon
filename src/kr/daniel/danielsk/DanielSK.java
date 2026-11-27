package kr.daniel.danielsk;

import ch.njol.skript.Skript;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class DanielSK extends JavaPlugin {
   private static DanielSK plugin;
   private static final int MAX_ASYNC_THREADS = 64;
   private final Set<FutureTask<?>> activeTasks = new HashSet<>();
   private final AtomicLong workerSequence = new AtomicLong();
   // Guarded by this; null means submissions are disabled.
   private ExecutorService executor;

   @Override
   @SuppressWarnings({"unchecked", "rawtypes"})
   public void onEnable() {
      plugin = this;
      synchronized (this) {
         executor = new ThreadPoolExecutor(
            0, MAX_ASYNC_THREADS, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            action -> {
               Thread worker = new Thread(action, "DanielSK-Async-" + workerSequence.incrementAndGet());
               worker.setDaemon(true);
               return worker;
            },
            new ThreadPoolExecutor.AbortPolicy());
      }
      Skript.registerEffect(EffOptionThread.class, "$ Async", "$ Thread");
	  Skript.registerEffect(EffRunCmdWithPerm.class,
    "(force|make) %players% (run|execute) (cmd|command)[s] %strings% with permission[s] %strings%",
    "(run|execute) %players% (command|cmd)[s] %strings% with permission[s] %strings%");
      Class<? extends PlayerEvent>[] eventTypes = new Class[]{PlayerInteractEvent.class, PlayerInteractEntityEvent.class};
      Skript.registerEvent("anyclick", EvtAnyClick.class, eventTypes, "anyclick[s]");
   }

   // FutureTask retains interruptible cancellation; CompletableFuture delivers
   // completion without polling or using the JVM common pool.
   public synchronized <T> void startAsync(Callable<T> action, Consumer<T> continuation) {
      if (executor == null) {
         throw new RejectedExecutionException("DanielSK is stopping");
      }
      final ExecutorService owner = executor;
      final CompletableFuture<T> completion = new CompletableFuture<>();
      completion.whenComplete((value, failure) -> {
         if (failure instanceof CancellationException) {
            return;
         }
         if (failure != null) {
            getLogger().log(Level.SEVERE, "Async Skript item failed", failure);
            return;
         }
         synchronized (DanielSK.this) {
            // Reject stale completions from a previous enable/disable cycle.
            if (executor != owner || !isEnabled()) {
               return;
            }
            try {
               Bukkit.getScheduler().runTask(this, () -> {
                  synchronized (DanielSK.this) {
                     if (executor != owner || !isEnabled()) {
                        return;
                     }
                  }
                  continuation.accept(value);
               });
            } catch (RuntimeException ex) {
               getLogger().log(Level.WARNING, "Could not schedule async continuation", ex);
            }
         }
      });
      final FutureTask<T> future = new FutureTask<T>(action) {
         @Override
         protected void done() {
            synchronized (DanielSK.this) {
               activeTasks.remove(this);
            }
            try {
               // done() runs only after terminal completion; get() does not wait.
               completion.complete(get());
            } catch (CancellationException ex) {
               completion.cancel(false);
            } catch (ExecutionException ex) {
               completion.completeExceptionally(ex.getCause());
            } catch (InterruptedException ex) {
               Thread.currentThread().interrupt();
               completion.completeExceptionally(ex);
            }
         }
      };
      activeTasks.add(future);
      try {
         owner.execute(future);
      } catch (RuntimeException | Error ex) {
         future.cancel(false);
         throw ex;
      }
   }

   @Override
   public void onDisable() {
      final ExecutorService retiring;
      final ArrayList<FutureTask<?>> pending;
      synchronized (this) {
         retiring = executor;
         executor = null;
         pending = new ArrayList<>(activeTasks);
      }
      try {
         Bukkit.getScheduler().cancelTasks(this);
      } finally {
         try {
            for (FutureTask<?> task : pending) {
               task.cancel(true);
            }
         } finally {
            if (retiring != null) {
               retiring.shutdownNow();
            }
         }
      }
   }

   public static DanielSK getInstance() {
      return plugin;
   }
}
