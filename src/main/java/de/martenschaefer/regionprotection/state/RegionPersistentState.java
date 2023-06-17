package de.martenschaefer.regionprotection.state;

import java.util.stream.Stream;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.util.TriState;
import de.martenschaefer.config.api.ModConfig;
import de.martenschaefer.regionprotection.ModUtils;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.IndexedRegionMap;
import de.martenschaefer.regionprotection.region.ProtectionRule;
import de.martenschaefer.regionprotection.region.RegionMap;
import de.martenschaefer.regionprotection.region.RegionRuleEnforcer;
import de.martenschaefer.regionprotection.region.RegionV2;
import de.martenschaefer.regionprotection.region.cache.PlayerRegionCache;
import de.martenschaefer.regionprotection.region.shape.ProtectionContext;
import de.martenschaefer.regionprotection.region.shape.UnionShape;
import de.martenschaefer.regionprotection.region.v1.RegionV1;
import com.mojang.datafixers.util.Pair;
import net.luckperms.api.event.node.NodeMutateEvent;
import org.jetbrains.annotations.Nullable;

public final class RegionPersistentState extends PersistentState {
    public static final String ID = "serverutils_region"; // RegionProtectionMod.MODID + "_region";

    private final IndexedRegionMap regions;
    private final PlayerRegionCache playerRegionCache;

    private RegionPersistentState() {
        this.regions = new IndexedRegionMap();
        this.playerRegionCache = new PlayerRegionCache();
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addRegion(RegionV2 region) {
        this.playerRegionCache.clear();
        return this.regions.add(region);
    }

    public boolean removeRegion(RegionV2 region) {
        this.playerRegionCache.clear();
        return this.removeRegion(region.key()) != null;
    }

    public RegionV2 removeRegion(String key) {
        this.playerRegionCache.clear();
        return this.regions.remove(key);
    }

    public void replaceRegion(RegionV2 from, RegionV2 to) {
        this.playerRegionCache.clear();
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

    public Pair<RegionV2, TriState> findRegion(ProtectionContext context, ServerPlayerEntity player, ProtectionRule rule) {
        return this.regions.findRegion(context, player, rule);
    }

    public Stream<RegionV2> findIntersectingRegions(RegistryKey<World> dimension, UnionShape shape) {
        return this.regions.findIntersectingRegions(dimension, shape);
    }

    public Stream<RegionV2> getRegionsInDimension(RegistryKey<World> dimension) {
        return this.regions.getRegionsInDimension(dimension);
    }

    public TriState checkPlayerRegion(ServerPlayerEntity player, ProtectionContext context, ProtectionRule rule) {
        return this.playerRegionCache.check(this, player, context, rule);
    }

    public TriState checkRegionGeneric(ProtectionContext context, ProtectionRule rule) {
        return this.regions.checkRegionGeneric(context, rule);
    }

    public Text getCacheStatsText() {
        return Text.empty()
            .append(Text.literal(String.valueOf(this.playerRegionCache.getCacheHits())).formatted(Formatting.GREEN)).append(":")
            .append(Text.literal(String.valueOf(this.playerRegionCache.getCacheSwaps())).formatted(Formatting.YELLOW)).append(":")
            .append(Text.literal(String.valueOf(this.playerRegionCache.getCacheMisses())).formatted(Formatting.RED));
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

        //noinspection resource
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
            ModUtils.getLuckPerms().getEventBus().subscribe(NodeMutateEvent.class, event ->
                get(server).playerRegionCache.clear()));
    }
}
