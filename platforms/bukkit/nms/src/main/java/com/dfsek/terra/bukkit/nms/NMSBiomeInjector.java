package com.dfsek.terra.bukkit.nms;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.bukkit.nms.config.VanillaBiomeProperties;


public class NMSBiomeInjector {

    public static <T> Optional<Holder<T>> getEntry(Registry<T> registry, Identifier identifier) {
        return registry.getOptional(identifier)
            .flatMap(registry::getResourceKey)
            .flatMap(registry::get);
    }

    public static Biome createBiome(Biome vanilla, VanillaBiomeProperties vanillaBiomeProperties)
    throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Biome.BiomeBuilder builder = new Biome.BiomeBuilder();
        builder.putAttributes(vanilla.getAttributes());

        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder();

        effects.waterColor(Objects.requireNonNullElse(vanillaBiomeProperties.getWaterColor(), vanilla.getWaterColor()))
            .grassColorModifier(Objects.requireNonNullElse(vanillaBiomeProperties.getGrassColorModifier(),
                vanilla.getSpecialEffects().grassColorModifier()));

        if(vanillaBiomeProperties.getGrassColor() == null) {
            vanilla.getSpecialEffects().grassColorOverride().ifPresent(effects::grassColorOverride);
        } else {
            effects.grassColorOverride(vanillaBiomeProperties.getGrassColor());
        }

        if(vanillaBiomeProperties.getFoliageColor() == null) {
            vanilla.getSpecialEffects().foliageColorOverride().ifPresent(effects::foliageColorOverride);
        } else {
            effects.foliageColorOverride(vanillaBiomeProperties.getFoliageColor());
        }

        if(vanillaBiomeProperties.getDryFoliageColor() == null) {
            vanilla.getSpecialEffects().dryFoliageColorOverride().ifPresent(effects::dryFoliageColorOverride);
        } else {
            effects.dryFoliageColorOverride(vanillaBiomeProperties.getDryFoliageColor());
        }

        if(vanillaBiomeProperties.getFogColor() != null) {
            builder.setAttribute(EnvironmentAttributes.FOG_COLOR, vanillaBiomeProperties.getFogColor());
        }

        if(vanillaBiomeProperties.getWaterFogColor() != null) {
            builder.setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, vanillaBiomeProperties.getWaterFogColor());
        }

        if(vanillaBiomeProperties.getSkyColor() != null) {
            builder.setAttribute(EnvironmentAttributes.SKY_COLOR, vanillaBiomeProperties.getSkyColor());
        }

        if(vanillaBiomeProperties.getParticleConfig() != null) {
            builder.setAttribute(EnvironmentAttributes.AMBIENT_PARTICLES, List.of(vanillaBiomeProperties.getParticleConfig()));
        }

        if(vanillaBiomeProperties.getMusic() != null) {
            builder.setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, vanillaBiomeProperties.getMusic());
        }

        if(vanillaBiomeProperties.getMusicVolume() != null) {
            builder.setAttribute(EnvironmentAttributes.MUSIC_VOLUME, vanillaBiomeProperties.getMusicVolume());
        }

        if(vanillaBiomeProperties.getLoopSound() != null
           || vanillaBiomeProperties.getMoodSound() != null
           || vanillaBiomeProperties.getAdditionsSound() != null) {
            AmbientSounds currentAmbientSounds = vanilla.getAttributes()
                .applyModifier(EnvironmentAttributes.AMBIENT_SOUNDS, EnvironmentAttributes.AMBIENT_SOUNDS.defaultValue());

            Optional<Holder<net.minecraft.sounds.SoundEvent>> loopSound = vanillaBiomeProperties.getLoopSound() == null
                ? currentAmbientSounds.loop()
                : Optional.of(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(vanillaBiomeProperties.getLoopSound()));
            Optional<AmbientMoodSettings> moodSound = vanillaBiomeProperties.getMoodSound() == null
                ? currentAmbientSounds.mood()
                : Optional.of(vanillaBiomeProperties.getMoodSound());

            List<AmbientAdditionsSettings> additions = new ArrayList<>(currentAmbientSounds.additions());
            if(vanillaBiomeProperties.getAdditionsSound() != null) {
                additions = List.of(vanillaBiomeProperties.getAdditionsSound());
            }

            builder.setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, new AmbientSounds(loopSound, moodSound, additions));
        }

        builder.hasPrecipitation(Objects.requireNonNullElse(vanillaBiomeProperties.getPrecipitation(), vanilla.hasPrecipitation()));

        builder.temperature(Objects.requireNonNullElse(vanillaBiomeProperties.getTemperature(), vanilla.getBaseTemperature()));

        builder.downfall(Objects.requireNonNullElse(vanillaBiomeProperties.getDownfall(), vanilla.climateSettings.downfall()));

        builder.temperatureAdjustment(
            Objects.requireNonNullElse(vanillaBiomeProperties.getTemperatureModifier(), vanilla.climateSettings.temperatureModifier()));

        builder.mobSpawnSettings(Objects.requireNonNullElse(vanillaBiomeProperties.getSpawnSettings(), vanilla.getMobSettings()));

        return builder
            .specialEffects(effects.build())
            .generationSettings(new BiomeGenerationSettings.PlainBuilder().build())
            .build();
    }

    public static String createBiomeID(ConfigPack pack, com.dfsek.terra.api.registry.key.RegistryKey biomeID) {
        return pack.getID()
                   .toLowerCase() + "/" + biomeID.getNamespace().toLowerCase(Locale.ROOT) + "/" + biomeID.getID().toLowerCase(Locale.ROOT);
    }
}
