package cn.edu.tju.pvpmagic.items;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
    // 存储高光目标的映射表：目标 -> 观看者集合
    private static final Map<UUID, Set<UUID>> TARGET_MARKED_BY = new HashMap<>();

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

        // 在添加新效果前先清理过期效果
        cleanupExpiredGlows(viewer);

        // 计算结束时间
        long endTime = viewer.level().getGameTime() + durationTicks;

        // 存储到观看者映射表
        GLOWING_EFFECTS.computeIfAbsent(viewer.getUUID(), k -> new HashMap<>())
                .put(target.getUUID(), endTime);

        // 存储到目标标记映射表
        TARGET_MARKED_BY.computeIfAbsent(target.getUUID(), k -> new HashSet<>())
                .add(viewer.getUUID());

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

        // 清理目标标记映射表
        Set<UUID> viewers = TARGET_MARKED_BY.get(target.getUUID());
        if (viewers != null) {
            viewers.remove(viewer.getUUID());
            if (viewers.isEmpty()) {
                TARGET_MARKED_BY.remove(target.getUUID());
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
                    // 清理目标标记映射表
                    Set<UUID> viewers = TARGET_MARKED_BY.get(targetId);
                    if (viewers != null) {
                        viewers.remove(viewer.getUUID());
                        if (viewers.isEmpty()) {
                            TARGET_MARKED_BY.remove(targetId);
                        }
                    }
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
                    removePrivateGlowing(viewer, target);
                }
            }
        }
        return false;
    }

    /**
     * 检查目标是否被标记为高光状态
     * @param target 目标玩家
     * @return 是否处于被高光标记状态
     */
    public static boolean isTargetMarked(Player target) {
        if (target == null) return false;
        // 先检查是否有过期的高光
        Set<UUID> viewerIds = TARGET_MARKED_BY.get(target.getUUID());
        if (viewerIds != null) {
            // 移除已过期的观看者
            viewerIds.removeIf(viewerId -> {
                ServerPlayer viewer = (ServerPlayer) target.level().getPlayerByUUID(viewerId);
                if (viewer != null && !hasGlowingEffect(viewer, target)) {
                    return true;
                }
                return false;
            });
            if (viewerIds.isEmpty()) {
                TARGET_MARKED_BY.remove(target.getUUID());
                return false;
            }
        }
        return TARGET_MARKED_BY.containsKey(target.getUUID());
    }

    /**
     * 发送高光数据包
     */
    private static void sendGlowingPacket(ServerPlayer viewer, Entity target, boolean glowing) {
        try {
            // 使用更安全的方法设置高光
            if (glowing) {
                target.setGlowingTag(true);
            } else {
                target.setGlowingTag(false);
            }

            // 获取实体数据，确保不为null
            List<SynchedEntityData.DataValue<?>> dataValues = target.getEntityData().getNonDefaultValues();
            if (dataValues == null) {
                dataValues = List.of(); // 使用空列表作为安全值
            }

            ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(
                    target.getId(),
                    dataValues
            );
            viewer.connection.send(packet);
        } catch (Exception e) {
            // 捕获异常防止崩溃
            System.err.println("发送高光数据包失败: " + e.getMessage());
        }
    }

    /**
     * 清理过期的高光效果
     */
    private static void cleanupExpiredGlows(ServerPlayer viewer) {
        long currentTime = viewer.level().getGameTime();
        Map<UUID, Long> targetMap = GLOWING_EFFECTS.get(viewer.getUUID());

        if (targetMap != null) {
            targetMap.entrySet().removeIf(entry -> {
                if (currentTime >= entry.getValue()) {
                    // 过期，移除高光
                    Player target = viewer.level().getPlayerByUUID(entry.getKey());
                    if (target != null) {
                        // 清理目标标记映射表
                        Set<UUID> viewers = TARGET_MARKED_BY.get(entry.getKey());
                        if (viewers != null) {
                            viewers.remove(viewer.getUUID());
                            if (viewers.isEmpty()) {
                                TARGET_MARKED_BY.remove(entry.getKey());
                            }
                        }
                        // 发送移除数据包
                        try {
                            target.setGlowingTag(false);
                            viewer.connection.send(new ClientboundSetEntityDataPacket(
                                    target.getId(),
                                    target.getEntityData().getNonDefaultValues()
                            ));
                        } catch (Exception e) {
                            System.err.println("清理高光失败: " + e.getMessage());
                        }
                    }
                    return true;
                }
                return false;
            });

            if (targetMap.isEmpty()) {
                GLOWING_EFFECTS.remove(viewer.getUUID());
            }
        }
    }
}