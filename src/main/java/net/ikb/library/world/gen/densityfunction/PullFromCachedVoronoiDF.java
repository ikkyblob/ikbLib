package net.ikb.library.world.gen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public class PullFromCachedVoronoiDF implements SeededDensityFunction {

    private static final MapCodec<PullFromCachedVoronoiDF> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    Codec.intRange(0,5).optionalFieldOf("mode", 0).forGetter((input) -> input.mode),
                    Codec.intRange(1,9).optionalFieldOf("ordinal", 1).forGetter((input) -> input.ordinal),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("cache").forGetter((input) -> input.cache)
            ).apply(instance, (PullFromCachedVoronoiDF::new))
    );

    public static final KeyDispatchDataCodec<PullFromCachedVoronoiDF> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final int mode;
    private final int ordinal;
    private final DensityFunction cache;
    private final CachedVoronoiDF cachedVoronoi;

    public PullFromCachedVoronoiDF(int mode, int ordinal, DensityFunction cache) {
        this.mode = mode;
        this.ordinal = ordinal;
        this.cache = cache;
        this.cachedVoronoi = getCachedVoronoiDF(cache);
    }

    @Override
    public double compute(FunctionContext pos) {
        return switch (mode) {
            default -> this.cachedVoronoi.getDistance(pos, ordinal - 1);
            case 1 -> this.cachedVoronoi.getValue(pos, ordinal - 1);
            case 2 -> this.cachedVoronoi.getVelocity(pos, ordinal - 1);
            case 3 -> this.cachedVoronoi.getPassive(pos, ordinal - 1);
            case 4 -> this.cachedVoronoi.getDirection(pos, ordinal - 1);
            case 5 -> this.cachedVoronoi.getRelDirection(pos, ordinal - 1);
        };
    }

    @Override
    public void fillArray(double[] doubles, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(doubles, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(this);
    }

    @Override
    public double minValue() {
        return Double.MIN_VALUE;
    }

    @Override
    public double maxValue() {
        return Double.MAX_VALUE;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    private static CachedVoronoiDF getCachedVoronoiDF(DensityFunction function) {
        return switch (function) {
            case CachedVoronoiDF voronoi -> voronoi;
            case DensityFunctions.MarkerOrMarked marker -> getCachedVoronoiDF(marker.wrapped());
            case DensityFunctions.HolderHolder(Holder<DensityFunction> holder) -> getCachedVoronoiDF(holder.value());
            case null, default ->
                    throw new NullPointerException("Trying to pull from something other than a CachedVoronoi");
        };
    }

    @Override
    public PullFromCachedVoronoiDF initialize(long levelSeed) {
        if (!this.cachedVoronoi.initialized()) this.cachedVoronoi.initialize(levelSeed);
        return this;
    }

    public boolean initialized() {
        return this.cachedVoronoi.initialized();
    }


}
