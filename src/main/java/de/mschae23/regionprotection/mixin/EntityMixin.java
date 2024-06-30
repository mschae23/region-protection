package de.mschae23.regionprotection.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.mschae23.regionprotection.RegionProtectionMod;
import de.mschae23.regionprotection.region.RegionRuleEnforcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {
    @WrapOperation(method = "tickPortalTeleportation", at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.isWorldAllowed (Lnet/minecraft/world/World;)Z"))
    private boolean onIsNetherAllowed(MinecraftServer server, World targetWorld, Operation<Boolean> original) {
        Entity self = (Entity) (Object) this;

        if (RegionProtectionMod.getConfig().enabled() && targetWorld.getRegistryKey() == World.NETHER && self instanceof ServerPlayerEntity player) {
            ActionResult result = RegionRuleEnforcer.onNetherPortalUse(player, player.getPos());

            if (result == ActionResult.FAIL) {
                return false;
            }
        }

        return original.call(server, targetWorld);
    }
}
