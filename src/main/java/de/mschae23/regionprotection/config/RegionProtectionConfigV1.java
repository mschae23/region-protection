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

package de.mschae23.regionprotection.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import de.mschae23.config.api.ModConfig;

public record RegionProtectionConfigV1(boolean enabled) implements ModConfig<RegionProtectionConfigV1> {
    public static final MapCodec<RegionProtectionConfigV1> TYPE_CODEC = Codec.BOOL.fieldOf("enabled").xmap(RegionProtectionConfigV1::new, RegionProtectionConfigV1::enabled);

    public static final ModConfig.Type<RegionProtectionConfigV1, RegionProtectionConfigV1> TYPE = new ModConfig.Type<>(1, TYPE_CODEC);

    public static final RegionProtectionConfigV1 DEFAULT =
        new RegionProtectionConfigV1(true);

    @Override
    public ModConfig.Type<RegionProtectionConfigV1, ?> type() {
        return TYPE;
    }

    @Override
    public RegionProtectionConfigV1 latest() {
        return this;
    }

    @Override
    public boolean shouldUpdate() {
        return true;
    }
}
