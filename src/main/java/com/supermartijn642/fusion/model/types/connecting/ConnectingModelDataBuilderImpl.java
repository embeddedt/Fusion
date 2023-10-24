package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.model.data.ConnectingModelDataBuilder;
import com.supermartijn642.fusion.api.model.data.VanillaModelDataBuilder;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created 02/05/2023 by SuperMartijn642
 */
public class ConnectingModelDataBuilderImpl implements ConnectingModelDataBuilder {

    private final VanillaModelDataBuilder<?,BlockModel> vanillaModel = VanillaModelDataBuilder.builder();
    private final Map<String,List<ConnectionPredicate>> predicates = new HashMap<>();

    @Override
    public ConnectingModelDataBuilder connection(ConnectionPredicate predicate){
        return this.connection("default", predicate);
    }

    @Override
    public ConnectingModelDataBuilder connection(String texture, ConnectionPredicate predicate){
        this.predicates.computeIfAbsent("default", s -> new ArrayList<>()).add(predicate);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder parent(ResourceLocation parent){
        this.vanillaModel.parent(parent);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder texture(String key, String reference){
        this.vanillaModel.texture(key, reference);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder texture(String key, ResourceLocation texture){
        this.vanillaModel.texture(key, texture);
        return this;
    }

    @Override
    public ConnectingModelData build(){
        BlockModel model = this.vanillaModel.build();
        ImmutableMap.Builder<String,ConnectionPredicate> predicates = ImmutableMap.builder();
        this.predicates.entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), DefaultConnectionPredicates.or(entry.getValue().toArray(new ConnectionPredicate[0]))))
            .forEach(pair -> predicates.put(pair.left(), pair.right()));
        return new ConnectingModelDataImpl(model, predicates.build());
    }
}
