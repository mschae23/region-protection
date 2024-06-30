package de.mschae23.regionprotection.region.shape;

import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.mojang.serialization.MapCodec;
import de.mschae23.regionprotection.registry.RegionProtectionRegistries;

public interface ProtectionShapeType<S extends ProtectionShape> {
    ProtectionShapeType<UniversalShape> UNIVERSAL = register("universal", UniversalShape.CODEC);
    ProtectionShapeType<DimensionShape> DIMENSION = register("dimension", DimensionShape.CODEC);
    ProtectionShapeType<BoxShape> BOX = register("box", BoxShape.CODEC);
    ProtectionShapeType<UnionShape> UNION = register("union", UnionShape.CODEC);

    MapCodec<S> codec();

    static <S extends ProtectionShape> ProtectionShapeType<S> register(String id, MapCodec<S> codec) {
        // Use "serverutils" namespace for compatibility with old region data
        return Registry.register(RegionProtectionRegistries.PROTECTION_SHAPE, Identifier.of("serverutils", id), () -> codec);
    }

    static void init() {
    }
}
