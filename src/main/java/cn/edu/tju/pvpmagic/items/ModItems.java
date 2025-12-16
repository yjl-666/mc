package cn.edu.tju.pvpmagic.items;

import cn.edu.tju.pvpmagic.Pvpmagic;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pvpmagic.MODID);
    public static final DeferredItem<Item> LOGO = ITEMS.register("logo",
            () -> new Logo(new Item.Properties()));
    public static final DeferredItem<Item> MAGIC_DUST = ITEMS.register("magic_dust",
            () -> new magic_dust(
                    new Item.Properties()
                            .stacksTo(64)
            )
    );
    public static final DeferredItem<Item> FIND_TARGET = ITEMS.register("find_target",
            () -> new find_target(
                    new Item.Properties()
                            .stacksTo(64)
            )
    );
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
