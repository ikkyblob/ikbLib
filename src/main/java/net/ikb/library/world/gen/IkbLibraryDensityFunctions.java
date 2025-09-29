package net.ikb.library.world.gen;

import com.mojang.serialization.MapCodec;
import net.ikb.library.IkbLibrary;
//import net.ikb.library.world.gen.densityfunction.CachedVoronoiDF;
//import net.ikb.library.world.gen.densityfunction.PullFromCachedVoronoiDF;
import net.ikb.library.world.gen.densityfunction.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class IkbLibraryDensityFunctions {

    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPE =
            DeferredRegister.create(BuiltInRegistries.DENSITY_FUNCTION_TYPE, IkbLibrary.MODID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<CachedVoronoiDF>>
            CACHED_VORONOI = DENSITY_FUNCTION_TYPE.register("cached_voronoi", CachedVoronoiDF.CODEC::codec);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<VoronoiDF>>
            VORONOI = DENSITY_FUNCTION_TYPE.register("voronoi", VoronoiDF.CODEC::codec);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<NearestSeaDF>>
            NEAR_SEA = DENSITY_FUNCTION_TYPE.register("near_sea", NearestSeaDF.CODEC::codec);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<PullFromCachedVoronoiDF>>
            PULL_CACHED_VORONOI = DENSITY_FUNCTION_TYPE.register("pull_cached_voronoi", PullFromCachedVoronoiDF.CODEC::codec);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<PullCachedCenterDF>>
            SAMPLE_CENTER = DENSITY_FUNCTION_TYPE.register("sample_voronoi_center", PullCachedCenterDF.CODEC::codec);

    public static void register(IEventBus eventBus) {
        DENSITY_FUNCTION_TYPE.register(eventBus);
    }

}
