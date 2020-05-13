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
import java.util.Date;

public class Library extends Folder {
    boolean is_root;
    ArrayList<Folder> childs;

    public static String calc_sha1_by_childs(ArrayList<Folder> childs) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        Library temp = new Library(false);
        temp.setChilds(childs);
        String content = temp.create_library_textual_content();

        return get_sha1(content);
    }

    public ArrayList<Folder> getChilds() {
        return childs;
    }

    public void setIs_root(boolean is_root) {
        this.is_root = is_root;
    }

    public void update_library_after_childs_creation(MagitSingleFolder magit_curr_library) throws ParseException, NoSuchAlgorithmException {
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
            str_date = super.date_to_string(getChilds().get(i).getLast_update());
            line.add(str_date);

            //add delimiter and new line
            res.add(String.join(delimiter, line).concat("\n"));
            line.clear();
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

    public void setChilds(ArrayList<Folder> childs) {
        this.childs = childs;
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

    public boolean directory_exist(String path) {
        Path directory = Paths.get(path);
        return Files.exists(directory);
    }

    public boolean directory_content_is_changed(Library node, String path) {
        boolean modified=false;

        for (int i = 0; i <node.getChilds().size() && !modified; i++) {

            if (node.getChilds().get(i).getType().equals("blob")) {
                modified = !Blob.file_exist(path+"\\"+node.getChilds().get(i).getName());
            }

            else{
                modified = !directory_exist(path+"\\"+node.getChilds().get(i).getName());
            }
        }

        return modified;
    }

    @Override
    public void create_local_file(Folder node, String path) {
        new File(path+"\\"+node.getName()).mkdir();
    }

    public Library create_commited_library(ArrayList<Folder> childs, String name, String last_updater, boolean is_root) throws NoSuchAlgorithmException, UnsupportedEncodingException, ParseException {
        Library new_lib = new Library(is_root);
        new_lib.setChilds(childs);
        new_lib.setName(name);
        new_lib.setLast_updater(last_updater);
        new_lib.setLast_update(get_current_time());
        new_lib.setType("library");
        String library_content = create_library_textual_content();
        new_lib.setTextual_content(library_content);
        new_lib.setSha1(get_sha1(library_content));

        return new_lib;
    }

    public static Library initialize_library_from_exist(File file, ArrayList<Folder> childs, boolean is_root, String[] commit_details) throws NoSuchAlgorithmException, UnsupportedEncodingException, ParseException {
        Library res = new Library(is_root);
        res.setChilds(childs);
        String content = res.create_library_textual_content();
        String sha1 = get_sha1(content);
        res.initialize_library(childs, content, sha1, get_current_time(), file.getName(), commit_details[0], is_root);

        return res;
    }

    private void initialize_library(ArrayList<Folder> childs, String content, String sha1, Date current_time, String name, String updater, boolean is_root) throws NoSuchAlgorithmException {
        //Library res = new Library(is_root);

        setChilds(childs);
        setTextual_content(content);
        setSha1(sha1);
        setType("library");
        setLast_update(current_time);
        setName(name);
        setLast_updater(updater);
    }

}
