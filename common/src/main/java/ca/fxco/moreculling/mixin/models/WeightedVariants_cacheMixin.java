package ca.fxco.moreculling.mixin.models;

import ca.fxco.moreculling.api.model.BakedOpacity;
import ca.fxco.moreculling.api.quad.QuadOpacity;
import ca.fxco.moreculling.platform.Services;
import ca.fxco.moreculling.utils.BitUtils;
import ca.fxco.moreculling.utils.CullingUtils;
import ca.fxco.moreculling.utils.DirectionUtils;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.WeightedVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(value = WeightedVariants.class, priority = 1010)
public abstract class WeightedVariants_cacheMixin implements BakedOpacity {

    @Unique // Only works on chunk update, so the best performance is after placing a block
    private byte solidFaces = 0; // 0 = all sides translucent

    @Override
    public boolean moreculling$hasTextureTranslucency(@Nullable BlockState blockState, @Nullable Direction direction) {
        if (direction == null) {
            return solidFaces != BitUtils.ALL_DIRECTIONS; // If any translucency, returns true
        }
        return !BitUtils.get(solidFaces, direction.ordinal());
    }

    @Override
    public void moreculling$resetTranslucencyCache(BlockState state) {
        solidFaces = 0;
        for (Direction face : DirectionUtils.DIRECTIONS) {
            List<BakedQuad> quads = Services.PLATFORM.getQuads((BlockStateModel) this, state,
                    face, CullingUtils.RANDOM, EmptyBlockAndTintGetter.INSTANCE, BlockPos.ZERO);
            if (quads.isEmpty()) { // no faces = translucent
                solidFaces = BitUtils.unset(solidFaces, face.ordinal());
            } else {
                solidFaces = BitUtils.set(solidFaces, face.ordinal());
                for (BakedQuad quad : quads) {
                    if (((QuadOpacity)  (Object) quad).moreculling$getTextureTranslucency()) {
                        solidFaces = BitUtils.unset(solidFaces, face.ordinal());
                        break;
                    }
                }
            }
        }
    }
}
