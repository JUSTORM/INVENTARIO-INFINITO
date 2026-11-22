package com.infiniteinv.mixin;

import com.infiniteinv.InfiniteInvMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends HandledScreen<ScreenHandler> {
    public CreativeInventoryScreenMixin(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addInfiniteButton(CallbackInfo ci) {
        int btnX = this.x + 172;
        int btnY = this.y + 92;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("∞"), button -> {
            ClientPlayNetworking.send(new InfiniteInvMod.OpenInfiniteInvPayload());
        })
        .dimensions(btnX, btnY, 18, 18)
        .tooltip(Tooltip.of(Text.literal("Abrir Inventario Infinito (∞ Slots)")))
        .build());
    }
}
