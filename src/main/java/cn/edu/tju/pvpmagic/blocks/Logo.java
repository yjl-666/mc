package cn.edu.tju.pvpmagic.blocks;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;

public class Logo extends Block{
    public Logo() {
        super(Properties.of().sound(SoundType.WOOD)
                .strength(2.0f, 3.0f)
                .mapColor(MapColor.WOOD)
                .requiresCorrectToolForDrops());
    }

}
