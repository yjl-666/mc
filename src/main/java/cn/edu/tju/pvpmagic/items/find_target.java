package cn.edu.tju.pvpmagic.items;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class find_target extends Item {
    // 存储高光效果的映射表：观看者 -> (目标 -> 结束时间戳)
    private static final Map<UUID, Map<UUID, Long>> GLOWING_EFFECTS = new HashMap<>();

    public find_target(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack=player.getItemInHand(hand);
        if(level.isClientSide())
        {
            return InteractionResultHolder.consume(itemStack);

        }
        ServerPlayer serverPlayer = (ServerPlayer) player;

        // 1. 获取100格范围内的所有玩家
        List<Player> nearbyPlayers = level.players().stream()
                .filter(p -> p != player)  // 排除自己
                .filter(p -> player.distanceTo(p) <= 100)
                .collect(Collectors.toList());
        if (nearbyPlayers.isEmpty()) {
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 40);
            return InteractionResultHolder.success(itemStack);
        }
        int time=400;
        for(Player p:nearbyPlayers )
        {
            addPrivateGlowing(serverPlayer,p,time);
        }


        // 7. 消耗物品和冷却
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.success(itemStack);
    }

    /**
     * 添加只有指定玩家可见的高光效果
     * @param viewer 观看者（只有他能看到高光）
     * @param target 目标实体
     * @param durationTicks 持续时间（tick）
     */
    public static void addPrivateGlowing(ServerPlayer viewer, Player target, int durationTicks) {
        if (viewer == null || target == null) return;

        // 计算结束时间
        long endTime = viewer.level().getGameTime() + durationTicks;

        // 存储到映射表
        GLOWING_EFFECTS.computeIfAbsent(viewer.getUUID(), k -> new HashMap<>())
                .put(target.getUUID(), endTime);

        // 发送高光数据包给观看者
        sendGlowingPacket(viewer, target, true);
    }

    /**
     * 为指定玩家移除对特定目标的高光效果
     * @param viewer 观看者
     * @param target 目标实体
     */
    public static void removePrivateGlowing(ServerPlayer viewer, Player target) {
        if (viewer == null || target == null) return;

        Map<UUID, Long> targetMap = GLOWING_EFFECTS.get(viewer.getUUID());
        if (targetMap != null) {
            targetMap.remove(target.getUUID());
            if (targetMap.isEmpty()) {
                GLOWING_EFFECTS.remove(viewer.getUUID());
            }
        }

        // 发送移除高光的数据包
        sendGlowingPacket(viewer, target, false);
    }

    /**
     * 清除指定玩家的所有高光效果
     * @param viewer 观看者
     */
    public static void clearAllGlowing(ServerPlayer viewer) {
        if (viewer == null) return;

        Map<UUID, Long> targetMap = GLOWING_EFFECTS.get(viewer.getUUID());
        if (targetMap != null) {
            // 遍历所有目标并发送移除数据包
            for (UUID targetId : targetMap.keySet()) {
                Player target = viewer.server.getPlayerList().getPlayer(targetId);
                if (target != null) {
                    sendGlowingPacket(viewer, target, false);
                }
            }
            GLOWING_EFFECTS.remove(viewer.getUUID());
        }
    }

    /**
     * 检查玩家是否有对特定目标的高光效果
     * @param viewer 观看者
     * @param target 目标实体
     * @return 是否仍有高光效果
     */
    public static boolean hasGlowingEffect(ServerPlayer viewer, Player target) {
        if (viewer == null || target == null) return false;

        Map<UUID, Long> targetMap = GLOWING_EFFECTS.get(viewer.getUUID());
        if (targetMap != null) {
            Long endTime = targetMap.get(target.getUUID());
            if (endTime != null) {
                long currentTime = viewer.level().getGameTime();
                if (currentTime < endTime) {
                    return true;
                } else {
                    // 超时，移除
                    targetMap.remove(target.getUUID());
                }
            }
        }
        return false;
    }

    /**
     * 发送高光数据包
     */
    private static void sendGlowingPacket(ServerPlayer viewer, Entity target, boolean glowing) {
        SynchedEntityData entityData = target.getEntityData();
        EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BYTE);

        byte flags = entityData.get(DATA_SHARED_FLAGS_ID);
        if (glowing) {
            flags |= 0x40; // 设置发光标志位
        } else {
            flags &= ~0x40; // 清除发光标志位
        }

        // 创建并发送数据包
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(
                target.getId(),
                entityData.packDirty()
        );
        viewer.connection.send(packet);
    }
    private static final Map<UUID, Map<UUID, Long>> GLOWING_TIMERS = new HashMap<>();

    private static void scheduleGlowRemoval(ServerPlayer viewer, Player target, int durationTicks) {
        // 直接存储过期时间，不执行延迟任务
    }

    // 在每个玩家更新时检查高光过期（添加这个方法）
    public static void checkGlowExpiry(Player player) {
        if (player.level().isClientSide()) return;

        long currentTime = player.level().getGameTime();

        // 检查该玩家作为观看者的高光是否过期
        Map<UUID, Long> targetMap = GLOWING_EFFECTS.get(player.getUUID());
        if (targetMap != null) {
            Iterator<Map.Entry<UUID, Long>> iterator = targetMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Long> entry = iterator.next();
                if (currentTime >= entry.getValue()) {
                    // 高光过期
                    Player target = player.level().getPlayerByUUID(entry.getKey());
                    if (target != null) {
                        removePrivateGlowing((ServerPlayer) player, target);
                    }
                    iterator.remove();
                }
            }
            if (targetMap.isEmpty()) {
                GLOWING_EFFECTS.remove(player.getUUID());
            }
        }
    }
}
// 在其他地方（如玩家tick事件）调用checkGlowExpiry
//添加高光效果：addPrivateGlowing(serverPlayer, target, duration)
//提前移除单个目标的高光：removePrivateGlowing(serverPlayer, target)
//清除玩家的所有高光效果：clearAllGlowing(serverPlayer)
//检查高光状态：hasGlowingEffect(serverPlayer, target)

