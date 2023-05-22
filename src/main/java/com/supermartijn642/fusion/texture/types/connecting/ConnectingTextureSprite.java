package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ConnectingTextureSprite extends TextureAtlasSprite {

    private final ConnectingTextureLayout layout;

    protected ConnectingTextureSprite(TextureAtlasSprite original, ConnectingTextureLayout layout){
        super(
            original.atlas(),
            new Info(original.getName(), original.getWidth(), original.getHeight(), AnimationMetadataSection.EMPTY),
            0,
            1,
            1,
            original.x,
            original.y,
            new NativeImage(NativeImage.PixelFormat.RGBA, 1, 1, true, 0)
        );
        this.layout = layout;
        this.mainImage = original.mainImage;
        this.u0 = original.u0;
        this.u1 = original.u1;
        this.v0 = original.v0;
        this.v1 = original.v1;
    }

    public ConnectingTextureLayout getLayout(){
        return this.layout;
    }
}
