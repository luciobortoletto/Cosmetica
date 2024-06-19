/*
 * Copyright 2022, 2023 EyezahMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.cosmetica.cosmetica.mixin.textures;

import cc.cosmetica.cosmetica.cosmetics.CosmeticaCapes;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player {
	public AbstractClientPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
		super(level, blockPos, f, gameProfile);
	}

	@Shadow
	protected abstract @Nullable PlayerInfo getPlayerInfo();

	@Unique
	private final CosmeticaCapes capeManager = new CosmeticaCapes(this);

	@ModifyReturnValue(at = @At("RETURN"), method = "getSkin")
	private PlayerSkin addCosmeticaCapes(PlayerSkin original) {
		@Nullable PlayerInfo playerInfo = this.getPlayerInfo();

		if (playerInfo != null) {
			GameProfile profile = playerInfo.getProfile();
			@Nullable PlayerSkin modified = this.capeManager.addCosmeticaCapes(profile, original);

			if (modified != null) {
				return modified;
			}
		}
		return original;
	}
}
