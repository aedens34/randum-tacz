# Random Loot (TaCZ compatible)

Forge 1.20.1 mod. Randomizes what blocks drop and what mobs drop. If TaCZ is
installed, TaCZ guns (base mod and every addon) can show up as random drops too.

## Build with GitHub
1. Create a new GitHub repository and upload everything in this folder
   (including the hidden `.github` folder).
2. Open the Actions tab and wait for the green check.
3. Download the `randomloot-jar` artifact, extract it, and put the .jar in your mods folder.

## Config
After the first world load, edit `serverconfig/randomloot-server.toml` inside the world folder.
- randomizeBlocks / randomizeMobs / randomizeOther
- chaosMode: false = each item always maps to the same replacement in a world; true = fresh roll every drop
- seedOffset: change it for a different mapping in the same world
- includeTaczGuns, taczGunPercent: chance a replacement is a TaCZ gun
- excludedItems: items that can never be a replacement
