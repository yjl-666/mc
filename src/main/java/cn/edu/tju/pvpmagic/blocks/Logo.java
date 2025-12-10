package cn.edu.tju.pvpmagic.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class Logo extends Block {
    public Logo() {
        super(Properties.of().strength(-1.0f, 1.0f)
                .sound(SoundType.WOOD)
                );
    }
}
