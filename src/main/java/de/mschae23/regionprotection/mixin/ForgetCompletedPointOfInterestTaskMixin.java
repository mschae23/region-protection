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

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryQueryResult;
import net.minecraft.entity.ai.brain.task.ForgetCompletedPointOfInterestTask;
import net.minecraft.entity.ai.brain.task.TaskTriggerer;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import de.mschae23.regionprotection.RegionProtectionMod;
import de.mschae23.regionprotection.region.RegionRuleEnforcer;
import com.mojang.datafixers.kinds.IdF;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ForgetCompletedPointOfInterestTask.class)
public abstract class ForgetCompletedPointOfInterestTaskMixin {
    @Redirect(method = "method_47187", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/brain/task/ForgetCompletedPointOfInterestTask;isBedOccupiedByOthers(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)Z"))
    private static boolean redirectTestPoi(ServerWorld world, BlockPos pos, LivingEntity entity, TaskTriggerer.TaskContext<LivingEntity> context, MemoryQueryResult<IdF.Mu, GlobalPos> queryResult, Predicate<RegistryEntry<PointOfInterestType>> poiPredicate) {
        if (!RegionProtectionMod.getConfig().enabled()) {
            return isBedOccupiedByOthers(world, pos, entity);
        }

        Optional<RegistryEntry<PointOfInterestType>> poiTypeOption = world.getPointOfInterestStorage().getType(pos);

        ActionResult result;

        if (poiTypeOption.isPresent()) {
            RegistryEntry<PointOfInterestType> poiType = poiTypeOption.get();


            if (poiType.isIn(PointOfInterestTypeTags.ACQUIRABLE_JOB_SITE)) {
                result = RegionRuleEnforcer.onVillagerWork(world, pos);
            } else if (poiType.matchesKey(PointOfInterestTypes.HOME)) {
                result = RegionRuleEnforcer.onVillagerHome(world, pos);
            } else {
                result = ActionResult.PASS;
            }
        } else {
            result = ActionResult.PASS;
        }

        //noinspection ConstantValue
        return result == ActionResult.FAIL || isBedOccupiedByOthers(world, pos, entity);
    }

    @Shadow private static boolean isBedOccupiedByOthers(ServerWorld world, BlockPos pos, LivingEntity entity) {
        throw new IllegalStateException();
    }
}
