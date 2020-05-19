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
import java.util.Date;

public class Blob extends DataStorage{


    public Blob() throws NoSuchAlgorithmException {
        initialize_sha1();
    }

    public static Blob create_blob(MagitBlob magit_blob) throws NoSuchAlgorithmException {
        Blob res = new Blob();
        res = initialize_blob(magit_blob);

        return res;
    }

    public static String find_sha1_to_existing_file(File file) throws IOException, NoSuchAlgorithmException {
        //File file = new File(path);
        return (get_sha1(FileUtils.readFileToString(file, StandardCharsets.UTF_8)));
    }

    static public Blob initialize_blob(MagitBlob curr_blob) throws NoSuchAlgorithmException {
        Blob new_blob=new Blob();
        try {
            new_blob.initialize_blob(curr_blob.getContent(),get_sha1(curr_blob.getContent()) ,curr_blob.getName(),"blob", string_to_date(curr_blob.getLastUpdateDate()), curr_blob.getLastUpdater());
            return new_blob;
        }

        catch (ParseException e) {
            System.out.println("Error to parse date\n");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Error to encoding sha1\n");
        }

        return new_blob;
    }

    public static Blob initialize_blob_from_eist(File file, String commit_details[]) throws NoSuchAlgorithmException, IOException, ParseException {
        Blob new_blob = new Blob();
        String content =  FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        new_blob.initialize_blob(content,get_sha1(content), file.getName(), "blob", get_current_time(), commit_details[0]);

        return new_blob;
    }

    private void initialize_blob(String content, String sha1, String name, String type, Date current_time, String updater) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        setTextual_content(content);
        setSha1(sha1);
        setName(name);
        setType(type);
        setLast_update(current_time);
        setLast_updater(updater);
    }

    public boolean file_content_changed(File file){

        return true;
    }

    public boolean file_content_changed(DataStorage node, String path) throws IOException {
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
    public void create_local_file(DataStorage node, String path) {
     create_and_write_to_file(node, path, node.getName());
    }

    public static void create_and_write_to_file(DataStorage folder, String path, String file_name) {
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
