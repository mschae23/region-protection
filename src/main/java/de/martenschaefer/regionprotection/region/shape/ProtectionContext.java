package de.martenschaefer.regionprotection.region.shape;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public record ProtectionContext(RegistryKey<World> dimension, Vec3d pos) {
}
