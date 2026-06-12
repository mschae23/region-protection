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

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
    @WrapOperation(method = "tickPortalTeleportation", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;isEnterableWithPortal(Lnet/minecraft/world/World;)Z"))
    private boolean onIsNetherAllowed(ServerWorld world, World targetWorld, Operation<Boolean> original) {
        Entity self = (Entity) (Object) this;

        if (RegionProtectionMod.getConfig().enabled() && targetWorld.getRegistryKey() == World.NETHER && self instanceof ServerPlayerEntity player) {
            ActionResult result = RegionRuleEnforcer.onNetherPortalUse(player, player.getEntityPos());

            if (result == ActionResult.FAIL) {
                return false;
            }
        }

        return original.call(world, targetWorld);
    }
}
