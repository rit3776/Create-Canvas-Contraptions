package dev.rit3776.canvascontraptions;

import com.tterrag.registrate.util.entry.ItemEntry;

import static dev.rit3776.canvascontraptions.CanvasContraptions.REGISTRATE;

public class CCItems {
    public static final ItemEntry<BlankDraftingPaperItem> BLANK_DRAFTING_PAPER = REGISTRATE.item("blank_drafting_paper", BlankDraftingPaperItem::new)
            .register();

    public static final ItemEntry<FilledDraftingPaperItem> FILLED_DRAFTING_PAPER = REGISTRATE.item("filled_drafting_paper", FilledDraftingPaperItem::new)
            .properties(p -> p.stacksTo(64))
            .register();

    public static final ItemEntry<DraftingTabletItem> DRAFTING_TABLET = REGISTRATE.item("drafting_tablet", DraftingTabletItem::new)
            .register();

    public static void register() {}
}
