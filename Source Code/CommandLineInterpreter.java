import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class CommandLineInterpreter {
    //This is the second attempt for this assignment

    Terminal t;

    CommandLineInterpreter() {
        t = new Terminal();
    }

    static final String[] possibleCommands = {"echo", "pwd", "cd", "ls", "rmdir",
            "mkdir", "touch", "rm", "cp",
            "cat", "exit"};

    public class Parser {
        String commandName;
        String[] args = new String[0];
        String fileName = "";

        public boolean parse(String command) {
            //get the position of first space
            String actualCommand = command;

            if (command.contains(">")) {
                if (command.indexOf(">") < 1) return false;
                actualCommand = command.substring(0, command.indexOf(">") - 1);
                fileName = command.substring(command.indexOf(">") + 2);
            }

            String[] temp = actualCommand.split(" ");
            if (temp.length > 0) {
                commandName = temp[0];
                args = new String[temp.length - 1];
                for (int i = 1; i < temp.length; i++) args[i - 1] = temp[i];
            }

            //check if that command exists
            for (String element : possibleCommands) {
                if (commandName.equals(element)) {
                    return true;
                }
            }
            return false;
        }

        public String getCommandName() {
            return this.commandName;
        }

        public String[] getArgs() {
            return this.args;
        }
    }


    public class Terminal {

        Parser parser;
        Scanner scanner;
        String currentDir;

        Terminal() {
            parser = new Parser();
            scanner = new Scanner(System.in);
            currentDir = System.getProperty("user.home") + File.separatorChar + "Desktop";
        }


        //helping methods
        public String fileToString(String dir) {
            String res = "";
            try {
                File ogFile = new File(dir);
                Scanner reader = new Scanner(ogFile);
                String tmp;
                while (reader.hasNext()) {
                    tmp = reader.nextLine();
                    res += '\n';
                    res += tmp;
                }
                if (res.length() > 1) res = res.substring(1); //remove the first \n if the file isn't empty
            } catch (IOException e) {
                System.out.println("File Doesn't exist !");
                res = "";
            }
            return res;
        }

        public String pathUpOneLevel(String path) {
            String res = "";
            //get location of the last folder's /
            int s = -1;
            for (int i = 0; i < path.length(); i++) {
                if (path.charAt(i) == File.separatorChar)
                    s = i;
            }
            if (s > 0)
                res = path.substring(0, s);
            else return path;
            return res;
        }

        public String getFullPath(String path) {
            String targetDir = currentDir;
            int s = path.indexOf(File.separatorChar);
            if (s == -1)     //if relative path
            {
                targetDir += File.separatorChar + path;
            } else targetDir = path;

            return targetDir;
        }

        public void writeToFile(String data, String dir) {
            if (dir.equals("")) return;
            dir = getFullPath(dir);
            try {
                File f = new File(dir);
                f.getParentFile().mkdirs();
                f.createNewFile();
                BufferedWriter w = new BufferedWriter(new FileWriter(f));
                w.write(data);
                w.close();
            } catch (IOException e) {
                System.out.println("Writing failed");
            }
        }

        void copyFile(String sourcePath, String destinationPath) {
            try {
                FileInputStream input = new FileInputStream(getFullPath(sourcePath));
                FileOutputStream output = new FileOutputStream(getFullPath(destinationPath));

                int ch;

                while ((ch = input.read()) != -1) {
                    output.write(ch);
                }

                input.close();
                output.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        //Main Terminal Methods
        public String echo(String[] args) {
            System.out.println(args[0]);
            return args[0];
        }

        public String pwd() {
            System.out.println(currentDir);
            return currentDir;
        }

        public String cat(String[] args) {
            args[0] = getFullPath(args[0]);

            String f = fileToString(args[0]);
            if (args.length == 2) {
                args[1] = getFullPath(args[1]);
                f += ('\n' + fileToString(args[1]));
            }

            System.out.println(f);
            return f;
        }

        public String cd(String[] args) {

            // we concat all array elements into the first element
            if (args.length > 1) {
                String path = "";
                for (int i = 0; i < args.length; i++) path += " " + args[i];
                path = path.substring(1);

                args = new String[1];
                args[0] = path;
            }

            if (args.length == 0)          //no args
                currentDir = System.getProperty("user.home");
            else if (args[0].equals(".."))      // this implies that args.length == 1
                currentDir = pathUpOneLevel(currentDir);
            else if (Files.exists(Path.of(args[0])))       //full path
                currentDir = args[0];
            else if (Files.exists(Path.of(currentDir + File.separator + args[0])))     //relative path
                currentDir += File.separator + args[0];
            else System.out.println("That Directory does not exist!");

            return currentDir;
        }

        public String mkdir(String[] args) {
            String argsConcatenated = "";
            for (int i = 0; i < args.length; i++) argsConcatenated += args[i] + " ";

            String[] temp = argsConcatenated.split("\"");
            ArrayList<String> filePaths = new ArrayList<String>();

            for (int i = 0; i < temp.length; i++) {
                if (i % 2 == 0) {
                    String[] spaceSeparated = temp[i].split(" ");
                    filePaths.addAll(Arrays.asList(spaceSeparated));
                } else {
                    filePaths.add(temp[i]);
                }
            }


            for (int i = 0; i < filePaths.size(); i++) {
                String targetDir = currentDir;
                if (filePaths.get(i).length() == 0 || filePaths.get(i).charAt(0) == ' ') continue;
                // if we enter an empty string, it creates a bad directory so we avoid it

                int s = filePaths.get(i).lastIndexOf(File.separatorChar);
                if (s == -1)     //if relative path
                {
                    targetDir += File.separatorChar + filePaths.get(i);
                } else targetDir = filePaths.get(i); //full path
                File f = new File(targetDir);

                if (f.exists()) {
                    System.out.println("Directory\' " + targetDir + " already exists!");
                } else if (!f.mkdirs())
                    System.out.println("Bad Directory");
                else
                    System.out.println("Directory created successfully");

            }
            return "";
        }

        public String ls(String[] args) {

            if (args.length > 0 && args[0].equals("-r")) {
                return lsr();
            }

            File file = new File(currentDir);
            String[] names = file.list();
            Arrays.sort(names);

            String s = "";
            for (String name : names) {
                System.out.println(name);
                s += name + '\n';
            }

            return s;
        }

        public String lsr() {
            File file = new File(currentDir);
            String[] names = file.list();
            Arrays.sort(names);
            int l = names.length;
            for (int i = 0; i < l / 2; i++) {
                String t = names[i];
                names[i] = names[l - 1 - i];
                names[l - 1 - i] = t;
            }

            String s = "";
            for (String name : names) {
                System.out.println(name);
                s += name + '\n';
            }

            return s;
        }

        public String rmdir(String args[]) {
            if (args.length == 0) return "";
            if (args[0].equals("*")) {
                File dir = new File(currentDir);
                File[] files = dir.listFiles();

                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) {
                            if (f.list().length == 0) f.delete();
                        }
                    }
                }
            } else { // removing a certain directory only if it's empty
                String targetDir = currentDir;

                if (Files.exists(Path.of(args[0]))) {     //full path
                    targetDir = currentDir;
                } else if (Files.exists(Path.of(currentDir + File.separator + args[0]))) // relative path
                {
                    targetDir = currentDir + File.separator + args[0];
                } else {
                    System.out.println("Folder does not exist.");
                    return "";
                }

                File file = new File(targetDir);
                if (file.isDirectory()) {
                    if (file.list().length == 0) file.delete();
                    else System.out.println("Folder not empty.");
                }
            }
            return "";
        }

        public String touch(String args[]) {
            //different approach to reading full and relative paths
            String path = getFullPath(args[0]);

            File f = new File(path);

            if (f.exists()) return path;
            try {
                f.createNewFile();
                return path;
            } catch (IOException e) {

                e.printStackTrace();
                return "";
            }
        }

        public String cp(String args[]) {

            int firstInd = 0, secondInd = 1;
            File first = new File(getFullPath(args[firstInd]));
            File second = new File(getFullPath(args[secondInd]));


            if (!first.exists()) {
                System.out.println("First file does not exist");
                return "";
            }
            if (!second.exists()) {
                System.out.println("Second file does not exist");
                return "";
            }
            if (!first.isFile() || !second.isFile()) {
                System.out.println("Not a file!");
                return "";
            }

            copyFile(args[0], args[1]);
            return "";
        }

        public String rm(String args[]) {

            for (int i = 0; i < args.length; i++) {
                File file = new File(getFullPath(args[i]));
                // Checking if file exists and if it is a file or not
                if (file.exists() && file.isFile()) {
                    // Deleting the file
                    file.delete();
                    System.out.println(file.getName() + " deleted successfully");
                } else {
                    System.out.println("no such file!");
                }
            }
            return "";

        }

        //add new function to the if chain at the bottom
        public void chooseCommandAction() {
            System.out.print(">> " + System.getProperty("user.name") + "@ " + currentDir + " :");
            String line = scanner.nextLine();
            if (!parser.parse(line)) {
                System.out.println("The command: \"" + parser.getCommandName() + '\"' + " is not recognised");
                return;
            }
            String comm = parser.getCommandName();
            String fileName = parser.fileName;


            if (comm.equals("echo"))
                writeToFile(echo(parser.getArgs()), fileName);
            else if (comm.equals("cat"))
                writeToFile(cat(parser.getArgs()), fileName);
            else if (comm.equals("pwd"))
                writeToFile(pwd(), fileName);
            else if (comm.equals("cd"))
                writeToFile(cd(parser.getArgs()), fileName);
            else if (comm.equals("mkdir"))
                mkdir(parser.getArgs());
            else if (comm.equals("ls")) {
                writeToFile(ls(parser.getArgs()), fileName);
            } else if (comm.equals("rmdir"))
                rmdir(parser.getArgs());
            else if (comm.equals("touch"))
                touch(parser.getArgs());
            else if (comm.equals("cp")) {
                cp(parser.getArgs());
            } else if (comm.equals("rm")) {
                rm(parser.getArgs());
            } else if (comm.equals("exit"))
                System.exit(0);


        }


    }

    public static void main(String args[]) {
        CommandLineInterpreter main = new CommandLineInterpreter();

        while (true)
            main.t.chooseCommandAction();
    }
}
