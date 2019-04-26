import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class RandomFileGenerator {
    public static void main(String[] args) throws IOException {
        int min = 32;
        int max = 126;

        String[] listOfFiles = new File("../inputData").list();
        assert listOfFiles != null;

        File output_file;

        int numberOfFileInDirectory = listOfFiles.length;

        output_file = new File("../inputData/file_" + numberOfFileInDirectory + ".txt");

        if(output_file.exists()) {
            if(output_file.delete()) {
                System.out.println("A file with same name already existed and hence deleted to create a new file");
            }
        }

        if(output_file.createNewFile()) {
            System.out.println("New file created: " + output_file.getName());
        }

        var len = 0;
        FileWriter f0 = new FileWriter(output_file);
        while(len < 1024 * 1024) {
            int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
            f0.write((char)randomNum);
            ++len;
        }
        f0.close();
        System.out.println("File successfully created!");
    }
}