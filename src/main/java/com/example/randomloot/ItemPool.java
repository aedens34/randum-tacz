package com.example.randomloot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Builds the pool of possible replacements and picks one for each drop. */
public final class ItemPool {

    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    private static volatile List<ResourceLocation> items = null;
    private static volatile List<ResourceLocation> guns = List.of();

    private ItemPool() {
    }

    public static synchronized void reset() {
        items = null;
        guns = List.of();
    }

    private static synchronized void ensureBuilt() {
        if (items != null) {
            return;
        }

        Set<String> excluded = new HashSet<>(Config.EXCLUDED_ITEMS.get());
        List<ResourceLocation> itemList = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            if (excluded.contains(id.toString())) {
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null || item == Items.AIR) {
                continue;
            }
            itemList.add(id);
        }
        // Sorted so the mapping never depends on mod load order.
        itemList.sort(Comparator.comparing(ResourceLocation::toString));

        List<ResourceLocation> gunList = new ArrayList<>();
        if (Config.INCLUDE_TACZ_GUNS.get() && ModList.get().isLoaded("tacz")) {
            gunList.addAll(TaczCompat.gunIds());
            gunList.sort(Comparator.comparing(ResourceLocation::toString));
        }

        guns = gunList;
        items = itemList;
        RandomLootMod.LOGGER.info("[RandomLoot] Random pool ready: {} items + {} TaCZ guns.",
                itemList.size(), gunList.size());
    }

    public static ItemStack replacement(ItemStack original, LootContext context) {
        ensureBuilt();
        List<ResourceLocation> itemList = items;
        List<ResourceLocation> gunList = guns;
        if (itemList == null || (itemList.isEmpty() && gunList.isEmpty())) {
            return original;
        }

        long roll1;
        long roll2;
        if (Config.CHAOS_MODE.get()) {
            RandomSource random = context.getRandom();
            roll1 = random.nextLong();
            roll2 = random.nextLong();
        } else {
            ResourceLocation originalId = ForgeRegistries.ITEMS.getKey(original.getItem());
            long base = context.getLevel().getSeed() * 31L
                    + Config.SEED_OFFSET.get() * GOLDEN
                    + (originalId == null ? 0L : originalId.toString().hashCode());
            roll1 = mix(base);
            roll2 = mix(roll1 + GOLDEN);
        }

        boolean useGun = !gunList.isEmpty()
                && (itemList.isEmpty() || Math.floorMod(roll1, 100L) < Config.TACZ_GUN_PERCENT.get());

        if (useGun) {
            ResourceLocation gunId = gunList.get((int) Math.floorMod(roll2, (long) gunList.size()));
            ItemStack gun = TaczCompat.buildGun(gunId);
            if (!gun.isEmpty()) {
                return gun;
            }
            if (itemList.isEmpty()) {
                return original;
            }
        }

        ResourceLocation id = itemList.get((int) Math.floorMod(roll2, (long) itemList.size()));
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == Items.AIR) {
            return original;
        }
        int count = Math.max(1, Math.min(original.getCount(), item.getMaxStackSize()));
        return new ItemStack(item, count);
    }

    /** SplitMix64 - a fast, well-spread hash so neighbouring inputs give unrelated outputs. */
    private static long mix(long z) {
        z += GOLDEN;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
