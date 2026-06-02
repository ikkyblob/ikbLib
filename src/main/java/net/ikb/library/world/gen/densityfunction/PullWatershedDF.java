package net.ikb.library.world.gen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public class PullWatershedDF implements SeededDensityFunction {

    private static final MapCodec<PullWatershedDF> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("watersheds").forGetter((input) -> input.cache),
                    Codec.intRange(0,4).optionalFieldOf("mode", 0).forGetter((input) -> input.mode)
            ).apply(instance, (PullWatershedDF::new))
    );

    public static final KeyDispatchDataCodec<PullWatershedDF> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final DensityFunction cache;
    private final CachedWatershedDF cachedWatershed;
    private final int mode;

    public PullWatershedDF(DensityFunction cache, int mode) {
        this.cache = cache;
        this.cachedWatershed = getCachedWatershedDF(cache);
        this.mode = mode;
    }

    @Override
    public double compute(FunctionContext pos) {
        return 0;
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

    private static CachedWatershedDF getCachedWatershedDF(DensityFunction function) {
        return switch (function) {
            case CachedWatershedDF voronoi -> voronoi;
            case DensityFunctions.MarkerOrMarked marker -> getCachedWatershedDF(marker.wrapped());
            case DensityFunctions.HolderHolder(Holder<DensityFunction> holder) -> getCachedWatershedDF(holder.value());
            case null, default ->
                    throw new NullPointerException("Trying to pull from something other than a CachedWatershed");
        };
    }

    @Override
    public PullWatershedDF initialize(long levelSeed) {
        if (!this.cachedWatershed.initialized()) this.cachedWatershed.initialize(levelSeed);
        return this;
    }

    public boolean initialized() {
        return this.cachedWatershed.initialized();
    }


}
