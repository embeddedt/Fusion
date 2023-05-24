package com.supermartijn642.fusion.model.types.vanilla;

import com.supermartijn642.fusion.api.model.data.VanillaModelDataBuilder;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public class VanillaModelDataBuilderImpl implements VanillaModelDataBuilder<VanillaModelDataBuilderImpl,BlockModel> {

    private final Map<String,String> textures = new HashMap<>();
    private ResourceLocation parent;

    @Override
    public VanillaModelDataBuilderImpl parent(ResourceLocation parent){
        this.parent = parent;
        return this;
    }

    @Override
    public VanillaModelDataBuilderImpl texture(String key, String reference){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");

        // Prepend '#' character
        if(reference.charAt(0) != '#')
            reference = '#' + reference;
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + reference + "'!");

        this.textures.put(key, reference);
        return this;
    }

    @Override
    public VanillaModelDataBuilderImpl texture(String key, ResourceLocation texture){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + texture + "'!");

        this.textures.put(key, texture.toString());
        return this;
    }

    @Override
    public BlockModel build(){
        return new BlockModel(this.parent, Collections.emptyList(), this.textures, false, false, ItemCameraTransforms.NO_TRANSFORMS, Collections.emptyList());
    }
}
