package net.ikb.library.world.gen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public class NearestSeaDF implements SeededDensityFunction {

    private static final MapCodec<NearestSeaDF> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("cache").forGetter((input) -> input.cache),
                    Codec.doubleRange(0,4).optionalFieldOf("peak", 0.25).forGetter((input) -> input.peak),
                    Codec.doubleRange(0,1).optionalFieldOf("cont_bias",0.5).forGetter((input) -> input.contBias),
                    Codec.BOOL.optionalFieldOf("distance", true).forGetter((input) -> input.distanceMode)
            ).apply(instance, (NearestSeaDF::new))
    );

    public static final KeyDispatchDataCodec<NearestSeaDF> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final double peak;
    private final double contBias;
    private final boolean distanceMode;
    private final DensityFunction cache;
    private final CachedVoronoiDF cachedVoronoi;

    public NearestSeaDF(DensityFunction cache, double peak, double contBias, boolean distanceMode) {
        this.cache = cache;
        this.peak = peak;
        this.contBias = contBias;
        this.distanceMode = distanceMode;
        this.cachedVoronoi = getCachedVoronoiDF(cache);
    }

    @Override
    public double compute(FunctionContext pos) {
        return this.distanceMode ? distToSea(
                this.cachedVoronoi.getValues(pos),
                this.cachedVoronoi.getVelocities(pos),
                this.cachedVoronoi.getDistances(pos),
                this.peak, this.contBias)
                : dirOfSea(
                        this.cachedVoronoi.getValues(pos),
                        this.cachedVoronoi.getVelocities(pos),
                        this.cachedVoronoi.getDistances(pos),
                        this.cachedVoronoi.getRelDirections(pos),
                        this.peak, this.contBias);
    }

    private static double distToSea(double[] val, double[] vel, double[] dist, double peak, double contBias) {
        if (val[0] >= contBias) {
            for (int j = 1; j < 9; j++) {
                if (val[j] < contBias) {
                    if (vel[j] > 0 && dist[j] - dist[0] < peak) return dist[j] - dist[0];
                    else if (vel[j] <= 0) return dist[j] - dist[0];
                }
            }
            return 10;
        } else return 0;
    }

    private static double dirOfSea(double[] val, double[] vel, double[] dist, double[] dir, double peak, double contBias) {
        if (val[0] >= contBias) {
            for (int j = 1; j < 9; j++) {
                if (val[j] < contBias) {
                    if (vel[j] > 0 && dist[j] - dist[0] < peak) return dir[j];
                    else if (vel[j] <= 0) return dir[j];
                }
            }
            return 10;
        } else return 0;
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
    public NearestSeaDF initialize(long levelSeed) {
        if (!this.cachedVoronoi.initialized()) this.cachedVoronoi.initialize(levelSeed);
        return this;
    }

    public boolean initialized() {
        return this.cachedVoronoi.initialized();
    }


}
