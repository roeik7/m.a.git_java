//to do:
//check name updated and difference lib_name and name

package ManagmentEngine.Utils;

import MagitRepository.MagitSingleCommit;
import ManagmentEngine.RepositoriesManagment.RepositoryExceptions.branch_not_found_exception;
import ManagmentEngine.RepositoriesManagment.RepositoryExceptions.file_not_exist_exception;
import ManagmentEngine.RepositoriesManagment.RepositoryExceptions.head_branch_deletion_exception;
import ManagmentEngine.RepositoriesManagment.RepositoryExceptions.illegal_branch_deletion_exception;

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
    Branch active_branch;
    Library curr_library;


    public Commit get_current_commit(){
        return curr_commit;
    }

    public Branch getBranches() {
        return active_branch;
    }

    public String getRepo_name() {
        return repo_name;
    }

    public Map get_all_branches(){
        return active_branch.getBranches();
    }

    public Library get_nagit_library() {
        return magit_library;
    }

    public void add_commit(Commit commit, Library new_root, String branch_name) throws NoSuchAlgorithmException, IOException {
        Library objects_folder, branches_folder;

        if(magit_library==null){
            exist = new Hashtable<String, DataStorage>();
            active_branch=new Branch(repo_path+"\\.magit\\branches\\head");
            magit_library = new Library(true);
            objects_folder = new Library(false);
            branches_folder = new Library(false);
            //commits= commits1;
            commits=new ArrayList<Commit>();
            magit_library.add_child(objects_folder);
            magit_library.add_child(branches_folder);
        }

        //active_branch.update_commit_pointer(commit);
        curr_library = new_root;
        add_commited_files(new_root);
        commits.add(commit);
        objects_folder = (Library) magit_library.getChilds().get(0);
        objects_folder.add_child(new_root);
    }

    private void add_commited_files(DataStorage new_root) {
        //add folder to hashtable
        if(!exist.containsKey(new_root.getSha1())){
            exist.put(new_root.getSha1(), new_root);
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

    public void switch_commit(Commit commit) {
        curr_commit=commit;

    }

    private void synchronize_tree_by_magit(String main_library_sha1) {

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

    public void add_branch(String new_branch, String commit_id) throws IOException {
        Commit point_to = find_commit_by_id(commit_id);
        active_branch.add_branch(new_branch,point_to);
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
        active_branch.switch_branch(branch_name);
    }

    public void add_new_branch(String new_branch) throws IOException {
        add_branch(new_branch,curr_commit.getId());
    }

    public void delete_branch(String branch_name) throws illegal_branch_deletion_exception {
        if(active_branch.getHead_name().equals(branch_name)){
            throw new illegal_branch_deletion_exception("The branch youre trying to delete is the head branch.");
        }

        active_branch.delete_branch(branch_name);

        //Commit co
    }

    public DataStorage get_DataStorage_by_sha1(String sha1) throws file_not_exist_exception {
        if(exist.containsKey(sha1)) {
            return exist.get(sha1);
        }

        throw new file_not_exist_exception("The file not exist in this repository.");
    }

    public void switch_branch(String branch_name) throws IOException, head_branch_deletion_exception, branch_not_found_exception {
        if(active_branch.getBranches().containsKey(branch_name)){
            switch_commit(active_branch.getBranches().get(branch_name).getLast());
            active_branch.switch_branch(branch_name);
            return;
        }

        throw new head_branch_deletion_exception("Youre trying to delete head branch.\nFailed to delete branch");

    }
}


