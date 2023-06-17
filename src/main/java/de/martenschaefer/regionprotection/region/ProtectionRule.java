package de.martenschaefer.regionprotection.region;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.util.StringIdentifiable;
import net.fabricmc.fabric.api.util.TriState;
import de.martenschaefer.regionprotection.util.RegionProtectionCodecs;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

public enum ProtectionRule implements StringIdentifiable {
    BlockBreak("block.break"),
    BlockPlace("block.place"),
    BlockUse("block.use"),
    ItemUse("item.use"),
    WorldModify("world.modify"),
    PortalNetherUse("portal.nether.use"),
    PortalEndUse("portal.end.use"),
    VillagerWork("villager.work"),
    VillagerHome("villager.home"),
    PlayerPvp("player.pvp"),
    ExplosionIgnite("explosion.ignite"),
    ExplosionDestroy("explosion.destroy");

    public static final com.mojang.serialization.Codec<ProtectionRule> CODEC = StringIdentifiable.createCodec(ProtectionRule::values);
    private static final Map<String, ProtectionRule> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(ProtectionRule::asString, Function.identity()));

    private final String name;

    ProtectionRule(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String asString() {
        return this.name;
    }

    @Nullable
    public static ProtectionRule byName(String name) {
        return BY_NAME.get(name);
    }

    public record Entry(ProtectionRule rule, TriState value) {
        public static final com.mojang.serialization.Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ProtectionRule.CODEC.fieldOf("rule").forGetter(Entry::rule),
            RegionProtectionCodecs.COMPACT_TRISTATE_CODEC.fieldOf("value").forGetter(Entry::value)
        ).apply(instance, instance.stable(Entry::new)));
    }
}
