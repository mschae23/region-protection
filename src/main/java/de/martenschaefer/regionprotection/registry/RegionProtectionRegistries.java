package de.martenschaefer.regionprotection.registry;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.shape.ProtectionShapeType;

public class RegionProtectionRegistries {
    public static final Registry<ProtectionShapeType<?>> PROTECTION_SHAPE = FabricRegistryBuilder.<ProtectionShapeType<?>>createSimple(RegistryKey.ofRegistry(RegionProtectionMod.id("protection_shape"))).buildAndRegister();

    private RegionProtectionRegistries() {
    }

    public static void init() {
    }
}
