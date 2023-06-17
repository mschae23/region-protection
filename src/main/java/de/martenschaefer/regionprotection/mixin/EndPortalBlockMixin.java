package de.martenschaefer.regionprotection.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.RegionRuleEnforcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
    @Inject(method = "onEntityCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getRegistryKey()Lnet/minecraft/registry/RegistryKey;", ordinal = 0), cancellable = true)
    private void onTeleport(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (RegionProtectionMod.getConfig().enabled() && entity instanceof ServerPlayerEntity player) {
            ActionResult result = RegionRuleEnforcer.onEndPortalUse(player, player.getPos());

            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }
}
