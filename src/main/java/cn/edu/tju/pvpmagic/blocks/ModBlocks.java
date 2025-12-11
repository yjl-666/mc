package cn.edu.tju.pvpmagic.blocks;


import cn.edu.tju.pvpmagic.Pvpmagic;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Pvpmagic.MODID);
    public static void register(IEventBus eventBus) {
        ModBlocks.BLOCKS.register(eventBus);
    }
    public static final DeferredBlock<Block> LOGO = BLOCKS.registerSimpleBlock("logo", new Logo().properties());
}
