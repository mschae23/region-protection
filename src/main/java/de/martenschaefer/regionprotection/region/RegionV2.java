package de.martenschaefer.regionprotection.region;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.fabricmc.fabric.api.util.TriState;
import de.martenschaefer.config.api.ModConfig;
import de.martenschaefer.regionprotection.region.shape.ProtectionShape;
import de.martenschaefer.regionprotection.region.shape.RegionShapes;
import de.martenschaefer.regionprotection.util.StringTriState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.jetbrains.annotations.NotNull;

public record RegionV2(String key, int level, RegionShapes shapes, Reference2ReferenceOpenHashMap<ProtectionRule, TriState> rules) implements ModConfig<RegionV2>, Comparable<RegionV2> {
    private static final Codec<RegionV2> TYPE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("key").forGetter(region -> region.key),
        Codec.INT.fieldOf("level").forGetter(region -> region.level),
        RegionShapes.CODEC.fieldOf("shapes").forGetter(region -> region.shapes),
        ProtectionRule.Entry.CODEC.listOf().fieldOf("rules").forGetter(RegionV2::rulesAsList)
    ).apply(instance, RegionV2::new));

    public static final ModConfig.Type<RegionV2, RegionV2> TYPE = new ModConfig.Type<>(2, TYPE_CODEC);

    public static final Codec<ModConfig<RegionV2>> REGION_CODEC = ModConfig.createCodec(RegionV2.TYPE.version(), RegionV2::getRegionType);

    public RegionV2(String key, int level, RegionShapes shapes, List<ProtectionRule.Entry> defaults) {
        this(key, level, shapes, new Reference2ReferenceOpenHashMap<>(defaults.stream().collect(
            Collectors.toMap(ProtectionRule.Entry::rule, ProtectionRule.Entry::value))));
    }

    public RegionV2 withAddedShape(String name, ProtectionShape shape) {
        var newShapes = this.shapes.withShape(name, shape);
        return new RegionV2(this.key, this.level, newShapes, this.rules);
    }

    public RegionV2 withReplacedShape(String name, ProtectionShape shape) {
        RegionShapes shapes = this.shapes.withShapeReplaced(name, shape);
        return new RegionV2(this.key, this.level, shapes, this.rules);
    }

    public RegionV2 withRemovedShape(String name) {
        var newShapes = this.shapes.removeShape(name);

        if (this.shapes == newShapes) {
            return this;
        }

        return new RegionV2(this.key, this.level, newShapes, this.rules);
    }

    public RegionV2 withLevel(int level) {
        return new RegionV2(this.key, level, this.shapes, this.rules);
    }

    public RegionV2 withRule(ProtectionRule rule, TriState value) {
        Reference2ReferenceOpenHashMap<ProtectionRule, TriState> rules = new Reference2ReferenceOpenHashMap<>(this.rules);
        rules.put(rule, value);

        return new RegionV2(this.key, this.level, this.shapes, rules);
    }

    public TriState getRule(ProtectionRule rule) {
        return this.rules.getOrDefault(rule, TriState.DEFAULT);
    }

    private List<ProtectionRule.Entry> rulesAsList() {
        return this.rules.entrySet().stream().map(entry -> new ProtectionRule.Entry(entry.getKey(), entry.getValue())).toList();
    }

    public Text rulesForDisplay() {
        if (this.rules.isEmpty()) {
            return Text.literal("  Empty").formatted(Formatting.YELLOW);
        }

        return this.rules.entrySet().stream().filter(entry -> entry.getValue() != TriState.DEFAULT)
            .map(entry -> {
                StringTriState state = StringTriState.from(entry.getValue());

                return Text.literal("  ").append(Text.literal(entry.getKey().asString()).formatted(Formatting.AQUA))
                    .append(": ")
                    .append(Text.literal(state.asString()).formatted(state.getFormatting()));
            }).reduce((a, b) -> a.append("\n").append(b)).orElseGet(Text::empty);
    }

    @Override
    public int compareTo(@NotNull RegionV2 o) {
        int levelCompare = Integer.compare(o.level, this.level);

        if (levelCompare != 0) {
            return levelCompare;
        } else {
            return this.key.compareTo(o.key);
        }
    }

    public static RegionV2 create(String key) {
        return new RegionV2(key, 0, new RegionShapes(), new Reference2ReferenceOpenHashMap<>());
    }

    @Override
    public Type<RegionV2, ?> type() {
        return TYPE;
    }

    @Override
    public RegionV2 latest() {
        return this;
    }

    @Override
    public boolean shouldUpdate() {
        return true;
    }

    private static ModConfig.Type<RegionV2, ?> getRegionType(int version) {
        return RegionV2.TYPE;
    }
}
