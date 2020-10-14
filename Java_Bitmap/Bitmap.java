/*
*Author: Andrew Murwin
*Date: August 10, 2019
*Description: Bitmap Class
*
*The Bitmap class is intended to create a user-friendly way to create .bmp files. It also deals with the non-intuitive
*aspects for the user, such as that RGB values are stored in BGR order and the origin of a bmp is in the bottom left.
*Most functions are intentionally private to prevent the user from overcomplicating their code and accidentally double
*including a header
* 
*The class include semi-robust error checking on validating data fed into the bitmap generators
*
*.jar Creation Instructions:
*	javac Bitmap.java
*	javac Pixel.java
*	jar cf Bitmap.jar Bitmap.class Pixel.class
*/
import java.io.*;

public class Bitmap {
	final private static int BYTES_PER_PIXEL = 3; // red, green, blue
	final private static int FILE_HEADER_SIZE = 14; // Number of bytes in the file header - predetermined by .bmp format
	final private static int INFO_HEADER_SIZE = 40; // Number of bytes in the info header - predetermined by .bmp format

	// Converts an integer to little-endian format array for use in file and info
	// headers
	private static int[] toLe(int number) {
		int[] leArray = new int[4];
		for (int i = 0; i < 4; i++) {
			leArray[i] = number % 256;
			number = number / 256;
		}
		return leArray;
	}

	// Generates bitmap file header for use in creating the bmp file
	private static int[] createBitmapFileHeader(int height, int width, int paddingSize) {

		// Size of file in bytes
		int fileSize = FILE_HEADER_SIZE + INFO_HEADER_SIZE + (((BYTES_PER_PIXEL * width) + paddingSize) * height);
		int[] fileHeader = new int[FILE_HEADER_SIZE];
		int[] sizeLeBytes = toLe(fileSize);
		fileHeader[0] = 66; // 'B' - Indicates Windows rather than OS/2
		fileHeader[1] = 77; // 'M' - indicates Windows rather than OS/2
		fileHeader[2] = sizeLeBytes[0]; // Image file size split into four bites
		fileHeader[3] = sizeLeBytes[1]; // The bites are writte in little endian style,
		fileHeader[4] = sizeLeBytes[2]; // Starting with the "smallest" (right-most) byte
		fileHeader[5] = sizeLeBytes[3]; // Byte 4
		fileHeader[10] = FILE_HEADER_SIZE + INFO_HEADER_SIZE; // Location of the start of the actual picture
		return fileHeader;
	}

	// Generates bitmap info header for use in creating the bmp file
	private static int[] createBitmapInfoHeader(int height, int width) {
		int[] infoHeader = new int[INFO_HEADER_SIZE];
		int[] widthLeBytes = toLe(width);
		int[] heightLeBytes = toLe(height);
		infoHeader[0] = INFO_HEADER_SIZE; // Size of info header
		infoHeader[4] = widthLeBytes[0]; // Byte 1 of width in little endian format
		infoHeader[5] = widthLeBytes[1]; // Byte 2
		infoHeader[6] = widthLeBytes[2]; // Byte 3
		infoHeader[7] = widthLeBytes[3]; // Byte 4
		infoHeader[8] = heightLeBytes[0]; // Byte 1 of height in little endian format
		infoHeader[9] = heightLeBytes[1]; // Byte 2
		infoHeader[10] = heightLeBytes[2]; // Byte 3
		infoHeader[11] = heightLeBytes[3]; // Byte 4
		infoHeader[12] = 1; // Number of color planes
		infoHeader[14] = BYTES_PER_PIXEL * 8; // Bits per pixel
		return infoHeader;
	}

	// Flattens user generated 2D array for use in functions
	public static void generateBitmapImage(Pixel[][] bits2D, String fileName) throws Exception {
		
		// Makes sure each sub array is the same length to prevent NullPointerExceptions
		// from occurring later on
		for (int i = 1; i < bits2D.length; i++) {
			if (!(bits2D[0].length == bits2D[i].length)) {
				throw new Exception("All sub-arrays must be of consistent length");
			}
		}
		try {
			Pixel[] bits = new Pixel[bits2D.length * bits2D[0].length];
			for (int i = 0; i < bits2D.length; i++) {
				for (int j = 0; j < bits2D[0].length; j++) {
					bits[(i * bits2D[0].length) + j] = bits2D[i][j];
				}
			}
			// Calls standardized Bitmap generator based on data extracted from array
			generateBitmapImage(bits2D.length, bits2D[0].length, bits, fileName);
		} catch (Exception e) { // Push any exceptions generated by standarized call up to main
			throw e;
		}
	}

	// Generates bitmap image
	public static void generateBitmapImage(int height, int width, Pixel[] bits, String fileName) throws Exception {
		
		// Confirms validity of dimensions to prevent creation of bitmaps with no data
		if (width < 1 || height < 1) {
			throw new Exception("Invalid height or width. Both parameters must have a value greater than 0.");
		}
		
		// Makes sure array provided is correct length for provided height and width
		if (bits.length != height * width) {
			throw new Exception("Invalid Pixel count.\nProvided: " + bits.length + "\nExpected: " + height * width);
		}
		
		// Padding size, rounds out to the 4th byte to follow bmp conventions
		int paddingSize = (4 - ((width * BYTES_PER_PIXEL) % 4)) % 4; 
		int[] fileHeader = createBitmapFileHeader(height, width, paddingSize);
		int[] bitmapHeader = createBitmapInfoHeader(height, width);

		// Make sure file name has correct extension
		if (fileName.length() > 4 && !fileName.substring(fileName.length() - 4).equals(".bmp")) {
			fileName = fileName.concat(".bmp");
		} else if (fileName.length() == 0) {
			fileName = "defaultOutput.bmp";
		} else {
			fileName = fileName.concat(".bmp");
		}

		// Stores data from file header, info header, and pixel array for output to file
		Pixel temp;
		byte[] data = new byte[((((width + paddingSize) * height) * 3) + 54)];
		int dataCounter = 0;
		for (int i = 0; i < 14; i++) {
			data[dataCounter++] = Integer.valueOf(fileHeader[i]).byteValue();
		}
		for (int i = 0; i < 40; i++) {
			data[dataCounter++] = Integer.valueOf(bitmapHeader[i]).byteValue();
		}
		for (int i = height - 1; i >= 0; i--) { //Order reversed due to origin being located in bottom left
			for (int j = 0; j < width; j++) {	//Bitmaps store pixels in Blue, Green, Red order
				temp = bits[(i * width) + j];
				data[dataCounter++] = Integer.valueOf(temp.getBlue()).byteValue();	
				data[dataCounter++] = Integer.valueOf(temp.getGreen()).byteValue();
				data[dataCounter++] = Integer.valueOf(temp.getRed()).byteValue();
			}
			for (int j = 0; j < paddingSize; j++) {
				data[dataCounter++] = 0;
			}
		}

		// Deletes pre-existing files with the same name
		File file = new File(fileName);
		if (file.exists()) {
			file.delete();
		}

		// Writes data to file
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(fileName, true));
			bos.write(data);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} finally { // Close the file even if there was an error to retain proper file access
			try {
				if (bos != null) {
					bos.close();
				}
			} catch (IOException e) {
				// Do nothing
			}
		}
	}
}