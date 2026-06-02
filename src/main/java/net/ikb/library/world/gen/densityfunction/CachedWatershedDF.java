package net.ikb.library.world.gen.densityfunction;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

import java.util.HashMap;

public class CachedWatershedDF implements SeededDensityFunction {

    private static final MapCodec<CachedWatershedDF> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("cache").forGetter((input) -> input.cache),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("sampler").forGetter((input) -> input.sampler)
            ).apply(instance, (CachedWatershedDF::new))
    );

    public static final KeyDispatchDataCodec<CachedWatershedDF> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);


    public CachedWatersheds sheds;
    private final DensityFunction cache;
    private final CachedVoronoiDF cachedVoronoi;
    private final DensityFunction sampler;


    public CachedWatershedDF(DensityFunction cache, DensityFunction sampler) {
        this.cache = cache;
        this.cachedVoronoi = getCachedVoronoiDF(cache);
        this.sampler = sampler;
        this.sheds = new CachedWatersheds();
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
    public CachedWatershedDF initialize(long levelSeed) {
        if (!this.cachedVoronoi.initialized()) this.cachedVoronoi.initialize(levelSeed);
        return this;
    }

    public boolean initialized() {
        return this.cachedVoronoi.initialized();
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




    public class CachedWatersheds {
        public boolean isFlowReflexive(Vec3i index1, Vec3i index2) {
            return (getFlowPlates(index1)[1].getIndex() == index2 && getFlowPlates(index2)[1].getIndex() == index1);
        }

        public boolean doesEitherFlow(Vec3i index1, Vec3i index2) {
            return (getFlowPlates(index1)[1].getIndex() == index2 || getFlowPlates(index2)[1].getIndex() == index1);
        }

        public double distanceToNearestRiverbed(DensityFunction.FunctionContext blockPos) {
            VoronoiPlate[] plates = cachedVoronoi.noise.getNearest(blockPos, cachedVoronoi.flat(), cachedVoronoi.scale(), cachedVoronoi.jitter(), cachedVoronoi.metric(), 9);
            Vec3i i0 = plates[0].getIndex();
            for (int i = 1; i < 9; i++) {
                if (doesEitherFlow(i0, plates[i].getIndex())) return plates[i].getDist(plates[0].getCenter(), cachedVoronoi.flat(), cachedVoronoi.metric());
            }
            return 4;
        }





        private final HashMap<Vec3i, double[]> memoGradients = new HashMap<>();
        private final HashMap<Vec3i, VoronoiPlate[]> memoFlowPlates = new HashMap<>();
        private final HashMap<Vec3i, double[]> memoRelDists = new HashMap<>();
        private final HashMap<Vec3i, Boolean> memoLake = new HashMap<>();
        private final HashMap<DensityFunction.FunctionContext, double[]> memoRiverbed = new HashMap<>();
        private final HashMap<DensityFunction.FunctionContext, double[]> memoWatershed = new HashMap<>();

        public double[] getGradients(DensityFunction.FunctionContext blockPos) {
            return getGradients(cachedVoronoi.getNearest(blockPos, 0).getIndex());
        }

        public double[] getGradients(Vec3i index) {
            if (memoGradients.containsKey(index)) {
                double[] val = memoGradients.get(index);
                if (val != null) return val;
            }
            Pair<VoronoiPlate[], double[]> val = cachedVoronoi.getWatersheds(index, sampler);
            memoGradients.put(index, val.getSecond());
            memoFlowPlates.put(index, val.getFirst());
            return val.getSecond();
        }

        public VoronoiPlate[] getFlowPlates(DensityFunction.FunctionContext blockPos) {
            return getFlowPlates(cachedVoronoi.getNearest(blockPos, 0).getIndex());
        }

        public VoronoiPlate[] getFlowPlates(Vec3i index) {
            if (memoFlowPlates.containsKey(index)) {
                VoronoiPlate[] val = memoFlowPlates.get(index);
                if (val != null) return val;
            }
            Pair<VoronoiPlate[], double[]> val = cachedVoronoi.getWatersheds(index, sampler);
            memoGradients.put(index, val.getSecond());
            memoFlowPlates.put(index, val.getFirst());
            return val.getFirst();
        }

        double[] getRelDists(DensityFunction.FunctionContext blockPos) {
            return getRelDists(cachedVoronoi.getNearest(blockPos, 0).getIndex());
        }

        double[] getRelDists(Vec3i index) {
            if (memoRelDists.containsKey(index)) {
                double[] val = memoRelDists.get(index);
                return val != null ? val : calcRelDists(index);
            } else return calcRelDists(index);
        }

        // gives the distances between the indexed plate's center and all nearby plate centers,
        // in order of ascending gradient (downhill to uphill)
        private double[] calcRelDists(Vec3i index) {
            VoronoiPlate[] plates = getFlowPlates(index);
            double[] relDists = new double[9];
            relDists[0] = 0;
            for (int i = 1; i < 9; i++) relDists[i] = plates[0].getDist(plates[i].getCenter(), cachedVoronoi.flat(), cachedVoronoi.metric());
            memoRelDists.put(index, relDists);
            return relDists;
        }

        boolean getLake(DensityFunction.FunctionContext blockPos, double bound) {
            return isLake(cachedVoronoi.getNearest(blockPos, 0).getIndex(), bound);
        }

        private Boolean isLake(Vec3i index, double bound) {
            if (memoLake.containsKey(index)) {
                Boolean val = memoLake.get(index);
                return val != null ? val : calcLake(index, bound);
            } else return calcLake(index, bound);
        }

        private Boolean calcLake(Vec3i index, double bound) {

            VoronoiPlate[] plates = getFlowPlates(index);
            double[] gradients = getGradients(index);
            double[] distances = getRelDists(index);

            for (int i = 1; i < 9; i++) {
                if (distances[i] <= bound) {
                    if (gradients[i] < 0) return false;
                }
            }

            return true;
        }

    }









}
