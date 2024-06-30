package de.mschae23.regionprotection.region.shape;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class DimensionShape implements ProtectionShape {
    public static final MapCodec<DimensionShape> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.xmap(id -> RegistryKey.of(RegistryKeys.WORLD, id), RegistryKey::getValue).fieldOf("dimension").forGetter(scope -> scope.dimension)
    ).apply(instance, DimensionShape::new));

    private final RegistryKey<World> dimension;

    public DimensionShape(RegistryKey<World> dimension) {
        this.dimension = dimension;
    }

    @Override
    public ProtectionShapeType<?> getType() {
        return ProtectionShapeType.DIMENSION;
    }

    @Override
    public boolean test(ProtectionContext context) {
        return this.testDimension(context.dimension());
    }

    @Override
    public boolean testDimension(RegistryKey<World> dimension) {
        return this.dimension.getValue().equals(dimension.getValue());
    }

    public boolean intersects(ProtectionShape other) {
        return other.testDimension(this.dimension);
    }

    @Override
    public MutableText display() {
        return Text.literal(this.dimension.getValue().toString()).formatted(Formatting.YELLOW);
    }

    @Override
    public MutableText displayShort() {
        return this.display();
    }
}
