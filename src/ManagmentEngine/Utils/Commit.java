package ManagmentEngine.Utils;

import MagitRepository.MagitSingleCommit;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Commit extends Folder {
    public String getMain_library_sha1() {
        return main_library_sha1;
    }

    private String main_library_sha1;

    public Commit() throws NoSuchAlgorithmException {
        super.initialize_sha1();
    }

    private ArrayList<String> precedings_commits_sha1;
    private String commit_essence;
    private String updater;
    private String id;


    public String getId() {
        return id;
    }

    public void initialize_commit(MagitSingleCommit magitSingleCommit, Library main_lib_curr_commit) throws ParseException {
        String prev_commit_sha1;
        type="commit";
        main_library_sha1 = main_lib_curr_commit.sha1;
        commit_essence = magitSingleCommit.getMessage();
        updater = magitSingleCommit.getAuthor();
        setLast_update( get_date(magitSingleCommit.getDateOfCreation()));
        id = magitSingleCommit.getId();
        sha1 = calc_commit_sha1();
        //textual_content = create_commit_content();
        precedings_commits_sha1 = new ArrayList<String>();
    }


    //sha1_main_library
    //precedings_sha1
    //commit_essence
    //lase update
    //last updater
    private String create_commit_content() {
        String res;
        ArrayList<String> line=new ArrayList<String>();
        String str_date, delimiter=", ";

        line.add(main_library_sha1);

        for (int i = 0; i <precedings_commits_sha1.size() ; i++) {
            line.add(precedings_commits_sha1.get(i));
        }

        line.add(commit_essence);

        //convert date to string
        str_date = date_to_string(getLast_update());
        line.add(str_date);
        line.add(updater);

        //add delimiter and new line
        res = String.join(delimiter, line).concat("\n");

        return res;
    }

    private String calc_commit_sha1() {
        String res = null;

        try {
            res = get_sha1(main_library_sha1 +"\n" + id +"\n" + updater + "\n" + getLast_updater()+"\n" + commit_essence);
        }
        catch (UnsupportedEncodingException e) {
            System.out.println("Error to calc sha1 in commit\n");
        }

        return res;
    }

    private Date get_date(String dateOfCreation) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
        return formatter.parse(dateOfCreation);
    }

    public void update_precedings_commits_sha1(MagitSingleCommit magit_commit,ArrayList<Commit>commits ) {
        String prev_commit_sha1;

        for (int i = 0; i < magit_commit.getPrecedingCommits().getPrecedingCommit().size(); i++) {
            prev_commit_sha1 = find_commit_sha1_by_id(magit_commit.getPrecedingCommits().getPrecedingCommit().get(i).getId(), commits);
            precedings_commits_sha1.add(prev_commit_sha1);
        }

        textual_content=create_commit_content();
    }

    private String find_commit_sha1_by_id(String preceding_commit_id, ArrayList<Commit> commits) {
        String res=null;
        boolean found = false;

        for (int i = 0; i <commits.size() && !found ; i++) {
          if(commits.get(i).getId().equals(preceding_commit_id)){
              res = commits.get(i).sha1;
              found=true;
          }
        }

        return res;
    }

    @Override
    public void create_local_file(Folder node, String path) {};
}
