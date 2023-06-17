package de.martenschaefer.regionprotection.region.shape;

import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import de.martenschaefer.regionprotection.registry.RegionProtectionRegistries;
import com.mojang.serialization.Codec;

public interface ProtectionShapeType<S extends ProtectionShape> {
    ProtectionShapeType<UniversalShape> UNIVERSAL = register("universal", UniversalShape.CODEC);
    ProtectionShapeType<DimensionShape> DIMENSION = register("dimension", DimensionShape.CODEC);
    ProtectionShapeType<BoxShape> BOX = register("box", BoxShape.CODEC);
    ProtectionShapeType<UnionShape> UNION = register("union", UnionShape.CODEC);

    Codec<S> codec();

    static <S extends ProtectionShape> ProtectionShapeType<S> register(String id, Codec<S> codec) {
        // Use "serverutils" namespace for compatibility with old region data
        return Registry.register(RegionProtectionRegistries.PROTECTION_SHAPE, new Identifier("serverutils", id), () -> codec);
    }

    static void init() {
    }
}
