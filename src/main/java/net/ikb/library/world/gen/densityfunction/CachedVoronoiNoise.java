package net.ikb.library.world.gen.densityfunction;

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

    HashMap<DensityFunction.FunctionContext, double[]> memoDists = new HashMap<>();
    HashMap<DensityFunction.FunctionContext, double[]> memoValues = new HashMap<>();
    HashMap<DensityFunction.FunctionContext, double[]> memoVelocities = new HashMap<>();
    HashMap<DensityFunction.FunctionContext, double[]> memoPassives = new HashMap<>();
    HashMap<DensityFunction.FunctionContext, double[]> memoDirections = new HashMap<>();
    HashMap<DensityFunction.FunctionContext, double[]> memoRelDirections = new HashMap<>();
    HashMap<DensityFunction.FunctionContext, List<DensityFunction.FunctionContext>> memoCenters = new HashMap<>();


    public double[] getDistances(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoDists.containsKey(blockPos)) {
            double[] val = memoDists.get(blockPos);
            return val != null ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 0);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 0);
    }

    public double[] getValues(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoValues.containsKey(blockPos)) {
            double[] val = memoValues.get(blockPos);
            return val != null ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 1);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 1);
    }

    public double[] getVelocities(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoVelocities.containsKey(blockPos)) {
            double[] val = memoVelocities.get(blockPos);
            return val != null ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 2);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 2);
    }

    public double[] getPassives(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoPassives.containsKey(blockPos)) {
            double[] val = memoPassives.get(blockPos);
            return val != null ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 3);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 3);
    }

    public double[] getDirections(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoDirections.containsKey(blockPos)) {
            double[] val = memoDirections.get(blockPos);
            return val != null ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 4);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 4);
    }

    public double[] getRelDirections(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck) {
        if (memoRelDirections.containsKey(blockPos)) {
            double[] val = memoRelDirections.get(blockPos);
            return val != null ? val : getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 5);
        } else return getVoronoi(blockPos, flat, scale, jitter, metric, maxCheck, 5);
    }

    private double[] getVoronoi(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int maxCheck, int mode) {

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
        double[] sortValues = new double[maxCheck];
        double[] sortVelocities = new double[maxCheck];
        double[] sortPassives = new double[maxCheck];
        double[] sortDirections = new double[maxCheck];
        double[] sortRelDirections = new double[maxCheck];

        Arrays.fill(sortDistances, Double.MAX_VALUE);

        for (int xi = -1; xi <= 1; xi++) {
            for (int zi = -1; zi <= 1; zi++) {

                Vec3i checkIndex = new Vec3i(posIndex.getX() + xi, posIndex.getY(), posIndex.getZ() + zi);

                VoronoiPlate checkPlate = null;
                if (this.MEMOIZED_PLATES.containsKey(checkIndex)) checkPlate = this.MEMOIZED_PLATES.get(checkIndex);
                if (!this.MEMOIZED_PLATES.containsKey(checkIndex) || checkPlate == null) {
                    checkPlate = new VoronoiPlate(seed, checkIndex, jitter);
                    this.MEMOIZED_PLATES.put(checkIndex, checkPlate == null ? new VoronoiPlate(seed, checkIndex, jitter) : checkPlate);
                }

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
        }

        for (int i = 0; i < maxCheck; i++) {
            sortValues[i] = sortPlates[i].getValue();
            sortVelocities[i] = i == 0 ? 0 : sortPlates[0].relativeVelocity(sortPlates[i]);
            sortPassives[i] = i == 0 ? 1 : sortPlates[0].velocity() == sortPlates[i].velocity() ? 1 : 0;
            sortDirections[i] = Mth.atan2(sortPlates[i].getCenter().z() - z, sortPlates[i].getCenter().x() - x) - 1.5707964F;
            sortRelDirections[i] = i == 0 ? 0 : Mth.atan2(sortPlates[i].getCenter().z() - sortPlates[0].getCenter().z(), sortPlates[i].getCenter().x() - sortPlates[0].getCenter().x()) - 1.5707964F;
        }

        memoDists.put(blockPos, sortDistances);
        memoValues.put(blockPos, sortValues);
        memoVelocities.put(blockPos, sortVelocities);
        memoDirections.put(blockPos, sortDirections);
        memoRelDirections.put(blockPos, sortRelDirections);
        return switch (mode) {
            default -> sortDistances;
            case 1 -> sortValues;
            case 2 -> sortVelocities;
            case 3 -> sortPassives;
            case 4 -> sortDirections;
            case 5 -> sortRelDirections;
        };
    }

    public double sample(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric, int ordinal, DensityFunction sampler) {
        if (memoCenters.containsKey(blockPos)) {
            List<DensityFunction.FunctionContext> val = memoCenters.get(blockPos);
            return val != null ?
                    sampler.compute(val.get(ordinal - 1))
                    : sampler.compute(computeCenters(blockPos, flat, scale, jitter, metric).get(ordinal - 1));
        } else return sampler.compute(computeCenters(blockPos, flat, scale, jitter, metric).get(ordinal - 1));
    }

    public List<DensityFunction.FunctionContext> computeCenters(DensityFunction.FunctionContext blockPos, boolean flat, double scale, double jitter, int metric) {

        double x = ((double) blockPos.blockX()) / scale;
        double y = flat ? 0 : ((double) blockPos.blockY()) / scale;
        double z = ((double) blockPos.blockZ()) / scale;

        Vec3i posIndex = new Vec3i(
                (int) (x >= 0 ? x + 0.5 : x - 0.5),
                (int) (y >= 0 ? y + 0.5 : y - 0.5),
                (int) (z >= 0 ? z + 0.5 : z - 0.5)
        );

        VoronoiPlate[] sortPlates = new VoronoiPlate[9];

        double[] sortDistances = new double[9];

        List<DensityFunction.FunctionContext> sortCenters = new ArrayList<>(9);

        Arrays.fill(sortDistances, Double.MAX_VALUE);

        for (int xi = -1; xi <= 1; xi++) {
            for (int zi = -1; zi <= 1; zi++) {

                Vec3i checkIndex = new Vec3i(posIndex.getX() + xi, posIndex.getY(), posIndex.getZ() + zi);

                VoronoiPlate checkPlate = null;
                if (this.MEMOIZED_PLATES.containsKey(checkIndex)) checkPlate = this.MEMOIZED_PLATES.get(checkIndex);
                if (!this.MEMOIZED_PLATES.containsKey(checkIndex) || checkPlate == null) {
                    checkPlate = new VoronoiPlate(seed, checkIndex, jitter);
                    this.MEMOIZED_PLATES.put(checkIndex, checkPlate == null ? new VoronoiPlate(seed, checkIndex, jitter) : checkPlate);
                }

                double checkDistance = checkPlate.getDist(new Vec3(x, y, z), flat, metric);

                for (int i = 0; i < 9; i++) {
                    if (checkDistance < sortDistances[i]) {
                        for (int j = 9; j > i; j--) {
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
        }

        for (int i = 0; i < 9; i++) {
            sortCenters.add(i, sortPlates[i].getCenterPos(scale));
        }

        memoCenters.put(blockPos, sortCenters);

        return sortCenters;
    }


}
