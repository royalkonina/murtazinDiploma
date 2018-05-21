import ca.pjer.ekmeans.EKmeans;
import utils.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import static utils.Utils.Pair;

public class Main {

    public static final int WINDOW_SIZE_CLUSTERS = 31;
    public static final int WINDOW_SIZE_OPENING = 5;
    public static final int MASK_SIZE = 5;
    public static final int NUMBER_OF_CLUSTERS = 2;
    public static final Color[] COLORS = {Color.RED, Color.GREEN};
    private static final int COUNT_IMAGES_TO_GENERATE = 100;
    private static int[] dx = {0, 1, 1, 1};
    private static int[] dy = {1, 0, 1, -1};
    //private static int[] dx = {1, 0};
    //private static int[] dy = {0, 1};
    private static int[] dx_bfs = {0, 0, 1, -1};
    private static int[] dy_bfs = {1, -1, 0, 0};
    private static int[] L5 = {1, 4, 6, 4, 1};
    private static int[] E5 = {-1, -2, 0, 2, 1};
    private static int[] S5 = {-1, 0, 2, 0, -1};
    private static int[] R5 = {1, -4, 6, -4, 1};
    private static int[][][] lawsMasks;
    private static int[] dividersLawsMasks;

    public static void main(String[] args) throws IOException {
        //Generator.generate(COUNT_IMAGES_TO_GENERATE);
        initLawsMasks();
        //updateWithLawsMasks();
        divideIntoClusters();
        makeSomeOpeningAndClosing();
        clearColoredImages();
        calculateError();
    }

    private static void updateWithLawsMasks() throws IOException {
        File directory = new File("src/resources/combined");
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            BufferedImage image = ImageIO.read(file);
            int[][] grayImage = getGrayImage(image);
            for (int i = 0; i < lawsMasks.length; i++) {
                int[][] grayImageAfterLaws = updateWithLawsMask(grayImage, lawsMasks[i], i);
                Utils.writeImageToFile(getImageFromGray(grayImageAfterLaws), "src/resources/after_laws/" + i + "_" + file.getName());
                System.out.println("Making Laws masks: Image " + file.getName() + " done");
            }
        }
    }

    private static BufferedImage getImageFromGray(int[][] grayImage) {
        int width = grayImage.length;
        int height = grayImage[0].length;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                result.setRGB(x, y, grayImage[x][y]);
            }
        }
        return result;
    }

    private static int[][] updateWithLawsMask(int[][] grayImage, int[][] lawsMask, int maskIndex) {
        int width = grayImage.length;
        int height = grayImage[0].length;
        int[][] result = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int meanValue = 0;
                for (int deltaX = -MASK_SIZE / 2, i = 0; deltaX <= MASK_SIZE / 2; deltaX++, i++) {
                    for (int deltaY = -MASK_SIZE / 2, j = 0; deltaY <= MASK_SIZE / 2; deltaY++, j++) {
                        int newX = mod(x + deltaX, width);
                        int newY = mod(y + deltaY, height);
                        meanValue += grayImage[newX][newY];
                    }
                }
                meanValue /= MASK_SIZE * MASK_SIZE;
                for (int deltaX = -MASK_SIZE / 2, i = 0; deltaX <= MASK_SIZE / 2; deltaX++, i++) {
                    for (int deltaY = -MASK_SIZE / 2, j = 0; deltaY <= MASK_SIZE / 2; deltaY++, j++) {
                        int newX = mod(x + deltaX, width);
                        int newY = mod(y + deltaY, height);
                        result[x][y] += (grayImage[newX][newY] - meanValue) * lawsMask[i][j];
                    }
                }
                result[x][y] += meanValue;
                //result[x][y] = Math.min(Math.max(result[x][y], 0), 255);
            }
        }
        return result;
    }

    private static int mod(int x, int width) {
        return (x < 0) ? (x + width) : ((x >= width) ? (x - width) : x);
    }

    private static void initLawsMasks() {
        int[][] lawsVectors = {L5, E5, S5, R5};
        lawsMasks = new int[lawsVectors.length * lawsVectors.length][L5.length][L5.length];
        dividersLawsMasks = new int[lawsVectors.length * lawsVectors.length];
        for (int i = 0; i < lawsVectors.length; i++) {
            for (int j = 0; j < lawsVectors.length; j++) {
                int index = i * lawsVectors.length + j;
                lawsMasks[index] = mult(lawsVectors[i], lawsVectors[j]);
                for (int x = 0; x < MASK_SIZE; x++) {
                    for (int y = 0; y < MASK_SIZE; y++) {
                        dividersLawsMasks[index] += Math.abs(lawsMasks[index][x][y]);
                    }
                }
            }
        }
    }

    private static int[][] mult(int[] first, int[] second) {
        int[][] result = new int[first.length][second.length];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < second.length; j++) {
                result[i][j] = first[i] * second[j];
            }
        }
        return result;
    }

    private static void calculateError() throws IOException {
        File sourcesDirectory = new File("src/resources/source_images");
        File resultsDirectory = new File("src/resources/cleared_images");
        File[] sources = Objects.requireNonNull(sourcesDirectory.listFiles());
        File[] results = Objects.requireNonNull(resultsDirectory.listFiles());
        double sumGOOD = 0;
        double sumTRUEobject = 0;
        double sumTRUEbackground = 0;
        double sumGOODfiltered = 0;
        double sumTRUEobjectfiltered = 0;
        double sumTRUEbackgroundfiltered = 0;
        int countNotFiltered = 0;
        for (int i = 0; i < sources.length; i++) {
            BufferedImage imageSource = ImageIO.read(sources[i]);
            BufferedImage imageResult = ImageIO.read(results[i]);
            int[][] binaryImageSource = getBinaryImage(imageSource);
            int[][] binaryImageResult = getBinaryImage(imageResult);
            int countTrueObjectPixels = getCountTrueObjectPixels(binaryImageSource, binaryImageResult);
            int countTrueBackgroundPixels = getCountTrueBackgroundPixels(binaryImageSource, binaryImageResult);
            int countGoodPixels = getCountGoodPixels(binaryImageSource, binaryImageResult);
            int objectSize = getObjectSize(binaryImageSource);
            int imageSize = imageResult.getWidth() * imageResult.getHeight();
            double ratioGOOD = (countGoodPixels + .0) / imageSize;
            double ratioTRUEobject = (countTrueObjectPixels + .0) / objectSize;
            double ratioTRUEbackground = (countTrueBackgroundPixels + .0) / (imageSize - objectSize);
            sumGOOD += ratioGOOD;
            sumTRUEobject += ratioTRUEobject;
            sumTRUEbackground += ratioTRUEbackground;
            if (ratioTRUEobject > 0.5) {
                countNotFiltered++;
                sumGOODfiltered += ratioGOOD;
                sumTRUEobjectfiltered += ratioTRUEobject;
                sumTRUEbackgroundfiltered += ratioTRUEbackground;
            }
            System.out.println(String.format("Error: Image %s ratioGOOD = %f, ratioTRUEobject = %f, ratioTRUEbackground = %f",
                    sources[i].getName(), ratioGOOD, ratioTRUEobject, ratioTRUEbackground));
        }
        System.out.println("Среднее значение ошибки изображения: " + (1.0 - sumGOOD / sources.length));
        System.out.println("Среднее значение ошибки объекта: " + (1.0 - sumTRUEobject / sources.length));
        System.out.println("Среднее значение ошибки фона: " + (1.0 - sumTRUEbackground / sources.length));
        System.out.println("Из 100 изображений осталось: " + countNotFiltered);
        System.out.println("Отфильтрованное Среднее значение ошибки изображения: " + (1.0 - sumGOODfiltered / countNotFiltered));
        System.out.println("Отфильтрованное Среднее значение ошибки объекта: " + (1.0 - sumTRUEobjectfiltered / countNotFiltered));
        System.out.println("Отфильтрованное Среднее значение ошибки фона: " + (1.0 - sumTRUEbackgroundfiltered / countNotFiltered));
    }

    private static int getCountTrueBackgroundPixels(int[][] imageSource, int[][] imageResult) {
        int count = 0;
        int width = imageSource.length;
        int height = imageSource[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (imageSource[x][y] == 0 && imageResult[x][y] == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int getCountGoodPixels(int[][] imageSource, int[][] imageResult) {
        int count = 0;
        int width = imageSource.length;
        int height = imageSource[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (imageSource[x][y] == imageResult[x][y]) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int getObjectSize(int[][] imageSource) {
        int count = 0;
        int width = imageSource.length;
        int height = imageSource[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (imageSource[x][y] == 1) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int getCountTrueObjectPixels(int[][] imageSource, int[][] imageResult) {
        int count = 0;
        int width = imageSource.length;
        int height = imageSource[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (imageSource[x][y] == 1 && imageResult[x][y] == 1) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void clearColoredImages() throws IOException {
        File directory = new File("src/resources/opened_and_closed");
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            BufferedImage image = ImageIO.read(file);
            int[][] binaryImage = getBinaryImage(image);
            int[] counts = getCountOfAssigments(binaryImage);
            System.out.print(String.format("Clearing: Image %s : RED= %d; GREEN = %d; RED/GREEN = %f", file.getName(), counts[0], counts[1], (counts[0] + .0) / counts[1]));
            int objectColor = counts[0] > counts[1] ? 1 : 0;
            Pair startObject = findObject(binaryImage, objectColor);
            binaryImage = fillObject(binaryImage, startObject, objectColor);

            Utils.writeImageToFile(getColoredImage(binaryImage), "src/resources/cleared_images/" + file.getName());
            System.out.println(" --- done");
        }
    }

    private static int[][] fillObject(int[][] binaryImage, Pair startObject, int objectColor) {
        int width = binaryImage.length;
        int height = binaryImage[0].length;
        int[][] result = new int[width][height];
        Deque<Pair> deque = new ArrayDeque<>();
        boolean[][] used = new boolean[width][height];
        deque.add(startObject);
        used[startObject.x][startObject.y] = true;
        while (!deque.isEmpty()) {
            Pair p = deque.pollFirst();
            for (int i = 0; i < dx_bfs.length; i++) {
                int x = p.x + dx_bfs[i];
                int y = p.y + dy_bfs[i];
                if (goodCoords(x, width) && goodCoords(y, height) && !used[x][y] && binaryImage[x][y] == objectColor) {
                    used[x][y] = true;
                    deque.add(new Pair(x, y));
                }
            }
        }
        boolean[][] used2 = new boolean[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (!used[i][j]) {
                    deque.add(new Pair(i, j));
                    used2[i][j] = true;
                    while (!deque.isEmpty()) {
                        Pair p = deque.pollFirst();
                        result[p.x][p.y] = 0;
                        for (int k = 0; k < dx_bfs.length; k++) {
                            int x = p.x + dx_bfs[k];
                            int y = p.y + dy_bfs[k];
                            if (goodCoords(x, width) && goodCoords(y, height) && !used2[x][y] && !used[x][y]) {
                                used2[x][y] = true;
                                deque.add(new Pair(x, y));
                            }
                        }
                    }
                    break;
                }
            }
        }
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!used2[x][y]) {
                    result[x][y] = 1;
                }
            }
        }
        return result;
    }

    private static Pair findObject(int[][] binaryImage, int objectColor) {
        int width = binaryImage.length;
        int height = binaryImage[0].length;
        boolean[][] used = new boolean[width][height];
        int maxCount = 0;
        Pair start = new Pair(0, 0);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!used[x][y] && binaryImage[x][y] == objectColor) {
                    int count = bfs(binaryImage, used, new Pair(x, y), objectColor);
                    if (count > maxCount) {
                        maxCount = count;
                        start = new Pair(x, y);
                    }
                }
            }
        }
        return start;
    }

    private static int bfs(int[][] binaryImage, boolean[][] used, Pair start, int objectColor) {
        int width = binaryImage.length;
        int height = binaryImage[0].length;
        Deque<Pair> deque = new ArrayDeque<>();
        deque.add(start);
        used[start.x][start.y] = true;
        int count = 1;
        while (!deque.isEmpty()) {
            Pair p = deque.pollFirst();
            count++;
            for (int i = 0; i < dx_bfs.length; i++) {
                int x = p.x + dx_bfs[i];
                int y = p.y + dy_bfs[i];
                if (goodCoords(x, width) && goodCoords(y, height) && !used[x][y] && binaryImage[x][y] == objectColor) {
                    used[x][y] = true;
                    deque.add(new Pair(x, y));
                }
            }
        }
        return count;
    }

    private static void makeSomeOpeningAndClosing() throws IOException {
        File directory = new File("src/resources/divided_into_clusters");
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            BufferedImage image = ImageIO.read(file);
            int[][] binaryImage = getBinaryImage(image);
            int[] counts = getCountOfAssigments(binaryImage);
            System.out.print(String.format("Opening: Image %s : RED= %d; GREEN = %d; RED/GREEN = %f", file.getName(), counts[0], counts[1], (counts[0] + .0) / counts[1]));
            if (counts[0] < counts[1]) {
                binaryImage = opening(binaryImage, 1);
                binaryImage = closing(binaryImage, 1);
            } else {
                binaryImage = closing(binaryImage, 1);
                binaryImage = opening(binaryImage, 1);
            }
            Utils.writeImageToFile(getColoredImage(binaryImage), "src/resources/opened_and_closed/" + file.getName());
            System.out.println(" --- done");
        }
    }

    private static int[] getCountOfAssigments(int[][] binaryImage) {
        int[] counts = new int[2];
        for (int x = 0; x < binaryImage.length; x++) {
            for (int y = 0; y < binaryImage[0].length; y++) {
                counts[binaryImage[x][y]]++;
            }
        }
        return counts;
    }

    private static int[][] getBinaryImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] result = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                result[x][y] = image.getRGB(x, y) == COLORS[0].getRGB() ? 0 : 1;
            }
        }
        return result;
    }

    private static void divideIntoClusters() throws IOException {
        File directory = new File("src/resources/combined");
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            BufferedImage image = ImageIO.read(file);
            int[][] grayImage = getGrayImage(image);
            double[][][] laws = getLawsMatrix(grayImage);
            double[][][] correlation = calcCorrelation(grayImage);
            laws = sum(correlation, laws);
            laws = doStandardScore(laws);
            double[][] points = getPointsForKMeans(laws);
            double[][] centroids = getCentroids(laws);
            EKmeans eKmeans = new EKmeans(centroids, points);
            eKmeans.run();
            int[] assigments = eKmeans.getAssignments();
            BufferedImage result = getColoredImage(image.getWidth(), image.getHeight(), assigments);
            Utils.writeImageToFile(result, "src/resources/divided_into_clusters/" + file.getName());
            System.out.println("Dividing into clusters: Image " + file.getName() + " done");
        }
    }

    private static double[][][] getLawsMatrix(int[][] grayImage) {
        int[] indexes = {3, 7};
        int m = indexes.length;
        int width = grayImage.length;
        int height = grayImage[0].length;
        double[][][] result = new double[m][width][height];
        for (int i = 0; i < m; i++) {
            int[][] updatedImage = updateWithLawsMask(grayImage, lawsMasks[indexes[i]], indexes[i]);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    result[i][x][y] = updatedImage[x][y];
                }
            }
        }
        return result;
    }

    private static double[][][] sum(double[][][] first, double[][][] second) {
        int width = first[0].length;
        int height = first[0][0].length;
        int depth1 = first.length;
        int depth2 = second.length;
        double[][][] result = new double[depth1 + depth2][width][height];
        for (int i = 0; i < depth1; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < height; k++) {
                    result[i][j][k] = first[i][j][k];
                }
            }
        }
        for (int i = 0; i < depth2; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < height; k++) {
                    result[i + depth1][j][k] = second[i][j][k];
                }
            }
        }
        return result;
    }

    private static double[][][] doStandardScore(double[][][] correlation) {
        int width = correlation[0].length;
        int height = correlation[0][0].length;
        double[][][] result = new double[correlation.length][width][height];
        for (int i = 0; i < correlation.length; i++) {
            double meanValue = 0;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    meanValue += correlation[i][x][y];
                    if(Double.isNaN(correlation[i][x][y])){
                        System.out.println("NaN found: i=" + i + " x=" + x + " y=" + y);
                    }
                }
            }
            meanValue /= width * height;

            double deviation = 0;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    deviation += Math.pow((meanValue - correlation[i][x][y]), 2);
                }
            }
            deviation = Math.sqrt(deviation / (width * height));
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    result[i][x][y] = (correlation[i][x][y] - meanValue) / deviation;
                }
            }
        }
        return result;
    }

    private static int[][] getBinaryImage(int width, int height, int[] assigments) {
        int[][] result = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                result[x][y] = assigments[x * width + y];
            }
        }
        return result;
    }

    private static BufferedImage getColoredImage(int width, int height, int[] assigments) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                result.setRGB(x, y, COLORS[assigments[x * width + y]].getRGB());
            }
        }
        return result;
    }

    private static BufferedImage getColoredImage(int[][] binaryImage) {
        int width = binaryImage.length;
        int height = binaryImage[0].length;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                result.setRGB(x, y, COLORS[binaryImage[x][y]].getRGB());
            }
        }
        return result;
    }

    private static double[][] getCentroids(double[][][] correlation) {
        double[][] result = new double[NUMBER_OF_CLUSTERS][correlation.length];
        int delta = WINDOW_SIZE_CLUSTERS + 1;
        int width = correlation[0].length;
        int height = correlation[0][0].length;
        for (int i = 0; i < NUMBER_OF_CLUSTERS; i++) {
            int y = delta + Utils.rnd.nextInt(height - 2 * delta);
            int x = delta + Utils.rnd.nextInt(width - 2 * delta);
            for (int j = 0; j < correlation.length; j++) {
                result[i][j] = correlation[j][x][y];
            }
        }
        return result;
    }

    private static double[][] getPointsForKMeans(double[][][] correlation) {
        int n = correlation[0][0].length * correlation[0].length;
        double[][] result = new double[n][correlation.length];
        for (int i = 0; i < correlation.length; i++) {
            for (int j = 0; j < correlation[0].length; j++) {
                for (int k = 0; k < correlation[0][0].length; k++) {
                    result[j * correlation[0].length + k][i] = correlation[i][j][k];
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
        double[][][] result = new double[dx.length][width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int i = 0; i < dx.length; i++) {
                    int n = dx[i];
                    int m = dy[i];
                    double meanValue = 0;
                    double dispersion = 0;
                    int countChisl = 0;
                    for (int deltaX = -WINDOW_SIZE_CLUSTERS / 2; deltaX <= WINDOW_SIZE_CLUSTERS / 2; deltaX++) {
                        for (int deltaY = -WINDOW_SIZE_CLUSTERS / 2; deltaY <= WINDOW_SIZE_CLUSTERS / 2; deltaY++) {
                            int newX = mod(x + deltaX, width);
                            int newY = mod(y + deltaY, height);
                            meanValue += image[newX][newY];
                            countChisl++;
                        }
                    }
                    meanValue /= countChisl;
                    int countZnam = 0;
                    for (int deltaX = -WINDOW_SIZE_CLUSTERS / 2; deltaX <= WINDOW_SIZE_CLUSTERS / 2; deltaX++) {
                        for (int deltaY = -WINDOW_SIZE_CLUSTERS / 2; deltaY <= WINDOW_SIZE_CLUSTERS / 2; deltaY++) {
                            int newX = mod(x + deltaX, width);
                            int newY = mod(y + deltaY, height);
                            dispersion += (image[newX][newY] - meanValue) * (image[newX][newY] - meanValue);
                            countZnam++;
                            result[i][x][y] += (image[newX][newY] - meanValue) * (image[mod(newX + n, width)][mod(newY + m, height)] - meanValue);
                            /*if (i == 0) {
                                result[dx.length + 1][x][y] += Math.pow((image[newX][newY] - meanValue), 3);
                                result[dx.length + 2][x][y] += Math.pow((image[newX][newY] - meanValue), 4);
                            }*/
                        }
                    }
                    result[i][x][y] *= (countZnam + .0) / (dispersion * countChisl);
                    if (Double.isNaN(result[i][x][y])) {
                        System.out.println("NaN found: i=" + i + " x=" + x + " y=" + y);
                    }
                    /*if (i == 0) {
                        result[4][x][y] *= (1.0) / Math.pow(Math.sqrt(dispersion), 3);
                        result[5][x][y] *= (1.0) / Math.pow(Math.sqrt(dispersion), 4);
                    }*/
                }
            }
        }
        return result;
    }

    private static boolean goodCoords(int x, int width) {
        return x >= 0 && x < width;
    }

    private static int[][] erosion(int[][] binaryImage, int assigment) {
        int width = binaryImage.length;
        int height = binaryImage[0].length;
        int[][] result = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int countAll = 0;
                int countHave = 0;
                for (int deltaX = -WINDOW_SIZE_OPENING / 2; deltaX <= WINDOW_SIZE_OPENING / 2; deltaX++) {
                    for (int deltaY = -WINDOW_SIZE_OPENING / 2; deltaY <= WINDOW_SIZE_OPENING / 2; deltaY++) {
                        int newX = mod(x + deltaX, width);
                        int newY = mod(y + deltaY, height);
                        countAll++;
                        if (binaryImage[newX][newY] == assigment) {
                            countHave++;
                        }
                    }
                }
                if (countHave == countAll) {
                    result[x][y] = assigment;
                }
            }
        }
        return result;
    }

    private static int[][] dilation(int[][] binaryImage, int assigment) {
        int width = binaryImage.length;
        int height = binaryImage[0].length;
        int[][] result = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (binaryImage[x][y] == assigment) {
                    for (int deltaX = -WINDOW_SIZE_OPENING / 2; deltaX <= WINDOW_SIZE_OPENING / 2; deltaX++) {
                        for (int deltaY = -WINDOW_SIZE_OPENING / 2; deltaY <= WINDOW_SIZE_OPENING / 2; deltaY++) {
                            int newX = mod(x + deltaX, width);
                            int newY = mod(y + deltaY, height);
                            result[newX][newY] = assigment;
                        }
                    }
                }
            }
        }
        return result;
    }

    private static int[][] opening(int[][] binaryImage, int assigment) {
        return dilation(erosion(binaryImage, assigment), assigment);
    }

    private static int[][] closing(int[][] binaryImage, int assigment) {
        return erosion(dilation(binaryImage, assigment), assigment);
    }

}
