package net.ikb.library.world.gen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;

public class CachedVoronoiDF implements SeededDensityFunction {

    private static final MapCodec<CachedVoronoiDF> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    Codec.LONG.optionalFieldOf("salt", 0L).forGetter((input) -> input.salt),
                    Codec.BOOL.optionalFieldOf("flat", true).forGetter((input) -> input.flat),
                    Codec.DOUBLE.fieldOf("scale").forGetter((input) -> input.scale),
                    Codec.doubleRange(0.0,0.5).optionalFieldOf("jitter", 0.4).forGetter((input) -> input.jitter),
                    Codec.intRange(0,5).optionalFieldOf("metric", 1).forGetter((input) -> input.metric),
                    Codec.intRange(1,9).optionalFieldOf("maxCheck", 3).forGetter((input) -> input.maxCheck)
            ).apply(instance, (CachedVoronoiDF::new))
    );

    public static final KeyDispatchDataCodec<CachedVoronoiDF> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Nullable
    public CachedVoronoiNoise noise = null;
    private final long salt;
    private final boolean flat;
    private final double scale;
    private final double jitter;
    private final int metric;
    private final int maxCheck;

    public CachedVoronoiDF(long salt, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        this.salt = salt;
        this.flat = flat;
        this.scale = scale;
        this.jitter = jitter;
        this.metric = metric;
        this.maxCheck = maxCheck;
    }

    @Override
    public double compute(FunctionContext pos) {
        return 0;
    }

    public double sampleAtCenter(FunctionContext blockPos, int ordinal, DensityFunction sampler) {
        return this.noise != null ? this.noise.sample(blockPos, flat, scale, jitter, metric, ordinal, sampler) : 0;
    }

    public double getDistance(FunctionContext pos, int index) {
        return this.maxCheck > index && this.noise != null ? this.noise.getDistances(pos, flat, scale, jitter, metric, maxCheck)[index] : 0;
    }

    public double getValue(FunctionContext pos, int index) {
        return this.maxCheck > index && this.noise != null ? this.noise.getValues(pos, flat, scale, jitter, metric, maxCheck)[index] : 0;
    }

    public double getVelocity(FunctionContext pos, int index) {
        return this.maxCheck > index && this.noise != null ? this.noise.getVelocities(pos, flat, scale, jitter, metric, maxCheck)[index] : 0;
    }

    public double getPassive(FunctionContext pos, int index) {
        return this.maxCheck > index && this.noise != null ? this.noise.getPassives(pos, flat, scale, jitter, metric, maxCheck)[index] : 0;
    }

    public double getDirection(FunctionContext pos, int index) {
        return this.maxCheck > index && this.noise != null ? this.noise.getDirections(pos, flat, scale, jitter, metric, maxCheck)[index] : 0;
    }

    public double getRelDirection(FunctionContext pos, int index) {
        return this.maxCheck > index && this.noise != null ? this.noise.getRelDirections(pos, flat, scale, jitter, metric, maxCheck)[index] : 0;
    }

    public double[] getDistances(FunctionContext pos) {
        return this.noise != null ? this.noise.getDistances(pos, flat, scale, jitter, metric, maxCheck) : new double[1];
    }

    public double[] getVelocities(FunctionContext pos) {
        return this.noise != null ? this.noise.getVelocities(pos, flat, scale, jitter, metric, maxCheck) : new double[1];
    }

    public double[] getValues(FunctionContext pos) {
        return this.noise != null ? this.noise.getValues(pos, flat, scale, jitter, metric, maxCheck) : new double[1];
    }

    public double[] getPassives(FunctionContext pos) {
        return this.noise != null ? this.noise.getPassives(pos, flat, scale, jitter, metric, maxCheck) : new double[1];
    }

    public double[] getDirections(FunctionContext pos) {
        return this.noise != null ? this.noise.getDirections(pos, flat, scale, jitter, metric, maxCheck) : new double[1];
    }

    public double[] getRelDirections(FunctionContext pos) {
        return this.noise != null ? this.noise.getRelDirections(pos, flat, scale, jitter, metric, maxCheck) : new double[1];
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
        return 0;
    }

    @Override
    public double maxValue() {
        return 0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    @Override
    public CachedVoronoiDF initialize(long levelSeed) {
        this.noise = CachedVoronoiNoise.create(levelSeed + this.salt);
        return this;
    }

    public boolean initialized() {return this.noise != null;}

}
