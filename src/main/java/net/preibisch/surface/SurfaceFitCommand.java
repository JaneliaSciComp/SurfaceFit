package net.preibisch.surface;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.FolderOpener;
import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.hdf5.HDF5ImageJ;
import sc.fiji.hdf5.HDF5_Writer_Vibez;

import static net.preibisch.surface.Test.process2;
import static net.preibisch.surface.Test.renderDepthMap;

@Plugin(type = Command.class)
public class SurfaceFitCommand implements Command {
    //@Parameter
    private String inputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/resampled/";

    //@Parameter
    private String outputDirectory = "/home/kharrington/Data/SEMA/Z1217_19m/Sec04/flatten/tmp-flattening-level200/heightSurf/";

    //@Parameter
    private long originalDimX;

    //@Parameter
    private long originalDimZ;

    @Override
    public void run() {
        // Open image sequence from input directory
        ImagePlus imp = FolderOpener.open(inputDirectory, " file=tif");
        // Reslice image
        IJ.run(imp, "Reslice [/]...", "output=1.000 start=Top avoid");

        Img img = ImageJFunctions.wrap(imp);
        
		final Img<IntType> surface = process2( img, 5, 40, 20 );

		final Img<FloatType> rendererSurface = img.factory().create( img );

		renderDepthMap( rendererSurface, surface );

		//Gauss3.gauss( 0.7, Views.extendMirrorSingle( rendererSurface ), rendererSurface );

		Util.getImagePlusInstance( img ).show();
		Util.getImagePlusInstance( surface ).show();

        ImagePlus impRenderSurface = Util.getImagePlusInstance(rendererSurface);

        IJ.run(impRenderSurface, "Reslice [/]...", "output=1.000 start=Top avoid");

        impRenderSurface.show();

        String outFilename = outputDirectory + "test.h5";

        HDF5ImageJ.hdf5write( impRenderSurface, outFilename, "volume");
    }

    public static void main(String[] args) {
        ImageJ imagej = new ImageJ();
        imagej.ui().showUI();

        imagej.command().run(SurfaceFitCommand.class, true);
    }
}
