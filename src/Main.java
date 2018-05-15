import ca.pjer.ekmeans.EKmeans;
import utils.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Main {

  public static final int WINDOW_SIZE = 9;
  public static final int MEAN_VALUE = 127;
  public static final int NUMBER_OF_CLUSTERS = 2;
  public static final int[][] WINDOW = new int[WINDOW_SIZE][WINDOW_SIZE];
  private static final int COUNT_IMAGES_TO_GENERATE = 100;
  private static int[] dx = {0, 1, 1, 1};
  private static int[] dy = {1, 0, 1, -1};
  private static double MIN_VALUE = -10000;


  public static void main(String[] args) throws IOException {
    //Generator.generate(COUNT_IMAGES_TO_GENERATE);
    File directory = new File("src/resources/combined2");
    for (File file : Objects.requireNonNull(directory.listFiles())) {
      BufferedImage image = ImageIO.read(file);
      int[][] grayImage = getGrayImage(image);
      double[][][] correlation = calcCorrelation(grayImage);
      double[][] points = getPointsForKMeans(correlation);
      double[][] centroids = getCentroids(correlation);
      EKmeans eKmeans = new EKmeans(centroids, points);
      eKmeans.run();
      int[] assigments = eKmeans.getAssignments();
      BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
      for (int x = 0; x < result.getWidth(); x++) {
        for (int y = 0; y < result.getHeight(); y++) {
          result.setRGB(x, y, getColor(assigments[x * result.getWidth() + y]).getRGB());
        }
      }
      Utils.writeImageToFile(result, "src/resources/results2/result" + file.getName());
      System.out.println("Image " + file.getName() + " done");
    }
  }

  private static Color getColor(int assigment) {
    switch (assigment) {
      case 0:
        return Color.RED;
      case 1:
        return Color.GREEN;
      case 2:
        return Color.BLUE;
      default:
        return Color.BLACK;
    }
  }

  private static double[][] getCentroids(double[][][] correlation) {
    double[][] result = new double[NUMBER_OF_CLUSTERS][dx.length];
    int delta = WINDOW_SIZE + 1;
    int width = correlation.length;
    int height = correlation[0].length;
    for (int i = 0; i < NUMBER_OF_CLUSTERS; i++) {
      for (int j = 0; j < dx.length; j++) {
        result[i][j] = correlation[delta + Utils.rnd.nextInt(width - 2 * delta)][delta + Utils.rnd.nextInt(height - 2 * delta)][j];
      }
    }
    return result;
  }

  private static double[][] getPointsForKMeans(double[][][] correlation) {
    int n = correlation.length * correlation[0].length;
    double[][] result = new double[n][dx.length];
    for (int i = 0; i < correlation.length; i++) {
      for (int j = 0; j < correlation[0].length; j++) {
        for (int k = 0; k < dx.length; k++) {
          result[i * correlation.length + j][k] = correlation[i][j][k];
        }
      }
    }
    return result;
  }

  private static int[][] getGrayImage(BufferedImage image) {
    int[][] imageArray = new int[image.getWidth()][image.getHeight()];
    Raster raster = image.getRaster();
    for (int i = 0; i < image.getWidth(); i++) {
      for (int j = 0; j < image.getHeight(); j++) {
        imageArray[i][j] = raster.getSample(i, j, 0);
      }
    }
    return imageArray;
  }

  private static double[][][] calcCorrelation(int[][] image) {
    int width = image.length;
    int height = image[0].length;
    double[][][] result = new double[width][height][dx.length];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        for (int i = 0; i < dx.length; i++) {
          int n = dx[i];
          int m = dy[i];
          double meanValue = 0;
          double dispersion = 0;
          int countChisl = 0;
          for (int deltaX = -WINDOW_SIZE / 2; deltaX <= WINDOW_SIZE / 2; deltaX++) {
            for (int deltaY = -WINDOW_SIZE / 2; deltaY <= WINDOW_SIZE / 2; deltaY++) {
              int newX = x + deltaX;
              int newY = y + deltaY;
              if (goodCoords(newX, width) && goodCoords(newY, height)) {
                meanValue += image[newX][newY];
                countChisl++;
              }
            }
          }
          meanValue /= countChisl;
          int countZnam = 0;
          for (int deltaX = -WINDOW_SIZE / 2; deltaX <= WINDOW_SIZE / 2; deltaX++) {
            for (int deltaY = -WINDOW_SIZE / 2; deltaY <= WINDOW_SIZE / 2; deltaY++) {
              int newX = x + deltaX;
              int newY = y + deltaY;
              if (goodCoords(newX + n, width) && goodCoords(newY + m, height) && goodCoords(newX, width) && goodCoords(newY, height)) {
                result[x][y][i] += (image[newX][newY] - meanValue) * (image[newX + n][newY + m] - meanValue);
                if (goodCoords(newX, width) && goodCoords(newY, height)) {
                  dispersion += (image[newX][newY] - meanValue) * (image[newX][newY] - meanValue);
                  countZnam++;
                }
              }
            }
          }
          result[x][y][i] *= (countZnam + .0) / (dispersion * countChisl);
        }
      }
    }
    return result;
  }


  private static boolean goodCoords(int x, int width) {
    return x >= 0 && x < width;
  }

  private static void erosion(BufferedImage image) {
    
  }
}
