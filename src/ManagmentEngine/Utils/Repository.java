//to do:
//check name updated and difference lib_name and name

package ManagmentEngine.Utils;

import MagitRepository.MagitSingleCommit;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Repository {
    String repo_path, repo_name;
    Library magit_library;  //first child point to objects (all managment files), second child point to branches
    ArrayList<Commit> commits;

    public Library get_nagit_library() {
        return magit_library;
    }

    public void add_commit(Commit commit, Library new_root) throws NoSuchAlgorithmException {
        Library objects, branches;

        if(magit_library==null){
            magit_library = new Library(true);
            objects = new Library(false);
            branches = new Library(false);
            commits=new ArrayList<Commit>();
            magit_library.add_child(objects);
            magit_library.add_child(branches);
            //magit_library.update_lib_sha1();
        }

        commits.add(commit);
        objects = (Library) magit_library.getChilds().get(0);
        objects.add_child(new_root);
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
}
