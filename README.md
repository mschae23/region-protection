# Server Utils
This is a small mod with server-side things I find useful.

Requires LuckPerms.

## Permissions

### Commands
- `serverutils.command.pos.root`: Permission for /pos command (defaults to `true`).
- `serverutils.command.pos.public`: Overrides the `command.pos.in_public_chat` config property
(if this is enabled, the player's coordinates will be shown to all players instead of just the one who used /pos).

### Other
- `serverutils.death.printcoords.enabled`: When a player dies, their coordinates will be shown in chat if this and the corresponding config option are enabled.
- `serverutils.death.printcoords.public`: Overrides the `death_coords.in_public_chat` config property
(if this is enabled, the player's coordinates will be sent to all players instead of just the one who died).
- `serverutils.key...`: Default prefix for permissions to open containers that are locked.
