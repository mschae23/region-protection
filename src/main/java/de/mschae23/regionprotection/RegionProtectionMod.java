package de.mschae23.regionprotection;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.RegistryOps;
import net.minecraft.util.Identifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import de.mschae23.config.api.ConfigIo;
import de.mschae23.config.api.ModConfig;
import de.mschae23.regionprotection.command.RegionCommand;
import de.mschae23.regionprotection.config.RegionProtectionConfigV1;
import de.mschae23.regionprotection.region.RegionRuleEnforcer;
import de.mschae23.regionprotection.region.RegionV2;
import de.mschae23.regionprotection.region.shape.ProtectionShapeType;
import de.mschae23.regionprotection.registry.RegionProtectionRegistries;
import de.mschae23.regionprotection.state.RegionPersistentState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import me.lucko.fabric.api.permissions.v0.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionProtectionMod implements ModInitializer {
    public static final String MODID = "regionprotection";
    @SuppressWarnings("unused")
    public static final Logger LOGGER = LoggerFactory.getLogger("Region Protection");

    private static final RegionProtectionConfigV1 LATEST_CONFIG_DEFAULT = RegionProtectionConfigV1.DEFAULT;
    private static final int LATEST_CONFIG_VERSION = LATEST_CONFIG_DEFAULT.version();
    private static final Codec<ModConfig<RegionProtectionConfigV1>> CONFIG_CODEC = ModConfig.createCodec(LATEST_CONFIG_VERSION, RegionProtectionMod::getConfigType);

    private static RegionProtectionConfigV1 CONFIG = LATEST_CONFIG_DEFAULT;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server ->
            CONFIG = ConfigIo.initializeConfig(Paths.get(MODID + ".json"), LATEST_CONFIG_VERSION, LATEST_CONFIG_DEFAULT, CONFIG_CODEC,
                RegistryOps.of(JsonOps.INSTANCE, server.getRegistryManager()), LOGGER::info, LOGGER::error)
        );

        RegionProtectionRegistries.init();
        ProtectionShapeType.init();

        // Command registration
        //noinspection CodeBlock2Expr
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RegionCommand.register(dispatcher);
        });

        // ArgumentTypeRegistry.registerArgumentType(id("protection_rule"), ProtectionRuleArgumentType.class, ConstantArgumentSerializer.of(ProtectionRuleArgumentType::protectionRule));
        // ArgumentTypeRegistry.registerArgumentType(id("tristate"), TriStateArgumentType.class, ConstantArgumentSerializer.of(TriStateArgumentType::triState));

        // Check permissions on the server once, so that they are registered and can be auto-completed
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CommandSource source = server.getCommandSource();

            Stream<String> commandPermissions = Arrays.stream(new String[][] {
                RegionCommand.PERMISSIONS,
            }).flatMap(Arrays::stream);

            Stream<String> regionPermissions = RegionPersistentState.get(server).getRegions().stream()
                .map(RegionV2::key).flatMap(name -> Arrays.stream(RegionRuleEnforcer.RULES)
                    .map(rule -> RegionRuleEnforcer.getBasePermission(name, rule)));

            Stream.concat(commandPermissions, regionPermissions)
                .forEach(permission -> Permissions.check(source, MODID + permission));
        });

        RegionPersistentState.init();
    }

    @SuppressWarnings({ "deprecation", "RedundantSuppression", "SwitchStatementWithTooFewBranches" })
    private static ModConfig.Type<RegionProtectionConfigV1, ?> getConfigType(int version) {
        return new ModConfig.Type<>(version, switch (version) {
            default -> RegionProtectionConfigV1.TYPE_CODEC;
        });
    }

    public static RegionProtectionConfigV1 getConfig() {
        return CONFIG;
    }

    public static Identifier id(String path) {
        return Identifier.of(MODID, path);
    }
}
