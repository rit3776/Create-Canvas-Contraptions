package dev.rit3776.canvascontraptions;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.rit3776.canvascontraptions.network.CCNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

@Mod(CanvasContraptions.MODID)
public class CanvasContraptions {
    public static final String MODID = "canvascontraptions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Registrate REGISTRATE = Registrate.create(MODID);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<DraftingPaperCopyRecipe>>
            DRAFTING_PAPER_COPY_SERIALIZER = SERIALIZERS.register("drafting_paper_copy",
                    () -> new SimpleCraftingRecipeSerializer<>(DraftingPaperCopyRecipe::new));

    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB = REGISTRATE.defaultCreativeTab("main",
            builder -> builder.title(Component.translatable("itemGroup.canvascontraptions"))
                    .icon(() -> CCItems.FILLED_DRAFTING_PAPER.asStack())
    ).register();

    public CanvasContraptions(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
        CCDataComponents.register(modEventBus);

        CCBlocks.register();
        CCItems.register();
        CCBlockEntities.register();

        modEventBus.addListener(this::setup);
        modEventBus.addListener(CCNetwork::register);

        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
    }

    private void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        ServerMapManager.clear();
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
