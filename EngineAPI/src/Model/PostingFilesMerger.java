package Model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

public class PostingFilesMerger {

    private String pathForUnunitiedFiles; // a path to where the disk will hold the posting files
    private int postingFilesIndex; //increases each time we create a new posting file
    private int mergedPostingFilesIndex; // increases each time we create a new UNITED posting file
    TreeMap<Integer, LinkedList<String>> lastTreePostingFile;
    private String pathForDocs;


    /**
     * Constructor
     *
     * @param path
     * @param pathForDocs
     */
    public PostingFilesMerger(String path, String pathForDocs) {
        pathForUnunitiedFiles = path;
        postingFilesIndex = 0;
        mergedPostingFilesIndex = 1;
        this.pathForDocs = pathForDocs;

    }


    /**
     * The function's called for when the dictionary had been filled and there are no documents left
     */
    public void uniteToOnePostingFile() throws IOException {
        uniteToOneFinalPostingFile();
    }

    /**
     * this function takes a tree map and writes it to ONE file in a specific directory
     *
     * @throws IOException
     */
    public void writeToDisk(TreeMap<Integer, LinkedList<String>> treeMap, boolean isMerged) throws IOException {

        postingFilesIndex++;
        String filePath = "";
        if (!isMerged) {
            filePath = pathForUnunitiedFiles + "/" + postingFilesIndex + ".txt";
        } else {
            filePath = pathForUnunitiedFiles + "/" + mergedPostingFilesIndex + "_" + ".txt";
            mergedPostingFilesIndex++;
        }
        File file = new File(filePath);
        FileOutputStream fileOutPutStream = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fileOutPutStream, StandardCharsets.UTF_8);
        String line = "";
        for (Map.Entry<Integer, LinkedList<String>> entry : treeMap.entrySet()) {
            LinkedList<String> entryValue = entry.getValue();
            line = convertInfoToString(entryValue) + "\n";
            writer.write(line);
        }
        writer.flush();
        writer.close();

    }

    /**
     * this function takes a linked list that the tree holds and convert it into a string that will be send to the
     * posting file
     *
     * @param info the linked list we want to convert
     * @return
     */
    private StringBuilder convertInfoToString(LinkedList<String> info) {

        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;
        for (String nodeData : info) {
            if (counter == 0) {
                stringBuilder.append(nodeData);
                counter++;
                continue;
            }
            stringBuilder.append(nodeData);
        }
        return stringBuilder;
    }

    /**
     * this function reads the posting file from the disk and creates the tree map
     *
     * @param reader
     * @return the tree map
     * @throws IOException
     */
    private TreeMap<Integer, LinkedList<String>> readFromDisk(BufferedReader reader) throws IOException {
        TreeMap<Integer, LinkedList<String>> newTreeMap = new TreeMap<>();

        String line = reader.readLine();
        while (line != null) {
            //first separate is to get the INDEX of the term in the tree map
            String[] firstSplit = line.split(":");
            String nodeKey = firstSplit[0];
            int key = Integer.parseInt(nodeKey);
            String[] secondSplit = firstSplit[1].split("~");
            LinkedList<String> infoOfTerm = convertInfoToLinkedList(secondSplit);
            String firstNode = infoOfTerm.getFirst();
            firstNode = key + ":" + firstNode;
            infoOfTerm.removeFirst();
            infoOfTerm.addFirst(firstNode);
            newTreeMap.put(key, infoOfTerm);
            line = reader.readLine();
        }
        //reader.close();
        return newTreeMap;
    }


    /**
     * this function takes an array and convert it to a linked list that we will add to the tree
     *
     * @param valuesArray
     * @return
     */
    private LinkedList<String> convertInfoToLinkedList(String[] valuesArray) {
        LinkedList<String> listOfDocs = new LinkedList<>();
        for (int i = 0; i < valuesArray.length; i++) {
            listOfDocs.add(valuesArray[i] + "~");
        }
        return listOfDocs;
    }


    /**
     * The function unites all the posting files in the same dir into one final posting-file
     *
     * @throws IOException
     */
    private void uniteToOneFinalPostingFile() throws IOException {

        File dirPostingFiles = new File(pathForUnunitiedFiles + "/");
        String[] dirPostingArr = dirPostingFiles.list();
        BufferedReader reader = new BufferedReader(new FileReader(pathForUnunitiedFiles + "/" + dirPostingArr[0]));
        //first, build one tree-map so we can unite all the rest into him
        lastTreePostingFile = readFromDisk(reader);
        reader.close();
        HashMap<Integer, LinkedList<String>> tempTreeToUnite;
        //go over each text file, create a tree-map out of him and unite it with the first tree we have
        for (int i = 1; i < dirPostingArr.length; i++) {
            BufferedReader tempReader = new BufferedReader(new FileReader(pathForUnunitiedFiles + "/" + dirPostingArr[i]));
            tempTreeToUnite = readHashFromDisk(tempReader);
            mergeTreeAndHash(tempTreeToUnite);
            tempTreeToUnite.clear();
            tempReader.close();
        }
        for (int i = 0; i < dirPostingArr.length; i++) {
            Files.deleteIfExists(Paths.get(pathForUnunitiedFiles + "/" + dirPostingArr[i]));
        }

        writeUnitedPostingsToDisk(lastTreePostingFile);
        lastTreePostingFile.clear();

    }


    /**
     * The function writes to the disk a new merged posting-file
     *
     * @param treeMap
     * @throws IOException
     */
    private void writeUnitedPostingsToDisk(TreeMap<Integer, LinkedList<String>> treeMap) throws IOException {

        String filePath = pathForUnunitiedFiles + "/" + "finalPosting.txt";
        File file = new File(filePath);
        FileOutputStream fileOutPutStream = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fileOutPutStream, StandardCharsets.UTF_8);
        for (Map.Entry<Integer, LinkedList<String>> entry : treeMap.entrySet()) {
            LinkedList<String> entryValue = entry.getValue();
            String line = convertInfoToString(entryValue) + "\n";
            writer.write(line);
        }
        writer.flush();
        writer.close();

    }


    /**
     * The function reads a temporary posting file and returns a hashMap that holds it
     *
     * @param reader
     * @return
     * @throws IOException
     */
    private HashMap<Integer, LinkedList<String>> readHashFromDisk(BufferedReader reader) throws IOException {
        HashMap<Integer, LinkedList<String>> newHashMap = new HashMap<>();

        String line = reader.readLine();
        while (line != null) {
            //first separate is to get the INDEX of the term in the tree map
            String[] firstSplit = line.split(":");
            String nodeKey = firstSplit[0];
            int key = Integer.parseInt(nodeKey);
            String[] secondSplit = firstSplit[1].split("~");
            LinkedList<String> infoOfTerm = convertInfoToLinkedList(secondSplit);
            String firstNode = infoOfTerm.getFirst();
            firstNode = key + ":" + firstNode;
            infoOfTerm.removeFirst();
            infoOfTerm.addFirst(firstNode);
            newHashMap.put(key, infoOfTerm);
            line = reader.readLine();
        }
        //reader.close();
        return newHashMap;
    }

    /**
     * the function receives a hashmap that holds a temp posting file and unites it with the treemap that holds the united posting files
     *
     * @param secondTree
     */
    private void mergeTreeAndHash(HashMap<Integer, LinkedList<String>> secondTree) {
        //TreeMap<Integer, LinkedList<String>> combinedTree = new TreeMap<>(firstTree);
        for (Integer termName : secondTree.keySet()) {
            if (lastTreePostingFile.containsKey(termName)) {
                //Remove the term's name from the string
                LinkedList<String> convertToArr = secondTree.get(termName);
                String convertedText = convertInfoToString(convertToArr).toString();
                String[] removeTheTerm = convertedText.split(":");
                //split and change the array to a linked list that will be added to the end of the current list
                if (removeTheTerm.length > 1) {
                    String[] arrToTree = removeTheTerm[1].split("~");
                    LinkedList<String> addToNewTree = convertInfoToLinkedList(arrToTree);
                    LinkedList<String> newListForTerm = lastTreePostingFile.get(termName);
                    newListForTerm.addAll(addToNewTree);
                    lastTreePostingFile.replace(termName, newListForTerm);
                }
            } else {
                lastTreePostingFile.put(termName, secondTree.get(termName));
            }
        }
        secondTree.clear();
    }


    /**
     * this function merges all of the documents txt file into one combined file
     *
     * @throws IOException
     */
    public void mergeDocumentsFile() throws IOException {
        File dirDocs = new File(pathForDocs);
        File combinedFile = new File(pathForDocs + "/documents.txt");
        combinedFile.createNewFile();
        String[] dirDocsArr = dirDocs.list();
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(pathForDocs + "/documents.txt"));
        for (int i = 1; i < dirDocsArr.length; i++) {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(pathForDocs + "/" + dirDocsArr[i]));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.append(line);
                bufferedWriter.newLine();
            }

            bufferedReader.close();
            Files.deleteIfExists(Paths.get(pathForDocs + "/" + dirDocsArr[i]));
        }
        bufferedWriter.close();
    }



    /**
     * this function deletes the all of the posting files and the documents files
     */
    /*
    public void resetAllPostingFiles(){
        File dirPostingFiles = new File (this.pathForUnunitiedFiles);
        String [] dirPosingArr = dirPostingFiles.list();
        for (int i=0;i<dirPosingArr.length;i++){
            File subDir = new File (this.pathForUnunitiedFiles+"/"+dirPosingArr[i]);
            String [] subDirArr = subDir.list();
            for (String fileName : subDirArr){
                File deletedFile = new File (this.pathForUnunitiedFiles+"/"+dirPosingArr[i]+"/"+fileName);
                deletedFile.delete();
            }
        }
    }
    */

    /**
     * function that deletes all of the files and folders
     */
    public void resetAllPostingFiles(){
        File withOutStem = new File (this.pathForUnunitiedFiles+"/"+"WithOutStem");
        File withStem = new File (this.pathForUnunitiedFiles+"/"+"WithStem");
        if(withOutStem.exists()){
            handleSubFiles("WithOutStem");
            withOutStem.delete();
        }
        if(withStem.exists()){
            handleSubFiles("WithStem");
            withStem.delete();
        }
    }

    /**
     * this function deletes all of the sub folders and files in a directory
     * @param folder
     */
    private void handleSubFiles(String folder){
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/Dictionary/Dictionary.txt");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/Dictionary");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/Disk1/finalPosting.txt");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/Disk1");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/Disk2/finalPosting.txt");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/Disk2");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/Disk3/finalPosting.txt");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/Disk3");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/Disk4/finalPosting.txt");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/Disk4");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/TermsTF.txt");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/documents/documents.txt");
        deleteFile(this.pathForUnunitiedFiles+"/"+folder+"/documents");

    }

    /**
     * private function that checks if the file exists and deletes it
     * @param path
     */
    private void deleteFile(String path){
        File file = new File (path);
        if(file.exists()){
            file.delete();
        }
    }

}