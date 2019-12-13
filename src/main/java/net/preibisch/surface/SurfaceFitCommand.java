package net.preibisch.surface;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.FolderOpener;
import io.scif.SCIFIOService;
import io.scif.services.DatasetIOService;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
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
import sc.fiji.hdf5.HDF5ImageJ;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static net.preibisch.surface.Test.*;

@Plugin(type = Command.class)
public class SurfaceFitCommand implements Command {
    @Parameter
    private String inputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/resampled/";

    @Parameter
    private String outputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/heightSurfaces.n5/";

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

    private int[] n5BlockSize = new int[]{512,512};

    @Override
    public void run() {
        // Open image sequence from input directory
        ImagePlus imp = FolderOpener.open(inputDirectory, " file=tif");
        imp.setTitle("Target");
        // Reslice image
        IJ.run(imp, "Reslice [/]...", "output=1.000 start=Top avoid");
        imp = IJ.getImage();
        Img img = ImageJFunctions.wrap(imp);

        // n5 prep
        N5Writer n5 = null;
        try {
            n5 = new N5FSWriter(outputDirectory);
            System.out.println("N5 location: " + outputDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Process bottom
        ImagePlus botSurfaceMap = getScaledSurfaceMap(getBotImg(img));
        RandomAccessibleInterval botSurfaceImg = ImageJFunctions.wrap(botSurfaceMap);
        try {
            //N5Utils.save(botSurfaceImg, n5, "/BotHeightmap", new int[]{512,512}, new Bzip2Compression());
            N5Utils.save(botSurfaceImg, n5, "/" + outputGroupname + "-bot", n5BlockSize, new RawCompression());
            System.out.println("Done writing bot");
        } catch (IOException e) {
            e.printStackTrace();
        }
        HDF5ImageJ.hdf5write(botSurfaceMap, outputDirectory.substring(0,outputDirectory.length()-3) + "h5", "/" + outputGroupname + "-bot");// FIXME hacky

        // Process top
        ImagePlus topSurfaceMap = getScaledSurfaceMap(getTopImg(img));
        RandomAccessibleInterval topSurfaceImg = ImageJFunctions.wrap(topSurfaceMap);
        try {
            //N5Utils.save(topSurfaceImg, n5, "/TopHeightmap", new int[]{512,512}, new Bzip2Compression());
            N5Utils.save(topSurfaceImg, n5, "/" + outputGroupname + "-top", n5BlockSize, new RawCompression());
            System.out.println("Done writing top");
        } catch (IOException e) {
            e.printStackTrace();
        }
        HDF5ImageJ.hdf5write(topSurfaceMap, outputDirectory.substring(0,outputDirectory.length()-3) + "h5", "/" + outputGroupname + "-top");// FIXME hacky
    }

    private ImagePlus getScaledSurfaceMap(Img img) {
        final Img<IntType> surface = process2( img, 5, 40, 20 );


//		final Img< FloatType > rendererSurface = img.factory().create( img );
//		renderDepthMap( rendererSurface, surface );

		//Gauss3.gauss( 0.7, Views.extendMirrorSingle( rendererSurface ), rendererSurface );
//        ImagePlus input = Util.getImagePlusInstance(img);
//        input.setTitle("Target");
//		//input.show();

        ImagePlus surfaceImp = Util.getImagePlusInstance(surface);
        surfaceImp.setTitle("Surface");
		//surfaceImp.show();

//        ImagePlus rendererSurfaceImp = Util.getImagePlusInstance(rendererSurface);
//        rendererSurfaceImp.setTitle("Generated surface");
		//rendererSurfaceImp.show();

        // To help with debugging
//		IJ.run(input, "Reslice [/]...", "output=1.000 start=Top avoid");
//		IJ.run(rendererSurfaceImp, "Reslice [/]...", "output=1.000 start=Top avoid");

        // Rescale height values
        float heightScaleFactor = originalDimX / img.dimension(0);
        Img<RealType> surfaceImg = ImageJFunctions.wrapReal(surfaceImp);
        Cursor<RealType> surfaceCur = surfaceImg.cursor();
        while( surfaceCur.hasNext() ) {
            surfaceCur.fwd();
            surfaceCur.get().mul(heightScaleFactor);
        }

        // Smooth image

        //surfaceImp.close();
        RandomAccessibleInterval<RealType> res = ops.filter().gauss(surfaceImg, 2);// this parameter differs from Dagmar's
        surfaceImp = ImageJFunctions.wrap(res, "Surface");

        // Upsample image
		IJ.run(surfaceImp, "Scale...", "x=- y=- width=" + originalDimX + " height=" + originalDimZ + " interpolation=Bicubic average create title=ScaleSurface");
        ImagePlus scaledSurfaceImp = WindowManager.getImage("ScaleSurface");
        scaledSurfaceImp.setTitle("ScaleSurfaceFetched");

        return scaledSurfaceImp;
    }

    public Img getBotImg(Img img) {
        FinalInterval botHalfInterval = Intervals.createMinMax(0, 0, 0, img.dimension(0)-1, img.dimension(1)-1, img.dimension(2)/2-1);
        Img<FloatType> botImg = ops.create().img(botHalfInterval, new FloatType());

        IterableInterval<UnsignedByteType> botView = Views.interval(img, botHalfInterval);
        Cursor<UnsignedByteType> botViewCur = botView.cursor();
        Cursor<FloatType> botCur = botImg.cursor();
        while(botCur.hasNext()) {
            botCur.fwd();
            botViewCur.fwd();
            botCur.get().set(botViewCur.get().getRealFloat());
        }
        return botImg;
    }

    public Img getTopImg(Img img) {
        FinalInterval topHalfInterval = Intervals.createMinMax(0, 0, img.dimension(2)/2, img.dimension(0)-1, img.dimension(1)-1, img.dimension(2)-1);
        FinalInterval topIntervalSize = Intervals.createMinMax(0, 0, 0, topHalfInterval.dimension(0)-1, topHalfInterval.dimension(1)-1, topHalfInterval.dimension(2)-1);
        Img<FloatType> topImg = ops.create().img(topIntervalSize, new FloatType());

        IterableInterval<UnsignedByteType> topView = Views.interval(img, topHalfInterval);
        Cursor<UnsignedByteType> topViewCur = topView.cursor();
        Cursor<FloatType> topCur = topImg.cursor();
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
        argmap.put("outputDirectory", "/home/kharrington/Data/SEMA/Z1217-19m/VNC/Sec04/heightSurfaces.n5/");
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
