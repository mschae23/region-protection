package de.martenschaefer.regionprotection.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.RegionRuleEnforcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Redirect(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;shouldDamagePlayer(Lnet/minecraft/entity/player/PlayerEntity;)Z"))
    private boolean redirectIsPvpEnabled(ServerPlayerEntity player, PlayerEntity attacker) {
        if (!RegionProtectionMod.getConfig().enabled()) {
            return player.shouldDamagePlayer(attacker);
        }

        ActionResult result = RegionRuleEnforcer.onPlayerPvp(player, player.getPos());

        if (result == ActionResult.FAIL) {
            return false;
        } else if (attacker instanceof ServerPlayerEntity serverAttacker) {
            result = RegionRuleEnforcer.onPlayerPvpSendDenied(serverAttacker, attacker.getPos());

            if (result == ActionResult.FAIL) {
                return false;
            }
        }

        return player.shouldDamagePlayer(attacker);
    }
}
