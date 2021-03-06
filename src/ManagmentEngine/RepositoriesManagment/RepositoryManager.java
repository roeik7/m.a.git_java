

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;

public class RepositoryManager {
    ArrayList<Repository> repositories;
    Repository curr_repository;
    MagitRepository jaxb_repository;
    boolean repository_load_successfully;


    //commit_details[0] - user_name
    //commit_details[1] - commit message
    //check if there are open changes in working stage - if so -> create the tree that is spread from the tree root (now the changes recorded to the repository)
    //after new tree creation - add new commit with the specified details(creator, current time)
    public void commit_changes(String []commit_details) throws NoSuchAlgorithmException, IOException, ParseException, no_active_repository_exception, everything_up_to_date_exception, failed_to_create_file_exception {
        if(repository_load_successfully){
            String changes[] = working_copy_area_status();
            Library new_root;
            Commit new_commit;
            File file = new File(curr_repository.getRepo_path());

            if(changes!=null) {
                new_root = (Library) curr_repository.create_tree_from_exist(commit_details, file, true);
                new_commit = Commit.create_new_commit(curr_repository, new_root, commit_details);
                curr_repository.add_commit(new_commit, new_root,curr_repository.getBranches().getHead_name());
            }

            else{
                throw new everything_up_to_date_exception("The working stage is clean, so theres nothing to commit");
            }
        }

        else{
            throw new no_active_repository_exception("There is no active repository so theres notheing to commit");
        }
    }

    //begin to traverse the current tree from tha path specified by the curr root and check if tha sha1 of any of the nodes changed
    //return:
    //changes[0] - content modified (file content or directory structure changed)
    //changes[1] - removed/renamed (files/directories removed or renamed)
    //changes[2] - added (new files/directory)

    public String [] working_copy_area_status(){
        if(repository_load_successfully){
            String [] changes= {"","",""};

            try {
                Library root = curr_repository.getCurr_structure();

                find_working_copy_changes(root, curr_repository.getRepo_path(), changes);
            } catch (IOException e) {
                System.out.println("Error to find changes in working copy area.\n");
                e.printStackTrace();
            }


            if(!added_changes(changes)){
                changes=null;
            }

            return changes;
        }

        return null;
    }

    //changes[0] - content modified
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

    //initialize completely new repository from given path
    //at the beginning will contain only one commit and one branch
    public void initialize_repository_from_local(String path, String repository_name, String username, String commit_message) throws failed_to_create_file_exception, branch_not_found_exception {
        try {
            repositories = repositories==null? new ArrayList<Repository>() : repositories;

            curr_repository = Repository.create_repository_structure_from_local(path, repository_name, username, commit_message);

            repositories.add(curr_repository);

            create_magit_directories();

            //objects directory store all managment files in repistory
            curr_repository.create_objects_content_local();
            //curr_repository.branch_init("master");
            repository_load_successfully=true;


        } catch (ParseException e) {
            System.out.println(e.getMessage());;
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    //initialize new repository by xml file that contain all the details such as, commits, folders, blobs, branches..
    public void initialize_repository_from_scratch_by_xml(xml_details xml) throws failed_to_create_file_exception, ParseException, NoSuchAlgorithmException, IOException, failed_to_create_local_structure_exception {
        repositories = repositories==null? new ArrayList<Repository>() : repositories;
        jaxb_repository=xml.getRepo_details();
        curr_repository = create_repository_by_jaxb_object();
        curr_repository.switch_commit(find_commit_by_id(find_current_commit_id()));

        repositories.add(curr_repository);

        create_magit_directories();

        //objects directory store all managment files in repistory
        curr_repository.create_objects_content_local();

        //Create the structure derived from the xml
        create_system_by_magit();
        repository_load_successfully=true;
    }

    public void export_active_repoistory_to_xml() throws no_active_repository_exception {
        try {

            if(repository_load_successfully){
                DocumentBuilderFactory do_factory=DocumentBuilderFactory.newInstance();
                DocumentBuilder doc_builder = do_factory.newDocumentBuilder();
                Document doc = doc_builder.newDocument();
                org.w3c.dom.Element root = doc.createElement("MagitRepository");
                root.setAttribute("name", curr_repository.getRepo_name());
                doc.appendChild(root);
                org.w3c.dom.Element location = doc.createElement("location");
                location.appendChild(doc.createTextNode(curr_repository.getRepo_path()));
                root.appendChild(location);
                org.w3c.dom.Element magit_blobs = doc.createElement("MagitBlobs");

                //add each blob to magit blobs
                add_blobs_element(magit_blobs,doc);
                root.appendChild(magit_blobs);

                Element magit_folders = doc.createElement("MagitFolders");

                //add each folder to magit folders
                add_folders_elements(magit_folders,doc);
                root.appendChild(magit_folders);
                Element magit_commits = doc.createElement("MagitCommits");

                //add each commit to magit comits
                add_commits_elements(magit_commits,doc);
                root.appendChild(magit_commits);

                Element magit_branches = doc.createElement("MagitBranches");
                Element head_name = doc.createElement("head");
                head_name.appendChild(doc.createTextNode(curr_repository.getBranches().getHead_name()));

                //add each branch to magit branches
                magit_branches.appendChild(head_name);
                add_branches_elements(magit_branches,doc);
                root.appendChild(magit_branches);

                return;
            }

            throw new no_active_repository_exception("Failed to export repository to xml: there is no active repository");

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

    }

    private void add_branches_elements(Element magit_branches, Document doc) {

        Element single_branch, name, pointed_to_commit;
        Set <String> branches = curr_repository.getBranches().getBranches().keySet();

        for(String branch_name : branches){
            single_branch = doc.createElement("MagitSingleBranch");

            name = doc.createElement("name");
            name.appendChild(doc.createTextNode(branch_name));
            single_branch.appendChild(name);

            pointed_to_commit = doc.createElement("pointed-commit");
            pointed_to_commit.setAttribute("id",curr_repository.getBranches().getBranches().get(branch_name).getLast().getSha1());

            single_branch.appendChild(pointed_to_commit);

            magit_branches.appendChild(single_branch);
        }



    }

    private void add_commits_elements(Element magit_commits, Document doc) {
        Element curr_commit_element, root_folder_id, message,author,date_of_creation,precidings_commits, single_preciding;

        ArrayList<Commit> all_commits = curr_repository.get_commits();

        for (int i = 0; i < all_commits.size(); i++) {
            curr_commit_element = doc.createElement("MagitSingleCommit");

            //initialize single commit
            curr_commit_element.setAttribute("id", all_commits.get(i).getSha1());

            //root folder of current commit
            root_folder_id = doc.createElement("root-folder");
            root_folder_id.setAttribute("id", all_commits.get(i).getMain_library_sha1());
            curr_commit_element.appendChild(root_folder_id);

            //commit message
            message=doc.createElement("message");
            message.appendChild(doc.createTextNode(all_commits.get(i).getCommit_essence()));
            curr_commit_element.appendChild(message);

            //author of current commit
            author = doc.createElement("author");
            author.appendChild(doc.createTextNode(all_commits.get(i).getLast_updater()));
            curr_commit_element.appendChild(author);

            //date of creation element
            date_of_creation = doc.createElement("date-of-creation");
            date_of_creation.appendChild(doc.createTextNode(DataStorage.date_to_string(all_commits.get(i).getLast_update())));
            curr_commit_element.appendChild(date_of_creation);

            precidings_commits = doc.createElement("preceding-commits");

            for (int j = 0; j <all_commits.get(i).getPrecedings_commits_sha1().size() ; j++) {
                single_preciding = doc.createElement("preceding-commit");
                single_preciding.setAttribute("id", all_commits.get(i).getPrecedings_commits_sha1().get(i));

                precidings_commits.appendChild(single_preciding);
            }

            curr_commit_element.appendChild(precidings_commits);
            magit_commits.appendChild(curr_commit_element);
        }
    }

    private void add_folders_elements(Element magit_folders, Document doc) {
        Element curr_folder_element, name, last_updater, last_update_date, items, item;
        Library curr_lib;
        Set <String>exists = curr_repository.getExist().keySet();

        for(String key : exists){
            if (curr_repository.getExist().get(key).getType().equals("library")){
                curr_lib = (Library) curr_repository.getExist().get(key);

                //single folder element
                curr_folder_element= doc.createElement("MagitFolder");
                curr_folder_element.setAttribute("id", curr_lib.getSha1());
                curr_folder_element.setAttribute("is-root", curr_lib.isIs_root()?"true":"false");

                //name element
                name = doc.createElement("name");
                name.appendChild(doc.createTextNode(curr_lib.getName()));
                curr_folder_element.appendChild(name);

                //last updater element
                last_updater=doc.createElement("last-updater");
                last_updater.appendChild(doc.createTextNode(curr_lib.getLast_updater()));
                curr_folder_element.appendChild(last_updater);

                //last date element
                last_update_date=doc.createElement("last-update-date");
                last_update_date.appendChild(doc.createTextNode(DataStorage.date_to_string(curr_lib.getLast_update())));
                curr_folder_element.appendChild(last_update_date);

                //items element
                items = doc.createElement("items");

                for (int i = 0; i <curr_lib.getChilds().size() ; i++) {

                    item = doc.createElement("item");
                    item.setAttribute("id",curr_lib.getChilds().get(i).getSha1());
                    item.setAttribute("type", curr_lib.getChilds().get(i).getType());
                    items.appendChild(item);
                }

                curr_folder_element.appendChild(items);

                //add blob element to blobs
                magit_folders.appendChild(curr_folder_element);
            }
        }
    }

    private void add_blobs_element(Element magit_blobs, Document doc) {
        Element curr_blob_element, name, last_updater, last_update_date, content;
        Blob curr_blob;
        Set <String>exists = curr_repository.getExist().keySet();

        for(String key : exists){
            if (curr_repository.getExist().get(key).getType().equals("blob")){
                curr_blob = (Blob)curr_repository.getExist().get(key);

                //initialize single blob element
                curr_blob_element= doc.createElement("MagitBlob");
                curr_blob_element.setAttribute("id", curr_blob.getSha1());

                //name element
                name = doc.createElement("name");
                name.appendChild(doc.createTextNode(curr_blob.getName()));
                curr_blob_element.appendChild(name);

                last_updater=doc.createElement("last-updater");
                last_updater.appendChild(doc.createTextNode(curr_blob.getLast_updater()));
                curr_blob_element.appendChild(last_updater);

                last_update_date=doc.createElement("last-update-date");
                last_update_date.appendChild(doc.createTextNode(DataStorage.date_to_string(curr_blob.getLast_update())));
                curr_blob_element.appendChild(last_update_date);

                content = doc.createElement("content");
                content.appendChild(doc.createTextNode(curr_blob.getTextual_content()));
                curr_blob_element.appendChild(content);


                //add blob element to blobs
                magit_blobs.appendChild(curr_blob_element);
            }
        }
    }

    //initialize repository details (name, path, libraries, blobs, commits, branches..) derives from the xml files
    private Repository create_repository_by_jaxb_object() throws NoSuchAlgorithmException, ParseException, IOException, failed_to_create_file_exception {
        String id;
        MagitSingleFolder root;
        Library main_lib_curr_commit;
        Repository new_repo = new Repository(jaxb_repository.getLocation(), jaxb_repository.getName() );
        Commit new_commit;

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

        return new_repo;
    }

    //return all commits details belong to active branch
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

    //return all branches in the system
    //format:
    //branch name
    //sha1 of commit point to
    //commit message
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

    public void add_new_branch(String new_branch) throws no_active_repository_exception, IOException, branch_name_exist_exception {
        try {
            if(repository_load_successfully){
                curr_repository.add_new_branch(new_branch);
            }
            else{
                throw new no_active_repository_exception("There is no active repository, so there is no branches to show.");
            }
        } catch (IOException e) {
            System.out.println("I/O Error: Failed to add new branch.\n");
            e.printStackTrace();
        }
    }

    //return all files details related to current commit
    //file name (full path)
    //type (blob/library)
    //file sha1
    //last updater
    //when updated
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

    //return details for each directory/files in current commit
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

    //if the given branch name exist - switch to this branch
    //if working tree isn't clean - failure
    //delete from local the old commit belong to old branch and create the structure derives from the new branch
    //in addition update the curr root in repository to be this that last commit point to
    public void checkout(String branch_name) throws head_branch_deletion_exception, branch_not_found_exception, failed_to_create_file_exception, failed_to_switch_branch_exception {
        try {
            if(working_copy_area_status()==null){
                curr_repository.switch_branch(branch_name);
            }
            else{
                throw new failed_to_switch_branch_exception("Need changes to be commited before switch to another branch");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //get the current root library by sha1 (in repository there is hash table point to all files in the history (key = sha1, value = node)
    private Library find_root_library_by_sha1(String main_library_sha1) {

        Library objects = (Library)curr_repository.get_nagit_library().getChilds().get(0);

        for (int i = 0; i <objects.getChilds().size() ; i++) {

            if(objects.getChilds().get(i).getSha1().equals(main_library_sha1)){
                return (Library)objects.getChilds().get(i);
            }
        }

        return null;
    }

    //return if there are changes to current commit (if there is waiting files in working stage)
    private boolean added_changes(String[] changes) {
        boolean is_changed=false;

        for (int i = 0; i <3 && !is_changed; i++) {
            if(!changes[i].equals("")){
                is_changed=true;
            }
        }

        return is_changed;
    }

    //check if added new files/directory in specified path and update changes if so
    private void update_new_folders(String path,Library node, String[] changes) {

        File file = new File(path);
        String folder_name, type;
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
                    changes[2] = changes[2].concat(type+path+fileEntry.getName()+"\n");
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

    private MagitSingleFolder find_folder_by_id(String id) {
        for (int i = 0; i <jaxb_repository.getMagitFolders().getMagitSingleFolder().size() ; i++) {
            if(jaxb_repository.getMagitFolders().getMagitSingleFolder().get(i).getId().equals(id)){
                return jaxb_repository.getMagitFolders().getMagitSingleFolder().get(i);
            }
        }

        System.out.println("Folder isnt exist. \n");
        return null;
    }

    //create libraries objects derives from the xml (stored at first in MagitSingleFolder)
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

    //create trees derives from the magit directory
    private void create_system_by_magit() {
        String commit_id = find_current_commit_id();
        Commit current_commit = find_commit_by_id(commit_id);
        Library root = find_root_library_by_sha1(current_commit.getMain_library_sha1());

        create_tree(root, curr_repository.getRepo_path());
    }

    private void create_tree(DataStorage root, String path) {

        Library lib;

        if(root.getType().equals("blob")){
            //root.create_local_file(root, path);
            return;
        }
        lib = (Library)root;

        for (int i = 0; i < lib.getChilds().size(); i++) {
            lib.getChilds().get(i).create_local_file(lib.getChilds().get(i), path);
            create_tree(lib.getChilds().get(i), path+"\\"+lib.getChilds().get(i).getName());
        }
    }

    //create local objects (will contain all files in the history) and branches (will contain head file specified the current branch) in magit directory
    public void create_magit_directories() throws failed_to_create_file_exception {
        File file = new File(curr_repository.getRepo_path().concat("\\.magit"));
        boolean success = file.mkdir();

        //create magit
        if(!success){
            throw new failed_to_create_file_exception("Failed to create .magit directory.\n");
        }

        //all files in repository (filenames - sha1)
        file= new File(curr_repository.getRepo_path().concat("\\.magit\\objects"));
        success = file.mkdir();

        //create objects
        if(!success){
            throw new failed_to_create_file_exception("Failed to create objects directory");
        }

        file=new File(curr_repository.getRepo_path().concat("\\.magit\\branches"));
        success=file.mkdir();

        //create branches
        if(!success){
            throw new failed_to_create_file_exception("Failed to create branches directory");

        }
    }

    //check if the given repository name exist
    public boolean repository_exist(String repository_name) {
        if(repositories!=null && repositories.contains(repository_name)){
            return true;
        }
        return false;
    }

    //delete old repository (by user request)
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

    //delete given branch if exist (by user request)
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

    //move all exists repository and if found the specifies repository -> update curr_repository
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
