package net.preibisch.surface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class Util
{
	final static ExecutorService service = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );

	public static < T extends RealType< T > & NativeType< T > > ImagePlus getImagePlusInstance(
			final RandomAccessibleInterval< T > img )
	{
		return getImagePlusInstance( img, false, String.valueOf( img.hashCode() ), Double.NaN, Double.NaN, service );
	}

	public static < T extends RealType< T > & NativeType< T > > ImagePlus getImagePlusInstance(
			final RandomAccessibleInterval< T > img,
			final double min,
			final double max )
	{
		return getImagePlusInstance( img, false, img.toString(), min, max, service );
	}

	public static < T extends RealType< T > & NativeType< T > > ImagePlus getImagePlusInstance(
			final RandomAccessibleInterval< T > img,
			final boolean virtualDisplay,
			final String title,
			final double min,
			final double max )
	{
		return getImagePlusInstance( img, virtualDisplay, title, min, max, service );
	}

	@SuppressWarnings("unchecked")
	public static < T extends RealType< T > & NativeType< T > > ImagePlus getImagePlusInstance(
			final RandomAccessibleInterval< T > img,
			final boolean virtualDisplay,
			final String title,
			final double min,
			final double max,
			final ExecutorService service )
	{
		ImagePlus imp = null;

		if ( img instanceof ImagePlusImg )
			try { imp = ((ImagePlusImg<T, ?>)img).getImagePlus(); } catch (ImgLibException e) {}

		if ( imp == null )
		{
			if ( virtualDisplay )
				imp = ImageJFunctions.wrap( img, title, service );
			else
				imp = ImageJFunctions.wrap( img, title, service ).duplicate();
		}

		final double[] minmax = getFusionMinMax( img, min, max );

		imp.setTitle( title );
		imp.setDimensions( 1, (int)img.dimension( 2 ), 1 );
		imp.setDisplayRange( minmax[ 0 ], minmax[ 1 ] );

		return imp;
	}

	public static < T extends RealType< T > > double[] getFusionMinMax(
			final RandomAccessibleInterval<T> img,
			final double min,
			final double max )
	{
		final double[] minmax;

		if ( Double.isNaN( min ) || Double.isNaN( max ) )
			minmax = minMaxApprox( img );
		else if ( min == 0 && max == 65535 )
		{
			// 16 bit input was assumed, little hack in case it was 8-bit
			minmax = minMaxApprox( img );
			if ( minmax[ 1 ] <= 255 )
			{
				minmax[ 0 ] = 0;
				minmax[ 1 ] = 255;
			}
		}
		else
			minmax = new double[]{ (float)min, (float)max };

		return minmax;
	}

	public static < T extends RealType< T > > double[] minMaxApprox( final RandomAccessibleInterval< T > img )
	{
		if ( Views.iterable( img ).size() > 100*100*100 )
			return minMaxApprox( img, 1000 );
		else
			return minMax( img );
	}
	
	public static < T extends RealType< T > > double[] minMaxApprox( final RandomAccessibleInterval< T > img, final int numPixels )
	{
		return minMaxApprox( img, new Random( 3535 ), numPixels );
	}

	public static < T extends RealType< T > > double[] minMaxApprox( final RandomAccessibleInterval< T > img, final Random rnd, final int numPixels )
	{
		final RandomAccess< T > ra = img.randomAccess();

		// run threads and combine results
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;

		for ( int i = 0; i < numPixels; ++i )
		{
			for ( int d = 0; d < img.numDimensions(); ++d )
				ra.setPosition( rnd.nextInt( (int)img.dimension( d ) ) + (int)img.min( d ), d );

			final double v = ra.get().getRealDouble();

			min = Math.min( min, v );
			max = Math.max( max, v );
		}

		return new double[]{ min, max };
	}

	public static Img< FloatType > openAs32Bit( final File file )
	{
		return openAs32Bit( file, new ArrayImgFactory< FloatType >( new FloatType() ) );
	}

	@SuppressWarnings("unchecked")
	public static ArrayImg< FloatType, ? > openAs32BitArrayImg( final File file )
	{
		return (ArrayImg< FloatType, ? >)openAs32Bit( file, new ArrayImgFactory< FloatType >( new FloatType() ) );
	}

	public static Img< FloatType > openAs32Bit( final File file, final ImgFactory< FloatType > factory )
	{
		if ( !file.exists() )
			throw new RuntimeException( "File '" + file.getAbsolutePath() + "' does not exisit." );

		final ImagePlus imp = new Opener().openImage( file.getAbsolutePath() );

		if ( imp == null )
			throw new RuntimeException( "File '" + file.getAbsolutePath() + "' coult not be opened." );

		final Img< FloatType > img;

		if ( imp.getStack().getSize() == 1 )
		{
			// 2d
			img = factory.create( new int[]{ imp.getWidth(), imp.getHeight() } );
			final ImageProcessor ip = imp.getProcessor();

			final Cursor< FloatType > c = img.localizingCursor();
			
			while ( c.hasNext() )
			{
				c.fwd();

				final int x = c.getIntPosition( 0 );
				final int y = c.getIntPosition( 1 );

				c.get().set( ip.getf( x, y ) );
			}

		}
		else
		{
			// >2d
			img = factory.create( new int[]{ imp.getWidth(), imp.getHeight(), imp.getStack().getSize() } );

			final Cursor< FloatType > c = img.localizingCursor();

			// for efficiency reasons
			final ArrayList< ImageProcessor > ips = new ArrayList< ImageProcessor >();

			for ( int z = 0; z < imp.getStack().getSize(); ++z )
				ips.add( imp.getStack().getProcessor( z + 1 ) );

			while ( c.hasNext() )
			{
				c.fwd();

				final int x = c.getIntPosition( 0 );
				final int y = c.getIntPosition( 1 );
				final int z = c.getIntPosition( 2 );

				c.get().set( ips.get( z ).getf( x, y ) );
			}
		}

		return img;
	}

	public static < T extends RealType< T > > double[] minMax( final RandomAccessibleInterval< T > img )
	{
		final IterableInterval< T > iterable = Views.iterable( img );

		// run threads and combine results
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;

		for ( final T type : iterable )
		{
			final double v = type.getRealDouble();

			min = Math.min( min, v );
			max = Math.max( max, v );
		}

		return new double[]{ min, max };
	}
}
