package com.supermartijn642.fusion.predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class NotConnectionPredicate implements ConnectionPredicate {

    public static final Serializer<NotConnectionPredicate> SERIALIZER = new Serializer<NotConnectionPredicate>() {
        @Override
        public NotConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicate") || !json.get("predicate").isJsonObject())
                throw new JsonParseException("Not-predicate must have object property 'predicate'!");
            // Deserialize the predicate
            JsonArray array = json.getAsJsonArray("predicates");
            ConnectionPredicate predicate = FusionPredicateRegistry.deserializeConnectionPredicate(json.getAsJsonObject("predicate"));
            return new NotConnectionPredicate(predicate);
        }

        @Override
        public JsonObject serialize(NotConnectionPredicate value){
            JsonObject json = new JsonObject();
            json.add("predicates", FusionPredicateRegistry.serializeConnectionPredicate(value.predicate));
            return json;
        }
    };

    private final ConnectionPredicate predicate;

    public <T extends ConnectionPredicate> NotConnectionPredicate(T predicate){
        this.predicate = predicate;
    }

    @Override
    public boolean shouldConnect(EnumFacing side, @Nullable IBlockState ownState, IBlockState otherState, IBlockState blockInFront, ConnectionDirection direction){
        return !this.predicate.shouldConnect(side, ownState, otherState, blockInFront, direction);
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}
