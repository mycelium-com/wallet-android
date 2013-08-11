package se.grunka.fortuna.tests;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import se.grunka.fortuna.Fortuna;

public class Image {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            usage();
        } else {
            int width = 0;
            try {
                width = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                usage();
                System.err.println("Width is not a number: " + args[0]);
                System.exit(1);
            }
            int height = 0;
            try {
                height = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                usage();
                System.err.println("Height is not a number: " + args[1]);
                System.exit(1);
            }
            System.err.println("Initializing RNG...");
            Fortuna fortuna = Fortuna.createInstance();
            System.err.println("Generating image...");
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, fortuna.nextBoolean() ? 0xffffff : 0x000000);
                }
            }
            String extension = args[2].substring(args[2].lastIndexOf('.') + 1);
            ImageIO.write(image, extension, new File(args[2]));
        }
    }

    private static void usage() {
        System.err.println("Usage: " + Image.class.getName() + " <width> <height> <file>");
    }
}
