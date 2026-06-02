package net.ikb.library.world.gen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;

public class VoronoiDistanceDF implements SeededDensityFunction {

    private static final MapCodec<VoronoiDistanceDF> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    Codec.LONG.optionalFieldOf("salt", 0L).forGetter((input) -> input.salt),
                    Codec.BOOL.optionalFieldOf("flat", true).forGetter((input) -> input.flat),
                    Codec.DOUBLE.fieldOf("scale").forGetter((input) -> input.scale),
                    Codec.doubleRange(0.0,0.5).optionalFieldOf("jitter", 0.4).forGetter((input) -> input.jitter),
                    Codec.intRange(0,5).optionalFieldOf("metric", 1).forGetter((input) -> input.metric),
                    Codec.BOOL.optionalFieldOf("rel", false).forGetter((input) -> input.rel),
                    Codec.intRange(1,9).optionalFieldOf("ordinal", 1).forGetter((input) -> input.ordinal)
            ).apply(instance, VoronoiDistanceDF::new)
    );

    public static final KeyDispatchDataCodec<VoronoiDistanceDF> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Nullable
    public VoronoiNoise noise = null;
    private final long salt;
    private final boolean flat;
    private final double scale;
    private final double jitter;
    private final int metric;
    private final boolean rel;
    private final int ordinal;

    public VoronoiDistanceDF(long salt, boolean flat, double scale, double jitter, int metric, boolean rel, int ordinal) {
        this.salt = salt;
        this.flat = flat;
        this.scale = scale;
        this.jitter = jitter;
        this.metric = metric;
        this.rel = rel;
        this.ordinal = ordinal;
    }

    @Override
    public double compute(FunctionContext pos) {
        if (this.noise == null) {
            throw new NullPointerException("VoronoiDistanceDF not initialized");
        } else return this.noise.getDistance(pos, this.flat, this.scale, this.jitter, this.metric, this.ordinal, this.rel);
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

    @Override
    public VoronoiDistanceDF initialize(long levelSeed) {
        this.noise = VoronoiNoise.create(levelSeed + this.salt);
        return this;
    }

    public boolean initialized() {
        return this.noise != null;
    }


}
