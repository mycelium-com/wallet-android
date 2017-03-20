package crl.android.pdfwriter;

// Copyright (c) 2005, Luc Maisonobe
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with
// or without modification, are permitted provided that
// the following conditions are met:
// 
//    Redistributions of source code must retain the
//    above copyright notice, this list of conditions and
//    the following disclaimer. 
//    Redistributions in binary form must reproduce the
//    above copyright notice, this list of conditions and
//    the following disclaimer in the documentation
//    and/or other materials provided with the
//    distribution. 
//    Neither the names of spaceroots.org, spaceroots.com
//    nor the names of their contributors may be used to
//    endorse or promote products derived from this
//    software without specific prior written permission. 
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
// CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
// THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
// USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
// IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
// USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.


import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class encodes a binary stream into a text stream.
 * 
 * <p>
 * The ASCII85encoding is suitable when binary data needs to be transmitted or stored as text. It has been defined by
 * Adobe for the PostScript and PDF formats (see PDF Reference, section 3.3 Details of Filtered Streams).
 * </p>
 * 
 * <p>
 * The encoded stream is about 25% larger than the corresponding binary stream (32 binary bits are converted into 40
 * encoded bits, and there may be start/end of line markers).
 * </p>
 * 
 * @author Luc Maisonobe
 * @see ASCII85Decoder
 */
public class ASCII85Encoder extends FilterOutputStream
{

	/**
	 * Create an encoder wrapping a sink of binary data.
	 * <p>
	 * Calling this constructor is equivalent to calling {@link #ASCII85Encoder(OutputStream,int,byte[],byte[])
	 * ASCII85Encoder(<code>out</code>, <code>-1</code>, <code>null</code>, <code>null</code>)}.
	 * </p>
	 * 
	 * @param out
	 *            sink of binary data to filter
	 */
	public ASCII85Encoder( OutputStream out )
	{
		super( out );
		lineLength = -1;
		c1 = -1;
		phase = 4;
	}

	/**
	 * Create an encoder wrapping a sink of binary data.
	 * <p>
	 * The additional arguments allow to specify some text formatting
	 * </p>
	 * <p>
	 * Note that specifying a negative number for <code>lineLength</code> is really equivalent to calling the one
	 * argument {@link #ASCII85Encoder(OutputStream) constructor}.
	 * </p>
	 * <p>
	 * If non-null start/end of line are used, they must contain only whitespace characters as other characters would
	 * otherwise interfere with the decoding process on the other side of the channel. For safety, it is recommended to
	 * stick to space (' ', 0x32) and horizontal tabulation ('\t', 0x9) characters for the start of line marker, and to
	 * line feed ('\n', 0xa) and carriage return ('\r', 0xd) characters according to the platform convention for the end
	 * of line marker.
	 * </p>
	 * 
	 * @param out
	 *            sink of binary data to filter
	 * @param lineLength
	 *            maximal length of a ligne (counting <code>sol</code> but not counting <code>eol</code>), if
	 *            negative lines will not be split
	 * @param sol
	 *            start of line marker to use (mainly for indentation purposes), may be null
	 * @param eol
	 *            end of line marker to use, may be null only if <code>lineLength</code> is negative
	 */
	public ASCII85Encoder( OutputStream out, int lineLength, byte[] sol, byte[] eol )
	{
		super( out );
		this.lineLength = lineLength;
		this.sol = sol;
		this.eol = eol;
		this.c1 = -1;
		this.phase = 4;
	}

	/**
	 * Closes this output stream and releases any system resources associated with the stream.
	 * 
	 * @exception IOException
	 *                if the underlying stream throws one
	 */
	public void close() throws IOException
	{

		if ( c1 >= 0 )
		{
			c4 += c5 / 85;
			c3 += c4 / 85;
			c2 += c3 / 85;
			c1 += c2 / 85;

			// output only the required number of bytes
			putByte( 33 + c1 );
			putByte( 33 + (c2 % 85) );
			if ( phase > 1 )
			{
				putByte( 33 + (c3 % 85) );
				if ( phase > 2 )
				{
					putByte( 33 + (c4 % 85) );
					if ( phase > 3 )
					{
						putByte( 33 + (c5 % 85) );
					}
				}
			}

			// output the end marker
			putByte( '~' );
			putByte( '>' );

		}

		// end the last line properly
		if ( length != 0 )
		{
			out.write( eol, 0, eol.length );
		}

		// close the underlying stream
		out.close();

	}

	/**
	 * Writes the specified byte to this output stream.
	 * 
	 * @param b
	 *            byte to write (only the 8 low order bits are used)
	 */
	public void write( int b ) throws IOException
	{

		b = b & 0xff;

		switch( phase )
		{
			case 1:
				c3 += 9 * b;
				c4 += 6 * b;
				c5 += b;
				phase = 2;
				break;
			case 2:
				c4 += 3 * b;
				c5 += b;
				phase = 3;
				break;
			case 3:
				c5 += b;
				phase = 4;
				break;
			default:
				if ( c1 >= 0 )
				{
					// there was a preceding quantum, we now know it was not the last
					if ( (c1 == 0) && (c2 == 0) && (c3 == 0) && (c4 == 0) && (c5 == 0) )
					{
						putByte( 'z' );
					}
					else
					{
						c4 += c5 / 85;
						c3 += c4 / 85;
						c2 += c3 / 85;
						c1 += c2 / 85;
						putByte( 33 + c1 );
						putByte( 33 + (c2 % 85) );
						putByte( 33 + (c3 % 85) );
						putByte( 33 + (c4 % 85) );
						putByte( 33 + (c5 % 85) );
					}
				}
				c1 = 0;
				c2 = 27 * b;
				c3 = c2;
				c4 = 9 * b;
				c5 = b;
				phase = 1;
		}

	}

	/**
	 * Put a byte in the underlying stream, inserting line breaks as needed.
	 * 
	 * @param b
	 *            byte to put in the underlying stream (only the 8 low order bits are used)
	 * @exception IOException
	 *                if the underlying stream throws one
	 */
	private void putByte( int b ) throws IOException
	{
		if ( lineLength >= 0 )
		{
			// split encoded lines if needed
			if ( (length == 0) && (sol != null) )
			{
				out.write( sol, 0, sol.length );
				length = sol.length;
			}
			out.write( b );
			if ( ++length >= lineLength )
			{
				out.write( eol, 0, eol.length );
				length = 0;
			}
		}
		else
		{
			out.write( b );
		}
	}

	/** Line length (not counting eol). */
	private int		lineLength;

	/** Start of line marker (indentation). */
	private byte[]	sol;

	/** End Of Line marker. */
	private byte[]	eol;

	/** Coefficients of the 32-bits quantum in base 85. */
	private int		c1;
	private int		c2;
	private int		c3;
	private int		c4;
	private int		c5;

	/** Phase (between 1 and 4) of raw bytes. */
	private int		phase;

	/** Current length of the line being written. */
	private int		length;

}