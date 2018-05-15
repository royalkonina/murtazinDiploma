import utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Generator {

    public static void generate() throws IOException {
        File directoryStone1 = new File("src/resources/stone1");
        File directoryCusion1 = new File("src/resources/cushion1");
        List<File> filesStones = Arrays.asList(Objects.requireNonNull(directoryStone1.listFiles()));
        List<File> filesCusion = Arrays.asList(Objects.requireNonNull(directoryCusion1.listFiles()));
        for (int i = 0; i < filesStones.size(); i++) {
            BufferedImage stoneImage = ImageIO.read(filesStones.get(i));
            BufferedImage cusihonImage = ImageIO.read(filesCusion.get(i));
            BufferedImage result = Utils.combineImages(stoneImage, cusihonImage);
            Utils.writeImageToFile(result, "src/resources/combined/combined" + i + ".png");
        }
    }

    public static void generate(int count) throws IOException {
        File directory = new File("src/resources/mixed_textures");
        List<File> files = Arrays.asList(Objects.requireNonNull(directory.listFiles()));
        for (int i = 0; i < count; i++) {
            int index1 = Utils.rnd.nextInt(files.size());
            int index2 = Utils.rnd.nextInt(files.size());
            while(files.get(index1).getName().startsWith(files.get(index2).getName().substring(0, 4))){
                index1 = Utils.rnd.nextInt(files.size());
                index2 = Utils.rnd.nextInt(files.size());
            }
            BufferedImage image1 = ImageIO.read(files.get(index1));
            BufferedImage image2 = ImageIO.read(files.get(index2));
            BufferedImage result = Utils.combineImages(image1, image2);
            Utils.writeImageToFile(result, "src/resources/combined2/" + files.get(index1).getName() + "_" + files.get(index2).getName());
        }
    }
}
