package net.ikb.library.world.gen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

import java.util.HashMap;

public class PullCachedCenterDF implements SeededDensityFunction {

    private static final MapCodec<PullCachedCenterDF> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    Codec.intRange(1,9).optionalFieldOf("ordinal", 1).forGetter((input) -> input.ordinal),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("cache").forGetter((input) -> input.cache),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("sampler").forGetter((input) -> input.sampler)
            ).apply(instance, (PullCachedCenterDF::new))
    );

    public static final KeyDispatchDataCodec<PullCachedCenterDF> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final int ordinal;
    private final DensityFunction cache;
    private final CachedVoronoiDF cachedVoronoi;
    private final DensityFunction sampler;

    public PullCachedCenterDF(int ordinal, DensityFunction cache, DensityFunction sampler) {
        this.ordinal = ordinal;
        this.cache = cache;
        this.cachedVoronoi = getCachedVoronoiDF(cache);
        this.sampler = sampler;
    }

    HashMap<Vec3i, double[]> memoSamples = new HashMap<>();

    @Override
    public double compute(FunctionContext pos) {
        Vec3i index = this.cachedVoronoi.getNearest(pos, ordinal - 1).getIndex();
        if (memoSamples.containsKey(index)) {
            double[] val = memoSamples.get(index);
            if (val != null) return val[0];
        }
        double[] val = this.cachedVoronoi.getSamples(pos, ordinal, sampler);
        memoSamples.put(index, val);
        return val[0];
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
    public PullCachedCenterDF initialize(long levelSeed) {
        if (!this.cachedVoronoi.initialized()) this.cachedVoronoi.initialize(levelSeed);
        return this;
    }

    public boolean initialized() {
        return this.cachedVoronoi.initialized();
    }


}
