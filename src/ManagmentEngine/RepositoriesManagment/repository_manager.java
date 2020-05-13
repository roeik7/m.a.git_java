

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
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;

public class repository_manager {
    ArrayList<Repository> repositories;
    Repository curr_repository;
    MagitRepository jaxb_repository;

    boolean manage_existing_repo;

    private Library find_root_library_by_sha1(String main_library_sha1) {

        Library objects = (Library)curr_repository.get_nagit_library().getChilds().get(0);

        for (int i = 0; i <objects.getChilds().size() ; i++) {

            if(objects.getChilds().get(i).getSha1().equals(main_library_sha1)){
                return (Library)objects.getChilds().get(i);
            }
        }

        return null;
    }

    public String [] working_copy_area_status(){
        String library_changes = "";
        String [] changes= {"","",""};

        try {
            String commit_id = find_current_commit_id();
            Commit current_commit = find_commit_by_id(commit_id);
            Library root = find_root_library_by_sha1(current_commit.getMain_library_sha1());

            find_working_copy_changes(root, curr_repository.getRepo_path(), changes);
        } catch (IOException e) {
            System.out.println("Error to find changes in working copy area.\n");
            e.printStackTrace();
        }


        if(!added_changes(changes)){
            changes=null;
        }
        //return "Modified: "+changes[0]+"Removed/Renamed: "+changes[1] + "Added: " + changes[2];
        return changes;
    }

    private boolean added_changes(String[] changes) {
        boolean is_changed=false;

        for (int i = 0; i <3 && !is_changed; i++) {
            if(!changes[i].equals("")){
                is_changed=true;
            }
        }

        return is_changed;
    }

    //commit_details[0] - user_name
    //commit_details[1] - commit message
    public boolean commit_changes(String []commit_details) throws NoSuchAlgorithmException, IOException, ParseException {
        String changes[] = working_copy_area_status();
        boolean commited = false;
        Library new_root;
        Commit new_commit;
        File file = new File(curr_repository.getRepo_path());
        if(changes!=null) {
            new_root = (Library) curr_repository.create_tree_from_exist(commit_details, file, true);
            new_commit = Commit.create_new_commit(curr_repository, new_root, commit_details);
            commited=true;
            curr_repository.add_commit(new_commit, new_root);
            curr_repository.switch_commit(new_commit);
        }



        return commited;
    }

    //changes[0] - modified
    //changes[1] - removed/renamed
    //changes[2] - added
    private void find_working_copy_changes(Folder node, String path, String changes[]) throws IOException {
        String new_path;
        boolean is_modified, exist;
        Blob blob;
        Library library;

        if(node.getType().equals("blob")){
            blob = (Blob)node;
            exist=Blob.file_exist(path);

            if(exist){
                is_modified=blob.file_content_changed(node, path);
                if(is_modified){
                    changes[0] = changes[0].concat("File: "+path+ " is modified.\n");
                    //res = "File: "+path+"\\"+blob.getName()+" is modified.\n";
                }
            }

            else{
                changes[1] = changes[1].concat("File: "+path + " is removed/renamed.\n");
            }

            return;
        }


        //library case
        library = (Library) node;
        exist =library.directory_exist(path);

        if (exist) {
            is_modified = library.directory_content_is_changed(library, path);
            if (is_modified){

                changes[0]=changes[0].concat("Directory: "+path+" is modified.\n");
            }
        }

        else{
            changes[1] = changes[1].concat("Directory: "+path+" is removed/renamed.\n");
        }

        update_new_folders(path, library, changes);

        for (int i = 0; i <library.getChilds().size() ; i++) {
            new_path = path + "\\" + library.getChilds().get(i).getName();
            find_working_copy_changes(library.getChilds().get(i), new_path, changes);
        }
    }

    private void update_new_folders(String path,Library node, String[] changes) {

        File file = new File(path);
        String folder_sha1, folder_name, type;
        boolean found = false;
        Folder child;
        if(file!=null){
            for (final File fileEntry : file.listFiles()) {
                folder_name = fileEntry.getName();

                for (int i = 0; i <node.getChilds().size() && !folder_name.equals(".magit") && !found ; i++) {
                    child=node.getChilds().get(i);

                    if(child.getName().equals(folder_name)){
                        found =true;
                    }
                }

                if (!found && !folder_name.equals(".magit")) {
                    type = fileEntry.isDirectory()?"New Directory: " : "New File: ";
                    changes[2] = changes[2].concat(type+path+fileEntry.getName());
                }

                found=false;
            }
        }
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

            create_system_structure(); //if its new repository - create system managment (.magit file and all related files)  (centralized information)
            curr_repository.setRepo_name(jaxb_repository.getName());
            curr_repository.setRepo_path(jaxb_repository.getLocation());

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
            new_commit.initialize_commit_by_magit_single_commit(jaxb_repository.getMagitCommits().getMagitSingleCommit().get(i), main_lib_curr_commit);
            new_repo.add_commit(new_commit, main_lib_curr_commit);
        }

        new_repo.update_precedings_commits(jaxb_repository.getMagitCommits().getMagitSingleCommit(), new_repo.get_commits());
        repositories.add(new_repo);
        curr_repository = new_repo;
        curr_repository.switch_commit(find_commit_by_id(find_current_commit_id()));

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
                MagitBlob mag_blob=find_blob_by_id(magit_curr_library.getItems().getItem().get(i).getId());
                blob=Blob.create_blob(mag_blob);
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

    private MagitBlob find_blob_by_id(String id) {
        String curr_id;

        for (int i = 0; i <jaxb_repository.getMagitBlobs().getMagitBlob().size()  ; i++) {
            curr_id = jaxb_repository.getMagitBlobs().getMagitBlob().get(i).getId();
            if(curr_id.equals(id)){
                return jaxb_repository.getMagitBlobs().getMagitBlob().get(i);
            }
        }

        return null;
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
            create_objects_and_branches(curr_repository.getRepo_path());
            Library objects = (Library) curr_repository.get_nagit_library().getChilds().get(0);

            for (int i = 0; i <objects.getChilds().size() ; i++) {
                create_magit_rec(objects.getChilds().get(i), curr_repository.getRepo_path()+"\\.magit\\objects");
                create_magit_rec(curr_repository.get_commits().get(i),curr_repository.getRepo_path()+"\\.magit\\objects");
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

        create_project(root, curr_repository.getRepo_path());
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
        File file = new File(curr_repository.getRepo_path().concat("\\.magit"));
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
