package com.endersystem.client;

import com.endersystem.EnderSystem;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.SilverfishEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

/** Silverfish model (big, via the scale attribute) with the centipede's own texture. */
public class CentipedeRenderer extends SilverfishEntityRenderer {
    private static final Identifier TEXTURE =
            Identifier.of(EnderSystem.MOD_ID, "textures/entity/desert_centipede.png");

    public CentipedeRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
