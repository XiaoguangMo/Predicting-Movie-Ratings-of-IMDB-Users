package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class IBCF {
	public static String training_set = "/Users/xiaoguangmo/Downloads/ml-100k/u5.base";
	public static String testing_set = "/Users/xiaoguangmo/Downloads/ml-100k/u5.test";	
	public static int numUser = 0;
	public static int numMovie = 0;
	public static float threshold = 1.0f;
	
	public static void main(String[] args) {
		if (args.length > 0 && args[1] != null && args[1].length() > 0) training_set = args[1];
		if (args.length > 0 && args[2] != null && args[2].length() > 0) testing_set = args[2];
		ArrayList<ArrayList<String>> training = readFile(training_set);
		int[][] item_user = getItemUserMatrix(training);
		float[] user_average_rating = getUserAverageRating(item_user);
		float[][] item_item = getItemItemMatrix(item_user, user_average_rating);
		
		System.out.println("Training done");
//		printMatrix(matrix);
//		printMatrix(item_user);
//		printMatrix(user_average_rating);
//		printMatrix(item_item);
		
		ArrayList<ArrayList<String>> testing = readFile(testing_set);
//		float accuracy = getAccuracy(testing, item_item, item_user);
//		System.out.println("Accuracy : " + accuracy);
		float rmsd = getRMSD(testing, item_item, item_user, user_average_rating);
		System.out.println("RMSD : " + rmsd);
		System.out.println("base : " + baseCaseRMSD(user_average_rating, testing));
	}
	public static float baseCaseRMSD(float[] user_average_rating, ArrayList<ArrayList<String>> real) {
		float sum = 0.0f;
		for (int i = 0; i < real.size(); i++) {
			if (Integer.parseInt(real.get(i).get(0)) >= user_average_rating.length) break;
			float a = Math.round(user_average_rating[Integer.parseInt(real.get(i).get(0))]);
			float b = Integer.parseInt(real.get(i).get(2));
			sum += (a - b) * (a - b);
		}
		sum /= (float)real.size();
		sum = (float) Math.sqrt(sum);
		return sum;
	}
	
	public static float getAccuracy(ArrayList<ArrayList<String>> real, float[][] item_item, int[][] item_user) {
		float hit = 0;
		for (int i = 0; i < real.size(); i++) {
			if (Integer.parseInt(real.get(i).get(0)) >= item_user.length || Integer.parseInt(real.get(i).get(1)) >= item_user[0].length) break;
			float a = getPrediction(Integer.parseInt(real.get(i).get(1)), Integer.parseInt(real.get(i).get(0)), item_item, item_user);
			float b = Integer.parseInt(real.get(i).get(2));
			if (Math.abs(a - b) <= threshold) hit++;
		}
		return hit / (float)real.size();
	}
	
	public static float getRMSD(ArrayList<ArrayList<String>> real, float[][] item_item, int[][] item_user, float[] user_average_rating) {
		float sum = 0.0f;
		for (int i = 0; i < real.size(); i++) {
			if (Integer.parseInt(real.get(i).get(0)) >= item_user.length || Integer.parseInt(real.get(i).get(1)) >= item_user[0].length) break;
			float a = getPrediction(Integer.parseInt(real.get(i).get(1)), Integer.parseInt(real.get(i).get(0)), item_item, item_user);
			float b = Integer.parseInt(real.get(i).get(2));
//			System.out.println(a + " " + b);
			if (a != 0.0f) {
				sum += (a - b) * (a - b);
			}
		}
		sum /= (float)real.size();
		sum = (float)Math.sqrt(sum);
		return sum;
	}
	
	public static float getPrediction(int item, int user, float[][] item_item, int[][] item_user) {
		float sumA = 0f, sumB = 0f;
		for (int i = 0; i < item_item.length; i++) {
			if (item_item[item][i] != 0 && item_user[user][i] != 0) {
				sumA += item_item[item][i] * item_user[user][i];
				sumB += item_item[item][i];
//				if (Float.isNaN(item_user[user][i])) System.out.println("nan");
//				if (sumB == 0) System.out.println("nan");
			}
		}
//		if (Float.isNaN(sumA)) System.out.println("a");
//		if (Float.isNaN(sumB)) System.out.println("b");
//		if (sumA == 0) System.out.println("0");
		return Float.isNaN(sumA / sumB) ? 0 : sumA / sumB;
	}
	
	public static float[][] getItemItemMatrix(int[][] matrix, float[] average) {
		float[][] ret = new float[numMovie][numMovie];
		for (int i = 0; i < matrix[0].length - 1; i++) {
			for (int j = i + 1; j < matrix[0].length; j++) {
				float sumA = 0f, sumB = 0f, sumC = 0f;
				for (int k = 0; k < matrix.length; k++) {
					if (matrix[k][i] > 0 && matrix[k][j] > 0) {
						float a = (float)matrix[k][i];
						float b = (float)matrix[k][j];
						sumA += (a - average[k]) * (b - average[k]);
						sumB += (a - average[k]) * (a - average[k]);
						sumC += (b - average[k]) * (b - average[k]);
					}
				}
				sumB = (float) Math.sqrt(sumB);
				sumC = (float) Math.sqrt(sumC);
//				if (Float.isNaN(sumA / (sumB * sumC))) System.out.println("nan");
//				if (!Float.isNaN(sumA / (sumB * sumC))) {
//					if (Math.abs(sumA / (sumB * sumC)) > 1.1) {
//						System.out.println(sumA / (sumB * sumC));
//					}
//				}
//				System.out.println(Float.isNaN(sumA / (sumB * sumC)) ? 0.0f : Math.abs(sumA / (sumB * sumC)));
				ret[j][i] = Float.isNaN(sumA / (sumB * sumC)) ? 0.0f : Math.abs(sumA / (sumB * sumC));
			}
		}
		return ret;
	}
	
	public static float[] getUserAverageRating(int[][] matrix) {
		float[] ret = new float[numUser];
		for (int i= 0; i < matrix.length; i++) {
			float sum = 0, count = 0;
			for (int j = 0; j < matrix[i].length; j++) {
				if (matrix[i][j] > 0) {
					sum += matrix[i][j];
					count++;
				}
			}
			if (count != 0) {
				ret[i] = sum / count;
			}
		}
		return ret;
	}
	
	public static int[][] getItemUserMatrix(ArrayList<ArrayList<String>> matrix) {
		int[][] ret = new int[numUser][numMovie];
		for (int i= 0; i < matrix.size(); i++) {
			ret[Integer.parseInt(matrix.get(i).get(0))][Integer.parseInt(matrix.get(i).get(1))] = Integer.parseInt(matrix.get(i).get(2));
		}
		return ret;
	}
	
	public static void printMatrix(float[] matrix) {
		for (int i = 0; i < matrix.length; i++) {
			System.out.println(matrix[i]);
		}
	}
	
	public static void printMatrix(ArrayList<ArrayList<String>> matrix) {
		for (int i = 0; i < matrix.size(); i++) {
			for (int j = 0; j < matrix.get(i).size(); j++) {
				System.out.print(matrix.get(i).get(j) + " ");
			}
			System.out.println();
		}
	}
	
	public static void printMatrix(float[][] matrix) {
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				System.out.print(matrix[i][j] + " ");
			}
			System.out.println();
		}
	}
	
	public static void printMatrix(int[][] matrix) {
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				System.out.print(matrix[i][j] + " ");
			}
			System.out.println();
		}
	}
	
	public static ArrayList<ArrayList<String>> readFile(String fileName) {
		ArrayList<ArrayList<String>> matrix = new ArrayList<ArrayList<String>>();
		int maxU = 0;
		int maxM = 0;
		int minU = Integer.MAX_VALUE;
		int minM = Integer.MAX_VALUE;
		int count = 0;
		HashMap<Integer, Integer> mapU = new HashMap<>();
		HashMap<Integer, Integer> mapM = new HashMap<>();
		
		try {
			File file = new File(fileName);
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				count++;
				String temp = scanner.nextLine();
				String[] temp2 = temp.split("\t");
				int u = Integer.parseInt(temp2[0]) - 1;
				int m = Integer.parseInt(temp2[1]) - 1;
				temp2[0] = "" + u;
				temp2[1] = "" + m;
				if (u > maxU) maxU = u;
				if (m > maxM) maxM = m;
				if (u < minU) minU = u;
				if (m < minM) minM = m;
				if (mapU.get(u) == null) {
					mapU.put(u, 1);
				} else {
					mapU.put(u, mapU.get(u) + 1);
				}
				if (mapM.get(m) == null) {
					mapM.put(m, 1);
				} else {
					mapM.put(m, mapM.get(m) + 1);
				}
				matrix.add(new ArrayList<>(Arrays.asList(temp2)));
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		numUser = maxU + 1;
		numMovie = maxM + 1;
		System.out.println("count of rating " + count);
		System.out.println("numU " + mapU.size());
		System.out.println("numM " + mapM.size());
		System.out.println("maxU " + maxU);
		System.out.println("maxM " + maxM);
		System.out.println("minU " + minU);
		System.out.println("minM " + minM);
		return matrix;
	}
}
