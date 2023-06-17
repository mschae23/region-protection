package de.martenschaefer.regionprotection.command;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.util.TriState;
import de.martenschaefer.regionprotection.ModUtils;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.ProtectionRule;
import de.martenschaefer.regionprotection.region.RegionMap;
import de.martenschaefer.regionprotection.state.RegionPersistentState;
import de.martenschaefer.regionprotection.region.RegionRuleEnforcer;
import de.martenschaefer.regionprotection.region.RegionV2;
import de.martenschaefer.regionprotection.region.shape.ProtectionContext;
import de.martenschaefer.regionprotection.region.shape.ProtectionShape;
import de.martenschaefer.regionprotection.region.shape.RegionShapes;
import de.martenschaefer.regionprotection.region.shape.ShapeBuilder;
import de.martenschaefer.regionprotection.util.StringTriState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.luckperms.api.cacheddata.CachedPermissionData;

public final class RegionCommand {
    private static final DynamicCommandExceptionType REGION_ALREADY_EXISTS_EXCEPTION = new DynamicCommandExceptionType(id ->
        new LiteralMessage("Region with the id '" + id + "' already exists"));
    private static final DynamicCommandExceptionType REGION_DOES_NOT_EXIST_EXCEPTION = new DynamicCommandExceptionType(id ->
        new LiteralMessage("Region with the id '" + id + "' does not exist"));
    private static final DynamicCommandExceptionType RULE_DOES_NOT_EXIST_EXCEPTION = new DynamicCommandExceptionType(name ->
        new LiteralMessage("Protection rule '" + name + "' does not exist"));
    private static final DynamicCommandExceptionType TRISTATE_DOES_NOT_EXIST_EXCEPTION = new DynamicCommandExceptionType(name ->
        new LiteralMessage("Protection rule value '" + name + "' does not exist"));

    // Shape command errors
    private static final SimpleCommandExceptionType NOT_CURRENTLY_BUILDING = new SimpleCommandExceptionType(
        new LiteralMessage("You are not currently building a shape! To start, run /region shape start"));
    private static final SimpleCommandExceptionType ALREADY_BUILDING = new SimpleCommandExceptionType(
        new LiteralMessage("You are already building a shape! To cancel, run /region shape stop"));
    private static final DynamicCommandExceptionType SHAPE_NOT_FOUND = new DynamicCommandExceptionType(name ->
        new LiteralMessage("Shape '" + name + "' does not exist"));

    private static final SuggestionProvider<ServerCommandSource> REGION_NAME_SUGGESTION_PROVIDER = (context, builder) -> {
        RegionMap regions = RegionPersistentState.get(context.getSource().getServer()).getRegions();

        CommandSource.suggestMatching(regions.stream().map(RegionV2::key), builder);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> REGION_SHAPE_NAME_SUGGESTION_PROVIDER = (context, builder) -> {
        RegionPersistentState regionState = RegionPersistentState.get(context.getSource().getServer());

        try {
            String key = StringArgumentType.getString(context, "name");
            RegionV2 region = regionState.getRegionByKey(key);

            if (region != null) {
                CommandSource.suggestMatching(Arrays.stream(region.shapes().getEntries()).map(RegionShapes.Entry::name), builder);
            }
        } catch (IllegalArgumentException e) {
            // Ignore
        }

        return builder.buildFuture();
    };

    public static final String PERMISSION_ROOT = ".command.region.root";

    public static final String[] PERMISSIONS = new String[] {
        PERMISSION_ROOT,
        ".command.region.add",
        ".command.region.remove",
        ".command.region.modify",
        ".command.region.shape",
        ".command.region.info",
        ".command.region.test",
        ".command.region.list",
        ".command.region.stats",
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("region")
            .requires(Permissions.require(RegionProtectionMod.MODID + PERMISSION_ROOT, 3))
            .then(CommandManager.literal("add")
                .requires(Permissions.require(RegionProtectionMod.MODID + ".command.region.add", true))
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .then(CommandManager.literal("with")
                        .then(CommandManager.literal("universal")
                            .executes(RegionCommand::executeAddWithUniversal))
                        .then(CommandManager.literal("dimension")
                            .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                .executes(RegionCommand::executeAddWithDimension)))
                        .then(CommandManager.literal("pos")
                            .then(CommandManager.argument("min", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("max", BlockPosArgumentType.blockPos())
                                    .executes(RegionCommand::executeAddWithLocalBox)))))))
            .then(CommandManager.literal("remove")
                .requires(Permissions.require(RegionProtectionMod.MODID + ".command.region.remove", true))
                .then(CommandManager.argument("name", StringArgumentType.word()).suggests(REGION_NAME_SUGGESTION_PROVIDER)
                    .executes(RegionCommand::executeRemove)))
            .then(CommandManager.literal("modify")
                .requires(Permissions.require(RegionProtectionMod.MODID + ".command.region.modify", true))
                .then(CommandManager.argument("name", StringArgumentType.word()).suggests(REGION_NAME_SUGGESTION_PROVIDER)
                    .then(CommandManager.literal("shape")
                        .then(CommandManager.literal("replace")
                            .then(CommandManager.literal("with")
                                .then(CommandManager.literal("universal")
                                    .executes(RegionCommand::executeModifyReplaceShapeWithUniversal))
                                .then(CommandManager.literal("dimension")
                                    .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                        .executes(RegionCommand::executeModifyReplaceShapeWithDimension)))
                                .then(CommandManager.literal("pos")
                                    .then(CommandManager.argument("min", BlockPosArgumentType.blockPos())
                                        .then(CommandManager.argument("max", BlockPosArgumentType.blockPos())
                                            .executes(RegionCommand::executeModifyReplaceShapeWithLocalBox)))))))
                    .then(CommandManager.literal("level")
                        .then(CommandManager.argument("level", IntegerArgumentType.integer())
                            .executes(RegionCommand::executeModifyLevel)))
                    .then(CommandManager.literal("rule")
                        .then(CommandManager.literal("set")
                            .then(CommandManager.argument("rule_name", StringArgumentType.word()).suggests((context, builder) -> {
                                ProtectionRule[] rules = ProtectionRule.values();

                                CommandSource.suggestMatching(Arrays.stream(rules).map(ProtectionRule::asString), builder);
                                return builder.buildFuture();
                            })
                                .then(CommandManager.argument("rule_value", StringArgumentType.word()).suggests((context, builder) -> {
                                    StringTriState[] states = StringTriState.values();

                                    CommandSource.suggestMatching(Arrays.stream(states).map(StringTriState::asString), builder);
                                    return builder.buildFuture();
                                }).executes(RegionCommand::executeModifySetRule)))))))
            .then(CommandManager.literal("shape")
                .requires(Permissions.require(RegionProtectionMod.MODID + ".command.region.shape", true))
                .then(CommandManager.literal("start").executes(RegionCommand::executeStartShape))
                .then(CommandManager.literal("stop").executes(RegionCommand::executeStopShape))
                .then(CommandManager.literal("add")
                    .then(CommandManager.literal("universal")
                        .executes(RegionCommand::executeShapeAddUniversal))
                    .then(CommandManager.literal("dimension")
                        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                            .executes(RegionCommand::executeShapeAddDimension)))
                    .then(CommandManager.literal("pos")
                        .then(CommandManager.literal("local")
                            .then(CommandManager.argument("min", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("max", BlockPosArgumentType.blockPos())
                                    .executes(RegionCommand::executeShapeAddLocalBox))))
                        .then(CommandManager.literal("in")
                            .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                .then(CommandManager.argument("min", BlockPosArgumentType.blockPos())
                                    .then(CommandManager.argument("max", BlockPosArgumentType.blockPos())
                                        .executes(RegionCommand::executeShapeAddBox)))))))
                .then(CommandManager.literal("finish")
                    .then(CommandManager.argument("shape_name", StringArgumentType.string())
                        .then(CommandManager.literal("to")
                            .then(CommandManager.argument("name", StringArgumentType.word()).suggests(REGION_NAME_SUGGESTION_PROVIDER)
                                .executes(RegionCommand::executeAddShapeToRegion)))))
                .then(CommandManager.literal("remove")
                    .then(CommandManager.argument("name", StringArgumentType.word()).suggests(REGION_NAME_SUGGESTION_PROVIDER)
                        .then(CommandManager.argument("shape_name", StringArgumentType.string()).suggests(REGION_SHAPE_NAME_SUGGESTION_PROVIDER)
                            .executes(RegionCommand::executeRemoveShapeFromRegion)))))
            .then(CommandManager.literal("info")
                .requires(Permissions.require(RegionProtectionMod.MODID + ".command.region.info", true))
                .then(CommandManager.argument("name", StringArgumentType.word()).suggests(REGION_NAME_SUGGESTION_PROVIDER)
                    .executes(RegionCommand::executeInfo)))
            .then(CommandManager.literal("test")
                .requires(Permissions.require(RegionProtectionMod.MODID + ".command.region.test", true).and(ServerCommandSource::isExecutedByPlayer))
                .executes(RegionCommand::executeTest)
                .then(CommandManager.literal("player")
                    .executes(RegionCommand::executeTest))
                .then(CommandManager.literal("generic")
                    .executes(RegionCommand::executeTestGeneric)))
            .then(CommandManager.literal("list")
                .requires(Permissions.require(RegionProtectionMod.MODID + ".command.region.list", true))
                .executes(RegionCommand::executeList))
            .then(CommandManager.literal("stats")
                .requires(Permissions.require(RegionProtectionMod.MODID + ".command.region.stats", true))
                .executes(RegionCommand::executeStats)));
    }

    private static int executeAddWithUniversal(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return addRegion(context, region -> region.withAddedShape(region.key(), ProtectionShape.universe()), false);
    }

    private static int executeAddWithDimension(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        RegistryKey<World> dimension = DimensionArgumentType.getDimensionArgument(context, "dimension").getRegistryKey();
        return addRegion(context, region -> region.withAddedShape(region.key(), ProtectionShape.dimension(dimension)), false);
    }

    private static int executeAddWithLocalBox(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        RegistryKey<World> dimension = context.getSource().getWorld().getRegistryKey();
        BlockPos min = BlockPosArgumentType.getBlockPos(context, "min");
        BlockPos max = BlockPosArgumentType.getBlockPos(context, "max");
        return addRegion(context, region -> region.withAddedShape(region.key(), ProtectionShape.box(dimension, min, max)), true);
    }

    private static int addRegion(CommandContext<ServerCommandSource> context, UnaryOperator<RegionV2> operator, boolean sendShapeCommandTip) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "name");

        ServerCommandSource source = context.getSource();
        RegionPersistentState regionState = RegionPersistentState.get(source.getServer());
        RegionV2 region = operator.apply(RegionV2.create(key));

        if (regionState.addRegion(region)) {
            // Check permissions for every rule once, for LuckPerms command auto-completion
            Arrays.stream(RegionRuleEnforcer.RULES).map(rule -> RegionRuleEnforcer.getBasePermission(region.key(), rule))
                .forEach(permission -> Permissions.check(source, RegionProtectionMod.MODID + permission));

            RegionShapes shapes = region.shapes();

            if (shapes.isEmpty()) {
                source.sendFeedback(() -> Text.literal("Added empty region as '" + key + "'"), true);
            } else {
                source.sendFeedback(() -> Text.literal("Added region as '" + key + "' with ").append(shapes.displayShort()), true);
            }

            if (sendShapeCommandTip) {
                source.sendFeedback(() -> Text.literal("Run ")
                    .append(Text.literal("/region shape start").formatted(Formatting.GRAY))
                    .append(" to include additional shapes in this region"), false);
            }

            return Command.SINGLE_SUCCESS;
        } else {
            throw REGION_ALREADY_EXISTS_EXCEPTION.create(key);
        }
    }

    private static int executeRemove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "name");

        ServerCommandSource source = context.getSource();
        RegionPersistentState regionState = RegionPersistentState.get(source.getServer());
        RegionV2 region = regionState.removeRegion(key);

        if (region != null) {
            source.sendFeedback(() -> Text.literal("Removed region '" + key + "'"), true);
            return Command.SINGLE_SUCCESS;
        } else {
            throw REGION_DOES_NOT_EXIST_EXCEPTION.create(key);
        }
    }

    private static int executeModifyReplaceShapeWithUniversal(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return modifyRegion(context, region -> region.withReplacedShape(region.key(), ProtectionShape.universe()), (oldRegion, modifiedRegion) ->
            Text.literal("Set shape of '" + modifiedRegion.key() + "' to ").append(modifiedRegion.shapes().displayShort()));
    }

    private static int executeModifyReplaceShapeWithDimension(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        RegistryKey<World> dimension = DimensionArgumentType.getDimensionArgument(context, "dimension").getRegistryKey();
        return modifyRegion(context, region -> region.withReplacedShape(region.key(), ProtectionShape.dimension(dimension)), (oldRegion, modifiedRegion) ->
            Text.literal("Set shape of '" + modifiedRegion.key() + "' to ").append(modifiedRegion.shapes().displayShort()));
    }

    private static int executeModifyReplaceShapeWithLocalBox(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        RegistryKey<World> dimension = context.getSource().getWorld().getRegistryKey();
        BlockPos min = BlockPosArgumentType.getBlockPos(context, "min");
        BlockPos max = BlockPosArgumentType.getBlockPos(context, "max");
        return modifyRegion(context, region -> region.withReplacedShape(region.key(), ProtectionShape.box(dimension, min, max)), (oldRegion, modifiedRegion) ->
            Text.literal("Set shape of '" + modifiedRegion.key() + "' to ").append(modifiedRegion.shapes().displayShort()));
    }

    private static int executeModifyLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int level = IntegerArgumentType.getInteger(context, "level");
        return modifyRegion(context, region -> region.withLevel(level), (oldRegion, modifiedRegion) ->
            Text.literal("Set level of '" + modifiedRegion.key() + "' from " + oldRegion.level() + " to " + level));
    }

    private static int executeModifySetRule(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String ruleString = StringArgumentType.getString(context, "rule_name");
        String stateString = StringArgumentType.getString(context, "rule_value");
        ProtectionRule rule = ProtectionRule.byName(ruleString);
        StringTriState value = StringTriState.byNameString(stateString);

        if (rule == null) {
            throw RULE_DOES_NOT_EXIST_EXCEPTION.create(ruleString);
        } else if (value == null) {
            throw TRISTATE_DOES_NOT_EXIST_EXCEPTION.create(stateString);
        }

        return modifyRegion(context, region -> region.withRule(rule, value.getState()), (oldRegion, modifiedRegion) ->
            Text.literal("Set rule ").append(Text.literal(rule.getName()).formatted(Formatting.GRAY))
                .append(" for '" + modifiedRegion.key() + "' to ").append(Text.literal(value.asString()).formatted(value.getFormatting())));
    }

    private static int modifyRegion(CommandContext<ServerCommandSource> context, UnaryOperator<RegionV2> operator, BiFunction<RegionV2, RegionV2, Text> feedback) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "name");

        ServerCommandSource source = context.getSource();
        RegionPersistentState regionState = RegionPersistentState.get(source.getServer());
        RegionV2 region = regionState.getRegionByKey(key);

        if (region != null) {
            RegionV2 modifiedRegion = operator.apply(region);
            regionState.replaceRegion(region, modifiedRegion);

            source.sendFeedback(() -> feedback.apply(region, modifiedRegion), true);
            return Command.SINGLE_SUCCESS;
        } else {
            throw REGION_DOES_NOT_EXIST_EXCEPTION.create(key);
        }
    }

    private static int executeInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "name");

        ServerCommandSource source = context.getSource();
        RegionPersistentState regionState = RegionPersistentState.get(source.getServer());
        RegionV2 region = regionState.getRegionByKey(key);

        if (region != null) {
            source.sendFeedback(() -> Text.literal("Region '" + key + "':")
                .append("\n Level: ").append(String.valueOf(region.level()))
                .append("\n Shape:\n").append(region.shapes().displayList())
                .append("\n Rules:\n").append(region.rulesForDisplay()), false);
            return Command.SINGLE_SUCCESS;
        } else {
            throw REGION_DOES_NOT_EXIST_EXCEPTION.create(key);
        }
    }

    private static int executeTest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return executeTestInternal(context, true);
    }

    private static int executeTestGeneric(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return executeTestInternal(context, false);
    }

    private static int executeTestInternal(CommandContext<ServerCommandSource> context, boolean player) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity playerEntity = source.getPlayerOrThrow();
        CachedPermissionData permissionData = ModUtils.getLuckPerms().getPlayerAdapter(ServerPlayerEntity.class).getPermissionData(playerEntity);
        RegionPersistentState regionState = RegionPersistentState.get(source.getServer());
        RegistryKey<World> dimension = context.getSource().getWorld().getRegistryKey();
        Vec3d pos = context.getSource().getPosition();
        ProtectionContext protectionContext = new ProtectionContext(dimension, pos);

        List<RegionV2> possibleRegions = regionState.findRegion(protectionContext).toList();

        MutableText rulesText = Text.empty();

        for (ProtectionRule rule : ProtectionRule.values()) {
            TriState state = TriState.DEFAULT;
            RegionV2 responsibleRegion = null;

            for (RegionV2 region : possibleRegions) {
                state = player ? ModUtils.toFabricTriState(permissionData.checkPermission(
                    RegionRuleEnforcer.getPermission(region.key(), rule.getName()))) : TriState.DEFAULT;
                state = !player || state == TriState.DEFAULT ? region.getRule(rule) : state;

                if (state != TriState.DEFAULT) {
                    responsibleRegion = region;
                    break;
                }
            }

            if (state != TriState.DEFAULT) {
                StringTriState stringState = StringTriState.from(state);

                rulesText.append("\n ").append(Text.literal(rule.getName()).formatted(Formatting.AQUA))
                    .append(": ").append(Text.literal(stringState.asString()).formatted(stringState.getFormatting()))
                    .append(" (").append(responsibleRegion.key()).append(")");
            }
        }

        source.sendFeedback(() -> Text.empty()
            .append("Regions:\n")
            .append(Text.literal(possibleRegions.stream()
                .map(RegionV2::key).map(key -> " - " + key)
                .collect(Collectors.joining("\n"))))
            .append("\n\nRules:").append(rulesText), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeList(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        RegionPersistentState regionState = RegionPersistentState.get(source.getServer());

        source.sendFeedback(() -> Text.empty()
            .append(Text.literal("Regions:\n"))
            .append(Text.literal(regionState.getRegions().stream()
                .map(RegionV2::key).map(key -> " - " + key)
                .collect(Collectors.joining("\n")))), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeStats(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        RegionPersistentState regionState = RegionPersistentState.get(source.getServer());

        source.sendFeedback(() -> Text.literal("Cache: ").append(regionState.getCacheStatsText()), false);
        return Command.SINGLE_SUCCESS;
    }

    // Shape commands

    private static int executeStartShape(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        ShapeBuilder builder = ShapeBuilder.start(player);

        if (builder != null) {
            source.sendFeedback(() -> Text.literal("Started building a shape!\nUse ")
                .append(Text.literal("/region shape add").formatted(Formatting.GRAY))
                .append(" to add primitives to this shape, and ")
                .append(Text.literal("/region shape finish").formatted(Formatting.GRAY))
                .append(" to add it to a region."), false);
            return Command.SINGLE_SUCCESS;
        } else {
            throw ALREADY_BUILDING.create();
        }
    }

    private static int executeStopShape(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        ShapeBuilder builder = ShapeBuilder.from(player);

        if (builder != null) {
            builder.finish();
            source.sendFeedback(() -> Text.literal("Canceled shape building"), false);
            return Command.SINGLE_SUCCESS;
        } else {
            throw NOT_CURRENTLY_BUILDING.create();
        }
    }

    private static int executeShapeAddBox(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        RegistryKey<World> dimension = DimensionArgumentType.getDimensionArgument(context, "dimension").getRegistryKey();
        BlockPos min = BlockPosArgumentType.getBlockPos(context, "min");
        BlockPos max = BlockPosArgumentType.getBlockPos(context, "max");
        return addShape(context.getSource(), ProtectionShape.box(dimension, min, max));
    }

    private static int executeShapeAddLocalBox(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        RegistryKey<World> dimension = context.getSource().getWorld().getRegistryKey();
        BlockPos min = BlockPosArgumentType.getBlockPos(context, "min");
        BlockPos max = BlockPosArgumentType.getBlockPos(context, "max");
        return addShape(context.getSource(), ProtectionShape.box(dimension, min, max));
    }

    private static int executeShapeAddDimension(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        RegistryKey<World> dimension = DimensionArgumentType.getDimensionArgument(context, "dimension").getRegistryKey();
        return addShape(context.getSource(), ProtectionShape.dimension(dimension));
    }

    private static int executeShapeAddUniversal(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return addShape(context.getSource(), ProtectionShape.universe());
    }

    private static int addShape(ServerCommandSource source, ProtectionShape shape) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        ShapeBuilder shapeBuilder = ShapeBuilder.from(player);

        if (shapeBuilder != null) {
            shapeBuilder.add(shape);
            source.sendFeedback(() -> Text.literal("Added ").append(shape.display()).append(" to current shape"), false);
        } else {
            throw NOT_CURRENTLY_BUILDING.create();
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeAddShapeToRegion(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        String key = StringArgumentType.getString(context, "name");
        String shapeName = StringArgumentType.getString(context, "shape_name");

        RegionPersistentState regionState = RegionPersistentState.get(source.getServer());
        RegionV2 region = regionState.getRegionByKey(key);

        if (region != null) {
            ShapeBuilder builder = ShapeBuilder.from(player);

            if (builder != null) {
                ProtectionShape shape = builder.finish();

                regionState.replaceRegion(region, region.withAddedShape(shapeName, shape));

                source.sendFeedback(() -> Text.literal("Added shape as '" + shapeName + "' to '" + region.key() + "'"), true);
                return Command.SINGLE_SUCCESS;
            } else {
                throw NOT_CURRENTLY_BUILDING.create();
            }
        } else {
            throw REGION_DOES_NOT_EXIST_EXCEPTION.create(key);
        }
    }

    private static int executeRemoveShapeFromRegion(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        String key = StringArgumentType.getString(context, "name");
        String shapeName = StringArgumentType.getString(context, "shape_name");

        RegionPersistentState regionState = RegionPersistentState.get(source.getServer());
        RegionV2 region = regionState.getRegionByKey(key);

        if (region != null) {
            RegionV2 modifiedRegion = region.withRemovedShape(shapeName);

            if (region == modifiedRegion) {
                throw SHAPE_NOT_FOUND.create(shapeName);
            }

            regionState.replaceRegion(region, modifiedRegion);

            source.sendFeedback(() -> Text.literal("Removed shape '" + shapeName + "' from '" + region.key() + "'"), true);
            return Command.SINGLE_SUCCESS;
        } else {
            throw REGION_DOES_NOT_EXIST_EXCEPTION.create(key);
        }
    }
}
