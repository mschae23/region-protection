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

package de.mschae23.regionprotection.region.shape;

import java.util.stream.Stream;
import com.mojang.serialization.Codec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import de.mschae23.regionprotection.registry.RegionProtectionRegistries;

public sealed interface ProtectionShape permits BoxShape, DimensionShape, UnionShape, UniversalShape {
    Codec<ProtectionShape> CODEC = RegionProtectionRegistries.PROTECTION_SHAPE.getCodec().dispatch(ProtectionShape::getType, ProtectionShapeType::codec);

    ProtectionShapeType<?> getType();

    boolean test(ProtectionContext context);

    boolean testDimension(RegistryKey<World> dimension);

    boolean intersects(ProtectionShape other);

    MutableText display();

    MutableText displayShort();

    default Stream<ProtectionShape> flatStream() {
        return Stream.of(this);
    }

    default ProtectionShape union(ProtectionShape other) {
        return union(this, other);
    }

    static ProtectionShape universe() {
        return UniversalShape.INSTANCE;
    }

    static ProtectionShape dimension(RegistryKey<World> dimension) {
        return new DimensionShape(dimension);
    }

    static ProtectionShape box(RegistryKey<World> dimension, BlockPos a, BlockPos b) {
        BlockPos min = new BlockPos(
            Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ())
        );

        BlockPos max = new BlockPos(
            Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()),
            Math.max(a.getZ(), b.getZ())
        );

        return new BoxShape(dimension, min, max);
    }

    static ProtectionShape union(ProtectionShape... scopes) {
        return new UnionShape(scopes);
    }
}
