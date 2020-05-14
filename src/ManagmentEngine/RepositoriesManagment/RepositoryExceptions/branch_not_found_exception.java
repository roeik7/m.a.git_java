package ManagmentEngine.RepositoriesManagment.RepositoryExceptions;

public class branch_not_found_exception  extends Exception {

    public branch_not_found_exception (String message){
        super(message);
    }
}