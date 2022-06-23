package com.eyezah.cosmetics.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ScrollableArea extends ContainerObjectSelectionList<ScrollableArea.Entry> {
	public ScrollableArea(Minecraft minecraft, int width, int height, int y0, int y1) {
		super(minecraft, width, height, y0, y1, 20); // last param: item height
	}

	public ScrollableArea(Minecraft minecraft, Screen parent, int spacing) {
		super(minecraft, parent.width, parent.height, 32, parent.height - 32, spacing);
	}

	public ScrollableArea(Minecraft minecraft, Screen parent) {
		this(minecraft, parent, 20);
	}

	public void addButton(int width, Component text, Button.OnPress callback) {
		this.addEntry(new Entry(width, text, callback));
	}

	class Entry extends ContainerObjectSelectionList.Entry<ScrollableArea.Entry> {
		Entry(int width, Component text, Button.OnPress callback) {
			this.button = new Button(0, 0, width, 20, text, callback);
		}

		private final Button button;

		@Override
		public List<? extends NarratableEntry> narratables() {
			return ImmutableList.of(this.button);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return ImmutableList.of(this.button);
		}

		@Override
		public void render(PoseStack poseStack, int i, int y, int k, int l, int m, int passMe1, int passMe2, boolean bl, float passMe3) {
			this.button.x = ScrollableArea.this.width / 2 - this.button.getWidth() / 2;
			this.button.y = y;
			this.button.render(poseStack, passMe1, passMe2, passMe3);
		}

		@Override
		public boolean mouseClicked(double d, double e, int i) {
			return this.button.mouseClicked(d, e, i);
		}
	}
}
