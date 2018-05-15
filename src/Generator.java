import utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static utils.Utils.rnd;

public class Generator {

    public final static int OFFSET = 10;

    public static void generate() throws IOException {
        File directoryStone1 = new File("src/resources/stone1");
        File directoryCusion1 = new File("src/resources/cushion1");
        List<File> filesStones = Arrays.asList(Objects.requireNonNull(directoryStone1.listFiles()));
        List<File> filesCusion = Arrays.asList(Objects.requireNonNull(directoryCusion1.listFiles()));
        for (int i = 0; i < filesStones.size(); i++) {
            BufferedImage stoneImage = ImageIO.read(filesStones.get(i));
            BufferedImage cusihonImage = ImageIO.read(filesCusion.get(i));
            BufferedImage[] result = combineImages(stoneImage, cusihonImage);
            Utils.writeImageToFile(result[0], "src/resources/combined/combined" + i + ".png");
            Utils.writeImageToFile(result[1], "src/resources/source_images/combined" + i + ".png");
        }
    }

    public static void generate(int count) throws IOException {
        File directory = new File("src/resources/mixed_textures");
        List<File> files = Arrays.asList(Objects.requireNonNull(directory.listFiles()));
        for (int i = 0; i < count; i++) {
            int index1 = rnd.nextInt(files.size());
            int index2 = rnd.nextInt(files.size());
            while (files.get(index1).getName().startsWith(files.get(index2).getName().substring(0, 4))) {
                index1 = rnd.nextInt(files.size());
                index2 = rnd.nextInt(files.size());
            }
            BufferedImage image1 = ImageIO.read(files.get(index1));
            BufferedImage image2 = ImageIO.read(files.get(index2));
            BufferedImage[] result = combineImages(image1, image2);
            String filename = files.get(index1).getName() + "_" + files.get(index2).getName();
            Utils.writeImageToFile(result[0], "src/resources/combined/" + filename);
            Utils.writeImageToFile(result[1], "src/resources/source_images/" + filename);
            System.out.println("Generating: Image " + filename + " done");
        }
    }

    public static BufferedImage[] combineImages(BufferedImage first, BufferedImage second) {
        int width = first.getWidth();
        int height = first.getHeight();
        int radius = width / 6;
        int delta = OFFSET + radius;
        Utils.Pair center = new Utils.Pair(delta + rnd.nextInt(width - 2 * delta), delta + rnd.nextInt(height - 2 * delta));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BufferedImage sourceImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if ((x - center.x) * (x - center.x) + (y - center.y) * (y - center.y) <= radius * radius) {
                    result.setRGB(x, y, second.getRGB(x, y));
                    sourceImage.setRGB(x, y, Main.COLORS[1].getRGB());
                } else {
                    result.setRGB(x, y, first.getRGB(x, y));
                    sourceImage.setRGB(x, y, Main.COLORS[0].getRGB());
                }
            }
        }
        return new BufferedImage[]{result, sourceImage};
    }
}
