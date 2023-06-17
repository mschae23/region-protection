package de.martenschaefer.regionprotection.region.shape;

import com.mojang.serialization.Codec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import de.martenschaefer.regionprotection.registry.RegionProtectionRegistries;

public interface ProtectionShape {
    Codec<ProtectionShape> CODEC = RegionProtectionRegistries.PROTECTION_SHAPE.getCodec().dispatch(ProtectionShape::getType, ProtectionShapeType::codec);

    ProtectionShapeType<?> getType();

    boolean test(ProtectionContext context);

    boolean testDimension(RegistryKey<World> dimension);

    MutableText display();

    MutableText displayShort();

    default ProtectionShape union(ProtectionShape other) {
        return union(this, other);
    }

    static ProtectionShape universe() {
        return UniversalShape.INSTANCE;
    }

    static ProtectionShape dimension(RegistryKey<World> dimension) {
        return new DimensionShape(dimension);
    }

    static ProtectionShape box(RegistryKey<World> dimension, BlockPos a, BlockPos b) {
        BlockPos min = new BlockPos(
            Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ())
        );

        BlockPos max = new BlockPos(
            Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()),
            Math.max(a.getZ(), b.getZ())
        );

        return new BoxShape(dimension, min, max);
    }

    static ProtectionShape union(ProtectionShape... scopes) {
        return new UnionShape(scopes);
    }
}
