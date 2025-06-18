package de.mschae23.regionprotection.state;

import java.util.stream.Stream;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.util.TriState;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.mschae23.config.api.ModConfig;
import de.mschae23.regionprotection.ModUtils;
import de.mschae23.regionprotection.region.IndexedRegionMap;
import de.mschae23.regionprotection.region.ProtectionRule;
import de.mschae23.regionprotection.region.RegionMap;
import de.mschae23.regionprotection.region.RegionRuleEnforcer;
import de.mschae23.regionprotection.region.RegionV2;
import de.mschae23.regionprotection.region.cache.PlayerRegionCache;
import de.mschae23.regionprotection.region.shape.ProtectionContext;
import de.mschae23.regionprotection.region.shape.UnionShape;
import net.luckperms.api.event.node.NodeMutateEvent;
import org.jetbrains.annotations.Nullable;

public final class RegionPersistentState extends PersistentState {
    public static final String ID = "serverutils_region"; // RegionProtectionMod.MODID + "_region";

    public static final Codec<RegionPersistentState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RegionV2.REGION_CODEC.xmap(ModConfig::latest, r -> r).listOf().xmap(regions -> {
            IndexedRegionMap map = new IndexedRegionMap();

            for (RegionV2 region: regions) {
                map.add(region);
            }

            return map;
        }, map -> map.stream().toList()).fieldOf("regions").forGetter(state -> state.regions)
    ).apply(instance, instance.stable(RegionPersistentState::new)));

    private final IndexedRegionMap regions;
    private final PlayerRegionCache playerRegionCache;

    private RegionPersistentState(IndexedRegionMap regions) {
        this.regions = regions;
        this.playerRegionCache = new PlayerRegionCache();
    }

    private RegionPersistentState() {
        this(new IndexedRegionMap());
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

    public void onWorldLoad(ServerWorld world) {
        this.regions.addDimension(world.getRegistryKey());
    }

    public void onWorldUnload(ServerWorld world) {
        this.regions.removeDimension(world.getRegistryKey());
    }

    public static RegionPersistentState get(MinecraftServer server) {
        PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
        return stateManager.getOrCreate(new PersistentStateType<>(ID, RegionPersistentState::new, CODEC, DataFixTypes.LEVEL));
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
