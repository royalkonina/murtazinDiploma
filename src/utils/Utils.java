package utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class Utils {

    public final static int SEED = 5;
    public final static int OFFSET = 10;
    public final static Random rnd = new Random(SEED);

    public static BufferedImage readImage(String filename) {
        try {
            return ImageIO.read(new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static BufferedImage combineImages(BufferedImage first, BufferedImage second) {
        int width = first.getWidth();
        int height = first.getHeight();
        int radius = width / 6;
        int delta = OFFSET + radius;
        Pair center = new Pair(delta + rnd.nextInt(width - 2 * delta), delta + rnd.nextInt(height - 2 * delta));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if ((x - center.x) * (x - center.x) + (y - center.y) * (y - center.y) <= radius * radius) {
                    result.setRGB(x, y, second.getRGB(x, y));
                } else {
                    result.setRGB(x, y, first.getRGB(x, y));
                }
            }
        }
        return result;
    }

    public static void writeImageToFile(BufferedImage result, String filename) {
        try {
            ImageIO.write(result, "PNG", new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static class Pair {
        int x, y;

        public Pair(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

}
