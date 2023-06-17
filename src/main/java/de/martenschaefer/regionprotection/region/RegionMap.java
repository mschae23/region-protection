package de.martenschaefer.regionprotection.region;

import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.util.TriState;
import de.martenschaefer.regionprotection.region.shape.ProtectionContext;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jetbrains.annotations.Nullable;

public interface RegionMap extends Iterable<RegionV2> {
    void clear();

    boolean add(RegionV2 authority);

    boolean replace(RegionV2 from, RegionV2 to);

    @Nullable
    RegionV2 remove(String key);

    @Nullable
    RegionV2 byKey(String key);

    boolean contains(String key);

    Set<String> keySet();

    int size();

    default boolean isEmpty() {
        return this.size() == 0;
    }

    Iterable<Object2ObjectMap.Entry<String, RegionV2>> entries();

    Stream<RegionV2> findRegion(ProtectionContext context);

    TriState checkRegion(ProtectionContext context, ServerPlayerEntity player, ProtectionRule rule);

    TriState checkRegionGeneric(ProtectionContext context, ProtectionRule rule);

    default Stream<RegionV2> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

}
