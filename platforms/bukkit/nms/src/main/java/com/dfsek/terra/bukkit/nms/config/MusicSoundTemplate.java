package com.dfsek.terra.bukkit.nms.config;

import com.dfsek.tectonic.api.config.template.annotations.Default;
import com.dfsek.tectonic.api.config.template.annotations.Value;
import com.dfsek.tectonic.api.config.template.object.ObjectTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.attribute.BackgroundMusic;


public class MusicSoundTemplate implements ObjectTemplate<BackgroundMusic> {
    @Value("sound")
    @Default
    private SoundEvent sound = null;

    @Value("min-delay")
    @Default
    private Integer minDelay = null;

    @Value("max-delay")
    @Default
    private Integer maxDelay = null;

    @Value("replace-current-music")
    @Default
    private Boolean replaceCurrentMusic = null;

    @Override
    public BackgroundMusic get() {
        if(sound == null || minDelay == null || maxDelay == null || replaceCurrentMusic == null) {
            return null;
        } else {
            return new BackgroundMusic(new Music(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), minDelay, maxDelay,
                replaceCurrentMusic));
        }
    }
}
