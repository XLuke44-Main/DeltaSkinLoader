# DeltaSkinLoader

DeltaSkinLoader is a minecraft mod that loads custom skins, capes, and elytras, fully locally.

## Usage

### Textures

Place textures in "config/DeltaSkinLoader"

- "skins/PlayerName.png" - Skins
- "capes/PlayerName.png" - Capes
- "elytras/PlayerName.png" - Elytras

Use the player's username as the filename, with matching capitalization.

Skins must be 64x64 or 64x32. Capes and elytra textures must be 64x32.

### Player Settings

Player settings are stored in "playerdata/PlayerName.json" and created automatically.

- "enabled" - Enables or disables custom textures for the player.
- "model" - Sets the arm type to "classic"/"steve" or "slim"/"alex"
- "skin" - Sets the default skin to "classic"/"steve" or "slim"/"alex"

### Configuration

General settings are stored in "config.json"

- "enableSkinOverride" - Enables custom skins.
- "enableCape" - Enables custom capes.
- "enableCustomElytra" - Enables custom elytra textures.
- "enableSkinTransparency" - Allows transparent areas in skins.
- "enableByDefault" - Sets whether custom skins are enabled by default, per player.
- "customDefaultSkins" - Enables custom default skins.

### Default Skins

The "noskinfile" section configures the fallback skin applied to players who do not have a local skin file assigned.

- "enabled" - Toggles skin overriding.
- "model" - Specifies the player model's arm type: "steve"/"classic or "alex"/"slim".
- "skin" - Specifies the skin to use: "steve"/"classic" or "alex"/"slim".

### Custom Default Skins

Set "customDefaultSkins" to "true" in "config.json"

Place these files in the "skins" folder:

- "custom_c.png" - Classic Skin
- "custom_s.png" - Slim Skin
