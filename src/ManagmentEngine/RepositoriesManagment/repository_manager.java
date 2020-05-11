

//to do:
// fix and add exceptions

package ManagmentEngine.RepositoriesManagment;

import MagitRepository.MagitBlob;
import MagitRepository.MagitRepository;
import MagitRepository.MagitSingleFolder;
import ManagmentEngine.RepositoriesManagment.RepositoryExceptions.failed_to_create_file_exception;
import ManagmentEngine.Utils.*;
import ManagmentEngine.XML_Managment.xml_details;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;

public class repository_manager {
    ArrayList<Repository> repositories;
    String repository_path;
    Repository curr_repository;
    MagitRepository jaxb_repository;
    String repository_name;

    MessageDigest digest;
    //Set<String>files;
    Library root;

    boolean manage_existing_repo;

    public String all_commit_libraries(){
        String commit_id = find_current_commit_id();
        Commit current_commit = find_commit_by_id(commit_id);
        Library root = find_root_library_by_sha1(current_commit.getMain_library_sha1());
        String library_desc= get_library_details(root,repository_path+"\\.magit\\objects");

        return library_desc;

    }

    private Library find_root_library_by_sha1(String main_library_sha1) {

        Library objects = (Library)curr_repository.get_nagit_library().getChilds().get(0);

        for (int i = 0; i <objects.getChilds().size() ; i++) {

            if(objects.getChilds().get(i).getSha1().equals(main_library_sha1)){
                return (Library)objects.getChilds().get(i);
            }
        }

        return null;
    }

    public String working_copy_area_status(){
        String library_changes = "";
        try {
            String commit_id = find_current_commit_id();
            Commit current_commit = find_commit_by_id(commit_id);
            Library root = find_root_library_by_sha1(current_commit.getMain_library_sha1());

            for (int i = 0; i <root.getChilds().size() ; i++) {
                library_changes += find_working_copy_changes(root.getChilds().get(i), repository_path);

            }
        } catch (IOException e) {
            System.out.println("Error to find changes in working copy area.\n");
            e.printStackTrace();
        }

        return library_changes;

    }

    private String find_working_copy_changes(Folder node, String path) throws IOException {
        //File file;
        boolean is_modified, exist;
        String res="";
        Blob blob;
        Library library;

        if(node.getType().equals("blob")){
            blob = (Blob)node;
            exist = blob.file_exist(blob, path);

            if(exist){
                is_modified=blob.file_content_changed(node, path);

                if(is_modified){
                    res = "File: "+path+"\\"+blob.getName()+" is modified.\n";
                }
            }

            else{
                res = "File: "+path+"\\"+blob.getName()+" is removed/renamed.\n";
            }

            return res;
        }

        library = (Library) node;

        for (int i = 0; i <library.getChilds().size() ; i++) {
            res+=find_working_copy_changes(library.getChilds().get(i), path+"\\"+library.getName());
        }

        exist = library.directory_exist(node, path);

        if (exist) {
            is_modified = library.directory_content_is_changed(library, path);
            if (is_modified){
                res+="Directory: "+library.getName()+" is modified.\n";
            }
        }

        else{
            res+="Directory: "+library.getName()+" is removed/renamed.\n";
        }


        return res;
    }

    private String get_library_details(Folder file, String path) {
        Library lib;
        String res;

        if(file.getType().equals("blob")){
            return file.toString(path +"\\"+file.getName());
        }

        res=file.toString(path);

        lib = (Library)file;

        for (int i = 0; i <lib.getChilds().size() ; i++) {
            res+=get_library_details(lib.getChilds().get(i), path+"\\"+file.getName());
        }

        return res;
    }

    private Commit find_commit_by_id(String commit_id) {
        Commit res=null;
        boolean found=false;

        for (int i = 0; i <curr_repository.get_commits().size() && !found ; i++) {
            if (curr_repository.get_commits().get(i).getId().equals(commit_id)){
                res=curr_repository.get_commits().get(i);
                found =true;
            }
        }

        return res;
    }

    private String find_current_commit_id() {
        String branch_name=jaxb_repository.getMagitBranches().getHead();
        String res="";
        boolean found = false;

        for (int i = 0; i <jaxb_repository.getMagitBranches().getMagitSingleBranch().size() && !found ; i++) {
            if(jaxb_repository.getMagitBranches().getMagitSingleBranch().get(i).getName().equals(branch_name)){
                res=jaxb_repository.getMagitBranches().getMagitSingleBranch().get(i).getPointedCommit().getId();
                found=true;
            }
        }

        return res;
    }

    public void initalize_repository(xml_details xml, boolean manage_existing){
        try {
            if(repositories==null){
                repositories = new ArrayList<Repository>();
            }
            jaxb_repository = xml.getRepo_details();
            repository_name = jaxb_repository.getName();
            repository_path=jaxb_repository.getLocation();
            create_system_structure(); //if its new repository - create system managment (.magit file and all related files)  (centralized information)

            if(!manage_existing){
                create_local_structure();
            }
        }

        catch (ParseException e) {
            System.out.println("Rrror to parse date "+e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error to get sha1 "+e.getMessage());
        }
    }

    private void create_system_structure() throws ParseException, NoSuchAlgorithmException {
        String id;
        MagitSingleFolder root;
        Library main_lib_curr_commit;
        Repository new_repo = new Repository();
        Commit new_commit;

        //foreach commit create tree structure
        for (int i = 0; i < jaxb_repository.getMagitCommits().getMagitSingleCommit().size(); i++) {
            id=jaxb_repository.getMagitCommits().getMagitSingleCommit().get(i).getRootFolder().getId();
            root = find_folder_by_id(id);
            main_lib_curr_commit= create_libraries_structure(root);
            new_commit = new Commit();
            new_commit.initialize_commit(jaxb_repository.getMagitCommits().getMagitSingleCommit().get(i), main_lib_curr_commit);
            new_repo.add_commit(new_commit, main_lib_curr_commit);
        }

        new_repo.update_precedings_commits(jaxb_repository.getMagitCommits().getMagitSingleCommit(), new_repo.get_commits());
        repositories.add(new_repo);
        curr_repository = new_repo;

        int x=5;
    }

    private MagitSingleFolder find_folder_by_id(String id) {
        for (int i = 0; i <jaxb_repository.getMagitFolders().getMagitSingleFolder().size() ; i++) {
            if(jaxb_repository.getMagitFolders().getMagitSingleFolder().get(i).getId().equals(id)){
                return jaxb_repository.getMagitFolders().getMagitSingleFolder().get(i);
            }
        }

        System.out.println("Folder isnt exist. \n");
        return null;
    }

    private MagitSingleFolder find_root_folder() {
        MagitSingleFolder res=null;
        boolean found = false;

        for (int i = 0; i <jaxb_repository.getMagitFolders().getMagitSingleFolder().size() && !found ; i++) {

            found = jaxb_repository.getMagitFolders().getMagitSingleFolder().get(i).isIsRoot();
            if(found){
                res=jaxb_repository.getMagitFolders().getMagitSingleFolder().get(i);
            }
        }

        return res;
    }

    private Library create_libraries_structure(MagitSingleFolder magit_curr_library) throws NoSuchAlgorithmException, ParseException {
        Blob blob;
        Library curr_node = new Library(magit_curr_library.isIsRoot());
        Library new_child;
        String id;
        curr_node.initialize_library(magit_curr_library);

        for (int i = 0; i < magit_curr_library.getItems().getItem().size(); i++) {
            if(magit_curr_library.getItems().getItem().get(i).getType().equals("blob")){
                id=magit_curr_library.getItems().getItem().get(i).getId();
                blob=Blob.create_blob(magit_curr_library.getItems().getItem().get(i).getId(), get_blob_by_id(id));
                curr_node.add_child(blob);
            }
            else {
                id=magit_curr_library.getItems().getItem().get(i).getId();
                new_child = create_libraries_structure(get_library_by_id(id));
                curr_node.add_child(new_child);
            }
        }

        curr_node.update_library_after_childs_creation(magit_curr_library);

        return curr_node;
    }

    private MagitSingleFolder get_library_by_id(String library_id) {
        MagitSingleFolder  res=null;
        boolean found=false;

        for (int i = 0; i <jaxb_repository.getMagitFolders().getMagitSingleFolder().size() && !found ; i++) {
            if(jaxb_repository.getMagitFolders().getMagitSingleFolder().get(i).getId().equals(library_id)){
                res = jaxb_repository.getMagitFolders().getMagitSingleFolder().get(i);
                found = true;
            }
        }

        return res;
    }

    private MagitBlob get_blob_by_id(String id) {
        MagitBlob res=null;
        boolean found=false;

        for (int i = 0; i <jaxb_repository.getMagitBlobs().getMagitBlob().size() && !found ; i++) {
            if(jaxb_repository.getMagitBlobs().getMagitBlob().get(i).getId().equals(id)){
                res = jaxb_repository.getMagitBlobs().getMagitBlob().get(i);
                found = true;
            }
        }

        return res;
    }

    private void create_local_structure() {
        try {
            create_magit_directory();
            create_objects_and_branches(repository_path);
            Library objects = (Library) curr_repository.get_nagit_library().getChilds().get(0);

            for (int i = 0; i <objects.getChilds().size() ; i++) {
                create_magit_rec(objects.getChilds().get(i), repository_path+"\\.magit\\objects");
                create_magit_rec(curr_repository.get_commits().get(i),repository_path+"\\.magit\\objects");
            }

            create_system_by_magit();
            //create_commits_files();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void create_system_by_magit() {
        String commit_id = find_current_commit_id();
        Commit current_commit = find_commit_by_id(commit_id);
        Library root = find_root_library_by_sha1(current_commit.getMain_library_sha1());

        create_project(root, repository_path);
        int x;
    }

    private void create_project(Folder root, String path) {

        Library lib;

        if(root.getType().equals("blob")){
            //root.create_local_file(root, path);
            return;
        }
        lib = (Library)root;

        for (int i = 0; i < lib.getChilds().size(); i++) {
            lib.getChilds().get(i).create_local_file(lib.getChilds().get(i), path);
            create_project(lib.getChilds().get(i), path+"\\"+lib.getChilds().get(i).getName());
        }
    }

    private void create_objects_and_branches(String repository_path) {
        File file = new File(repository_path+"\\.magit\\objects");
        boolean created = file.mkdir();
        file = new File(repository_path+"\\.magit\\branches");
        created = file.mkdir() && created;

        if(!created){
            System.out.println("Failed to create objects and branches\n");
        }
    }

    private void create_magit_rec(Folder folder, String path) {
        Library file;

        if (folder.getType().equals("blob")) {
            Blob.create_and_write_to_file(folder, path, folder.getSha1());
            return;
        }

        else if(folder.getType().equals("library")){
            file = (Library) folder;

            for (int i = 0; i <file.getChilds().size() ; i++) {
                create_magit_rec(file.getChilds().get(i),path);
            }
        }

        Blob.create_and_write_to_file(folder, path, folder.getSha1() );
    }

    public void create_magit_directory() throws failed_to_create_file_exception {
        File file = new File(repository_path.concat("\\.magit"));
        boolean success = file.mkdir();

        if(!success){
            throw new failed_to_create_file_exception("cant create .magit directory.\n");
        }

        System.out.println("Magit directory created\n");

    }

    public boolean repository_exist(String repo_path) {
        Path path= Paths.get(repo_path);

        return Files.exists(path);
    }

    public String delete_repository(String path) {
        String message = "Success";
        try {
            FileUtils.deleteDirectory(new File(path));
            message="Success";

        } catch (IOException e) {
            message = "Cant delete content.\n"+e.getMessage();

        }

        return message;
    }
}
