package de.martenschaefer.regionprotection.region;

import java.util.Arrays;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.util.TriState;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.shape.ProtectionContext;
import de.martenschaefer.regionprotection.state.RegionPersistentState;

public final class RegionRuleEnforcer {
    private static final Text DENIED_TEXT = Text.literal("You cannot do that in this region!");

    public static final String[] RULES = Arrays.stream(ProtectionRule.values()).map(ProtectionRule::getName).toArray(String[]::new);

    private RegionRuleEnforcer() {
    }

    public static ActionResult onBlockBreak(PlayerEntity player, BlockPos pos) {
        return onEventSendDenied(player, Vec3d.ofCenter(pos), ProtectionRule.BlockBreak);
    }

    public static ActionResult onBlockPlace(PlayerEntity player, BlockPos pos) {
        return onEventSendDenied(player, Vec3d.ofCenter(pos), ProtectionRule.BlockPlace);
    }

    public static ActionResult onBlockUse(PlayerEntity player, BlockPos pos) {
        return onEvent(player, Vec3d.ofCenter(pos), ProtectionRule.BlockUse, true);
    }

    public static ActionResult onItemUse(PlayerEntity player, Hand hand, Vec3d pos) {
        return onEvent(player, pos, ProtectionRule.ItemUse, true);
    }

    public static ActionResult onWorldModify(PlayerEntity player, BlockPos pos) {
        return onEventSendDenied(player, Vec3d.ofCenter(pos), ProtectionRule.WorldModify, true);
    }

    public static ActionResult onNetherPortalUse(PlayerEntity player, Vec3d pos) {
        return onEventSendDenied(player, pos, ProtectionRule.PortalNetherUse);
    }

    public static ActionResult onEndPortalUse(PlayerEntity player, Vec3d pos) {
        return onEventSendDenied(player, pos, ProtectionRule.PortalEndUse);
    }

    public static ActionResult onVillagerWork(ServerWorld world, BlockPos pos) {
        return onEventGeneric(world, Vec3d.ofCenter(pos), ProtectionRule.VillagerWork);
    }

    public static ActionResult onVillagerHome(ServerWorld world, BlockPos pos) {
        return onEventGeneric(world, Vec3d.ofCenter(pos), ProtectionRule.VillagerHome);
    }

    public static ActionResult onPlayerPvp(ServerPlayerEntity player, Vec3d pos) {
        return onEvent(player, pos, ProtectionRule.PlayerPvp);
    }

    public static ActionResult onPlayerPvpSendDenied(ServerPlayerEntity player, Vec3d pos) {
        return onEventSendDenied(player, pos, ProtectionRule.PlayerPvp);
    }

    public static ActionResult onExplosionDestroy(ServerPlayerEntity player, BlockPos pos) {
        return onEvent(player, Vec3d.ofCenter(pos), ProtectionRule.ExplosionDestroy);
    }

    public static ActionResult onExplosionDestroy(ServerWorld world, BlockPos pos) {
        return onEventGeneric(world, Vec3d.ofCenter(pos), ProtectionRule.ExplosionDestroy);
    }

    public static ActionResult onExplosionIgnite(ServerPlayerEntity player, BlockPos pos) {
        return onEvent(player, Vec3d.ofCenter(pos), ProtectionRule.ExplosionIgnite);
    }

    public static ActionResult onExplosionIgnite(ServerPlayerEntity player, Vec3d pos) {
        return onEvent(player, pos, ProtectionRule.ExplosionIgnite);
    }

    public static ActionResult onExplosionIgnite(ServerWorld world, BlockPos pos) {
        return onEventGeneric(world, Vec3d.ofCenter(pos), ProtectionRule.ExplosionIgnite);
    }

    public static ActionResult onEvent(PlayerEntity player, Vec3d pos, ProtectionRule rule) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            RegistryKey<World> dimension = serverPlayer.getServerWorld().getRegistryKey();
            ProtectionContext context = new ProtectionContext(dimension, pos);
            RegionPersistentState regionState = RegionPersistentState.get(serverPlayer.getServerWorld().getServer());

            TriState result = regionState.checkPlayerRegion(serverPlayer, context, rule);
            return result == TriState.FALSE ? ActionResult.FAIL : ActionResult.PASS;
        }

        return ActionResult.PASS;
    }

    public static ActionResult onEventSendDenied(PlayerEntity player, Vec3d pos, ProtectionRule rule) {
        ActionResult result = onEvent(player, pos, rule);

        if (result == ActionResult.FAIL && player instanceof ServerPlayerEntity serverPlayer) {
            sendDeniedText(serverPlayer);
        }

        return result;
    }

    public static ActionResult onEvent(PlayerEntity player, Vec3d pos, ProtectionRule rule, boolean syncInventory) {
        ActionResult result = onEvent(player, pos, rule);

        if (syncInventory && result == ActionResult.FAIL) {
            // Sync the player's inventory, as it may have used the item already.
            player.getInventory().markDirty();
            player.playerScreenHandler.updateToClient();
        }

        return result;
    }

    public static ActionResult onEventSendDenied(PlayerEntity player, Vec3d pos, ProtectionRule rule, boolean syncInventory) {
        ActionResult result = onEvent(player, pos, rule, syncInventory);

        if (result == ActionResult.FAIL && player instanceof ServerPlayerEntity serverPlayer) {
            sendDeniedText(serverPlayer);
        }

        return result;
    }

    public static ActionResult onEventGeneric(ServerWorld world, Vec3d pos, ProtectionRule rule) {
        RegistryKey<World> dimension = world.getRegistryKey();
        ProtectionContext context = new ProtectionContext(dimension, pos);
        RegionPersistentState regionState = RegionPersistentState.get(world.getServer());

        TriState result = regionState.checkRegionGeneric(context, rule);
        return result == TriState.FALSE ? ActionResult.FAIL : ActionResult.PASS;
    }

    public static void init() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> onBlockBreak(player, pos));
    }

    public static String getBasePermission(String key, String action) {
        return ".region." + key + "." + action;
    }

    public static String getPermission(String key, String rule) {
        return RegionProtectionMod.MODID + ".region." + key + "." + rule;
    }

    public static void sendDeniedText(ServerPlayerEntity player) {
        player.sendMessageToClient(DENIED_TEXT, true);
    }
}
