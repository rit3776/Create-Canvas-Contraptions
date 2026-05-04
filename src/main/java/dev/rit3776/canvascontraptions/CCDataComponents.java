package dev.rit3776.canvascontraptions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public class CCDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = 
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CanvasContraptions.MODID);

    public record DraftingLayout(List<Integer> mapIds, int width, int height, String fileName, int selectedIndex) {
        public static final Codec<DraftingLayout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.listOf().fieldOf("map_ids").forGetter(DraftingLayout::mapIds),
            Codec.INT.fieldOf("width").forGetter(DraftingLayout::width),
            Codec.INT.fieldOf("height").forGetter(DraftingLayout::height),
            Codec.STRING.fieldOf("file_name").forGetter(DraftingLayout::fileName),
            Codec.INT.fieldOf("selected_index").forGetter(DraftingLayout::selectedIndex)
        ).apply(instance, DraftingLayout::new));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DraftingLayout> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), DraftingLayout::mapIds,
            ByteBufCodecs.VAR_INT, DraftingLayout::width,
            ByteBufCodecs.VAR_INT, DraftingLayout::height,
            ByteBufCodecs.STRING_UTF8, DraftingLayout::fileName,
            ByteBufCodecs.VAR_INT, DraftingLayout::selectedIndex,
            DraftingLayout::new
        );
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DraftingLayout>> DRAFTING_LAYOUT = 
            COMPONENTS.register("drafting_layout", () -> DataComponentType.<DraftingLayout>builder()
                    .persistent(DraftingLayout.CODEC)
                    .networkSynchronized(DraftingLayout.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<DraftingLayout>>> TABLET_LIBRARY = 
            COMPONENTS.register("tablet_library", () -> DataComponentType.<List<DraftingLayout>>builder()
                    .persistent(DraftingLayout.CODEC.listOf())
                    .networkSynchronized(DraftingLayout.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
