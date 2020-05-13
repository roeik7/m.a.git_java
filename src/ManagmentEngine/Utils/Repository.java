//to do:
//check name updated and difference lib_name and name

package ManagmentEngine.Utils;

import MagitRepository.MagitSingleCommit;

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
    Map<String, Folder> exist;
    Commit curr_commit;

    Library curr_library;
    private ArrayList<Commit> commits1;

    public Library get_nagit_library() {
        return magit_library;
    }

    public void add_commit(Commit commit, Library new_root) throws NoSuchAlgorithmException {
        Library objects, branches;

        if(magit_library==null){
            exist = new Hashtable<String, Folder>();
            magit_library = new Library(true);
            objects = new Library(false);
            branches = new Library(false);
            commits= commits1;
            magit_library.add_child(objects);
            magit_library.add_child(branches);
            //magit_library.update_lib_sha1();
        }

        curr_library = new_root;
        add_commited_files(new_root);
        commits.add(commit);
        objects = (Library) magit_library.getChilds().get(0);
        objects.add_child(new_root);
    }

    private void add_commited_files(Folder new_root) {
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

    public Folder create_tree_from_exist(String commit_details[], File file, boolean is_root) throws IOException, ParseException, NoSuchAlgorithmException {
        ArrayList<Folder> childs;
        Blob new_blob;
        Library new_lib;
        String sha1;

        if (file.isFile()){

            sha1 = Blob.find_sha1_to_existing_file(file);
            new_blob = exist.containsKey(sha1)? (Blob) exist.get(sha1) : Blob.initialize_blob_from_eist(file, commit_details);

            return new_blob;
        }

        childs=new ArrayList<Folder>();

        for (final File fileEntry : file.listFiles()) {
            if (!fileEntry.getName().equals(".magit")){
                childs.add(create_tree_from_exist(commit_details,fileEntry,false));
            }
        }

        sha1= Library.calc_sha1_by_childs(childs);
        new_lib = exist.containsKey(sha1)? (Library) exist.get(sha1) : Library.initialize_library_from_exist(file, childs, is_root, commit_details);

        return new_lib;
    }
}


