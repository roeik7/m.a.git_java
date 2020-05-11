package ManagmentEngine.Utils;

import MagitRepository.MagitBlob;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Blob extends Folder{


    public Blob() throws NoSuchAlgorithmException {
        initialize_sha1();
    }

    public static Blob create_blob(String id, MagitBlob magit_blob) throws NoSuchAlgorithmException {
        Blob res = new Blob();
        res.initialize_blob(magit_blob);

        return res;
    }

    public void initialize_blob(MagitBlob curr_blob) {
        try {
            setTextual_content(curr_blob.getContent());
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
            setLast_update(formatter.parse(curr_blob.getLastUpdateDate()));
            setLast_updater(curr_blob.getLastUpdater());
            setName(curr_blob.getName());
            String sha1 = super.get_sha1(curr_blob.getContent());
            setSha1(sha1);
            setType("blob");
        }

        catch (ParseException e) {
            System.out.println("Error to parse date\n");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Error to encoding sha1\n");
        }
    }

    public boolean file_content_changed(File file){

        return true;
    }

    public boolean file_content_changed(Folder node, String path) throws IOException {
        File file = new File(path+"\\"+node.getName());

        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        return content.equals(node.textual_content);
    }

    static public boolean file_exist(Blob blob, String path){
        File f = new File(path+"\\"+blob.getName());

        return f.exists();
    }

    @Override
    public void create_local_file(Folder node, String path) {
     create_and_write_to_file(node, path, node.getName());
    }

    public static void create_and_write_to_file(Folder folder, String path, String file_name) {
        try {
            File file = new File(path+"\\"+file_name);
            file.createNewFile();
            FileWriter myWriter = new FileWriter(path+"\\"+file_name);
            myWriter.write(folder.getTextual_content());
            myWriter.close();

        } catch (IOException e) {
            System.out.println("Error I/O operation.\n");
            e.printStackTrace();
        }
    }
}
