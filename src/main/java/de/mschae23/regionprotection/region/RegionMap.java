/*
 * Copyright (C) 2026  mschae23
 *
 * This file is part of Region protection.
 *
 * Region protection is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.mschae23.regionprotection.region;

import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.util.TriState;
import de.mschae23.regionprotection.region.shape.ProtectionContext;
import com.mojang.datafixers.util.Pair;
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

    Pair<RegionV2, TriState> findRegion(ProtectionContext context, ServerPlayerEntity player, ProtectionRule rule);

    TriState checkRegionGeneric(ProtectionContext context, ProtectionRule rule);

    default Stream<RegionV2> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }
}
