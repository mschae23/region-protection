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

package de.mschae23.regionprotection.command.argument;

import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.fabricmc.fabric.api.util.TriState;
import de.mschae23.regionprotection.util.StringTriState;
import com.mojang.brigadier.context.CommandContext;

@Deprecated
public class TriStateArgumentType extends EnumArgumentType<StringTriState> {
    private TriStateArgumentType() {
        super(StringTriState.CODEC, StringTriState::values);
    }

    public static TriStateArgumentType triState() {
        return new TriStateArgumentType();
    }

    public static TriState getTriState(CommandContext<ServerCommandSource> context, String id) {
        return context.getArgument(id, StringTriState.class).getState();
    }
}
