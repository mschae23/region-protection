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

package de.mschae23.regionprotection.util;

import net.fabricmc.fabric.api.util.TriState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public final class RegionProtectionCodecs {
    public static final Codec<TriState> COMPACT_TRISTATE_CODEC = Codec.intRange(0, 2).comapFlatMap(
        value -> switch (value) {
            case 0 -> DataResult.success(TriState.DEFAULT);
            case 1 -> DataResult.success(TriState.FALSE);
            case 2 -> DataResult.success(TriState.TRUE);
            default -> DataResult.error(() -> "Invalid int value for TriState: " + value);
        }, state -> switch (state) {
            case DEFAULT -> 0;
            case FALSE -> 1;
            case TRUE -> 2;
        }
    );

    private RegionProtectionCodecs() {
    }
}
