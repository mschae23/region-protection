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
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Redirect(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;shouldDamagePlayer(Lnet/minecraft/entity/player/PlayerEntity;)Z"))
    private boolean redirectIsPvpEnabled(ServerPlayerEntity player, PlayerEntity attacker) {
        if (!RegionProtectionMod.getConfig().enabled()) {
            return player.shouldDamagePlayer(attacker);
        }

        ActionResult result = RegionRuleEnforcer.onPlayerPvp(player, player.getEntityPos());

        if (result == ActionResult.FAIL) {
            return false;
        } else if (attacker instanceof ServerPlayerEntity serverAttacker) {
            result = RegionRuleEnforcer.onPlayerPvpSendDenied(serverAttacker, attacker.getEntityPos());

            if (result == ActionResult.FAIL) {
                return false;
            }
        }

        return player.shouldDamagePlayer(attacker);
    }
}
