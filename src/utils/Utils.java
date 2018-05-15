package utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class Utils {

    public final static int SEED = 5;
    public final static Random rnd = new Random(SEED);



    public static void writeImageToFile(BufferedImage result, String filename) {
        try {
            ImageIO.write(result, "PNG", new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static class Pair {
        public final int x, y;

        public Pair(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

}
