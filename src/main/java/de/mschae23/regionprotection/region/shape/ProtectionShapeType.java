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

import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.mojang.serialization.MapCodec;
import de.mschae23.regionprotection.RegionProtectionMod;
import de.mschae23.regionprotection.registry.RegionProtectionRegistries;

public interface ProtectionShapeType<S extends ProtectionShape> {
    ProtectionShapeType<UniversalShape> UNIVERSAL = register("universal", UniversalShape.CODEC);
    ProtectionShapeType<DimensionShape> DIMENSION = register("dimension", DimensionShape.CODEC);
    ProtectionShapeType<BoxShape> BOX = register("box", BoxShape.CODEC);
    ProtectionShapeType<UnionShape> UNION = register("union", UnionShape.CODEC);

    MapCodec<S> codec();

    static <S extends ProtectionShape> ProtectionShapeType<S> register(String id, MapCodec<S> codec) {
        return Registry.register(RegionProtectionRegistries.PROTECTION_SHAPE, RegionProtectionMod.id(id), () -> codec);
    }

    static void init() {
    }
}
