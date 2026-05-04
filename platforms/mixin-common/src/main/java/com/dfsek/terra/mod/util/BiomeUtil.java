package com.dfsek.terra.mod.util;

import net.minecraft.registry.Registries;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Builder;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.mod.config.VanillaBiomeProperties;
import com.dfsek.terra.mod.mixin.access.BiomeAccessor;


public class BiomeUtil {
    public static final Map<Identifier, List<Identifier>>
        TERRA_BIOME_MAP = new HashMap<>();

    public static Biome createBiome(Biome vanilla, VanillaBiomeProperties vanillaBiomeProperties) {
        BiomeEffects.Builder effects = new BiomeEffects.Builder();

        net.minecraft.world.biome.Biome.Builder builder = new Builder();
        builder.addEnvironmentAttributes(vanilla.getEnvironmentAttributes());

        effects.waterColor(Objects.requireNonNullElse(vanillaBiomeProperties.getWaterColor(), vanilla.getWaterColor()))
            .grassColorModifier(
                Objects.requireNonNullElse(vanillaBiomeProperties.getGrassColorModifier(), vanilla.getEffects().grassColorModifier()));

        if(vanillaBiomeProperties.getGrassColor() == null) {
            vanilla.getEffects().grassColor().ifPresent(effects::grassColor);
        } else {
            effects.grassColor(vanillaBiomeProperties.getGrassColor());
        }

        if(vanillaBiomeProperties.getFoliageColor() == null) {
            vanilla.getEffects().foliageColor().ifPresent(effects::foliageColor);
        } else {
            effects.foliageColor(vanillaBiomeProperties.getFoliageColor());
        }

        if(vanillaBiomeProperties.getDryFoliageColor() == null) {
            vanilla.getEffects().dryFoliageColor().ifPresent(effects::dryFoliageColor);
        } else {
            effects.dryFoliageColor(vanillaBiomeProperties.getDryFoliageColor());
        }

        if(vanillaBiomeProperties.getFogColor() != null) {
            builder.setEnvironmentAttribute(EnvironmentAttributes.FOG_COLOR_VISUAL, vanillaBiomeProperties.getFogColor());
        }

        if(vanillaBiomeProperties.getWaterFogColor() != null) {
            builder.setEnvironmentAttribute(EnvironmentAttributes.WATER_FOG_COLOR_VISUAL, vanillaBiomeProperties.getWaterFogColor());
        }

        if(vanillaBiomeProperties.getSkyColor() != null) {
            builder.setEnvironmentAttribute(EnvironmentAttributes.SKY_COLOR_VISUAL, vanillaBiomeProperties.getSkyColor());
        }

        if(vanillaBiomeProperties.getParticleConfig() != null) {
            builder.setEnvironmentAttribute(EnvironmentAttributes.AMBIENT_PARTICLES_VISUAL,
                List.of(vanillaBiomeProperties.getParticleConfig()));
        }

        if(vanillaBiomeProperties.getMusic() != null) {
            builder.setEnvironmentAttribute(EnvironmentAttributes.BACKGROUND_MUSIC_AUDIO,
                new BackgroundMusic(vanillaBiomeProperties.getMusic()));
        }

        if(vanillaBiomeProperties.getMusicVolume() != null) {
            builder.setEnvironmentAttribute(EnvironmentAttributes.MUSIC_VOLUME_AUDIO, vanillaBiomeProperties.getMusicVolume());
        }

        if(vanillaBiomeProperties.getLoopSound() != null
           || vanillaBiomeProperties.getMoodSound() != null
           || vanillaBiomeProperties.getAdditionsSound() != null) {
            AmbientSounds ambientSounds = vanilla.getEnvironmentAttributes()
                .apply(EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO, AmbientSounds.DEFAULT);
            Optional<net.minecraft.registry.entry.RegistryEntry<net.minecraft.sound.SoundEvent>> loop = vanillaBiomeProperties.getLoopSound() == null
                ? ambientSounds.loop()
                : Optional.of(Registries.SOUND_EVENT.getEntry(vanillaBiomeProperties.getLoopSound()));
            Optional<net.minecraft.sound.BiomeMoodSound> mood = vanillaBiomeProperties.getMoodSound() == null
                ? ambientSounds.mood()
                : Optional.of(vanillaBiomeProperties.getMoodSound());
            List<net.minecraft.sound.BiomeAdditionsSound> additions = vanillaBiomeProperties.getAdditionsSound() == null
                ? ambientSounds.additions()
                : List.of(vanillaBiomeProperties.getAdditionsSound());
            builder.setEnvironmentAttribute(EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO,
                new AmbientSounds(loop, mood, additions));
        }

        builder.precipitation(Objects.requireNonNullElse(vanillaBiomeProperties.getPrecipitation(), vanilla.hasPrecipitation()));

        builder.temperature(Objects.requireNonNullElse(vanillaBiomeProperties.getTemperature(), vanilla.getTemperature()));

        builder.downfall(Objects.requireNonNullElse(vanillaBiomeProperties.getDownfall(),
            ((BiomeAccessor) ((Object) vanilla)).getWeather().downfall()));

        builder.temperatureModifier(Objects.requireNonNullElse(vanillaBiomeProperties.getTemperatureModifier(),
            ((BiomeAccessor) ((Object) vanilla)).getWeather().temperatureModifier()));

        builder.spawnSettings(Objects.requireNonNullElse(vanillaBiomeProperties.getSpawnSettings(), vanilla.getSpawnSettings()));

        return builder
            .effects(effects.build())
            .generationSettings(new GenerationSettings.Builder().build())
            .build();
    }

    public static String createBiomeID(ConfigPack pack, com.dfsek.terra.api.registry.key.RegistryKey biomeID) {
        return pack.getID()
                   .toLowerCase() + "/" + biomeID.getNamespace().toLowerCase(Locale.ROOT) + "/" + biomeID.getID().toLowerCase(Locale.ROOT);
    }

    public static Map<Identifier, List<Identifier>> getTerraBiomeMap() {
        return Map.copyOf(TERRA_BIOME_MAP);
    }
}
