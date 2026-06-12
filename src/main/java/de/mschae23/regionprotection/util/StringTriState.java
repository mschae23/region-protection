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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;
import net.fabricmc.fabric.api.util.TriState;
import org.jetbrains.annotations.Nullable;

public enum StringTriState implements StringIdentifiable {
    FALSE("false", TriState.FALSE, Formatting.RED),
    DEFAULT("default", TriState.DEFAULT, Formatting.GRAY),
    TRUE("true", TriState.TRUE, Formatting.GREEN);

    public static final com.mojang.serialization.Codec<StringTriState> CODEC = StringIdentifiable.createCodec(StringTriState::values);
    private static final Map<String, StringTriState> BY_NAME_ORIGINAL = Arrays.stream(values()).collect(Collectors.toMap(StringTriState::asString, Function.identity()));
    private static final Map<String, TriState> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(StringTriState::asString, StringTriState::getState));

    private final String name;
    private final TriState state;
    private final Formatting formatting;

    StringTriState(String name, TriState state, Formatting formatting) {
        this.name = name;
        this.state = state;
        this.formatting = formatting;
    }

    public TriState getState() {
        return this.state;
    }

    public Formatting getFormatting() {
        return this.formatting;
    }

    @Override
    public String asString() {
        return this.name;
    }

    @Nullable
    public static TriState byName(String name) {
        return BY_NAME.get(name);
    }

    @Nullable
    public static StringTriState byNameString(String name) {
        return BY_NAME_ORIGINAL.get(name);
    }

    public static StringTriState from(TriState state) {
        return switch (state) {
            case FALSE -> FALSE;
            case DEFAULT -> DEFAULT;
            case TRUE -> TRUE;
        };
    }
}
