//to do:
//check name updated and difference lib_name and name

package ManagmentEngine.Utils;

import MagitRepository.MagitSingleCommit;
import ManagmentEngine.RepositoriesManagment.RepositoryExceptions.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Repository {
    String repo_path, repo_name;
    Library magit_library;  //first child point to objects (all managment files), second child point to branches
    ArrayList<Commit> commits;
    Map<String, DataStorage> exist;
    Commit curr_commit;
    Branch branches_managment;
    Library curr_structure;

    public Repository(String repo_path, String repo_name) throws NoSuchAlgorithmException, IOException {
        this.repo_path = repo_path;
        this.repo_name = repo_name;
        commits = new ArrayList<>();
        exist = new Hashtable<String, DataStorage>();
        magit_library = new Library(false);
        branches_managment = new Branch(repo_path+"\\.magit\\branches","master");
    }


    //head_in_target - target ancestor of head
    //target_in_head - head ancestor of target
    //none - no fast forward
    enum FAST_FORWARD{
        TARGET_IN_HEAD,
        HEAD_IN_TARGET,
        NONE
    }

    public static Repository create_repository_structure_from_local(String path, String repository_name, String username, String commit_message) throws ParseException, NoSuchAlgorithmException, IOException, failed_to_create_file_exception {
        Repository new_repository = new Repository(path, repository_name);
        File file = new File(path);
        String commit_details []= {username, commit_message};
        Commit new_commit = new Commit();
        Library new_root = (Library) new_repository.create_tree_from_exist(commit_details, file, true);

        new_commit.initialize_commit(
                new_root, //root
                commit_message,
                username,
                DataStorage.date_to_string(DataStorage.get_current_time()),
                null); //id field - relevant only for xml pull

        new_repository.add_commit(new_commit,new_root,"master");

        //new_repository.branches_managment.add_branch("master", new_repository.curr_commit);
        //new_repository.add_root_to_objects();
        return new_repository;
    }

    public Commit get_current_commit(){
        return curr_commit;
    }

    public Branch getBranches() {
        return branches_managment;
    }

    public Library getCurr_structure() {
        return curr_structure;
    }

    public String getRepo_name() {
        return repo_name;
    }

    public Map get_all_branches(){
        return branches_managment.getBranches();
    }

    public Library get_nagit_library() {
        return magit_library;
    }

    public void add_commit(Commit commit, Library new_root, String branch_name) throws NoSuchAlgorithmException, IOException, failed_to_create_file_exception {
        Library objects_folder, branches_folder;

        if(magit_library.getChilds()==null){
            objects_folder = new Library(false);
            branches_folder = new Library(false);
            commits=new ArrayList<Commit>();
            magit_library.add_child(objects_folder);
            magit_library.add_child(branches_folder);
        }

        //update commit pointer in current branch
        branches_managment.add_commit(branch_name,commit, true);

        //add new files to exist(hash table) and add files locally in .magit directory
        add_commited_files(new_root);
        commits.add(commit);
        switch_commit(commit);
        objects_folder = (Library) magit_library.getChilds().get(0);
        objects_folder.add_child(new_root);
    }

    private void add_commited_files(DataStorage new_root) {
        //add folder to hashtable
        if(!exist.containsKey(new_root.getSha1())){
            exist.put(new_root.getSha1(), new_root);
            Blob.create_and_write_to_file(new_root,repo_path+"\\.magit\\objects",new_root.sha1);
        }

        if(new_root.getType().equals("blob")){
            return;
        }

        for (int i = 0; i <((Library)new_root).getChilds().size() ; i++) {
            add_commited_files(((Library)new_root).getChilds().get(i));
        }
    }

    public void setRepo_path(String repo_path) {
        this.repo_path = repo_path;
    }

    public void setRepo_name(String repo_name) {
        this.repo_name = repo_name;
    }

    public ArrayList<Commit> get_commits() {
        if(commits==null){
            commits=new ArrayList<Commit>();
        }

        return commits;
    }

    public void update_precedings_commits(List<MagitSingleCommit> magitSingleCommit, ArrayList<Commit> commits) {
        for (int i = 0; i < commits.size(); i++) {
            commits.get(i).update_precedings_commits_sha1(magitSingleCommit.get(i), commits);
        }
    }

    public String getRepo_path() {
        return repo_path;
    }

    public void switch_commit(Commit commit) throws IOException, failed_to_create_file_exception {
        delete_curr_strcuture();
        curr_commit=commit;
        update_new_structure();
        create_new_strcuture_local(exist.get(curr_commit.getMain_library_sha1()),repo_path);

    }

    private void create_new_strcuture_local(DataStorage node, String curr_path) throws IOException, failed_to_create_file_exception {

        File file;
        file=new File(curr_path+"\\"+node.getName());

        if(node.type.equals("blob")){
            if(!file.createNewFile()){
                    throw new failed_to_create_file_exception("Failes to create file after switch commit");
            }
            return;
        }

        if(!file.mkdir()) {
            throw new failed_to_create_file_exception("Failed to create directory after switch commit");
        }


        Library curr_lib = (Library)node;

        for (int i = 0; i < curr_lib.getChilds().size(); i++) {
            create_new_strcuture_local(curr_lib.getChilds().get(i), curr_path+"\\"+curr_lib.getName());
        }
    }

    private void delete_curr_strcuture() throws IOException {
        FileUtils.deleteDirectory(new File(repo_path));
    }

    private void update_new_structure() {
        curr_structure = (Library) exist.get(curr_commit.getMain_library_sha1());
    }

    public DataStorage create_tree_from_exist(String commit_details[], File file, boolean is_root) throws IOException, ParseException, NoSuchAlgorithmException {
        ArrayList<DataStorage> childs;
        Blob new_blob;
        Library new_lib;
        String sha1;

        if (file.isFile()){

            sha1 = Blob.find_sha1_to_existing_file(file);
            new_blob = exist.containsKey(sha1)? (Blob) exist.get(sha1) : Blob.initialize_blob_from_eist(file, commit_details);

            return new_blob;
        }

        childs=new ArrayList<DataStorage>();

        for (final File fileEntry : file.listFiles()) {
            if (!fileEntry.getName().equals(".magit")){
                childs.add(create_tree_from_exist(commit_details,fileEntry,false));
            }
        }

        sha1= Library.calc_sha1_by_childs(childs);
        new_lib = exist.containsKey(sha1)? (Library) exist.get(sha1) : Library.initialize_library_from_exist(file, childs, is_root, commit_details);

        return new_lib;
    }

    public Map<String, DataStorage> getExist() {
        return exist;
    }

    private Commit find_commit_by_id(String commit_id) {
        for (int i = 0; i <commits.size() ; i++) {
            if(commits.get(i).getId().equals(commit_id)){
                return commits.get(i);
            }
        }

        return null;
    }

    public void branch_init(String branch_name) throws IOException, branch_not_found_exception {
        branches_managment.switch_branch(branch_name);
    }

    public void add_new_branch(String branch_name) throws IOException, branch_name_exist_exception {
        branches_managment.add_branch(branch_name,  curr_commit);
    }

    public void delete_branch(String branch_name) throws illegal_branch_deletion_exception {
        if(branches_managment.getHead_name().equals(branch_name)){
            throw new illegal_branch_deletion_exception("The branch youre trying to delete is the head branch.");
        }

        branches_managment.delete_branch(branch_name);

        //Commit co
    }

    public DataStorage get_DataStorage_by_sha1(String sha1) throws file_not_exist_exception {
        if(exist.containsKey(sha1)) {
            return exist.get(sha1);
        }

        throw new file_not_exist_exception("The file not exist in this repository.");
    }

    public void switch_branch(String branch_name) throws IOException, head_branch_deletion_exception, branch_not_found_exception, failed_to_create_file_exception {
        if(branches_managment.getBranches().containsKey(branch_name)){
            switch_commit(branches_managment.getBranches().get(branch_name).getLast());
            branches_managment.switch_branch(branch_name);

            return;
        }

        throw new head_branch_deletion_exception("Youre trying to delete head branch.\nFailed to delete branch");

    }

    public void create_objects_content_local() {
        Library objects = (Library) magit_library.getChilds().get(0);

        for (int i = 0; i <objects.getChilds().size() ; i++) {
            //regular files and directories
            create_local_magit_content_rec(objects.getChilds().get(i), getRepo_path()+"\\.magit\\objects");

            //commits files
            create_local_magit_content_rec(commits.get(i),getRepo_path()+"\\.magit\\objects");
        }

    }

    private void create_local_magit_content_rec(DataStorage folder, String path) {
        Library file;

        if (folder.getType().equals("blob")) {
            Blob.create_and_write_to_file(folder, path, folder.getSha1());
            return;
        }

        else if(folder.getType().equals("library")){
            file = (Library) folder;

            for (int i = 0; i <file.getChilds().size() ; i++) {
                create_local_magit_content_rec(file.getChilds().get(i),path);
            }
        }

        Blob.create_and_write_to_file(folder, path, folder.getSha1() );
    }

}


