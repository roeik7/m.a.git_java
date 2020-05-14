package UI;

import ManagmentEngine.RepositoriesManagment.RepositoryExceptions.failed_to_create_local_structure_exception;
import ManagmentEngine.RepositoriesManagment.repository_manager;
import ManagmentEngine.XML_Managment.xml_details;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws ParseException, NoSuchAlgorithmException, IOException, failed_to_create_local_structure_exception {
        execute_operations();
    }

    private static void execute_operations() {
        boolean exit = false;
        repository_manager managment_engine = new repository_manager();
        String username="Administrator"; //default
        int user_choice=0;

        while(!exit){
            user_choice=show_manu();
            handle_user_choice(exit, user_choice, username, managment_engine);
        }


    }

    private static void handle_user_choice(boolean exit, int user_choice, String usermame, repository_manager managment_engine) {
        switch (user_choice){
            case 1:
                handle_update_user_name(usermame);
                break;
            case 2:
                handle_load_xml(managment_engine);
                break;
            case 3:
                handle_switch_repository(managment_engine);
                break;
            case 4:
                handle_commit_history(managment_engine);
                break;
            case 5:
                show_working_stage_area(managment_engine);
            case 6:
                show_all_branches(managment_engine);
                break;
            case 7:
                create_new_branch(managment_engine);
                break;
            case 8:
                remove_branch(managment_engine);
                break;
            case 9:
                handle_checkout(managment_engine);
                break;
            case 10:
                handle_branch_history(managment_engine);
                break;
            case 11:
                exit=true;
                break;
            default:
                System.out.println("Invalid choice\nEnter number between 1-11");
                break;


        }
    }

    private static void handle_branch_history(repository_manager managment_engine) {
        try {
            System.out.println(managment_engine.active_brance_history());
        } catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.no_active_repository_exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void handle_checkout(repository_manager managment_engine) {
        System.out.println("Please insert name for the nre branch: ");
        Scanner stdin = new Scanner(System.in);
        String branch_name = stdin.hasNext()? stdin.nextLine() : "";
        if(branch_name.contains(" ")){
            System.out.println("Branch name must be witout spaces");
        }
        else{
            try {
                managment_engine.checkout(branch_name);
            } catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.head_branch_deletion_exception e) {
                System.out.println(e.getMessage());
            } catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.branch_not_found_exception e1) {
                System.out.println(e1.getMessage());
            }
        }
    }

    private static void remove_branch(repository_manager managment_engine) {
        try {
            System.out.println("Please insert name for the nre branch: ");
            Scanner stdin = new Scanner(System.in);
            String branch_name = stdin.hasNext()? stdin.nextLine() : "";
            if(branch_name.contains(" ")||branch_name.contains("")){
                System.out.println("Branch name must be witout spaces");
            }
            else{
                managment_engine.delete_branch(branch_name);
            }
        }
        catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.no_active_repository_exception e) {
            System.out.println(e.getMessage());
        } catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.illegal_branch_deletion_exception e1) {
            System.out.println(e1.getMessage());
        }
    }

    private static void create_new_branch(repository_manager managment_engine) {

        try {

            System.out.println("Please insert name for the nre branch: ");
            Scanner stdin = new Scanner(System.in);
            String branch_name = stdin.hasNext()? stdin.nextLine() : " ";
            if(branch_name.contains(" ")||branch_name.contains("")){
                System.out.println("Branch name must be witout spaces");
            }
            else{
                managment_engine.add_new_branch(branch_name);
            }
        }
        catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.no_active_repository_exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void show_all_branches(repository_manager managment_engine) {
        try {
            ArrayList<String> all_branche = managment_engine.get_branches();
            for (int i = 0; i <all_branche.size() ; i++) {
                System.out.println(all_branche.get(i));
            }
        } catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.no_active_repository_exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void show_working_stage_area(repository_manager managment_engine) {
        String res [] = managment_engine.working_copy_area_status();

        if(res!=null) {
            System.out.println("Modified:\n" + res[0] + "\n" +
                    "Removed/Renamed:\n" + res[1] + "\n" +
                    "New files added:\n" + res[2]);
            return;
        }
        System.out.println("Working stage area clean. nothing has been changed.");

    }

    private static void handle_commit_history(repository_manager managment_engine) {
        String tree_details ;
        try {
            tree_details = managment_engine.commit_history();
            System.out.println(tree_details);
        } catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.no_active_repository_exception e) {
            System.out.println(e.getMessage());
        } catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.file_not_exist_exception file_not_exist_exception) {
            file_not_exist_exception.printStackTrace();
        }
    }

    private static void handle_switch_repository(repository_manager managment_engine) {
        System.out.println("Please choose:\n"+
                           "1. Handle existing repository.\n" +
                           "2. Initliaze new repoistory with xml."
                );
        Scanner stdin = new Scanner(System.in);
        int user_choice= stdin.hasNextInt()? stdin.nextInt() : 0;
        if(user_choice!=0){
            if(user_choice==1){
                System.out.println("Enter repository name:\n");
                String repo_name = stdin.nextLine();
                if(managment_engine.repository_exist(repo_name)){
                    try {
                        managment_engine.initialize_old_repository(repo_name);
                    } catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.repository_not_found_exception repository_not_found_exception) {
                        System.out.println(repository_not_found_exception.getMessage());
                    }
                }
            }
            else{
                handle_load_xml(managment_engine);
            }
        }
    }

    private static void handle_update_user_name(String usermame) {
        Scanner stdin = new Scanner(System.in);
        System.out.println("Please enter your new username: \n");
        usermame=stdin.hasNext()? stdin.nextLine() : "";
        if(usermame.contains("")||usermame.contains(" ")){
            System.out.println("Username must be string without spaces");
        }
        else{
            System.out.println("Hey "+usermame +"\nYour username updated successfully.\n");
        }
    }

    private static void handle_load_xml(repository_manager managment_engine) {
        Scanner stdin = new Scanner(System.in);
        xml_details xml = new xml_details();
        System.out.println("Please enter full path to your file: \n");
        String file_path = stdin.nextLine();
        boolean file_loaded_successfully = xml.xml_is_valid(file_path);
        boolean manage_existing_repo = false;

        if (!file_loaded_successfully) {
            System.out.println("\nFailed to load the xml file.\nPlease insert another file path.");

        } else {
            System.out.println("\nThe file uploaded successfully.");

            //in case there is already repository there (.magit folder exist) - delete or go with it
            if (managment_engine.repository_contain_magit(xml.get_location() + "\\.magit")) {
                System.out.println("There is already repository at this path.\n" +
                        "Choose:\n" +
                        "1. Delete existing repository \n" +
                        "2. Manage existing repository.\n");
                int user_choice = stdin.nextInt();

                if (user_choice == 1) {
                    try {
                        managment_engine.delete_repository(xml.get_location());
                    } catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.failed_to_delete_repository_exception failed_to_delete_repository_exception) {
                        System.out.println(failed_to_delete_repository_exception.getMessage());
                    }
                    System.out.println("The repository deleted successfully.\nGoing to initialize your repository from xml...");
                }
                else {
                    manage_existing_repo = true;
                }
            }
        }

        try {
            managment_engine.initalize_repository(xml, manage_existing_repo);
        } catch (ManagmentEngine.RepositoriesManagment.RepositoryExceptions.failed_to_create_local_structure_exception failed_to_create_local_structure_exception) {
            System.out.println(failed_to_create_local_structure_exception.getMessage());
        }
    }

    private static int show_manu() {

        Scanner stdin = new Scanner(System.in);
        int choice=0;

        System.out.println("Please enter your choice:\n" +
                "1.  Update username.\n" +
                "2.  Load repository.\n" +
                "3.  Switch repository.\n"+
                "4.  Show all history related to current commit.\n" +
                "5.  Check status (working stage area).\n" +
                "6.  Show all branches.\n" +
                "7.  Create new branch.\n" +
                "8.  Remove branch.\n" +
                "9.  Checkout.\n" +
                "10. Show history of active branch.\n" +
                "11. Exit.\n");
        if (stdin.hasNextInt()){
            choice = stdin.nextInt();
        }

        return choice;
    }
}
