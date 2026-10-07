# CustomWeapons

Config-driven custom bows/crossbows for Paper/Spigot 1.20+.

## Build
    mvn clean package
The jar appears at `target/CustomWeapons.jar`. Drop it in your server's `plugins/` folder.

## Commands
- `/weapons list`
- `/weapons give <weapon> [player]`   (customweapons.admin)
- `/weapons reload`                   (customweapons.admin)

## Adding a new weapon (no code)
Copy any block under `weapons:` in config.yml, give it a new id, change the
ability/options, then `/weapons reload`.

## Adding a new ability (code)
In `Abilities.java`, add a `register("MY_ABILITY", (plugin, shooter, target, options) -> { ... });`
then use `ability: MY_ABILITY` in config.yml.

## Player Tracker compass
- `/weapons tracker [player]` gives the compass.
- Right-click it to open a GUI of online players (paged) and click one to track them.
- Sneak + right-click clears the target.
- Works in every dimension (lodestone-compass trick). If the target is in another dimension,
  it points to their last known position in your current dimension.
- Settings are under `tracker:` in config.yml.

## Shadow Blade (dash_sword)
Netherite sword. `/weapons give dash_sword`
- Right-click: dash up to 20 blocks in the direction you look (3s cooldown), stops before walls.
- Crouch + right-click: blindness (255) for 30s on everyone within 50 blocks (120s cooldown).
All numbers are in config.yml under `weapons: dash_sword:`.
