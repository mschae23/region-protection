package de.martenschaefer.regionprotection.region;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.util.TriState;
import de.martenschaefer.regionprotection.ModUtils;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.shape.ProtectionContext;
import de.martenschaefer.regionprotection.region.shape.UnionShape;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import net.luckperms.api.cacheddata.CachedPermissionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SortedRegionHashMap implements RegionMap {
    private final ObjectSortedSet<RegionV2> regions = new ObjectRBTreeSet<>();
    private final Object2ObjectMap<String, RegionV2> byKey = new Object2ObjectOpenHashMap<>();

    @Override
    public void clear() {
        this.regions.clear();
        this.byKey.clear();
    }

    @Override
    public boolean add(RegionV2 region) {
        if (this.byKey.put(region.key(), region) == null) {
            this.regions.add(region);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(RegionV2 from, RegionV2 to) {
        if (from.key().equals(to.key()) && this.byKey.replace(from.key(), from, to)) {
            this.regions.remove(from);
            this.regions.add(to);
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public RegionV2 remove(String key) {
        var authority = this.byKey.remove(key);
        if (authority != null) {
            this.regions.remove(authority);
            return authority;
        }
        return null;
    }

    @Override
    @Nullable
    public RegionV2 byKey(String key) {
        return this.byKey.get(key);
    }

    @Override
    public boolean contains(String key) {
        return this.byKey.containsKey(key);
    }

    @Override
    public Set<String> keySet() {
        return this.byKey.keySet();
    }

    @Override
    public Iterable<Object2ObjectMap.Entry<String, RegionV2>> entries() {
        return Object2ObjectMaps.fastIterable(this.byKey);
    }

    @Override
    public Stream<RegionV2> findRegion(ProtectionContext context) {
        return this.regions.stream().filter(region -> region.shapes().test(context));
    }

    @Override
    @Nullable
    public Pair<RegionV2, TriState> findRegion(ProtectionContext context, ServerPlayerEntity player, ProtectionRule rule) {
        CachedPermissionData permissions = ModUtils.getLuckPerms().getPlayerAdapter(ServerPlayerEntity.class).getPermissionData(player);
        String prefix = RegionProtectionMod.MODID + ".region.";
        String suffix = "." + rule.getName();

        for (RegionV2 region : this.regions) {
            if (region.shapes().test(context)) {
                TriState result = ModUtils.toFabricTriState(permissions.checkPermission(prefix + region.key() + suffix));
                result = result == TriState.DEFAULT ? region.getRule(rule) : result;

                if (result != TriState.DEFAULT) {
                    return Pair.of(region, result);
                }
            }
        }

        return null;
    }

    public Stream<RegionV2> findIntersectingRegions(UnionShape shape) {
        return this.regions.stream().filter(region -> region.shapes().intersects(shape));
    }

    @Override
    public TriState checkRegionGeneric(ProtectionContext context, ProtectionRule rule) {
        for (RegionV2 region : this.regions) {
            if (region.shapes().test(context)) {
                TriState result = region.getRule(rule);

                if (result != TriState.DEFAULT) {
                    return result;
                }
            }
        }

        return TriState.DEFAULT;
    }

    @Override
    public int size() {
        return this.regions.size();
    }

    @Override
    public boolean isEmpty() {
        return this.regions.isEmpty();
    }

    @Override
    @NotNull
    public Iterator<RegionV2> iterator() {
        return this.regions.iterator();
    }

    @Override
    public Stream<RegionV2> stream() {
        return this.regions.stream();
    }
}
