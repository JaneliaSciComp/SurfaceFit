package net.preibisch.surface;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.FolderOpener;
import io.scif.SCIFIOService;
import io.scif.services.DatasetIOService;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.*;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.LanczosInterpolator;
import net.imglib2.interpolation.randomaccess.NLinearInterpolator;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.Bzip2Compression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import sc.fiji.hdf5.HDF5ImageJ;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static net.preibisch.surface.Test.*;

@Plugin(type = Command.class)
public class SurfaceFitCommand implements Command {
    @Parameter
    private String inputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/resampled/";

    @Parameter
    private String outputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/heightSurf/";

    @Parameter
    private long originalDimX = 9007;

    @Parameter
    private long originalDimZ = 9599;

    @Parameter
    private String outputGroupname = "level200";

    @Parameter
    private SCIFIOService io;

    @Parameter
    private DatasetIOService dataset;

    @Parameter
    private OpService ops;

    @Parameter
    private Context context;

    //@Parameter(required = false)
    private boolean terminateOnCompletion = false;

//    @Parameter
//    private UIService ui;

    private int[] n5BlockSize = new int[]{512,512};

    @Override
    public void run() {
        System.out.println("SurfaceFitCommand");
        System.out.println("Input directory: " + inputDirectory);
        System.out.println("Output directory: " + outputDirectory);
        System.out.println("Output groupname: " + outputGroupname);

        // Open image sequence from input directory
        ImagePlus imp = FolderOpener.open(inputDirectory, " file=tif");
        //imp.setTitle("Target");
        // Reslice image
        //IJ.run(imp, "Reslice [/]...", "output=1.000 start=Top avoid");
        //imp = IJ.getImage();

        // Reslice using imglib
        Img<UnsignedByteType> img = ImageJFunctions.wrapReal(imp);


        RandomAccessibleInterval<UnsignedByteType> rotView = Views.invertAxis(Views.rotate(img, 1, 2),1);

        final RandomAccessibleInterval<DoubleType> resliceRai = Converters.convert(rotView, (a, d) -> d.setReal(a.getRealDouble()), new DoubleType());

        // n5 prep
//        N5Writer n5 = null;
//        try {
//            n5 = new N5FSWriter(outputDirectory);
//            System.out.println("N5 location: " + outputDirectory);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // Process bottom
        RandomAccessibleInterval botSurfaceImg = getScaledSurfaceMap(getBotImg(resliceRai, ops), 0, originalDimX, originalDimZ, ops);

//        try {
//            //N5Utils.save(botSurfaceImg, n5, "/BotHeightmap", new int[]{512,512}, new Bzip2Compression());
//            N5Utils.save(botSurfaceImg, n5, "/" + outputGroupname + "-bot", n5BlockSize, new RawCompression());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // Old code for mimicing existing pipeline
//        RealType botMean = ops.stats().mean(Views.iterable(Views.hyperSlice(botSurfaceImg, 0, 0)));
//        HDF5ImageJ.hdf5write(ImageJFunctions.wrap(botSurfaceImg, "BotSurfaceMap"), outputDirectory + outputGroupname + "-bot" + ".h5", "/volume");
//        System.out.println("Done writing bot");
//
//        try {
//            BufferedWriter bw = new BufferedWriter(new FileWriter(outputDirectory + outputGroupname + "-bot.txt"));
//            bw.write("" + botMean.getRealDouble());
//            bw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        // Process top
        RandomAccessibleInterval topSurfaceImg = getScaledSurfaceMap(getTopImg(resliceRai, ops), resliceRai.dimension(2)/2, originalDimX, originalDimZ, ops);

//        try {
//            //N5Utils.save(topSurfaceImg, n5, "/TopHeightmap", new int[]{512,512}, new Bzip2Compression());
//            N5Utils.save(topSurfaceImg, n5, "/" + outputGroupname + "-top", n5BlockSize, new RawCompression());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        RealType topMean = ops.stats().mean(Views.iterable(Views.hyperSlice(topSurfaceImg, 0, 0)));
//        HDF5ImageJ.hdf5write(ImageJFunctions.wrap(topSurfaceImg, "TopSurfaceMap"), outputDirectory + outputGroupname + "-top" + ".h5", "/volume");
//        System.out.println("Done writing top");
//
//        try {
//            BufferedWriter bw = new BufferedWriter(new FileWriter(outputDirectory + outputGroupname + "-top.txt"));
//            bw.write("" + topMean.getRealDouble());
//            bw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        if( terminateOnCompletion )
            System.exit(0);
    }

        /**
     *
     * @param img
     * @param offset - this accounts for the fact that processing the "top" is run in an interval offset
     * @return
     */
    public static Pair<RandomAccessibleInterval<IntType>, DoubleType> getScaledSurfaceMapAndAverage(RandomAccessibleInterval img, long offset, long originalDimX, long originalDimZ, OpService ops) {
        final Img<IntType> surfaceImg = process2( img, 5, 40, 20 );

        // Rescale height values
        float heightScaleFactor = originalDimX / img.dimension(0) / 2;
        Cursor<IntType> surfaceCur = surfaceImg.cursor();

        DoubleType avg = new DoubleType(0);
        DoubleType tmp = new DoubleType();

        long count = 0;
        while( surfaceCur.hasNext() ) {
            surfaceCur.fwd();
            surfaceCur.get().add(new IntType((int) offset));
            surfaceCur.get().mul(heightScaleFactor);
            tmp.setReal(surfaceCur.get().getRealDouble());
            avg.add(tmp);
            count++;
        }
        avg.div(new DoubleType(count));

        long[] border = new long[]{10, 10};// TODO border chosen arbitrarily
        RandomAccessibleInterval<IntType> extendedSurfaceImg = Views.interval(Views.extendMirrorSingle(surfaceImg),
                new long[]{surfaceImg.min(0) - border[0], surfaceImg.min(1) - border[1]},
                new long[]{surfaceImg.max(0) + border[0], surfaceImg.max(1) + border[1]});
        RandomAccessibleInterval<IntType> res = ops.filter().gauss(extendedSurfaceImg, 2.0);// this parameter differs from Dagmar's

        long[] newDims = new long[]{originalDimX, originalDimZ};

        NLinearInterpolatorFactory<IntType> interpolatorFactory = new NLinearInterpolatorFactory<>();

        double[] scaleFactors = new double[]{originalDimX / res.dimension(0), originalDimZ / res.dimension(1)};

        RealRandomAccessible<IntType> interp = Views.interpolate(Views.extendMirrorSingle(res), interpolatorFactory);

        IntervalView<IntType> interval = Views.interval(Views.raster(RealViews.affineReal(
			interp,
			new Scale(scaleFactors))), new FinalInterval(newDims));


        Pair<RandomAccessibleInterval<IntType>, DoubleType> pair = new Pair<RandomAccessibleInterval<IntType>, DoubleType>() {
            @Override
            public RandomAccessibleInterval<IntType> getA() {
                return interval;
            }

            @Override
            public DoubleType getB() {
                return avg;
            }
        };
        return pair;
    }

    /**
     *
     * @param img
     * @param offset - this accounts for the fact that processing the "top" is run in an interval offset
     * @return
     */
    public static RandomAccessibleInterval<IntType> getScaledSurfaceMap(RandomAccessibleInterval img, long offset, long originalDimX, long originalDimZ, OpService ops) {
        final Img<IntType> surfaceImg = process2( img, 5, 40, 20 );

        // Rescale height values
        float heightScaleFactor = ((float)originalDimX) / ((float)img.dimension(0)) / 2f;

        Cursor<IntType> surfaceCur = surfaceImg.cursor();
        while( surfaceCur.hasNext() ) {
            surfaceCur.fwd();
            surfaceCur.get().mul(heightScaleFactor);
            surfaceCur.get().add(new IntType((int) offset));// FIXME beware of this casting
        }

        RandomAccessibleInterval<IntType> res = ops.filter().gauss(surfaceImg, 2);// this parameter differs from Dagmar's

        long[] newDims = new long[]{originalDimX, originalDimZ};

        NLinearInterpolatorFactory<IntType> interpolatorFactory = new NLinearInterpolatorFactory<>();

        double[] scaleFactors = new double[]{originalDimX / res.dimension(0), originalDimZ / res.dimension(1)};

        RealRandomAccessible<IntType> interp = Views.interpolate(Views.extendMirrorSingle(res), interpolatorFactory);

        IntervalView<IntType> interval = Views.interval(Views.raster(RealViews.affineReal(
			interp,
			new Scale(scaleFactors))), new FinalInterval(newDims));

        return interval;
    }

    public static Img getBotImg(RandomAccessibleInterval img, OpService ops) {
        FinalInterval botHalfInterval = Intervals.createMinMax(0, 0, 0, img.dimension(0)-1, img.dimension(1)-1, img.dimension(2)/2-1);
        Img<DoubleType> botImg = ops.create().img(botHalfInterval, new DoubleType());

        IterableInterval<DoubleType> botView = Views.interval(img, botHalfInterval);
        Cursor<DoubleType> botViewCur = botView.cursor();
        Cursor<DoubleType> botCur = botImg.cursor();
        while(botCur.hasNext()) {
            botCur.fwd();
            botViewCur.fwd();
            botCur.get().set(botViewCur.get().getRealFloat());
        }
        return botImg;
    }

    public static Img getTopImg(RandomAccessibleInterval img, OpService ops) {
        FinalInterval topHalfInterval = Intervals.createMinMax(0, 0, img.dimension(2)/2, img.dimension(0)-1, img.dimension(1)-1, img.dimension(2)-1);
        FinalInterval topIntervalSize = Intervals.createMinMax(0, 0, 0, topHalfInterval.dimension(0)-1, topHalfInterval.dimension(1)-1, topHalfInterval.dimension(2)-1);
        Img<DoubleType> topImg = ops.create().img(topIntervalSize, new DoubleType());

        IterableInterval<DoubleType> topView = Views.interval(img, topHalfInterval);
        Cursor<DoubleType> topViewCur = topView.cursor();
        Cursor<DoubleType> topCur = topImg.cursor();
        while(topCur.hasNext()) {
            topCur.fwd();
            topViewCur.fwd();
            topCur.get().set(topViewCur.get().getRealFloat());
        }
        return topImg;
    }

    public static void main(String[] args) {
        ImageJ imagej = new ImageJ();
        imagej.ui().showUI();

        Map<String, Object> argmap = new HashMap<>();
        argmap.put("inputDirectory", "/nrs/flyem/alignment/Z1217-19m/VNC/Sec04/flatten/tmp-flattening-level200/resampled/");
        //argmap.put("outputDirectory", "/nrs/flyem/alignment/Z1217-19m/VNC/Sec04/flatten/tmp-flattening-level200/heightSurf/");
        argmap.put("outputDirectory", "/home/kharrington/Data/SEMA/Z1217-19m/VNC/Sec04/heightSurf/");
        argmap.put("originalDimX", 23254);
        argmap.put("originalDimZ", 26358);
        argmap.put("outputGroupname", "level200");

//        argmap.put("inputDirectory", args[1]);
//        argmap.put("outputDirectory", args[2]);
//        argmap.put("originalDimX", args[3]);
//        argmap.put("originalDimZ", args[4]);
//        argmap.put("outputBasename", args[5]);

        imagej.command().run(SurfaceFitCommand.class, true, argmap);
    }

    // Curtis says this should work for CLI execution, but leads to exceptions
//    public static void main(String... args) {
//        SciJava sj = new SciJava();
//        sj.launch(args);
//        sj.context().dispose();
//    }
}
