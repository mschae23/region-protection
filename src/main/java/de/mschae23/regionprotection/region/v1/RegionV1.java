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

package de.mschae23.regionprotection.region.v1;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.mschae23.config.api.ModConfig;
import de.mschae23.regionprotection.region.RegionV2;
import de.mschae23.regionprotection.region.shape.RegionShapes;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

public record RegionV1(String key, int level, RegionShapes shapes) implements ModConfig<RegionV2> {
    public static final MapCodec<RegionV1> OLD_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("key").forGetter(region -> region.key),
        Codec.INT.fieldOf("level").forGetter(region -> region.level),
        RegionShapes.CODEC.fieldOf("shapes").forGetter(region -> region.shapes)
    ).apply(instance, RegionV1::new));

    public static final ModConfig.Type<RegionV2, RegionV1> TYPE = new ModConfig.Type<>(1, OLD_CODEC);

    @Override
    public Type<RegionV2, ?> type() {
        return TYPE;
    }

    @Override
    public RegionV2 latest() {
        return new RegionV2(this.key, this.level, this.shapes, new Reference2ReferenceOpenHashMap<>());
    }

    @Override
    public boolean shouldUpdate() {
        return true;
    }
}
