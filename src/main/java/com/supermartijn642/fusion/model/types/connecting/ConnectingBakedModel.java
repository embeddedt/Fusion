package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureSprite;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureType;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingBakedModel extends WrappedBakedModel {

    private static final int BLOCK_VERTEX_DATA_UV_OFFSET = findUVOffset(DefaultVertexFormats.BLOCK);
    private static final ModelProperty<SurroundingBlockData> SURROUNDING_BLOCK_DATA_MODEL_PROPERTY = new ModelProperty<>();
    public static final ThreadLocal<Boolean> ignoreModelRenderTypeCheck = ThreadLocal.withInitial(() -> false);

    private final TransformationMatrix modelRotation;
    private final List<ConnectionPredicate> predicates;
    // [cullface][hashcode * 6]
    private final Map<RenderKey,List<BakedQuad>> quadCache = new HashMap<>();
    private final RenderKey mutableKey = new RenderKey(0, null, null);
    private List<RenderType> customRenderTypes;

    public ConnectingBakedModel(IBakedModel original, TransformationMatrix modelRotation, List<ConnectionPredicate> predicates){
        super(original);
        this.modelRotation = modelRotation;
        this.predicates = predicates;
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random random, @Nonnull IModelData modelData){
        SurroundingBlockData data = modelData.hasProperty(SURROUNDING_BLOCK_DATA_MODEL_PROPERTY) ? modelData.getData(SURROUNDING_BLOCK_DATA_MODEL_PROPERTY) : null;
        int hashCode = data == null ? 0 : data.hashCode();

        // Find the current render type
        RenderType renderType = MinecraftForgeClient.getRenderLayer();

        // Get the correct cache and quads
        this.mutableKey.update(hashCode, side, renderType);
        List<BakedQuad> quads;
        synchronized(this.quadCache){
            quads = this.quadCache.get(this.mutableKey);
        }

        // Compute the quads if they don't exist yet
        if(quads == null){
            ignoreModelRenderTypeCheck.set(true);
            boolean isOriginalRenderType = state == null || renderType == null || RenderTypeLookup.canRenderInLayer(state, renderType);
            ignoreModelRenderTypeCheck.set(false);
            quads = this.remapQuads(this.original.getQuads(state, side, random, modelData), data, renderType, isOriginalRenderType);
            synchronized(this.quadCache){
                if(!this.quadCache.containsKey(this.mutableKey)){
                    RenderKey key = new RenderKey(hashCode, side, renderType);
                    this.quadCache.put(key, quads);
                }else
                    quads = this.quadCache.get(this.mutableKey);
            }
        }

        // Safety check even though this should never happen
        if(quads == null)
            throw new IllegalStateException("Tried returning null list from ConnectingBakedModel#getQuads for side '" + side + "'!");

        return quads;
    }

    private List<BakedQuad> remapQuads(List<BakedQuad> originalQuads, SurroundingBlockData surroundingBlocks, RenderType renderType, boolean originalRenderType){
        if(surroundingBlocks == null)
            return originalQuads;
        return originalQuads.stream().map(quad -> this.remapQuad(quad, surroundingBlocks, renderType, originalRenderType)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected BakedQuad remapQuad(BakedQuad quad, SurroundingBlockData surroundingBlocks, RenderType renderType, boolean originalRenderType){
        TextureAtlasSprite sprite = quad.getSprite();
        if(SpriteHelper.getTextureType(sprite) != DefaultTextureTypes.CONNECTING)
            return originalRenderType ? quad : null;

        ConnectingTextureData.RenderType spriteRenderType = ((ConnectingTextureSprite)sprite).getRenderType();
        if(spriteRenderType == null ? !originalRenderType : FusionClient.getRenderTypeMaterial(spriteRenderType) != renderType)
            return null;

        ConnectingTextureLayout layout = ((ConnectingTextureSprite)sprite).getLayout();

        int[] vertexData = quad.getVertices();
        // Make sure we don't change the original quad
        vertexData = Arrays.copyOf(vertexData, vertexData.length);

        // Adjust the uv
        SurroundingBlockData.SideConnections connections = surroundingBlocks.getConnections(quad.getDirection());
        int[] uv = ConnectingTextureType.getStatePosition(layout, connections.top, connections.topRight, connections.right, connections.bottomRight, connections.bottom, connections.bottomLeft, connections.left, connections.topLeft);
        adjustVertexDataUV(vertexData, uv[0], uv[1], sprite);

        // Create a new quad
        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.shouldApplyDiffuseLighting());
    }

    private static int[] adjustVertexDataUV(int[] vertexData, int newU, int newV, TextureAtlasSprite sprite){
        int vertexSize = DefaultVertexFormats.BLOCK.getIntegerSize();
        int vertices = vertexData.length / vertexSize;
        int uvOffset = BLOCK_VERTEX_DATA_UV_OFFSET / 4;

        for(int i = 0; i < vertices; i++){
            int offset = i * vertexSize + uvOffset;

            float width = sprite.getU1() - sprite.getU0();
            float u = Float.intBitsToFloat(vertexData[offset]) + width * newU;
            vertexData[offset] = Float.floatToRawIntBits(u);

            float height = sprite.getV1() - sprite.getV0();
            float v = Float.intBitsToFloat(vertexData[offset + 1]) + height * newV;
            vertexData[offset + 1] = Float.floatToRawIntBits(v);
        }
        return vertexData;
    }

    private static int findUVOffset(VertexFormat vertexFormat){
        int index;
        VertexFormatElement element = null;
        for(index = 0; index < vertexFormat.getElements().size(); index++){
            VertexFormatElement el = vertexFormat.getElements().get(index);
            if(el.getUsage() == VertexFormatElement.Usage.UV){
                element = el;
                break;
            }
        }
        if(index == vertexFormat.getElements().size() || element == null)
            throw new RuntimeException("Expected vertex format to have a UV attribute");
        if(element.getType() != VertexFormatElement.Type.FLOAT)
            throw new RuntimeException("Expected UV attribute to have data type FLOAT");
        if(element.getByteSize() < 4)
            throw new RuntimeException("Expected UV attribute to have at least 4 dimensions");
        return vertexFormat.offsets.getInt(index);
    }

    public SurroundingBlockData getModelData(ILightReader level, BlockPos pos, BlockState state){
        return SurroundingBlockData.create(level, pos, this.modelRotation, this.predicates);
    }

    @Override
    public @Nonnull IModelData getModelData(@Nonnull ILightReader level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData modelData){
        return new ModelDataMap.Builder().withInitial(SURROUNDING_BLOCK_DATA_MODEL_PROPERTY, this.getModelData(level, pos, state)).build();
    }

    public List<RenderType> getCustomRenderTypes(){
        if(this.customRenderTypes == null)
            this.calculateCustomRenderTypes();
        return this.customRenderTypes;
    }

    private void calculateCustomRenderTypes(){
        Set<RenderType> renderTypes = new HashSet<>();
        for(Direction cullFace : new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, null}){
            this.original.getQuads(null, cullFace, new Random(42)).stream()
                .map(BakedQuad::getSprite)
                .filter(sprite -> SpriteHelper.getTextureType(sprite) == DefaultTextureTypes.CONNECTING)
                .map(sprite -> ((ConnectingTextureSprite)sprite).getRenderType())
                .filter(Objects::nonNull)
                .map(FusionClient::getRenderTypeMaterial)
                .forEach(renderTypes::add);
        }
        this.customRenderTypes = Arrays.asList(renderTypes.toArray(new RenderType[0]));
    }

    @Override
    public boolean isCustomRenderer(){
        return super.isCustomRenderer();
    }

    @Override
    public ItemCameraTransforms getTransforms(){
        return super.getTransforms();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return ItemOverrideList.EMPTY;
    }

    private static class RenderKey {
        private int surroundingBlockData;
        private Direction face;
        private RenderType renderType;

        private RenderKey(int surroundingBlockData, Direction face, RenderType renderType){
            this.surroundingBlockData = surroundingBlockData;
            this.face = face;
            this.renderType = renderType;
        }

        void update(int surroundingBlockData, Direction face, RenderType renderType){
            this.surroundingBlockData = surroundingBlockData;
            this.face = face;
            this.renderType = renderType;
        }

        @Override
        public boolean equals(Object o){
            if(this == o) return true;
            if(o == null || this.getClass() != o.getClass()) return false;

            RenderKey renderKey = (RenderKey)o;

            if(this.surroundingBlockData != renderKey.surroundingBlockData) return false;
            if(this.face != renderKey.face) return false;
            return Objects.equals(this.renderType, renderKey.renderType);
        }

        @Override
        public int hashCode(){
            int result = this.surroundingBlockData;
            result = 31 * result + (this.face != null ? this.face.hashCode() : 0);
            result = 31 * result + (this.renderType != null ? this.renderType.hashCode() : 0);
            return result;
        }
    }
}
