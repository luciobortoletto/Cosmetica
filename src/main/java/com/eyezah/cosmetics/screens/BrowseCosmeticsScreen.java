package com.eyezah.cosmetics.screens;

import benzenestudios.sulphate.Anchor;
import benzenestudios.sulphate.ExtendedScreen;
import benzenestudios.sulphate.SulphateScreen;
import cc.cosmetica.api.CosmeticType;
import cc.cosmetica.api.CosmeticsPage;
import cc.cosmetica.api.CustomCosmetic;
import com.eyezah.cosmetics.Cosmetica;
import com.eyezah.cosmetics.cosmetics.model.CosmeticStack;
import com.eyezah.cosmetics.screens.widget.CosmeticSelection;
import com.eyezah.cosmetics.screens.widget.FetchingCosmetics;
import com.eyezah.cosmetics.screens.widget.SearchEditBox;
import com.eyezah.cosmetics.screens.widget.TextWidget;
import com.eyezah.cosmetics.utils.LoadState;
import com.eyezah.cosmetics.utils.TextComponents;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BrowseCosmeticsScreen<T extends CustomCosmetic> extends SulphateScreen {
	protected BrowseCosmeticsScreen(@Nullable Screen parent, CosmeticType<T> type, CosmeticStack<T> overrider) {
		super(TextComponents.translatable("cosmetica.selection.select").append(TextComponents.translatable("cosmetica.entry." + getTranslationPart(type))), parent);
		this.type = type;
		this.overrider = overrider;
	}

	private final CosmeticType<T> type;
	private final CosmeticStack<T> overrider;
	private String searchQuery = "";
	private LoadState state = LoadState.LOADING;
	@Nullable
	private CosmeticSelection dataSelection; // null on initial load. Created in the fetcher
	private CosmeticSelection viewSelection; // the display version for funny resize hack
	private FetchingCosmetics<CosmeticsPage<T>> currentFetcher;
	private int page = 1;
	private boolean nextPage;
	private SearchEditBox searchBox;
	private Button proceed;

	private static final Component SEARCH_ELLIPSIS = new TranslatableComponent("cosmetica.selection.search");
	private static final int SEARCH_Y = 32;

	@Override
	protected void addWidgets() {
		this.setAnchorY(Anchor.CENTRE, () -> this.height / 2);
		this.setRows(3);
		this.searchBox = null; // it doesn't exist unless we want it this screen
		this.proceed = null;

		switch (this.state) {
		case RELOADING:
			this.addMainGUI(true);
		case LOADING:
			this.currentFetcher = this.addRenderableWidget(new FetchingCosmetics<>(getTranslationPart(this.type), () -> ImmutableList.of(Cosmetica.api.getRecentCosmetics(this.type, this.page, 8, Optional.ofNullable(this.searchQuery))),
			(fetcher, results) -> {
				if (results.isEmpty()) {
					this.state = LoadState.FAILED;
				}
				else {
					this.dataSelection = new CosmeticSelection(this.minecraft, this, this.type.getUrlString(), this.font, s -> {});
					CosmeticsPage<T> page = results.get(0);

					for (T result : page.getCosmetics()) {
						this.dataSelection.add(result.getName(), result.getId());
					}

					this.nextPage = page.hasNextPage();
					this.state = LoadState.LOADED;
				}

				this.rebuildGUI();
			}));
			this.currentFetcher.y = this.height / 2 - 20;
			this.currentFetcher.x = this.width / 2 - this.currentFetcher.getWidth() / 2;

			if (this.state == LoadState.LOADING) this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 2 + 20, 200, 20, CommonComponents.GUI_CANCEL, b -> this.onClose()));
			break;
		case LOADED:
			this.addMainGUI(false);
			break;
		case FAILED:
			this.addWidget((x, y, w, h, component) -> new TextWidget(x, y, w, h, true, component), TextComponents.translatable("cosmetica.selection.err"));
			this.addButton(TextComponents.translatable("cosmetica.okay"), b -> this.onClose());
			break;
		}
	}

	// hack for keeping items on resizing
	private CosmeticSelection createViewSelection() {
		this.viewSelection = new CosmeticSelection(this.minecraft, this, this.type.getUrlString(), this.font, s -> {if (this.proceed != null) this.proceed.active = true;});
		this.viewSelection.copy(this.dataSelection);
		this.viewSelection.matchSelected(this.dataSelection);
		return this.viewSelection;
	}

	public void resize(Minecraft minecraft, int i, int j) {
		@Nullable Button lastProceed = this.proceed;
		if (this.viewSelection != null) this.dataSelection.matchSelected(this.viewSelection);

		if (this.searchBox == null)
			super.resize(minecraft, i, j);
		else {
			String query = this.searchBox.getValue();
			this.init(minecraft, i, j);
			if (this.searchBox != null) this.searchBox.setValue(query);
		}

		if (lastProceed != null && this.proceed != null) this.proceed.active = lastProceed.active;
	}

	private void addMainGUI(boolean loadEdition) {
		// top
		this.searchBox = this.addRenderableWidget(new SearchEditBox(this.font, this.width / 2 - 100, SEARCH_Y, 200, 20, new TranslatableComponent("cosmetica.selection.search")));
		this.searchBox.setMaxLength(128);
		this.searchBox.setOnEnter(value -> {
			this.setFocused(null);
			this.searchQuery = value;
			this.page = 1;
			this.state = LoadState.RELOADING;
			this.rebuildGUI();
		});

		this.addWidget(this.searchBox);

		if (loadEdition) {
			this.addRenderableOnly(this.createViewSelection());
		}
		else {
			this.addRenderableWidget(this.createViewSelection());
		}

		// bottom
		this.setAnchorY(Anchor.TOP, () -> this.height - 50);

		Button pageBack = this.addButton(100, 20, TextComponents.translatable("cosmetica.selection.pageBack"), b -> {
			this.page--;
			this.state = LoadState.RELOADING;
			this.rebuildGUI();
		});

		if (this.page == 1 || loadEdition) pageBack.active = false;

		Button clear = this.addButton(100, 20, TextComponents.translatable("cosmetica.selection.clear").append(TextComponents.translatable("cosmetica.entry." + ApplyCosmeticsScreen.getTranslationPart(this.type))), b -> {});

		if (loadEdition) clear.active = false;

		Button pageForward = this.addButton(100, 20, TextComponents.translatable("cosmetica.selection.pageForward"), b -> {
			this.page++;
			this.state = LoadState.RELOADING;
			this.rebuildGUI();
		});

		if (!this.nextPage || loadEdition) pageForward.active = false;

		this.addButton(150, 20, CommonComponents.GUI_CANCEL, b -> this.onClose());
		this.proceed = this.addButton(150, 20, TextComponents.translatable("cosmetica.selection.proceed"), b -> this.minecraft.setScreen(new ApplyCosmeticsScreen<T>(this, (PlayerRenderScreen) this.parent, this.type, this.overrider, this.viewSelection.getSelectedId())));
		this.proceed.active = false;
	}

	private void rebuildGUI() {
		if (this.proceed != null) this.proceed.active = false;
		this.resize(this.minecraft, this.width, this.height);
	}

	@Override
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);

		for (Widget widget : ((ExtendedScreen) this).getWidgets()) { // renderables
			widget.render(matrices, mouseX, mouseY, delta);
		}

		drawCenteredString(matrices, this.font, this.title, this.width / 2, 15, 0xFFFFFF); // re-add title

		if (this.searchBox != null) {
			drawString(matrices, this.font, SEARCH_ELLIPSIS, this.width / 2 - 100, SEARCH_Y, 10526880);
			this.searchBox.render(matrices, mouseX, mouseY, delta);
		}

		if (this.state == LoadState.RELOADING) {
			this.fillGradient(matrices, 0, 32 + 25, this.width, this.height - 65 + 4, -1072689136, -804253680);
		}

		if (this.state == LoadState.RELOADING || this.state == LoadState.LOADING) {
			this.currentFetcher.render(matrices, mouseX, mouseY, delta);
		}
	}

	@Override
	public void tick() {
		this.minecraft.getTextureManager().tick();
	}

	private static String getTranslationPart(CosmeticType<?> type) {
		return switch (type.getUrlString()) {
			case "cape" -> "Capes";
			case "hat" -> "Hats";
			case "shoulderbuddy" -> "ShoulderBuddies";
			default -> "BackBlings";
		};
	}
}