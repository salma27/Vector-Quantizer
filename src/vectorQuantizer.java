import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import javax.imageio.ImageIO;

//C:\Users\lenovo\Desktop\DSC100279383.jpg
public class vectorQuantizer {
	public static Vector<int[][]> all = new Vector<int[][]>();
	public static Vector<String> compressed = new Vector<String>();
	public static Vector<double[][]> decompressed = new Vector<double[][]>();
	public static double[][] decompressedImg;
	public static int[][] deImg;
	public static int oh = 0 , ow = 0;
	public static int[][] originalImg;
	public static int[][] original;	//afterPadding
	public static vec ori;	//original after padding + w + h
	public static double[][] avg;
	public static int hb = 0 , wb = 0; 
	public static int cbs = 0;
	public static Map<String , double[][]> mp = new HashMap<String , double[][]>();
	public static Vector<double[][]> blocks = new Vector<double[][]>();
	public static Vector<Block> block = new Vector<Block>();	//each block + its group 
	public static int type;
	
	public void main(String img , int hbb , int wbb , int cbss , boolean co , boolean de) throws FileNotFoundException, UnsupportedEncodingException {
		String fileName = img;
		hb = hbb;
		wb = wbb;
		cbs = cbss;
		if(co) {
			originalImg = readImage(fileName); 
			original = padding();
			divideImg();
			avg = getAvg();
		}
		if(cbs != 0) {
			if(co) {
				splitting();
				checkGroups();
				code();
				fillCompressed();
				fillFile();
				
			}
			if(de) {
				//readFile();
				fillDecompressed();
				reconstruct();
				
			}
		}
		else {
			writeImage(ori.data ,"C:\\Users\\lenovo\\Desktop\\output.jpg",ori.w,ori.h);
		}
		System.out.println("done");
	}
	public static void print() {
		System.out.println(compressed.size());
		for(int i = 0 ; i < compressed.size() ; i++) {
			System.out.println(compressed.elementAt(i));
		}
		ArrayList<String> codes = new ArrayList<String>(mp.keySet());
		System.out.println(codes.size());
		for(int k = 0 ; k < codes.size() ; k++) {
			System.out.println(codes.get(k));
			for(int i = 0 ; i < hb ; i++) {
				for(int j = 0; j < wb ; j++) {
					System.out.println(mp.get(codes.get(k))[i][j]);
				}
			}
		}
		System.out.println(ori.h);
		System.out.print(ori.w);
		System.out.println("");
	}
	public static void readFile() throws FileNotFoundException {
		Scanner file = new Scanner(new File("C:\\Users\\lenovo\\Desktop\\compressed.txt"));
		int compressedSize = Integer.parseInt(file.nextLine());
		String code = "";
		for(int i = 0 ; i < compressedSize ; i++) {
			code = "";
			code = file.nextLine();
			compressed.add(code);	
		}
		int mpSize = Integer.parseInt(file.nextLine());
		String key = "";
		double[][] arr = new double[hb][wb];
		for(int k = 0 ; k < mpSize ; k++) {
			key = "";
			key = file.nextLine();
			for(int i = 0 ; i < hb ; i++) {
				for(int j = 0 ; j < wb ; j++) {
					arr[i][j] = Double.parseDouble(file.nextLine());
				}
			}
			mp.put(key , arr);
		}
		int x = Integer.parseInt(file.nextLine());
		int y = Integer.parseInt(file.nextLine());
		type = Integer.parseInt(file.nextLine());
		ori = new vec(x , y);
		ori.h = x;
		ori.w = y;
		file.close();
	}
	public static void fillFile() throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter("C:\\Users\\lenovo\\Desktop\\compressed.txt", "UTF-8");
		writer.println(compressed.size());
		for(int i = 0 ; i < compressed.size() ; i++) {
			writer.println(compressed.elementAt(i));
		}
		ArrayList<String> codes = new ArrayList<String>(mp.keySet());
		writer.println(codes.size());
		for(int k = 0 ; k < codes.size() ; k++) {
			writer.println(codes.get(k));
			for(int i = 0 ; i < hb ; i++) {
				for(int j = 0; j < wb ; j++) {
					writer.println(mp.get(codes.get(k))[i][j]);
				}
			}
		}
		writer.println(ori.h);
		writer.println(ori.w);
		writer.print(type);
		writer.close();
	}
	public static void fillDecompressed() {
		for(int i = 0 ; i < compressed.size() ; i++) {
			decompressed.add(mp.get(compressed.elementAt(i)));
		}	
	}
  
	public static void reconstruct() {
		decompressedImg = new double[ori.h][ori.w];
		deImg = new int[ori.h][ori.w];
		int index = 0;
		for(int i = 0 ; i  < ori.h ; i += hb) {
			for(int j = 0 ; j < ori.w ; j += wb) {
				for(int m = i , k = 0; m < i + hb ; m++ , k++) {
					for(int n = j , p = 0 ; n < j + wb ; n++ , p++) {
						decompressedImg[m][n] = decompressed.get(index)[k][p];
						deImg[m][n] = (int)Math.round(decompressedImg[m][n]);
						//System.out.println(deImg[m][n]);
					}
				}
				index++;
			}
		}
		//System.out.println(ori.w  + ori.h);
		writeImage(deImg , "C:\\Users\\lenovo\\Desktop\\output.jpg" , ori.w , ori.h);
	}
	public static void fillCompressed() {
		for(int i = 0 ; i  < all.size() ; i++) {
			compressed.add("");
		}
		int index = 0;
		for(int k = 0 ; k < block.size() ; k++) {
			for(int i = 0 ; i < block.get(k).group.size() ; i++) {
				index = all.indexOf(block.get(k).group.get(i));
				compressed.set(index, block.get(k).code);
			}
		}
	}
	public static int blockIndex(int[][] arr) {
		int index = 0;
		double distance = Double.MAX_VALUE;
		double tmp = 0;
		for(int k = 0 ; k < blocks.size() ; k++) {
			for(int i = 0 ; i < hb ; i++) {
				for(int j = 0 ; j < wb ; j++) {
					tmp += Math.abs(blocks.get(k)[i][j] - arr[i][j]);
				}
			}
			if(distance > tmp) {
				distance = tmp;
				index = k;
			}
			tmp = 0;
		}
		return index;
	}
	public static int blockIndexGroup(int[][] arr) {
		int index = 0;
		double distance = Double.MAX_VALUE;
		double tmp = 0;
		for(int k = 0 ; k < block.size() ; k++) {
			for(int i = 0 ; i < hb ; i++) {
				for(int j = 0 ; j < wb ; j++) {
					tmp += Math.abs(block.get(k).block[i][j] - arr[i][j]);
				}
			}
			if(distance > tmp) {
				distance = tmp;
				index = k;
			}
			tmp = 0;
		}
		return index;
	}
	public static void code() {
		int size = (int) (Math.log(cbs) / Math.log(2));
		for(int i = 0 ; i < block.size() ; i++) {
			block.elementAt(i).code = Integer.toBinaryString(i);
			while(block.elementAt(i).code.length() < size) {
				block.elementAt(i).code = "0" + block.elementAt(i).code;
			}
			mp.put(block.elementAt(i).code , block.elementAt(i).block);
		}
	}
	public static void getAvgBlock() {
		for(int k = 0 ; k < block.size() ; k++) {
			double res[][] = new double[hb][wb];
			for(int i = 0 ; i < hb ; i++) {
				for(int j = 0 ; j < wb ; j++) {
					res[i][j] = 0;
				}
			}
			for(int i = 0 ; i < block.get(k).group.size() ; i++) {
				res = addMatrices(res , block.get(k).group.get(i));
			}
			for(int i = 0 ; i  < hb ; i++) {
				for(int j = 0  ; j < wb ; j++) {
					res[i][j] /= block.get(k).group.size();
				}
			}
			block.get(k).blockToCompare = res;
		}
	}
	public static boolean checkDiff() {
		getDiff();
		for(int i = 0 ; i  < block.size() ; i++) {
			if(block.get(i).diff != 0) {
				return false;
			}
		}
		return true;
	}
	public static boolean checkError() {
		getDiff();
		for(int i = 0 ; i  < block.size() ; i++) {
			if(block.get(i).diff > 1) {
				return false;
			}
		}
		return true;
	}
	public static void getDiff() {
		double tmp = 0;
		for(int k = 0 ; k < block.size() ; k++) {
			for(int i = 0 ; i < hb ; i++) {
				for(int j = 0 ; j < wb ; j++) {
					tmp += Math.abs(block.get(k).block[i][j] - block.get(k).blockToCompare[i][j]);
				}
			}
			block.get(k).diff = tmp;
			tmp = 0;
		}
	}
	public static void checkGroups() {
		for(int k = 0 ; k < 10 ; k++) {
			if(checkDiff()) {
				break;
			}
			else if(checkError()) {
				break;
			}
			else {
				for(int j = 0 ; j < block.size() ; j++) {
					block.get(j).block = block.get(j).blockToCompare;
					block.get(j).group.clear();
				}
				for(int i = 0 ; i < all.size() ; i++) {
					int index = blockIndexGroup(all.get(i));
					block.get(index).group.add(all.get(i));
				}
				getAvgBlock();
			}
		}
 
	}
	public static void grouping(){
		block.clear();
		for(int i = 0 ; i < blocks.size() ; i++) {
			double[][] tmp = blocks.get(i);
			Block b = new Block();
			b.block = tmp;
			block.add(b);
		}
		for(int i = 0 ; i < all.size() ; i++) {
			int index = blockIndex(all.get(i));
			block.get(index).group.add(all.get(i));
		}
	}
	public static class Block{
		double[][] block;
		double[][] blockToCompare;
		double diff;
		String code;
		Vector<int[][]> group = new Vector<int[][]>();
		public Block() {
			code = "";
			diff = 0.0;
			block = new double[hb][wb];
			blockToCompare = new double[hb][wb];
		}
	}
	public static void splitting() {
		Vector<double[][]> parent = new Vector<double[][]>();
		parent.add(avg);
		while(cbs != blocks.size()) {
			blocks.clear();
			block.clear();
			for(int k = 0 ; k  < parent.size() ; k++) {
				double[][] arr1 = new double[hb][wb];
				double[][] arr2 = new double[hb][wb];
				for(int i = 0 ; i  < hb ; i++) {
					for(int j = 0 ; j < wb ; j++) {
						if(parent.get(k)[i][j] % 1 == 0) {
							arr1[i][j] = parent.get(k)[i][j] - 1;
							arr2[i][j] = parent.get(k)[i][j] + 1;
						}
						else {
							arr1[i][j] = Math.floor(parent.get(k)[i][j]);
							arr2[i][j] = Math.ceil(parent.get(k)[i][j]);
						}
					}
				}
				blocks.add(arr1);
				blocks.add(arr2);
				grouping();
				getAvgBlock();
			}
			parent.clear();
			for(int i = 0 ; i < blocks.size() ; i++) {
				double[][] arr = block.get(i).blockToCompare;
				parent.add(arr);
			}
		}
	}
	public static class vec{
		int h;
		int w;
		int[][] data;
		public vec() {
			h = 0; 
			w = 0;
			this.data = new int[h][w];
			for(int i = 0 ; i < h ; i++) {
				for(int j = 0 ; j  < w ; j++) {
					this.data[i][j] = 0;
				}
			}
		}
		public vec(int h , int w , int[][]data) {
			this.h = h;
			this.w = w;
			this.data = new int[h][w];
			for(int i = 0 ; i < h ; i++) {
				for(int j = 0 ; j  < w ; j++) {
					this.data[i][j] = data[i][j];
				}
			}
		}
		public vec(int h , int w) {
			this.h = h;
			this.w = w;
			this.data = new int[h][w];
			for(int i = 0 ; i < h ; i++) {
				for(int j = 0 ; j  < w ; j++) {
					this.data[i][j] = 0;
				}
			}
		}
	}
	public static int[][] padding() {
		int[][]tmp;
		int tmph =oh;
		int tmpw = ow;
		while(oh % hb != 0) {
			tmph++;
		}
		while(ow % wb != 0) {
			tmpw++;
		}
		tmp = new int[tmph][tmpw];
		for(int i = 0 ; i < oh ; i++) {
			for(int j = 0 ; j  < ow ; j++) {
				tmp[i][j] = originalImg[i][j];
			}
		}
		for(int i = oh ; i < tmph ; i++) {
			for(int j = ow ; j < tmpw ; j++) {
				tmp[i][j] = 0;
			}
		}
 
		ori = new vec(tmph , tmpw , tmp);
 
		return tmp;
	}
	public static void divideImg() {
		int[][]arr = null;
		System.out.println();
		for(int i = 0 ; i  < ori.h ; i += hb) {
			for(int j = 0 ; j < ori.w ; j += wb) {
				arr = new int[hb][wb];
				for(int m = i , k = 0; m < i + hb ; m++ , k++) {
					for(int n = j , p = 0 ; n < j + wb ; n++ , p++) {
						arr[k][p] = ori.data[m][n];
					}
				}
				all.add(arr);
			}
		}
	}
	public static double[][] addMatrices(double[][]m1 ,double[][]m2) {
		double[][]res = new double[hb][wb];
		for(int i = 0 ; i < hb ; i++) {
			for(int j = 0 ;  j < wb ; j++) {
				res[i][j] = m1[i][j] + m2[i][j];
			}
		}
		return res;
	}
	public static double[][] addMatrices(double[][]m1 ,int[][]m2) {
		double[][]res = new double[hb][wb];
		for(int i = 0 ; i < hb ; i++) {
			for(int j = 0 ;  j < wb ; j++) {
				res[i][j] = m1[i][j] + m2[i][j];
			}
		}
		return res;
	}
	public static double[][] getAvg() {
		double res[][] = new double[hb][wb];
		for(int i = 0 ; i < hb ; i++) {
			for(int j = 0 ; j < wb ; j++) {
				res[i][j] = 0;
			}
		}
		for(int i = 0 ; i < all.size() ; i++) {
			res = addMatrices(res , all.get(i));
		}
		for(int i = 0 ; i  < hb ; i++) {
			for(int j = 0  ; j < wb ; j++) {
				res[i][j] /= all.size();
			}
		}
		return res;
	}
 
	public static int[][] readImage( String filePath ){
		try {
			BufferedImage image;
			File input 	= new File( filePath );
 
			image 			= ImageIO.read(input);
			int width 		= image.getWidth();
			int height 		= image.getHeight();
			int imageType 	= image.getType();
			type = imageType;
			ow = width;
			oh = height;
			int pixel[][] = new int[height][width];
			for ( int i = 0; i < height; ++i ) {
				for ( int j = 0; j < width; ++j ) {
					Color pixelColor = new Color( image.getRGB(j, i) );
 
					int red 	= (int)( 0.299 * pixelColor.getRed() 	);
					int green 	= (int)( 0.587 * pixelColor.getGreen() 	);
					int blue 	= (int)( 0.114 * pixelColor.getBlue() 	);	
					pixel[i][j] = red + green + blue;
				}
			}
			return pixel;
		}
		catch (Exception e) {
			System.out.println( "Image can't be readed" );
		}
		return null;
	}
	public static void writeImage(int[][] pixels,String outputFilePath,int width,int height)
    {
        File fileout=new File(outputFilePath);
        BufferedImage image2=new BufferedImage(width , height , type );
 
        for(int x=0;x<height ;x++)
        {
            for(int y=0;y<width;y++)
            {
            	Color pixelColor = new Color( pixels[x][y] , pixels[x][y] , pixels[x][y] );
            	image2.setRGB( y , x , pixelColor.getRGB() );
            }
        }
 
        try
        {
            ImageIO.write(image2, "jpg", fileout);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}