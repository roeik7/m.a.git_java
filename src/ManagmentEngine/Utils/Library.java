package ManagmentEngine.Utils;

import MagitRepository.MagitSingleFolder;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

public class Library extends Folder {
    boolean is_root;
    ArrayList<Folder> childs;

    public ArrayList<Folder> getChilds() {
        return childs;
    }


//need to initialize all fields here and not in manager

    public void update_library_after_childs_creation(MagitSingleFolder magit_curr_library) throws ParseException {
        try {
            String library_content = create_library_textual_content();
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
            setLast_update(formatter.parse(magit_curr_library.getLastUpdateDate()));
            setTextual_content(library_content);
            setSha1(get_sha1(library_content));
            setLast_updater(magit_curr_library.getLastUpdater());
        } catch (UnsupportedEncodingException e) {
            System.out.println("Error to encode sha1 to library\n");
        }
    }

    public boolean isIs_root() {
        return is_root;
    }

    private ArrayList<String> get_childs_description() {
        ArrayList<String> res = new ArrayList<String>();
        ArrayList<String> line=new ArrayList<String>();
        String str_date, delimiter=", ";

        for (int i = 0; i <getChilds().size() ; i++) {
            line.add(getChilds().get(i).getName());
            line.add(getChilds().get(i).getSha1());
            line.add(getChilds().get(i).getType());
            line.add(getChilds().get(i).getLast_updater());

            //convert date to string
            str_date = super.date_to_string(getLast_update());
            line.add(str_date);

            //add delimiter and new line
            res.add(String.join(delimiter, line).concat("\n"));
        }

        return res;
    }

    public Library(boolean is_root) throws NoSuchAlgorithmException {
        //blobs_childs = new ArrayList<Blob>();
        //libraries_childs = new ArrayList<Library>();
        childs = new ArrayList<Folder>();

        this.is_root=is_root;
        initialize_sha1();
    }

    public void add_child(Folder node){
        childs.add(node);
    }

    public void update_lib_sha1() {
        create_library_textual_content();
    }

    public String create_library_textual_content() {
        ArrayList<String> lines = this.get_childs_description();
        sort_lines_lexicographic(lines);
        StringBuilder array_to_string = new StringBuilder();

        for (int i = 0; i <lines.size() ; i++) {
            array_to_string.append(lines.get(i));
        }

        return array_to_string.toString();
    }

    private void sort_lines_lexicographic(ArrayList<String> lines) {
        for (int i = 0; i < lines.size() - 1; ++i) {
            for (int j = i + 1; j < lines.size(); ++j) {
                if (lines.get(i).compareTo(lines.get(j)) > 0) {
                    //swap line[i] <-> line[j]
                    Collections.swap(lines, i, j);
                }
            }
        }
    }

    public void initialize_library(MagitSingleFolder magit_curr_library) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
            setLast_update(formatter.parse(magit_curr_library.getLastUpdateDate()));
            setLast_updater(magit_curr_library.getLastUpdater());
            setName(magit_curr_library.getName());
            setType("library");
        }

        catch (ParseException e) {
            System.out.println("Error to parse date\n");
        }
    }

    public boolean directory_exist(Folder node, String path) {
        Path directory = Paths.get(path+"\\"+node.getName());
        return Files.exists(directory);
    }

    public boolean directory_content_is_changed(Library node, String path) {
        boolean modified=false;

        for (int i = 0; i <node.getChilds().size() && !modified; i++) {
            if (node.getChilds().get(i).getType().equals("blob")) {
                modified = Blob.file_exist((Blob)node.getChilds().get(i),path+"\\"+node.getName());
            }

            else{
                modified = directory_exist(node.getChilds().get(i), path+"\\"+node.getName());
            }
        }

        return modified;
    }

    @Override
    public void create_local_file(Folder node, String path) {
        new File(path+"\\"+node.getName()).mkdir();
    }
}
