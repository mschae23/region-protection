package de.martenschaefer.regionprotection.region.shape;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class BoxShape implements ProtectionShape {
    public static final Codec<BoxShape> CODEC = RecordCodecBuilder.create(instance -> instance.group(
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
            } else if (other instanceof BoxShape box) {
                return this.min.getX() <= box.max.getX() && this.max.getX() >= box.min.getX()
                    && this.min.getY() <= box.max.getY() && this.max.getY() >= box.min.getY()
                    && this.min.getZ() <= box.max.getZ() && this.max.getZ() >= box.min.getZ();
            } else {
                throw new IllegalArgumentException("Unknown other protection shape (from BoxShape)");
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
