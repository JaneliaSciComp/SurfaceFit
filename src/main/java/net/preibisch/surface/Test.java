package net.preibisch.surface;

import java.io.File;

import de.mpicbg.scf.mincostsurface.MinCostZSurface;
import ij.IJ;
import ij.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class Test
{
	public static void main( String[] args )
	{
		new ImageJ();

		final Img< FloatType > img = Util.openAs32Bit( new File( "resampled_orth_upper.tif" ) );

		final Img< IntType > surface = process2( img, 5, 40, 20 );

		final Img< FloatType > rendererSurface = img.factory().create( img );

		renderDepthMap( rendererSurface, surface );

		//Gauss3.gauss( 0.7, Views.extendMirrorSingle( rendererSurface ), rendererSurface );

		Util.getImagePlusInstance( img ).show();
		Util.getImagePlusInstance( surface ).show();
		Util.getImagePlusInstance( rendererSurface ).show();
	}

	public static < T extends RealType<T> > void renderDepthMap( final RandomAccessibleInterval< T > render, final RandomAccessibleInterval< IntType > surface )
	{
		final Cursor< IntType > c = Views.iterable( surface ).localizingCursor();
		final RandomAccess< T > r = render.randomAccess();

		final long[] l = new long[ 3 ];

		while (c.hasNext() )
		{
			c.fwd();
			c.localize( l );
			l[ 2 ] = c.get().get() - 1;// this offset is necessary to match the image

			r.setPosition( l );
			r.get().setOne();
		}
	}

	public static < T extends RealType<T> > void renderDepthMap2( final RandomAccessibleInterval< T > render, final RandomAccessibleInterval< T > surface )
	{
		final Cursor< T > c = Views.iterable( surface ).localizingCursor();
		final RandomAccess< T > r = render.randomAccess();

		final long[] l = new long[ 3 ];

		while (c.hasNext() )
		{
			c.fwd();
			c.localize( l );
			l[ 2 ] = (long) (c.get().getRealFloat() - 1);// this offset is necessary to match the image
			System.out.println(l[ 2 ]);

			r.setPosition( l );
			r.get().setOne();
		}
	}

	public static < T extends RealType<T> > Img< IntType > process2(
			final Img< T > cost_orig,
			final int max_dz, // max delta z, default = 1, constraint on the surface altitude change from one pixel to another
			final int min_dist, // Max_distance between surfaces (in pixel), default = 15
			final int max_dist // Min_distance between surfaces (in pixel), default = 3
			)
	{
		final int nDim = cost_orig.numDimensions();

		if ( nDim != 3 )
			throw new RuntimeException( "Dim=3 required." );

		long end,start;
		long[] dims_orig = new long[ nDim ];
		cost_orig.dimensions( dims_orig );


		///////////////////////////////////////////////////////////////////////////////////////////////////
		// creating a surface detector solver instance  ///////////////////////////////////////////////////
		MinCostZSurface<T> ZSurface_detector = new MinCostZSurface<T>();
		
		
		///////////////////////////////////////////////////////////////////////////////////////////////////
		// filling the surface graph for a single surface 
		start = System.currentTimeMillis();
		
		ZSurface_detector.Create_Surface_Graph( cost_orig, max_dz );
		//ZSurface_detector.Create_Surface_Graph( cost_orig, max_dz );
		//ZSurface_detector.Add_NoCrossing_Constraint_Between_Surfaces(1, 2, min_dist, max_dist);
		
		end = System.currentTimeMillis();
		IJ.log("...done inserting edges. (" + (end - start) + "ms)");
		
		
		///////////////////////////////////////////////////////////////////////////////////////////////////
		// computing the maximum flow //////////////////////////////////////////////////////////////////////
		IJ.log("Calculating max flow");
		start = System.currentTimeMillis();
		
		ZSurface_detector.Process();
		float maxFlow = ZSurface_detector.getMaxFlow();
		
		end = System.currentTimeMillis();
		IJ.log("...done. Max flow is " + maxFlow + ". (" + (end - start) + "ms)");
		
		
		/////////////////////////////////////////////////////////////////////////////////////////////
		// building the depth map, upsample the result 	and display it //////////////////////////////
		IJ.log("n surfaces: " + ZSurface_detector.getNSurfaces() );
		Img<IntType> depth_map1 =  ZSurface_detector.get_Altitude_MapInt(1);
		//Img<FloatType> depth_map2 =  ZSurface_detector.get_Altitude_Map(2);

		//ImageJFunctions.show( depth_map1, "altitude map1" );
		//ImageJFunctions.show( depth_map2, "altitude map2" );

		/*
		if (display_excerpt)
		{
			IJ.log("creating z surface reslice" );
			Img<T> excerpt1 = img_utils.ZSurface_reslice(image_orig, upsampled_depthMap1, output_height/2, output_height/2);
			ImageJFunctions.show(excerpt1, "excerpt");
			Img<T> excerpt2 = img_utils.ZSurface_reslice(image_orig, upsampled_depthMap2, output_height/2, output_height/2);
			ImageJFunctions.show(excerpt2, "excerpt");
		}
		*/
		IJ.log("processing done");
		
		return depth_map1;
	}

}
