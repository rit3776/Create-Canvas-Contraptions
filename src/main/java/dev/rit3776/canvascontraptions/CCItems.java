package dev.rit3776.canvascontraptions;

import com.tterrag.registrate.util.entry.ItemEntry;

import static dev.rit3776.canvascontraptions.CanvasContraptions.REGISTRATE;

public class CCItems {
    public static final ItemEntry<BlankDraftingPaperItem> BLANK_DRAFTING_PAPER = REGISTRATE
            .item("blank_drafting_paper", BlankDraftingPaperItem::new)
            .model((c, p) -> p.generated(c, CanvasContraptions.asResource("item/" + c.getName())))
            .register();

    public static final ItemEntry<FilledDraftingPaperItem> FILLED_DRAFTING_PAPER = REGISTRATE
            .item("filled_drafting_paper", FilledDraftingPaperItem::new)
            .model((c, p) -> p.generated(c, CanvasContraptions.asResource("item/" + c.getName())))
            .register();

    public static final ItemEntry<DraftingTabletItem> DRAFTING_TABLET = REGISTRATE
            .item("drafting_tablet", DraftingTabletItem::new)
            .model((c, p) -> p.generated(c, CanvasContraptions.asResource("item/" + c.getName())))
            .register();

    public static void register() {}
}
