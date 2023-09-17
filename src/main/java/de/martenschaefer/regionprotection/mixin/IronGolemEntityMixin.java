package de.martenschaefer.regionprotection.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import de.martenschaefer.regionprotection.region.RegionRuleEnforcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronGolemEntity.class)
public class IronGolemEntityMixin extends GolemEntity {
    protected IronGolemEntityMixin(EntityType<? extends GolemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "canSpawn", at = @At("RETURN"), cancellable = true)
    private void injectCanSpawn(WorldView worldView, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }

        if (worldView instanceof ServerWorld world) {
            cir.setReturnValue(RegionRuleEnforcer.onSpawnIronGolem(world, this.getBlockPos()) != ActionResult.FAIL);
        }
    }
}
