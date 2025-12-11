package cn.edu.tju.pvpmagic.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class Logo extends Item {
    public Logo() {
        super(BlockBehaviour.Properties.of().strength(-1.0f, 1.0f)
                .sound(SoundType.WOOD)
                );
    }
}
