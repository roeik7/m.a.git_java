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

    public Blob create_commited_blob(String filename, String path, String last_updater) throws NoSuchAlgorithmException, IOException, ParseException {
        Blob comitted_blob = new Blob();
        File new_file = new File(path+"\\"+filename);
        String file_content = FileUtils.readFileToString(new_file, StandardCharsets.UTF_8);
        comitted_blob.setTextual_content(file_content);
        comitted_blob.setLast_update(Folder.get_current_time());
        comitted_blob.setLast_updater(last_updater);
        comitted_blob.setName(filename);
        comitted_blob.setType("blob");
        comitted_blob.setSha1(super.get_sha1(file_content));

        return comitted_blob;
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
        File file = new File(path);
        boolean modified =false;
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        if(content!=null){
            modified = !content.equals(node.textual_content);
        }

        return modified;
    }

    static public boolean file_exist(String path){
        File f = new File(path);

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
