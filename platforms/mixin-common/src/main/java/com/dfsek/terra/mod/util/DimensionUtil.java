package com.dfsek.terra.mod.util;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionType.MonsterSettings;
import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.mod.ModPlatform;
import com.dfsek.terra.mod.config.MonsterSettingsConfig;
import com.dfsek.terra.mod.config.VanillaWorldProperties;
import com.dfsek.terra.mod.implmentation.TerraIntProvider;


public class DimensionUtil {
    public static DimensionType createDimension(VanillaWorldProperties vanillaWorldProperties, DimensionType defaultDimension,
                                                ModPlatform platform) {
        MonsterSettingsConfig monsterSettingsConfig;
        if(vanillaWorldProperties.getMonsterSettings() != null) {
            monsterSettingsConfig = vanillaWorldProperties.getMonsterSettings();
        } else {
            monsterSettingsConfig = new MonsterSettingsConfig();
        }

        EnvironmentAttributeMap.Builder attributes = EnvironmentAttributeMap.builder().addAll(defaultDimension.attributes());
        applyDimensionAttributes(vanillaWorldProperties, monsterSettingsConfig, attributes);
        MonsterSettings monsterSettings = getMonsterSettings(defaultDimension, monsterSettingsConfig);

        return new DimensionType(
            defaultDimension.hasFixedTime(),
            vanillaWorldProperties.getHasSkyLight() == null ? defaultDimension.hasSkyLight() : vanillaWorldProperties.getHasSkyLight(),
            vanillaWorldProperties.getHasCeiling() == null ? defaultDimension.hasCeiling() : vanillaWorldProperties.getHasCeiling(),
            vanillaWorldProperties.getCoordinateScale() == null
            ? defaultDimension.coordinateScale()
            : vanillaWorldProperties.getCoordinateScale(),
            vanillaWorldProperties.getHeight() == null ? defaultDimension.minY() : vanillaWorldProperties.getHeight().getMin(),
            vanillaWorldProperties.getHeight() == null ? defaultDimension.height() : vanillaWorldProperties.getHeight().getRange(),
            vanillaWorldProperties.getLogicalHeight() == null
            ? defaultDimension.logicalHeight()
            : vanillaWorldProperties.getLogicalHeight(),
            vanillaWorldProperties.getInfiniburn() == null
            ? defaultDimension.infiniburn()
            : TagKey.of(RegistryKeys.BLOCK, vanillaWorldProperties.getInfiniburn()),
            vanillaWorldProperties.getAmbientLight() == null ? defaultDimension.ambientLight() : vanillaWorldProperties.getAmbientLight(),
            monsterSettings,
            getSkybox(vanillaWorldProperties.getEffects(), defaultDimension),
            getCardinalLightType(vanillaWorldProperties.getEffects(), defaultDimension),
            attributes.build(),
            defaultDimension.timelines()
        );
    }

    private static void applyDimensionAttributes(VanillaWorldProperties vanillaWorldProperties,
                                                 MonsterSettingsConfig monsterSettingsConfig,
                                                 EnvironmentAttributeMap.Builder attributes) {
        if(vanillaWorldProperties.getBedWorks() != null) {
            attributes.with(EnvironmentAttributes.BED_RULE_GAMEPLAY,
                vanillaWorldProperties.getBedWorks() ? BedRule.OVERWORLD : BedRule.OTHER_DIMENSION);
        }

        if(vanillaWorldProperties.getRespawnAnchorWorks() != null) {
            attributes.with(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY,
                vanillaWorldProperties.getRespawnAnchorWorks());
        }

        if(vanillaWorldProperties.getCloudHeight() != null) {
            attributes.with(EnvironmentAttributes.CLOUD_HEIGHT_VISUAL,
                vanillaWorldProperties.getCloudHeight().floatValue());
        }

        if(monsterSettingsConfig.getHasRaids() != null) {
            attributes.with(EnvironmentAttributes.CAN_START_RAID_GAMEPLAY,
                monsterSettingsConfig.getHasRaids());
        }

        if(monsterSettingsConfig.getPiglinSafe() != null) {
            attributes.with(EnvironmentAttributes.PIGLINS_ZOMBIFY_GAMEPLAY,
                !monsterSettingsConfig.getPiglinSafe());
        }
    }

    private static DimensionType.Skybox getSkybox(Identifier effects, DimensionType defaultDimension) {
        if(effects == null) {
            return defaultDimension.skybox();
        }

        if(Identifier.ofVanilla("overworld").equals(effects)) {
            return DimensionType.Skybox.OVERWORLD;
        }

        if(Identifier.ofVanilla("the_end").equals(effects)) {
            return DimensionType.Skybox.END;
        }

        if(Identifier.ofVanilla("the_nether").equals(effects)) {
            return DimensionType.Skybox.NONE;
        }

        return defaultDimension.skybox();
    }

    private static DimensionType.CardinalLightType getCardinalLightType(Identifier effects, DimensionType defaultDimension) {
        if(Identifier.ofVanilla("the_nether").equals(effects)) {
            return DimensionType.CardinalLightType.NETHER;
        }

        return defaultDimension.cardinalLightType();
    }

    @NotNull
    private static MonsterSettings getMonsterSettings(DimensionType defaultDimension, MonsterSettingsConfig monsterSettingsConfig) {
        MonsterSettings defaultMonsterSettings = defaultDimension.monsterSettings();

        return new MonsterSettings(
            monsterSettingsConfig.getMonsterSpawnLight() == null ? defaultMonsterSettings.monsterSpawnLightTest() : new TerraIntProvider(
                monsterSettingsConfig.getMonsterSpawnLight()),
            monsterSettingsConfig.getMonsterSpawnBlockLightLimit() == null
            ? defaultMonsterSettings.monsterSpawnBlockLightLimit()
            : monsterSettingsConfig.getMonsterSpawnBlockLightLimit()
        );
    }
}
