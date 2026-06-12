/*
 * Copyright (C) 2026  mschae23
 *
 * This file is part of Region protection.
 *
 * Region protection is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.mschae23.regionprotection.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import de.mschae23.regionprotection.RegionProtectionMod;
import de.mschae23.regionprotection.region.RegionRuleEnforcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "shouldDamagePlayer", at = @At("HEAD"))
    private void injectIsPvpEnabled(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (!RegionProtectionMod.getConfig().enabled()) {
            return;
        }

        ServerPlayerEntity attacker = (ServerPlayerEntity) (Object) this;

        ActionResult attackerResult = RegionRuleEnforcer.onPlayerPvp(attacker, attacker.getEntityPos());

        if (attackerResult == ActionResult.FAIL) {
            RegionRuleEnforcer.sendDeniedText(attacker);
            cir.setReturnValue(false);
            return;
        } else if (player instanceof ServerPlayerEntity serverPlayer) {
            ActionResult attackedResult = RegionRuleEnforcer.onPlayerPvp(serverPlayer, player.getEntityPos());

            if (attackedResult == ActionResult.FAIL) {
                RegionRuleEnforcer.sendDeniedText(attacker);
                cir.setReturnValue(false);
                return;
            }

            if (attackerResult == ActionResult.SUCCESS && attackedResult == ActionResult.SUCCESS) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
