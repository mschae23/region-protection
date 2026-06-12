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

package de.mschae23.regionprotection.mixin.shape;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.network.ServerPlayerEntity;
import de.mschae23.regionprotection.region.shape.ProtectionShape;
import de.mschae23.regionprotection.region.shape.ShapeBuilder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements ShapeBuilder {
    private List<ProtectionShape> shapes;

    @Override
    public void start() {
        this.shapes = new ArrayList<>();
    }

    @Override
    public void add(ProtectionShape shape) {
        this.shapes.add(shape);
    }

    @Override
    public ProtectionShape finish() {
        ProtectionShape[] shapes = this.shapes.toArray(new ProtectionShape[0]);
        this.shapes = null;
        return ProtectionShape.union(shapes);
    }

    @Override
    public boolean isBuilding() {
        return this.shapes != null;
    }
}
