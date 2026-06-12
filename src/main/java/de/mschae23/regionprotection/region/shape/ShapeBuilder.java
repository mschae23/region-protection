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

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public interface ShapeBuilder {
    @Nullable
    static ShapeBuilder start(ServerPlayerEntity player) {
        var builder = (ShapeBuilder) player;
        if (!builder.isBuilding()) {
            builder.start();
            return builder;
        } else {
            return null;
        }
    }

    @Nullable
    static ShapeBuilder from(ServerPlayerEntity player) {
        var builder = (ShapeBuilder) player;
        if (builder.isBuilding()) {
            return builder;
        }
        return null;
    }

    void start();

    void add(ProtectionShape shape);

    ProtectionShape finish();

    boolean isBuilding();
}
