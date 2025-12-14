package cn.edu.tju.pvpmagic.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
public class magic_dust extends Item {
    public magic_dust(Properties properties) {
        super(properties);
    }
    @Override
    public InteractionResultHolder<ItemStack>use(Level level,Player player,InteractionHand hand)
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
                if (y<(-64)){
                    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.magic_dust.fail"));
                    return InteractionResultHolder.consume(itemStack);
                }
            }
            serverPlayer.teleportTo(x+0.5,y+1,z+0.5);
            if (!serverPlayer.gameMode.isCreative()){
                itemStack.shrink(1);}
            player.getCooldowns().addCooldown(this,20);
            return InteractionResultHolder.success(itemStack);
        }else{
            return InteractionResultHolder.consume(itemStack);
        }

    }
}
