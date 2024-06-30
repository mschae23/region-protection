package de.mschae23.regionprotection.region.shape;

import net.minecraft.registry.RegistryKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import com.mojang.serialization.MapCodec;

public final class UniversalShape implements ProtectionShape {
    public static final UniversalShape INSTANCE = new UniversalShape();

    public static MapCodec<UniversalShape> CODEC = MapCodec.unit(INSTANCE);

    private UniversalShape() {
    }

    @Override
    public ProtectionShapeType<?> getType() {
        return ProtectionShapeType.UNIVERSAL;
    }

    @Override
    public boolean test(ProtectionContext context) {
        return true;
    }

    @Override
    public boolean testDimension(RegistryKey<World> dimension) {
        return true;
    }

    @Override
    public boolean intersects(ProtectionShape other) {
        return true;
    }

    @Override
    public MutableText display() {
        return Text.literal("Universal").formatted(Formatting.YELLOW);
    }

    @Override
    public MutableText displayShort() {
        return this.display();
    }
}
