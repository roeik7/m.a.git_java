package ManagmentEngine.XML_Managment;
import MagitRepository.MagitRepository;
import ManagmentEngine.XML_Managment.XML_Exceptions.file_extension_invalid;
import ManagmentEngine.XML_Managment.XML_Exceptions.file_not_exist_exception;
import ManagmentEngine.XML_Managment.XML_Exceptions.invalid_properties_exception;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.HashSet;
import java.util.Set;


public class xml_details {

    String file_path;
    File xml_file;
    MagitRepository repo_details;


    public MagitRepository getRepo_details() {
        return repo_details;
    }


//    public xml_details(String file_path) {
//        this.file_path = file_path;
//        xml_file= new File(file_path);
//    }

    public String get_location(){
        return repo_details.getLocation();
    }

    public boolean xml_is_valid(String file_path){
        try{
            this.file_path=file_path;
            file_exist();
            extension_valid();
            repo_details= get_repository_by_jaxb(file_path);
            unique_id();
            folders_pointers_valid();
            commits_points_to_root();
            //head_points_to_valid_branch();

            return true;
        }

        catch(Exception e){
            System.out.println(e.getMessage());
            return false;
        }

    }

    private void commits_points_to_root() throws invalid_properties_exception {

        String folder_id;
        Boolean is_root;
        for (int i = 0; i <repo_details.getMagitCommits().getMagitSingleCommit().size() ; i++) {

            folder_id=repo_details.getMagitCommits().getMagitSingleCommit().get(i).getRootFolder().getId();

            for (int j = 0; j <repo_details.getMagitFolders().getMagitSingleFolder().size() ; j++) {

                is_root = repo_details.getMagitFolders().getMagitSingleFolder().get(j).isIsRoot();
                if(repo_details.getMagitFolders().getMagitSingleFolder().get(j).getId()==folder_id && !is_root){
                    throw new invalid_properties_exception("commit isnt points to valid root folder");
                }
            }
        }
    }

    private void folders_pointers_valid() throws invalid_properties_exception {
        Set<String> blobs_ids=get_blobs_ids();
        Set<String>folders_ids=get_folders_ids();
        String point_to,type;

        for (int i = 0; i <repo_details.getMagitFolders().getMagitSingleFolder().size() ; i++) {

            for (int j = 0; j < repo_details.getMagitFolders().getMagitSingleFolder().get(i).getItems().getItem().size(); j++) {
                point_to=repo_details.getMagitFolders().getMagitSingleFolder().get(i).getItems().getItem().get(j).getId();
                type=repo_details.getMagitFolders().getMagitSingleFolder().get(i).getItems().getItem().get(j).getType();

                if(type=="blob"&& !blobs_ids.contains(point_to)){
                    throw new invalid_properties_exception("blob id isnt exist");
                }
                if (type=="folder"&& !folders_ids.contains(point_to)){
                    throw new invalid_properties_exception("folder id isnt exist");
                }
            }
        }
    }

    private Set<String> get_folders_ids() {
        Set<String> res = new HashSet<String>();

        for (int i = 0; i <repo_details.getMagitFolders().getMagitSingleFolder().size() ; i++) {
            res.add(repo_details.getMagitFolders().getMagitSingleFolder().get(i).getId());
        }

        return res;
    }

    private Set<String> get_blobs_ids() {
        Set<String> res = new HashSet<String>();

        for (int i = 0; i <repo_details.getMagitBlobs().getMagitBlob().size() ; i++) {
            res.add(repo_details.getMagitBlobs().getMagitBlob().get(i).getId());
        }

        return res;
    }

    private void unique_id() throws invalid_properties_exception {
        Set<String> exist = new HashSet<String>();

        for (int i = 0; i <repo_details.getMagitBlobs().getMagitBlob().size() ; i++) {
            if (exist.contains(repo_details.getMagitBlobs().getMagitBlob().get(i).getId())){
                throw new invalid_properties_exception("blobs id's must be unique");
            }
            exist.add(repo_details.getMagitBlobs().getMagitBlob().get(i).getId());
        }

        exist.clear();

        for (int i = 0; i <repo_details.getMagitFolders().getMagitSingleFolder().size() ; i++) {
            if (exist.contains(repo_details.getMagitFolders().getMagitSingleFolder().get(i).getId())){
                throw new invalid_properties_exception("folders id's must be unique");
            }
            exist.add(repo_details.getMagitFolders().getMagitSingleFolder().get(i).getId());
        }

        exist.clear();

        for (int i = 0; i <repo_details.getMagitCommits().getMagitSingleCommit().size() ; i++) {
            if (exist.contains(repo_details.getMagitCommits().getMagitSingleCommit().get(i).getId())){
                throw new invalid_properties_exception("commits id's must be unique");
            }
            exist.add(repo_details.getMagitCommits().getMagitSingleCommit().get(i).getId());
        }

    }

    private void extension_valid() throws file_extension_invalid {
        boolean is_xml= file_path.substring(file_path.indexOf('.')+1).equals("xml");
        if(!is_xml){
            throw new file_extension_invalid("Not xml file\n");
        }
    }

    private void file_exist() throws file_not_exist_exception {
        xml_file= new File(file_path);
        if(!xml_file.exists()){
            throw new file_not_exist_exception("File is not exist.\n");
        }
    }

    private MagitRepository get_repository_by_jaxb(String filePath) throws  JAXBException {
        xml_file= new File(filePath);
        //assertFileIsValid();
        try {
            JAXBContext jax = JAXBContext.newInstance(MagitRepository.class);
            Unmarshaller jUnmarsh = jax.createUnmarshaller();
            this.repo_details= (MagitRepository) jUnmarsh.unmarshal((xml_file));
        }
        catch (JAXBException e) {
            throw new JAXBException("Error to parse file" + e.getMessage() +'\n');
        }

        return repo_details;
    }
}