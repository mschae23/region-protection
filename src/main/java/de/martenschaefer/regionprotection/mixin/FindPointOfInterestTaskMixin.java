package de.martenschaefer.regionprotection.mixin;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.entity.ai.brain.MemoryQuery;
import net.minecraft.entity.ai.brain.MemoryQueryResult;
import net.minecraft.entity.ai.brain.task.FindPointOfInterestTask;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.RegionRuleEnforcer;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FindPointOfInterestTask.class)
public class FindPointOfInterestTaskMixin {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Redirect(method = "method_46885", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;limit(J)Ljava/util/stream/Stream;", remap = false))
    private static Stream<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> redirectPoiStream(Stream<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> stream, long maxSize, boolean bl, MutableLong mutableLong, Long2ObjectMap<FindPointOfInterestTask.RetryMarker> long2ObjectMap, Predicate<RegistryEntry<PointOfInterestType>> poiPredicate, MemoryQueryResult<com.mojang.datafixers.kinds.Const.Mu<Unit>, MemoryQuery.Value<GlobalPos>> queryResult, Optional<Byte> entityStatus, ServerWorld world, PathAwareEntity entity, long time) {
        if (!RegionProtectionMod.getConfig().enabled()) {
            return stream.limit(maxSize);
        }

        return stream.filter(pair -> {
            ActionResult result;

            if (pair.getFirst().isIn(PointOfInterestTypeTags.ACQUIRABLE_JOB_SITE)) {
                result = RegionRuleEnforcer.onVillagerWork(world, pair.getSecond());
            } else if (pair.getFirst().matchesKey(PointOfInterestTypes.HOME)) {
                result = RegionRuleEnforcer.onVillagerHome(world, pair.getSecond());
            } else {
                result = ActionResult.PASS;
            }

            return result != ActionResult.FAIL;
        }).limit(maxSize);
    }
}
