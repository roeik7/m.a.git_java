package ManagmentEngine.RepositoriesManagment.RepositoryExceptions;

public class file_not_exist_exception extends Exception {
    public file_not_exist_exception (String message){
        super(message);
    }
}