package de.martenschaefer.regionprotection.state;

import java.util.stream.Stream;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.util.TriState;
import de.martenschaefer.config.api.ModConfig;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.IndexedRegionMap;
import de.martenschaefer.regionprotection.region.ProtectionRule;
import de.martenschaefer.regionprotection.region.RegionMap;
import de.martenschaefer.regionprotection.region.RegionRuleEnforcer;
import de.martenschaefer.regionprotection.region.RegionV2;
import de.martenschaefer.regionprotection.region.shape.ProtectionContext;
import de.martenschaefer.regionprotection.region.v1.RegionV1;
import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Nullable;

public final class RegionPersistentState extends PersistentState {
    public static final String ID = "serverutils_region"; // RegionProtectionMod.MODID + "_region";

    private final IndexedRegionMap regions;

    private RegionPersistentState() {
        this.regions = new IndexedRegionMap();
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addRegion(RegionV2 region) {
        return this.regions.add(region);
    }

    public boolean removeRegion(RegionV2 region) {
        return this.removeRegion(region.key()) != null;
    }

    public RegionV2 removeRegion(String key) {
        return this.regions.remove(key);
    }

    public void replaceRegion(RegionV2 from, RegionV2 to) {
        this.regions.replace(from, to);
    }

    @Nullable
    public RegionV2 getRegionByKey(String key) {
        return this.regions.byKey(key);
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    public RegionMap getRegions() {
        return this.regions;
    }

    public Stream<RegionV2> findRegion(ProtectionContext context) {
        return this.getRegions().findRegion(context);
    }

    public TriState checkRegion(ProtectionContext context, ServerPlayerEntity player, ProtectionRule rule) {
        return this.regions.checkRegion(context, player, rule);
    }

    public TriState checkRegionGeneric(ProtectionContext context, ProtectionRule rule) {
        return this.regions.checkRegionGeneric(context, rule);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound root) {
        NbtList regions = new NbtList();

        for (RegionV2 region : this.regions) {
            var result = RegionV2.REGION_CODEC.encodeStart(NbtOps.INSTANCE, region);
            result.get().ifLeft(regions::add)
                .ifRight(partial -> RegionProtectionMod.LOGGER.error("Error writing region data as persistent state: " + partial.message()));
        }

        root.put("regions", regions);
        return root;
    }

    private static RegionPersistentState readNbt(NbtCompound root) {
        RegionPersistentState regionState = new RegionPersistentState();

        NbtList regions = root.getList("regions", NbtElement.COMPOUND_TYPE);

        for (NbtElement regionElement : regions) {
            RegionV2.REGION_CODEC.decode(NbtOps.INSTANCE, regionElement)
                .map(Pair::getFirst)
                .get()
                .mapLeft(ModConfig::latest)
                .ifLeft(regionState::addRegion)
                .ifRight(partial ->
                    // Fallback to old region format
                    RegionV1.OLD_CODEC.decode(NbtOps.INSTANCE, regionElement)
                        .map(Pair::getFirst)
                        .get()
                        .ifLeft(RegionV1::latest)
                        .ifRight(partialOld -> RegionProtectionMod.LOGGER.error("Error reading region data as persistent state: " + partial.message()))
                );
        }

        return regionState;
    }

    public void onWorldLoad(ServerWorld world) {
        this.regions.addDimension(world.getRegistryKey());
    }

    public void onWorldUnload(ServerWorld world) {
        this.regions.removeDimension(world.getRegistryKey());
    }

    public static RegionPersistentState get(MinecraftServer server) {
        PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
        return stateManager.getOrCreate(RegionPersistentState::readNbt, RegionPersistentState::new, ID);
    }

    public static void init() {
        ServerWorldEvents.LOAD.register((server, world) -> RegionPersistentState.get(server).onWorldLoad(world));
        ServerWorldEvents.UNLOAD.register((server, world) -> RegionPersistentState.get(server).onWorldUnload(world));

        RegionRuleEnforcer.init();
    }
}
