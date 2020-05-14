

//to do:
// fix and add exceptions

package ManagmentEngine.RepositoriesManagment;

import MagitRepository.MagitBlob;
import MagitRepository.MagitRepository;
import MagitRepository.MagitSingleFolder;
import ManagmentEngine.RepositoriesManagment.RepositoryExceptions.*;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class repository_manager {
    ArrayList<Repository> repositories;
    Repository curr_repository;
    MagitRepository jaxb_repository;
    boolean repository_load_successfully;
    boolean manage_existing_repo;

    public String active_brance_history() throws no_active_repository_exception {
        String res;

        if(repository_load_successfully){
            res = curr_repository.getBranches().get_head_branch_history();
        }
        else{
            throw new no_active_repository_exception("There is no active reposstory, so there's nothing to show about commits.");
        }

        return res;
    }

    public ArrayList get_branches() throws no_active_repository_exception {
        Map<String, LinkedList<Commit>> active_branches = curr_repository.get_all_branches();
        ArrayList<String> branches_details=new ArrayList<>();
        String single_branch = new String();
        Iterator<Map.Entry<String, LinkedList<Commit>> > it = active_branches.entrySet().iterator();

        if(repository_load_successfully){

            while(it.hasNext()){
                Map.Entry<String, LinkedList<Commit>> entry = it.next();
                single_branch=("Brance Name: "+entry.getKey()+"\n"+
                        "Sha-1 of pointed commit: "+entry.getValue().getLast().getSha1()+"\n"+
                        "Commit Message: "+entry.getValue().getLast().getCommit_essence()+"\n");
                branches_details.add(single_branch);
            }
        }
        else{
            throw new no_active_repository_exception("There is no active repository, so there is no branches to show.");
        }



    return branches_details;
    }

    public void add_new_branch(String new_branch) throws no_active_repository_exception {
        try {
            if(repository_load_successfully){
                curr_repository.add_new_branch(new_branch);
            }
            else{
                throw new no_active_repository_exception("There is no active repository, so there is no branches to show.");
            }
        } catch (IOException e) {
            System.out.println("Error to add new branch.\n");
            e.printStackTrace();
        }
    }

    public String commit_history() throws file_not_exist_exception, no_active_repository_exception {
        String res;
        if(repository_load_successfully){
            Commit curr_commit=curr_repository.get_current_commit();
            Library  root= (Library) curr_repository.get_DataStorage_by_sha1(curr_commit.getMain_library_sha1());
            res = get_tree_details(root, curr_repository.getRepo_path());
            return res;
        }

        throw new no_active_repository_exception("There is no active repository.");
    }

    private String get_tree_details(DataStorage root, String path) {
        String res;

        if(root.getType().equals("blob")){
            return "File Path: "+path+"\n"+
                    "Type: Blob\n"+
                    "File SHA1: "+root.getSha1()+"\n"+
                    "Last updater: "+root.getLast_updater()+"\n"+
                    "Last update: "+root.getLast_update()+"\n\n";
        }
        res = "Library Path: "+path+"\n"+
                "Type: Library\n"+
                "Library SHA1: "+root.getSha1()+"\n"+
                "Last updater: "+root.getLast_updater()+"\n"+
                "Last update: "+root.getLast_update()+"\n\n";

        for (int i = 0; i <((Library)root).getChilds().size() ; i++) {
            res+=get_tree_details(((Library) root).getChilds().get(i), path+"\\"+((Library) root).getChilds().get(i).getName());
        }

        return res;
    }

    public void checkout(String branch_name) throws head_branch_deletion_exception, branch_not_found_exception {
        try {
            curr_repository.switch_branch(branch_name);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public String [] working_copy_area_status(){
        String library_changes = "";
        String [] changes= {"","",""};

        try {
            //String commit_id = find_current_commit_id();
            //Commit current_commit = find_commit_by_id(commit_id);
            Commit curr_commit = curr_repository.get_current_commit();
            Library root = find_root_library_by_sha1(curr_commit.getMain_library_sha1());

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
            curr_repository.add_commit(new_commit, new_root,jaxb_repository.getMagitBranches().getHead());
            curr_repository.switch_commit(new_commit);
        }



        return commited;
    }

    //changes[0] - modified
    //changes[1] - removed/renamed
    //changes[2] - added
    private void find_working_copy_changes(DataStorage node, String path, String changes[]) throws IOException {
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
        DataStorage child;
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

    public void initalize_repository(xml_details xml, boolean manage_existing) throws failed_to_create_local_structure_exception {
        try {
            if(repositories==null){
                repositories = new ArrayList<Repository>();
            }

            //get the parsed object of the xml
            jaxb_repository = xml.getRepo_details();

            //if its new repository - create system managment (.magit and all related files)  (centralized information)
            create_system_structure();
            curr_repository.setRepo_name(jaxb_repository.getName());
            curr_repository.setRepo_path(jaxb_repository.getLocation());

            //If the project is not saved with the client
            if(!manage_existing){
                create_local_structure();
            }

            repository_load_successfully=true;

        }

        catch (ParseException e) {
            System.out.println("Rrror to parse date.\n"+e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error to get sha1.\n"+e.getMessage());
        } catch (IOException e) {
            System.out.println("Error to add new branch.\n");
            e.printStackTrace();
        }
    }

    private void create_system_structure() throws ParseException, NoSuchAlgorithmException, IOException {
        String id;
        MagitSingleFolder root;
        Library main_lib_curr_commit;
        Repository new_repo = new Repository();
        Commit new_commit;

        new_repo.setRepo_path(jaxb_repository.getLocation());
        new_repo.setRepo_name(jaxb_repository.getName());

        //foreach commit create tree structure
        for (int i = 0; i < jaxb_repository.getMagitCommits().getMagitSingleCommit().size(); i++) {
            id=jaxb_repository.getMagitCommits().getMagitSingleCommit().get(i).getRootFolder().getId();
            root = find_folder_by_id(id);
            main_lib_curr_commit= create_libraries_structure(root);
            new_commit = new Commit();
            new_commit.initialize_commit_by_magit_single_commit(jaxb_repository.getMagitCommits().getMagitSingleCommit().get(i), main_lib_curr_commit);
            new_repo.add_commit(new_commit, main_lib_curr_commit,jaxb_repository.getMagitBranches().getMagitSingleBranch().get(i).getName());
        }

        new_repo.update_precedings_commits(jaxb_repository.getMagitCommits().getMagitSingleCommit(), new_repo.get_commits());
        repositories.add(new_repo);
        curr_repository = new_repo;
        curr_repository.switch_commit(find_commit_by_id(find_current_commit_id()));
        add_brances_from_xml(jaxb_repository);
        int x=5;
    }

    private void add_brances_from_xml(MagitRepository jaxb_repository) throws IOException {
        String branch_name, commit_id;
        for (int i = 0; i <jaxb_repository.getMagitBranches().getMagitSingleBranch().size() ; i++) {
            branch_name=jaxb_repository.getMagitBranches().getMagitSingleBranch().get(i).getName();
            commit_id = jaxb_repository.getMagitBranches().getMagitSingleBranch().get(i).getPointedCommit().getId();
            curr_repository.add_branch(branch_name, commit_id);
        }
        curr_repository.getBranches().set_head_branch(jaxb_repository.getMagitBranches().getHead());
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

    private void create_local_structure() throws failed_to_create_local_structure_exception {
        try {
            if(repository_contain_magit(curr_repository.getRepo_path())){
                create_magit_directory();
                create_objects_and_branches(curr_repository.getRepo_path());
                Library objects = (Library) curr_repository.get_nagit_library().getChilds().get(0);

                for (int i = 0; i <objects.getChilds().size() ; i++) {
                    create_magit_rec(objects.getChilds().get(i), curr_repository.getRepo_path()+"\\.magit\\objects");
                    create_magit_rec(curr_repository.get_commits().get(i),curr_repository.getRepo_path()+"\\.magit\\objects");
                }
            }

            create_system_by_magit();
            String branch_name = jaxb_repository==null? curr_repository.getBranches().getHead_name() : jaxb_repository.getMagitBranches().getHead();
            curr_repository.branch_init(branch_name);
            //create_commits_files();

        } catch (Exception e) {
            throw new failed_to_create_local_structure_exception("Failed to create local structure.");
        }
    }

    private void create_system_by_magit() {
        String commit_id = find_current_commit_id();
        Commit current_commit = find_commit_by_id(commit_id);
        Library root = find_root_library_by_sha1(current_commit.getMain_library_sha1());

        create_project(root, curr_repository.getRepo_path());
    }

    private void create_project(DataStorage root, String path) {

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

    private void create_magit_rec(DataStorage folder, String path) {
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

    public boolean repository_exist(String repository_name) {
        if(repositories.contains(repository_name)){
            return true;
        }
        return false;
    }

    public boolean delete_repository(String path) throws failed_to_delete_repository_exception {
        boolean deleted= false;
        try {
            FileUtils.deleteDirectory(new File(path));
            deleted=true;
        } catch (IOException e) {
            throw new failed_to_delete_repository_exception("Failed to delete this repoistory.\n");
        }

        return deleted;
    }

    public void delete_branch(String branch_name) throws no_active_repository_exception, illegal_branch_deletion_exception {
        try {
            if(repository_load_successfully){
                curr_repository.delete_branch(branch_name);
            }
            throw new no_active_repository_exception("There is no active repository, so there is no branch to delete");
        } catch (illegal_branch_deletion_exception | no_active_repository_exception e) {
            throw new illegal_branch_deletion_exception(e.getMessage());
        }
    }


    public void initialize_old_repository(String repo_name) throws repository_not_found_exception {
        for (int i = 0; repositories!=null && i <repositories.size() ; i++) {
            if(repositories.get(i).getRepo_name().equals(repo_name)){
                curr_repository = repositories.get(i);
                repository_load_successfully=true;
                return;
            }
        }
        repository_load_successfully=false;
        throw new repository_not_found_exception("Old repository may not exist.");
    }


    //check if the specified path contain magit file (versions management file)
    public boolean repository_contain_magit(String repository_path) {
        Path path= Paths.get(repository_path);

        return Files.exists(path);
    }
}
