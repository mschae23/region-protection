package de.martenschaefer.regionprotection.region.cache;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.util.TriState;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.ProtectionRule;
import de.martenschaefer.regionprotection.region.RegionV2;
import de.martenschaefer.regionprotection.region.shape.BoxShape;
import de.martenschaefer.regionprotection.region.shape.ProtectionContext;
import de.martenschaefer.regionprotection.region.shape.ProtectionShape;
import de.martenschaefer.regionprotection.region.shape.UnionShape;
import de.martenschaefer.regionprotection.state.RegionPersistentState;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.lucko.fabric.api.permissions.v0.Permissions;
import org.jetbrains.annotations.Nullable;

public final class PlayerRegionCache {
    private final Object2ObjectOpenHashMap<UUID, PlayerCacheEntry> byPlayer;
    private int cacheHits;
    private int cacheSwaps;
    private int cacheMisses;

    public PlayerRegionCache() {
        this.byPlayer = new Object2ObjectOpenHashMap<>();
        this.cacheHits = 0;
        this.cacheSwaps = 0;
        this.cacheMisses = 0;
    }

    public TriState check(RegionPersistentState state, ServerPlayerEntity player, ProtectionContext context, ProtectionRule rule) {
        PlayerCacheEntry playerEntry = this.byPlayer.computeIfAbsent(player.getUuid(), uuid -> new PlayerCacheEntry(new Reference2ObjectOpenHashMap<>()));

        PlayerRuleCacheEntry ruleEntry = playerEntry.byRule.get(rule);

        if (ruleEntry != null && ruleEntry.active && ruleEntry.dimension.equals(context.dimension().getValue())) {
            // RegionProtectionMod.LOGGER.info("Potential cache hit: " + rule.getName());

            ObjectBidirectionalIterator<Map.Entry<RegionV2, PlayerRuleCacheEntry>> intersectingIterator = ruleEntry.intersecting.entrySet().iterator();

            // Test if there is a region with a higher level that should be prioritized
            while (intersectingIterator.hasNext()) {
                Map.Entry<RegionV2, PlayerRuleCacheEntry> intersectingRuleEntryPair = intersectingIterator.next();
                // RegionProtectionMod.LOGGER.info("Potential cache swap (from " + (ruleEntry.region != null ? ruleEntry.region.key() : "null")
                //    + " to " + (intersectingRuleEntryPair.getValue().region != null ? intersectingRuleEntryPair.getValue().region.key() : "null") + "): "
                //    + rule.getName());

                if (ruleEntry.region != null && intersectingRuleEntryPair.getKey().level() <= ruleEntry.region.level()) {
                    break;
                }

                PlayerRuleCacheEntry intersectingRuleEntry = intersectingRuleEntryPair.getValue();

                for (BoxShape shape : intersectingRuleEntry.shapes) {
                    if (shape.testOnlyPosition(context.pos())) {
                        this.cacheSwaps++;
                        // RegionProtectionMod.LOGGER.info("Cache swap (1): " + rule.getName());
                        intersectingRuleEntry.shapes.addAndMoveToFirst(shape);

                        ruleEntry.intersecting = new Object2ObjectRBTreeMap<>();
                        Object2ObjectRBTreeMap<RegionV2, PlayerRuleCacheEntry> intersecting = new Object2ObjectRBTreeMap<>();
                        UnionShape union = new UnionShape(intersectingRuleEntry.shapes.toArray(BoxShape[]::new));

                        state.findIntersectingRegions(context.dimension(), union).filter(region -> region != intersectingRuleEntry.region).forEach(region -> {
                            TriState value = Permissions.getPermissionValue(player,
                                RegionProtectionMod.MODID + ".region." + region.key() + "." + rule.getName());
                            value = value == TriState.DEFAULT ? region.getRule(rule) : value;

                            if (value != TriState.DEFAULT) {
                                intersecting.put(region, new PlayerRuleCacheEntry(region,
                                    new ObjectLinkedOpenHashSet<>(Arrays.stream(region.shapes().getFlatShapes()).filter(shape2 ->
                                            shape2.testDimension(context.dimension()) && shape2 instanceof BoxShape && shape2.intersects(union))
                                        .map(shape2 -> (BoxShape) shape2).iterator()),
                                    context.dimension().getValue(), new Object2ObjectRBTreeMap<>(), value, true));
                            }
                        });
                        intersectingRuleEntry.intersecting = intersecting;

                        playerEntry.byRule.put(rule, intersectingRuleEntry);
                        return intersectingRuleEntry.value;
                    }
                }
            }

            // For dimension-wide regions, or when no region matches
            if (ruleEntry.shapes.isEmpty()) {
                this.cacheHits++;
                // RegionProtectionMod.LOGGER.info("Cache hit (empty): " + rule.getName());
                return ruleEntry.value;
            }

            for (BoxShape shape : ruleEntry.shapes) {
                if (shape.testOnlyPosition(context.pos())) {
                    this.cacheHits++;
                    ruleEntry.shapes.addAndMoveToFirst(shape);
                    // RegionProtectionMod.LOGGER.info("Cache hit (shape): " + rule.getName());
                    return ruleEntry.value;
                }
            }

            // Check if there is a region with a lower level that we could use to prevent a cache miss
            while (intersectingIterator.hasNext()) {
                Map.Entry<RegionV2, PlayerRuleCacheEntry> intersectingRuleEntryEntry = intersectingIterator.next();
                PlayerRuleCacheEntry intersectingRuleEntry = intersectingRuleEntryEntry.getValue();

                boolean found = intersectingRuleEntry.shapes.isEmpty();

                if (!found) {
                    for (BoxShape shape : intersectingRuleEntry.shapes) {
                        if (shape.testOnlyPosition(context.pos())) {
                            intersectingRuleEntry.shapes.addAndMoveToFirst(shape);
                            found = true;
                        }
                    }
                }

                if (found) {
                    this.cacheSwaps++;
                    // RegionProtectionMod.LOGGER.info("Cache swap (2): " + rule.getName());

                    intersectingRuleEntry.intersecting = ruleEntry.intersecting;
                    ruleEntry.intersecting = new Object2ObjectRBTreeMap<>();
                    intersectingRuleEntry.intersecting.put(ruleEntry.region, ruleEntry);
                    intersectingRuleEntry.intersecting.remove(intersectingRuleEntry.region);

                    playerEntry.byRule.put(rule, intersectingRuleEntry);
                    return intersectingRuleEntry.value;
                }
            }

            // Cache miss, rebuild entry
            ruleEntry.shapes.clear();
        }

        this.cacheMisses++;
        // RegionProtectionMod.LOGGER.info("Cache miss: " + rule.getName());

        @Nullable
        Pair<RegionV2, TriState> resultPair = state.findRegion(context, player, rule);

        if (resultPair == null) { // No region matches
            Object2ObjectRBTreeMap<RegionV2, PlayerRuleCacheEntry> intersecting = new Object2ObjectRBTreeMap<>();

            // Add all regions in that dimension as intersecting
            state.getRegionsInDimension(context.dimension()).forEach(region -> {
                TriState value = Permissions.getPermissionValue(player,
                    RegionProtectionMod.MODID + ".region." + region.key() + "." + rule.getName());
                value = value == TriState.DEFAULT ? region.getRule(rule) : value;

                if (value != TriState.DEFAULT) {
                    intersecting.put(region, new PlayerRuleCacheEntry(region,
                        new ObjectLinkedOpenHashSet<>(Arrays.stream(region.shapes().getFlatShapes()).filter(shape ->
                                shape.testDimension(context.dimension()) && shape instanceof BoxShape)
                            .map(shape -> (BoxShape) shape).iterator()),
                        context.dimension().getValue(), new Object2ObjectRBTreeMap<>(), value, true));
                }
            });

            playerEntry.byRule.put(rule, new PlayerRuleCacheEntry(null, new ObjectLinkedOpenHashSet<>(), context.dimension().getValue(), intersecting, TriState.DEFAULT, true));
            return TriState.DEFAULT;
        } else {
            ProtectionShape[] shapes = resultPair.getFirst().shapes().getFlatShapes();
            UnionShape union = new UnionShape(shapes);
            Object2ObjectRBTreeMap<RegionV2, PlayerRuleCacheEntry> intersecting = new Object2ObjectRBTreeMap<>();

            state.findIntersectingRegions(context.dimension(), union).filter(region -> region != resultPair.getFirst()).forEach(region -> {
                TriState value = Permissions.getPermissionValue(player,
                    RegionProtectionMod.MODID + ".region." + region.key() + "." + rule.getName());
                value = value == TriState.DEFAULT ? region.getRule(rule) : value;

                if (value != TriState.DEFAULT) {
                    intersecting.put(region, new PlayerRuleCacheEntry(region,
                        new ObjectLinkedOpenHashSet<>(Arrays.stream(region.shapes().getFlatShapes()).filter(shape ->
                                shape.testDimension(context.dimension()) && shape instanceof BoxShape && shape.intersects(union))
                            .map(shape -> (BoxShape) shape).iterator()),
                        context.dimension().getValue(), new Object2ObjectRBTreeMap<>(), value, true));
                }
            });

            playerEntry.byRule.put(rule, new PlayerRuleCacheEntry(resultPair.getFirst(), new ObjectLinkedOpenHashSet<>(Arrays.stream(shapes).filter(shape ->
                    shape.testDimension(context.dimension()) && shape instanceof BoxShape)
                .map(shape -> (BoxShape) shape).iterator()), context.dimension().getValue(), intersecting, resultPair.getSecond(), true));
            return resultPair.getSecond();
        }
    }

    public void clear() {
        this.byPlayer.clear();

        this.cacheHits = 0;
        this.cacheSwaps = 0;
        this.cacheMisses = 0;
    }

    public void clearForPlayer(UUID uuid) {
        this.byPlayer.remove(uuid);
    }

    public int getCacheHits() {
        return this.cacheHits;
    }

    public int getCacheSwaps() {
        return this.cacheSwaps;
    }

    public int getCacheMisses() {
        return this.cacheMisses;
    }

    private record PlayerCacheEntry(Reference2ObjectOpenHashMap<ProtectionRule, PlayerRuleCacheEntry> byRule) {
    }

    private static final class PlayerRuleCacheEntry {
        @Nullable
        private final RegionV2 region;
        private final ObjectLinkedOpenHashSet<BoxShape> shapes;
        private final Identifier dimension;
        private Object2ObjectRBTreeMap<RegionV2, PlayerRuleCacheEntry> intersecting;
        private final TriState value;
        private final boolean active;

        private PlayerRuleCacheEntry(@Nullable RegionV2 region, ObjectLinkedOpenHashSet<BoxShape> shapes, Identifier dimension, Object2ObjectRBTreeMap<RegionV2, PlayerRuleCacheEntry> intersecting, TriState value, boolean active) {
            this.region = region;
            this.shapes = shapes;
            this.dimension = dimension;
            this.intersecting = intersecting;
            this.value = value;
            this.active = active;
        }
    }
}
