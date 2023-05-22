package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.model.FusionBlockModel;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Function;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
@Mixin(value = BlockModel.class, priority = 900)
public class BlockModelMixin implements BlockModelExtension {

    @Unique
    private ModelInstance<?> fusionModel;

    @ModifyVariable(
        method = "getMaterials(Ljava/util/function/Function;Ljava/util/Set;)Ljava/util/Collection;",
        at = @At("HEAD"),
        ordinal = 0
    )
    private Function<ResourceLocation,IUnbakedModel> adjustModelGetter(Function<ResourceLocation,IUnbakedModel> modelGetter){
        return location -> {
            IUnbakedModel model = modelGetter.apply(location);
            if(model instanceof FusionBlockModel)
                return ((FusionBlockModel)model).hasVanillaModel() ? ((FusionBlockModel)model).getVanillaModel() : FusionBlockModel.DUMMY_MODEL;
            return model;
        };
    }

    @Override
    public ModelInstance<?> getFusionModel(){
        return this.fusionModel;
    }

    @Override
    public void setFusionModel(ModelInstance<?> fusionModel){
        this.fusionModel = fusionModel;
    }
}
