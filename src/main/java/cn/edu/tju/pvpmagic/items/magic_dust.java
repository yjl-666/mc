package cn.edu.tju.pvpmagic.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class magic_dust extends Item {
    public magic_dust(Properties properties) {
        super(properties);
    }
    @Override
    public @NotNull InteractionResultHolder<ItemStack>use(Level level, Player player, @NotNull InteractionHand hand)
    {
        ItemStack itemStack=player.getItemInHand(hand);
        if(!level.isClientSide())
        {
            ServerPlayer serverPlayer=(ServerPlayer)player;
            BlockPos blockPos=serverPlayer.blockPosition();
            int x=blockPos.getX();
            int z=blockPos.getZ();
            int y=255;
            for(;;y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!level.isEmptyBlock(pos)) {
                    break;
                }
            }
            serverPlayer.teleportTo(x+0.5,y+1,z+0.5);
            itemStack.shrink(1);
            player.getCooldowns().addCooldown(this,20);
            return InteractionResultHolder.success(itemStack);
        }else{
            return InteractionResultHolder.consume(itemStack);
        }

    }
}