package de.martenschaefer.regionprotection.region;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.util.TriState;
import de.martenschaefer.regionprotection.region.shape.ProtectionContext;
import de.martenschaefer.regionprotection.region.shape.UnionShape;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class IndexedRegionMap implements RegionMap {
    private final RegionMap main = new SortedRegionHashMap();
    private final Reference2ObjectMap<RegistryKey<World>, SortedRegionHashMap> byDimension = new Reference2ObjectOpenHashMap<>();

    public void addDimension(RegistryKey<World> dimension) {
        SortedRegionHashMap map = new SortedRegionHashMap();

        for (RegionV2 region : this.main) {
            if (region.shapes().testDimension(dimension)) {
                map.add(region);
            }
        }

        this.byDimension.put(dimension, map);
    }

    public void removeDimension(RegistryKey<World> dimension) {
        this.byDimension.remove(dimension);
    }

    public Stream<RegionV2> findRegion(ProtectionContext context) {
        SortedRegionHashMap map = this.byDimension.get(context.dimension());

        if (map != null) {
            return map.findRegion(context);
        }

        return Stream.empty();
    }

    @Override
    @Nullable
    public Pair<RegionV2, TriState> findRegion(ProtectionContext context, ServerPlayerEntity player, ProtectionRule rule) {
        SortedRegionHashMap map = this.byDimension.get(context.dimension());

        if (map != null) {
            return map.findRegion(context, player, rule);
        } else {
            return null;
        }
    }

    public Stream<RegionV2> findIntersectingRegions(RegistryKey<World> dimension, UnionShape shape) {
        SortedRegionHashMap map = this.byDimension.get(dimension);

        if (map != null) {
            return map.findIntersectingRegions(shape);
        } else {
            return Stream.empty();
        }
    }

    public Stream<RegionV2> getRegionsInDimension(RegistryKey<World> dimension) {
        SortedRegionHashMap map = this.byDimension.get(dimension);

        if (map != null) {
            return map.stream();
        } else {
            return Stream.empty();
        }
    }

    @Override
    public TriState checkRegionGeneric(ProtectionContext context, ProtectionRule rule) {
        SortedRegionHashMap map = this.byDimension.get(context.dimension());

        if (map != null) {
            return map.checkRegionGeneric(context, rule);
        } else {
            return TriState.DEFAULT;
        }
    }

    @Override
    public void clear() {
        this.main.clear();
        this.byDimension.clear();
    }

    @Override
    public boolean add(RegionV2 authority) {
        if (this.main.add(authority)) {
            this.addToDimension(authority);
            return true;
        }

        return false;
    }

    @Override
    public boolean replace(RegionV2 from, RegionV2 to) {
        if (this.main.replace(from, to)) {
            this.replaceInDimension(from, to);
            return true;
        }

        return false;
    }

    @Override
    @Nullable
    public RegionV2 remove(String key) {
        var region = this.main.remove(key);

        if (region != null) {
            this.removeFromDimension(key);
            return region;
        }

        return null;
    }

    @Override
    @Nullable
    public RegionV2 byKey(String key) {
        return this.main.byKey(key);
    }

    @Override
    public boolean contains(String key) {
        return this.main.contains(key);
    }

    @Override
    public Set<String> keySet() {
        return this.main.keySet();
    }

    @Override
    public int size() {
        return this.main.size();
    }

    @Override
    public Iterable<Object2ObjectMap.Entry<String, RegionV2>> entries() {
        return this.main.entries();
    }

    @NotNull
    @Override
    public Iterator<RegionV2> iterator() {
        return this.main.iterator();
    }

    @Override
    public Stream<RegionV2> stream() {
        return this.main.stream();
    }

    private void addToDimension(RegionV2 region) {
        for (Reference2ObjectMap.Entry<RegistryKey<World>, SortedRegionHashMap> entry : Reference2ObjectMaps.fastIterable(this.byDimension)) {
            RegistryKey<World> dimension = entry.getKey();

            if (region.shapes().testDimension(dimension)) {
                SortedRegionHashMap map = entry.getValue();
                map.add(region);
            }
        }
    }

    private void replaceInDimension(RegionV2 from, RegionV2 to) {
        for (Reference2ObjectMap.Entry<RegistryKey<World>, SortedRegionHashMap> entry : Reference2ObjectMaps.fastIterable(this.byDimension)) {
            boolean fromIncluded = from.shapes().testDimension(entry.getKey());
            boolean toIncluded = to.shapes().testDimension(entry.getKey());

            if (fromIncluded && toIncluded) {
                entry.getValue().replace(from, to);
            } else if (fromIncluded) {
                entry.getValue().remove(from.key());
            } else if (toIncluded) {
                entry.getValue().add(to);
            }
        }
    }

    private void removeFromDimension(String key) {
        for (SortedRegionHashMap map : this.byDimension.values()) {
            map.remove(key);
        }
    }
}
