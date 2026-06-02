package net.ikb.library.world.gen.densityfunction;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CachedVoronoiNoise {

    public static CachedVoronoiNoise create(long seed) {
        return new CachedVoronoiNoise(seed);
    }

    CachedVoronoiNoise(long seed) {
        this.seed = seed;
    }

    public long seed;

    private final HashMap<Vec3i, VoronoiPlate> MEMOIZED_PLATES = new HashMap<>(); //vector is the plate index

    private final HashMap<DensityFunction.FunctionContext, double[]> memoDists = new HashMap<>();
    private final HashMap<DensityFunction.FunctionContext, double[]> memoValues = new HashMap<>();
    private final HashMap<DensityFunction.FunctionContext, double[]> memoVelocities = new HashMap<>();
    private final HashMap<DensityFunction.FunctionContext, double[]> memoPassives = new HashMap<>();
    private final HashMap<DensityFunction.FunctionContext, double[]> memoDirections = new HashMap<>();
    private final HashMap<DensityFunction.FunctionContext, double[]> memoRelDirections = new HashMap<>();
    private final HashMap<DensityFunction.FunctionContext, VoronoiPlate[]> memoNearest = new HashMap<>();

    public double[] getDistances(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoDists.containsKey(blockPos)) {
            double[] val = memoDists.get(blockPos);
            return val != null && val.length >= maxCheck ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 0);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 0);
    }

    public double[] getValues(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoValues.containsKey(blockPos)) {
            double[] val = memoValues.get(blockPos);
            return val != null && val.length >= maxCheck ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 1);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 1);
    }

    public double[] getVelocities(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoVelocities.containsKey(blockPos)) {
            double[] val = memoVelocities.get(blockPos);
            return val != null && val.length >= maxCheck ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 2);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 2);
    }

    public double[] getPassives(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoPassives.containsKey(blockPos)) {
            double[] val = memoPassives.get(blockPos);
            return val != null && val.length >= maxCheck ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 3);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 3);
    }

    public double[] getDirections(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoDirections.containsKey(blockPos)) {
            double[] val = memoDirections.get(blockPos);
            return val != null && val.length >= maxCheck ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 4);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 4);
    }

    public double[] getRelDirections(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoRelDirections.containsKey(blockPos)) {
            double[] val = memoRelDirections.get(blockPos);
            return val != null && val.length >= maxCheck ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 5);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 5);
    }



    public VoronoiPlate[] getNearest(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoNearest.containsKey(blockPos)) {
            VoronoiPlate[] val = memoNearest.get(blockPos);
            return val != null && val.length >= maxCheck ? val : calcNearest(blockPos, flat, scale, jitter, metric, maxCheck);
        } else return calcNearest(blockPos, flat, scale, jitter, metric, maxCheck);
    }



    private double[] getVoronoi(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck, int mode) {

        VoronoiPlate[] sortPlates = getNearest(blockPos, flat, scale, jitter, metric, maxCheck);
        double[] outputs = new double[maxCheck];

        switch (mode) {
            default -> {
                double x = ((double) blockPos.blockX()) / scale;
                double y = flat ? 0 : ((double) blockPos.blockY()) / scale;
                double z = ((double) blockPos.blockZ()) / scale;
                for (int i = 0; i < maxCheck; i++) outputs[i] = sortPlates[i].getDist(new Vec3(x, y, z), flat, metric);
                memoDists.put(blockPos, outputs);
            } case 1 -> {
                for (int i = 0; i < maxCheck; i++) outputs[i] = sortPlates[i].getValue();
                memoValues.put(blockPos, outputs);
            } case 2 -> {
                for (int i = 0; i < maxCheck; i++) outputs[i] = i == 0 ? 0 : sortPlates[0].relativeVelocity(sortPlates[i]);
                memoVelocities.put(blockPos, outputs);
            } case 3 -> {
                for (int i = 0; i < maxCheck; i++) outputs[i] = i == 0 ? 1 : sortPlates[0].velocity() == sortPlates[i].velocity() ? 1 : 0;
                memoPassives.put(blockPos, outputs);
            } case 4 -> {
                double x = ((double) blockPos.blockX()) / scale;
                double z = ((double) blockPos.blockZ()) / scale;
                for (int i = 0; i < maxCheck; i++) outputs[i] = Mth.atan2(sortPlates[i].getCenter().z() - z, sortPlates[i].getCenter().x() - x) - 1.5707964F;
                memoDirections.put(blockPos, outputs);
            } case 5 -> {
                for (int i = 0; i < maxCheck; i++) outputs[i] = i == 0 ? 0 : Mth.atan2(sortPlates[i].getCenter().z() - sortPlates[0].getCenter().z(), sortPlates[i].getCenter().x() - sortPlates[0].getCenter().x()) - 1.5707964F;
                memoRelDirections.put(blockPos, outputs);
            }
        }

        return outputs;

    }

    private VoronoiPlate[] calcNearest(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {

        double x = ((double) blockPos.blockX()) / scale;
        double y = flat ? 0 : ((double) blockPos.blockY()) / scale;
        double z = ((double) blockPos.blockZ()) / scale;

        Vec3i posIndex = new Vec3i(
                (int) (x >= 0 ? x + 0.5 : x - 0.5),
                (int) (y >= 0 ? y + 0.5 : y - 0.5),
                (int) (z >= 0 ? z + 0.5 : z - 0.5)
        );

        VoronoiPlate[] sortPlates = new VoronoiPlate[maxCheck];

        double[] sortDistances = new double[maxCheck];

        Arrays.fill(sortDistances, Double.MAX_VALUE);

        for (int xi = -1; xi <= 1; xi++) for (int zi = -1; zi <= 1; zi++) {

                Vec3i checkIndex = new Vec3i(posIndex.getX() + xi, posIndex.getY(), posIndex.getZ() + zi);

                VoronoiPlate checkPlate = getPlate(checkIndex, jitter);
                if (checkPlate == null) checkPlate = getPlate(checkIndex, jitter);

                double checkDistance = checkPlate.getDist(new Vec3(x, y, z), flat, metric);

                for (int i = 0; i < maxCheck; i++) {
                    if (checkDistance < sortDistances[i]) {
                        for (int j = maxCheck - 1; j > i; j--) {
                            if (sortPlates[j - 1] != null) {
                                sortDistances[j] = sortDistances[j - 1];
                                sortPlates[j] = sortPlates[j - 1];
                            }
                        }
                        sortDistances[i] = checkDistance;
                        sortPlates[i] = checkPlate;
                        break;
                    }
                }
            }

        memoDists.put(blockPos, sortDistances);
        memoNearest.put(blockPos, sortPlates);

        return sortPlates;

    }


    //Center Sampler Methods
    HashMap<DensityFunction.FunctionContext, List<DensityFunction.FunctionContext>> memoCenters = new HashMap<>(); //uses a list because FunctionContext doesn't like doing arrays

    public double sample(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int ordinal, DensityFunction sampler) {
        if (memoCenters.containsKey(blockPos)) {
            List<DensityFunction.FunctionContext> val = memoCenters.get(blockPos);
            return val != null ?
                    sampler.compute(val.get(ordinal - 1))
                    : sampler.compute(computeCenters(blockPos, flat, scale, jitter, metric, ordinal).get(ordinal - 1));
        } else return sampler.compute(computeCenters(blockPos, flat, scale, jitter, metric, ordinal).get(ordinal - 1));
    }

    public List<DensityFunction.FunctionContext> computeCenters(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {

        VoronoiPlate[] sortPlates = getNearest(blockPos, flat, scale, jitter, metric, maxCheck);
        List<DensityFunction.FunctionContext> sortCenters = new ArrayList<>(9);

        for (int i = 0; i < maxCheck; i++) {
            sortCenters.add(i, sortPlates[i].getCenterPos(scale));
        }

        memoCenters.put(blockPos, sortCenters);

        return sortCenters;
    }

    public VoronoiPlate getPlate(Vec3i index, double jitter) {
        VoronoiPlate plate = null;
        if (this.MEMOIZED_PLATES.containsKey(index)) plate = this.MEMOIZED_PLATES.get(index);
        if (!this.MEMOIZED_PLATES.containsKey(index) || plate == null) {
            plate = new VoronoiPlate(seed, index, jitter);
            this.MEMOIZED_PLATES.put(index, plate == null ? new VoronoiPlate(seed, index, jitter) : plate);
            return plate == null ? new VoronoiPlate(seed, index, jitter) : plate;
        }
        return plate;
    }



    //Watershed Methods


    public static Pair<VoronoiPlate[],double[]> getDefaultWatersheds() {
        return Pair.of(new VoronoiPlate[]{new VoronoiPlate(0, Vec3i.ZERO, 0)}, new double[]{0});
    }


    // outputs a list of nearby plates and their relative gradients
    // item 0 is always the input plate
    // all other items are sorted in order of from most downhill gradient [1] to most uphill gradient [8]
    public Pair<VoronoiPlate[], double[]> getWatersheds(Vec3i index, double jitter, DensityFunction sampler, double scale, boolean flat, int metric) {

        VoronoiPlate[] sortPlates = new VoronoiPlate[9];

        double[] sortedGrad = new double[9];

        sortPlates[0] = getPlate(index, jitter);

        sortedGrad[0] = sampler.compute(sortPlates[0].getCenterPos(scale));

        for (int xi = -1; xi <= 1; xi++) for (int zi = -1; zi <= 1; zi++)

            if (xi != 0 && zi != 0) {

                Vec3i checkIndex = new Vec3i(index.getX() + xi, index.getY(), index.getZ() + zi);

                VoronoiPlate checkPlate = getPlate(checkIndex, jitter);

                double checkGradient = (sampler.compute(checkPlate.getCenterPos(scale)) - sortedGrad[0]) / checkPlate.getDist(sortPlates[0].getCenter(), flat, metric);

                for (int i = 1; i < 9; i++) {
                    if (checkGradient < sortedGrad[i]) {
                        for (int j = 8; j > i; j--) {
                            if (sortPlates[j - 1] != null) {
                                sortedGrad[j] = sortedGrad[j - 1];
                                sortPlates[j] = sortPlates[j - 1];
                            }
                        }
                        sortedGrad[i] = checkGradient;
                        sortPlates[i] = checkPlate;
                        break;
                    }
                }
            }

        return Pair.of(sortPlates, sortedGrad);

    }



}
