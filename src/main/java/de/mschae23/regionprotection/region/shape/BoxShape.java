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

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class BoxShape implements ProtectionShape {
    public static final MapCodec<BoxShape> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.xmap(id -> RegistryKey.of(RegistryKeys.WORLD, id), RegistryKey::getValue).fieldOf("dimension").forGetter(scope -> scope.dimension),
            BlockPos.CODEC.fieldOf("min").forGetter(scope -> scope.min),
            BlockPos.CODEC.fieldOf("max").forGetter(scope -> scope.max)
    ).apply(instance, BoxShape::new));

    private final RegistryKey<World> dimension;
    private final BlockPos min;
    private final BlockPos max;

    public BoxShape(RegistryKey<World> dimension, BlockPos min, BlockPos max) {
        this.dimension = dimension;
        this.min = min;
        this.max = max;
    }

    @Override
    public ProtectionShapeType<?> getType() {
        return ProtectionShapeType.BOX;
    }

    @Override
    public boolean test(ProtectionContext context) {
        return context.dimension().getValue().equals(this.dimension.getValue())
            && context.pos().x >= this.min.getX() && context.pos().x < (this.max.getX() + 1)
            && context.pos().y >= this.min.getY() && context.pos().y < (this.max.getY() + 1)
            && context.pos().z >= this.min.getZ() && context.pos().z < (this.max.getZ() + 1);
    }

    @Override
    public boolean testDimension(RegistryKey<World> dimension) {
        return dimension.getValue().equals(this.dimension.getValue());
    }

    public boolean testOnlyPosition(Vec3d pos) {
        return pos.x >= this.min.getX() && pos.x < (this.max.getX() + 1)
            && pos.y >= this.min.getY() && pos.y < (this.max.getY() + 1)
            && pos.z >= this.min.getZ() && pos.z < (this.max.getZ() + 1);
    }

    @Override
    public boolean intersects(ProtectionShape other) {
        if (other.testDimension(this.dimension)) {
            if (other instanceof UnionShape union) {
                for (ProtectionShape shape : union.getScopes()) {
                    if (this.intersects(shape)) {
                        return true;
                    }
                }

                return false;
            } else if (other instanceof UniversalShape || other instanceof DimensionShape) {
                return true;
            } else if (other instanceof BoxShape box) {
                return this.min.getX() <= box.max.getX() && this.max.getX() >= box.min.getX()
                    && this.min.getY() <= box.max.getY() && this.max.getY() >= box.min.getY()
                    && this.min.getZ() <= box.max.getZ() && this.max.getZ() >= box.min.getZ();
            } else {
                throw new IllegalArgumentException("Unknown other protection shape (from BoxShape): " + other);
            }
        } else {
            return false;
        }
    }

    @Override
    public MutableText display() {
        return Text.literal("[")
            .append(this.displayPos(this.min).formatted(Formatting.AQUA))
            .append("; ")
            .append(this.displayPos(this.max).formatted(Formatting.AQUA))
            .append("] in ")
            .append(Text.literal(this.dimension.getValue().toString()).formatted(Formatting.YELLOW))
            .formatted(Formatting.GRAY);
    }

    @Override
    public MutableText displayShort() {
        return this.display();
    }

    private MutableText displayPos(BlockPos pos) {
        return Text.literal("(" + pos.getX() + "; " + pos.getY() + "; " + pos.getZ() + ")");
    }
}
