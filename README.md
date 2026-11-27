
A lightweight Skript addon for legacy Minecraft servers, providing single-item asynchronous execution, temporary-permission command execution, and entity interaction events.

## Features

### Asynchronous execution

```skript
$ Async
$ Thread
```

Both directives have the same behavior: execute the immediately following Skript trigger item on a worker thread, then resume the remaining trigger on the Bukkit main thread after successful completion. They do not make the entire trigger asynchronous.

The addon uses a dedicated `ExecutorService`, `FutureTask` for cancellation, and `CompletableFuture` for completion callbacks. There is no per-tick completion polling.

| Setting | Value |
| --- | --- |
| Core workers | 0 |
| Maximum workers | 64 |
| Idle timeout | 60 seconds |
| Work queue | `SynchronousQueue` (no queued backlog) |
| Rejection policy | `AbortPolicy` |
| Executor scope | DanielSK only, shared across its Skript invocations |

If no worker is available and the pool cannot grow, the addon logs a warning and stops the current trigger. Failed or cancelled work does not resume the trigger. On plugin disable, active tasks receive cancellation/interrupt requests and the executor is shut down. Interrupts are cooperative, not forced termination.

**Important:** Only use the async directives with operations explicitly safe to execute off the main thread. Arbitrary Skript variables, event objects, Bukkit world operations, permission changes, and control-flow constructs are not guaranteed to be safe. The implementation uses reflection against Skript's internal `TriggerItem.walk(Event)` method, ignores its return value, and resumes from the next item's `getNext()`, so complex trigger items may not behave like normal synchronous execution.

### Execute commands with temporary permissions

Syntax (one player, one command, one permission per invocation):

```text
(force|make) %player% (run|execute) (cmd|command) %string% with permission %string%
(run|execute) %player% (command|cmd) %string% with permission %string%
```

Examples:

```skript
force player run command "spawn" with permission "essentials.spawn"
make player execute command "fly" with permission "essentials.fly"
run player command "heal" with permission "essentials.heal"
```

Dynamic arguments:

```skript
set {_cmd} to "fly"
set {_perm} to "essentials.fly"
force player run command {_cmd} with permission {_perm}
```

- A leading `/` is stripped from the command.
- If the player already has the requested permission, the command is dispatched directly through Skript.
- Otherwise, a temporary Bukkit `PermissionAttachment` is added and removed in a `finally` block after dispatch.
- Missing player, command, or permission values cause the effect to skip execution without an array-index exception.
- Multiple permissions per invocation are not supported. Run this effect on the main thread and use trusted command and permission inputs only. Temporary permission applies during dispatch, not to arbitrary asynchronous work scheduled by the command.

### Entity interaction event

```skript
on anyclick:
    send "Entity interaction detected." to player
```

The registered aliases are `anyclick` and `anyclicks`. Despite its name, this event only accepts `PlayerInteractEntityEvent` (entity interaction), not ordinary block or air clicks or entity attacks. The unconditional console debug print has been removed.
