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

package de.mschae23.regionprotection;

import java.util.function.Supplier;
import net.fabricmc.fabric.api.util.TriState;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.util.Tristate;

public final class ModUtils {
    private static LuckPerms luckPerms = null;

    private ModUtils() {
    }

    // General utils

    public static TriState toFabricTriState(Tristate state) {
        return switch (state) {
            case TRUE -> TriState.TRUE;
            case FALSE -> TriState.FALSE;
            case UNDEFINED -> TriState.DEFAULT;
        };
    }

    public static TriState orTriState(TriState state, Supplier<TriState> other) {
        return state == TriState.DEFAULT ? other.get() : state;
    }

    public static LuckPerms getLuckPerms() {
        if (luckPerms == null) {
            luckPerms = LuckPermsProvider.get();
        }

        return luckPerms;
    }
}
