package ManagmentEngine.Utils;

import ManagmentEngine.RepositoriesManagment.RepositoryExceptions.branch_name_exist_exception;
import ManagmentEngine.RepositoriesManagment.RepositoryExceptions.branch_not_found_exception;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class Branch {
    private Commit head;
    private String head_name;
    private String head_file_path;
    private Map<String, LinkedList<Commit>> branches;

    public void setHead(Commit head) {
        this.head = head;
    }

    public void setBranches(Map<String, LinkedList<Commit>> branches) {
        this.branches = branches;
    }

    public void setHead_file_path(String head_file_path) {
        this.head_file_path = head_file_path;
    }

    public Commit getHead() {
        return head;
    }

    public Map<String, LinkedList<Commit>> getBranches() {
        return branches;
    }

    public String getHead_file_path() {
        return head_file_path;
    }

    public Branch(String branches_path, String branch_name) throws IOException {
        branches=new Hashtable<String, LinkedList<Commit>>();
        head_file_path=branches_path;
        head_name=branch_name;
    }

    public String getHead_name() {
        return head_name;
    }

    public void set_head_branch(String branch_name){
        head=branches.get(branch_name).getFirst();
        head_name=branch_name;
    }

    public void add_branch(String branch_name, Commit curr_commit) throws IOException, branch_name_exist_exception {
        if(!branch_exist(branch_name)){
            LinkedList<Commit> commits = new LinkedList<>();
            commits.add(curr_commit);
            branches.put(branch_name, commits);
        }

        throw new branch_name_exist_exception("The Branch name you entered is already exist");
    }

    public boolean branch_exist(String branch_name){
        return branches.containsKey(branch_name);
    }

    public void switch_branch(String branch_name) throws IOException, branch_not_found_exception {
        if(branches.containsKey(branch_name)){
            update_head_file(branch_name);
            head=branches.get(branch_name).getLast();
            head_name=branch_name;
            return;
        }

        throw new branch_not_found_exception("The branch isnt found.");

    }

    private void update_head_file(String branch_name) throws IOException {
        File file = new File(head_file_path+"\\HEAD");
        if(!file.exists()){
            file.createNewFile();
        }
        FileWriter fd = new FileWriter(head_file_path+"\\HEAD");
        fd.write(branch_name);
        fd.close();
    }

    public Set<String> get_all_branches(){
        return branches.keySet();

    }

    public void update_commit_pointer(Commit commit) {
        head=commit;
    }

    public void delete_branch(String branch_name) {
        branches.remove(branch_name);
    }

    public String get_head_branch_history() {
        LinkedList<Commit> commits = branches.get(head_name);
        String res="";
        for (int i = 0; i <commits.size() ; i++) {
            res+="Commit SHA-1: "+commits.get(i).getSha1()+"\n"+
                    "Commit Message: "+commits.get(i).getCommit_essence()+"\n"+
                    "Created At: "+commits.get(i).getLast_update()+"\n"+
                    "Commites By: "+commits.get(i).getLast_updater()+"\n\n";

        }

        return res;
    }

    public void add_commit(String branch_name, Commit commit, boolean initial_commit) {
        if(branches.containsKey(branch_name)){
            LinkedList<Commit>commits = branches.get(branch_name);
            commits.add(commit);
        }
        else if(initial_commit){
            LinkedList<Commit>commits = new LinkedList<>();
            commits.add(commit);
            branches.put(branch_name,commits);
        }
    }
}
