package dev.rit3776.canvascontraptions;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(CanvasContraptions.MODID)
public class CanvasContraptions {
    public static final String MODID = "canvascontraptions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Registrate REGISTRATE = Registrate.create(MODID);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

    public static final RegistryObject<SimpleCraftingRecipeSerializer<DraftingPaperCopyRecipe>>
            DRAFTING_PAPER_COPY_SERIALIZER = SERIALIZERS.register("drafting_paper_copy",
                    () -> new SimpleCraftingRecipeSerializer<>(DraftingPaperCopyRecipe::new));

    public static final RegistryEntry<CreativeModeTab> TAB = REGISTRATE.defaultCreativeTab("main",
            builder -> builder.title(Component.translatable("itemGroup.canvascontraptions"))
                    .icon(() -> CCItems.FILLED_DRAFTING_PAPER.asStack())
                    .displayItems((p, o) -> {
                        o.accept(CCItems.BLANK_DRAFTING_PAPER.get());
                        o.accept(CCItems.FILLED_DRAFTING_PAPER.get());
                        o.accept(CCItems.DRAFTING_TABLET.get());
                    })
    ).register();

    public CanvasContraptions() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        SERIALIZERS.register(modEventBus);

        CCBlocks.register();
        CCItems.register();
        CCBlockEntities.register();

        modEventBus.addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
    }

    private void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        ServerMapManager.clear();
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MovementBehaviour.REGISTRY.register(CCBlocks.PAINTED_BLOCK.get(), new PaintedMovementBehaviour());
            dev.rit3776.canvascontraptions.network.CCNetwork.register();
        });
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MODID, path);
    }
}
