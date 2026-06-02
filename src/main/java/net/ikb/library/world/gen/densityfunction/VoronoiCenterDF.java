package net.ikb.library.world.gen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;

public class VoronoiCenterDF implements SeededDensityFunction {

    private static final MapCodec<VoronoiCenterDF> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    Codec.LONG.optionalFieldOf("salt", 0L).forGetter((input) -> input.salt),
                    Codec.BOOL.optionalFieldOf("flat", true).forGetter((input) -> input.flat),
                    Codec.DOUBLE.fieldOf("scale").forGetter((input) -> input.scale),
                    Codec.doubleRange(0.0,0.5).optionalFieldOf("jitter", 0.4).forGetter((input) -> input.jitter),
                    Codec.intRange(0,5).optionalFieldOf("metric", 1).forGetter((input) -> input.metric),
                    Codec.intRange(1,9).optionalFieldOf("ordinal", 1).forGetter((input) -> input.ordinal),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("sampler").forGetter((input) -> input.sampler)
            ).apply(instance, VoronoiCenterDF::new)
    );

    public static final KeyDispatchDataCodec<VoronoiCenterDF> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Nullable
    public VoronoiNoise noise = null;
    private final long salt;
    private final boolean flat;
    private final double scale;
    private final double jitter;
    private final int metric;
    private final int ordinal;
    private final DensityFunction sampler;

    public VoronoiCenterDF(long salt, boolean flat, double scale, double jitter, int metric, int ordinal, DensityFunction sampler) {
        this.salt = salt;
        this.flat = flat;
        this.scale = scale;
        this.jitter = jitter;
        this.metric = metric;
        this.ordinal = ordinal;
        this.sampler = sampler;
    }
    public VoronoiCenterDF(long salt, boolean flat, double scale, double jitter, int metric, int ordinal, DensityFunction sampler, VoronoiNoise noise) {
        this.salt = salt;
        this.flat = flat;
        this.scale = scale;
        this.jitter = jitter;
        this.metric = metric;
        this.ordinal = ordinal;
        this.sampler = sampler;
        this.noise = noise;
    }

    @Override
    public double compute(FunctionContext pos) {
        if (this.noise == null) {
            throw new NullPointerException("VoronoiCenterDF not initialized");
        } else return this.sampler.compute(this.noise.getPlate(pos, this.flat, this.scale, this.jitter, this.metric, this.ordinal).getCenterPos(this.scale, this.flat));
    }

    @Override
    public void fillArray(double[] doubles, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(doubles, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(
                new VoronoiCenterDF(
                        this.salt,
                        this.flat,
                        this.scale,
                        this.jitter,
                        this.metric,
                        this.ordinal,
                        this.sampler.mapAll(visitor),
                        this.noise
                )
        );
    }

    @Override
    public double minValue() {
        return this.sampler.minValue();
    }

    @Override
    public double maxValue() {
        return this.sampler.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    @Override
    public VoronoiCenterDF initialize(long levelSeed) {
        this.noise = VoronoiNoise.create(levelSeed + this.salt);
        return this;
    }

    public boolean initialized() {
        return this.noise != null;
    }


}
