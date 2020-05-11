package UI;

import ManagmentEngine.RepositoriesManagment.repository_manager;
import ManagmentEngine.XML_Managment.xml_details;

import java.util.Scanner;

public class Main {

    public static void main(String[] args){
        repository_manager x = new repository_manager();
        xml_details xml = new xml_details();
        xml.xml_is_valid("C:\\Users\\roik\\Desktop\\ex1-medium.xml");
        x.initalize_repository(xml, false);
        int r=4;
        //x.create_magit_file("C:\\Users\\roik\\Desktop");

        //execute_operations();
    }


    private static void execute_operations() {
        repository_manager managment_engine = new repository_manager();
        String username="Administrator"; //default
        int user_choice=0;

        while(user_choice!=11){
            user_choice=show_manu();
            handle_user_choice(user_choice, username, managment_engine);
        }


    }

    private static void handle_user_choice(int user_choice, String usermame, repository_manager managment_engine) {
        Scanner stdin = new Scanner(System.in);
        String input;
        boolean file_loaded_successfuly=false;
        String file_path;

        switch (user_choice){
            case 1:
                handle_update_user_name(usermame);
                break;
            case 2:
                handle_load_xml(file_loaded_successfuly, managment_engine);
                break;
//            case 3:
//                handle_load_xml(file_loaded_successfuly);
//                break;
            case 4:
                //handle_commit_history();


        }
    }

    private static void handle_update_user_name(String usermame) {
        Scanner stdin = new Scanner(System.in);
        System.out.println("Please enter your new username: \n");
        usermame=stdin.nextLine();
        System.out.println("Hey "+usermame +"\nYour username updated successfully.\n");
    }

    private static void handle_load_xml(boolean file_loaded_successfully, repository_manager managment_engine) {
        Scanner stdin = new Scanner(System.in);
        xml_details xml = new xml_details();
        System.out.println("Please enter full path to your file: \n");
        String file_path=stdin.nextLine();
        file_loaded_successfully = xml.xml_is_valid(file_path);
        boolean manage_existing_repo=false;

        if(!file_loaded_successfully){
            System.out.println("\nPlease insert another file path.\n");

        }
        else{
            System.out.println("\nThe file uploaded successfully.\n");
            if(managment_engine.repository_exist(xml.get_location()+"\\.magit")){
                System.out.println("There is already repository at this path.\n"+

                        "1. Delete existing repository \n" +
                        "2. Manage existing repository.\n");
                int to_delete = stdin.nextInt();

                if(to_delete==1){
                    String message = managment_engine.delete_repository(xml.get_location());
                    if(!message.equals("Success")){
                        System.out.println(message);
                    }

                    else{
                        System.out.println("Repository cleaned successfully\n");
                    }
                }

                else{
                    manage_existing_repo=true;
                }


            }

            managment_engine.initalize_repository(xml, manage_existing_repo);

        }
    }

    private static int show_manu() {

        Scanner stdin = new Scanner(System.in);
        int choice;
        System.out.println("Please enter your choice:\n" +
                "1.  Update username.\n" +
                "2.  Load repository.\n" +
                "3.  Switch repository.\n"+
                "4.  Show all history related to current commit.\n" +
                "5.  Check status (working copy area).\n" +
                "6.  Show all branches.\n" +
                "7.  Create new branch.\n" +
                "8.  Remove branch.\n" +
                "9.  Checkout.\n" +
                "10. Show history of active branch.\n" +
                "11. Exit.\n");

        choice = stdin.nextInt();

        return choice;
    }
}
