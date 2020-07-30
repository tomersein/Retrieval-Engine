package Model;

import javafx.util.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Indexer extends Thread {

    private HashMap<String, Pair<Integer, Integer>> mainDictionary; //the main Dictionary
    private HashSet<String> allEntitiesInDictionary; //holds supposedly entities in the dictionary
    private String path;
    private int indexForMainDictionary; //will continue increasing the more letters we add to the main dictionary, serves as point to the treemap

    //We decided to divide our posting file into 4 different files, each one holds words in a specific range of letters (a-f, g-m and etc)
    private TreeMap<Integer, LinkedList<String>> firstTreeToBePosted; //tree of the first posting file
    private TreeMap<Integer, LinkedList<String>> secondTreeToBePosted; //tree of the second posting file
    private TreeMap<Integer, LinkedList<String>> thirdTreeToBePosted; //tree of the third posting file
    private TreeMap<Integer, LinkedList<String>> fourthTreeToBePosted; //tree of the fourth posting file


    private PostingFilesMerger firstDisk; //the disk that holds the first posting file
    private PostingFilesMerger secondDisk; //the disk that holds the second posting file
    private PostingFilesMerger thirdDisk; //the disk that holds the third posting file
    private PostingFilesMerger fourthDisk;  //the disk that holds the fourth posting file


    private Stemmer stemmer;
    private boolean isStemming;
    private String pathForDocuments;
    private HashMap<String, Integer> termsTF;

    /**
     * Constructor
     */
    public Indexer(String path, String pathForDocuments, boolean isStemming) {
        mainDictionary = new HashMap<>();
        firstTreeToBePosted = new TreeMap<>();
        secondTreeToBePosted = new TreeMap<>();
        thirdTreeToBePosted = new TreeMap<>();
        fourthTreeToBePosted = new TreeMap<>();
        this.path = path;
        this.pathForDocuments = pathForDocuments;
        indexForMainDictionary = 1;
        this.isStemming = isStemming;
        createFolders();
        firstDisk = new PostingFilesMerger(path + "Disk1", path + "/documents");
        secondDisk = new PostingFilesMerger(path + "Disk2", path + "/documents");
        thirdDisk = new PostingFilesMerger(path + "Disk3", path + "/documents");
        fourthDisk = new PostingFilesMerger(path + "Disk4", path + "/documents");

        allEntitiesInDictionary = new HashSet<>();
        stemmer = new Stemmer();
        termsTF = new HashMap<>();

    }

    /**
     * The function creates a directory for each disk
     */
    private void createFolders() {
        for (int i = 1; i <= 4; i++) {
            new File(path + "/Disk" + i).mkdir();
        }
    }

    /**
     * Once this function is called for it means there are no more words to add to the dictionary and the posting files
     *
     * @throws IOException
     */
    public int finishInvertedIndexer() throws IOException {

        clearWrongEntities();
        int sum = mainDictionary.size();
        allEntitiesInDictionary.clear();
        allEntitiesInDictionary = null;
        writeDictionaryToDisk();
        mainDictionary.clear();
        mainDictionary = null;
        writeTermsTfToDisk();
        termsTF.clear();
        termsTF = null;

        if (firstTreeToBePosted.size() > 0) {
            firstDisk.writeToDisk(firstTreeToBePosted, false);
            firstTreeToBePosted = null;
        }
        if (secondTreeToBePosted.size() > 0) {
            secondDisk.writeToDisk(secondTreeToBePosted, false);
            secondTreeToBePosted = null;
        }
        if (thirdTreeToBePosted.size() > 0) {
            thirdDisk.writeToDisk(thirdTreeToBePosted, false);
            thirdTreeToBePosted = null;
        }
        if (fourthTreeToBePosted.size() > 0) {
            fourthDisk.writeToDisk(fourthTreeToBePosted, false);
        }
        fourthTreeToBePosted = null;

        //unite all of the text files in each disk into one, leaving only 4 posting files in the end of the program
        firstDisk.uniteToOnePostingFile();
        firstDisk = null;
        secondDisk.uniteToOnePostingFile();
        secondDisk = null;
        thirdDisk.uniteToOnePostingFile();
        thirdDisk = null;
        fourthDisk.uniteToOnePostingFile();

        fourthDisk.mergeDocumentsFile();

        readDictionaryFromDisk();
        fourthDisk = null;
        return sum;

    }


    /**
     * writes the dictionary to the disk to allow more memory for uniting temp posting files
     */
    public void writeDictionaryToDisk() throws IOException {
        String pathOfDictionary = this.path + "/Dictionary";
        File dirFile = new File(pathOfDictionary);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        String filePath = pathOfDictionary + "/" + "Dictionary.txt";
        File file = new File(filePath);
        FileOutputStream outputStream = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        StringBuilder tempLine = new StringBuilder();
        for (Map.Entry<String, Pair<Integer, Integer>> entry : mainDictionary.entrySet()) {
            tempLine.append(entry.getKey().replaceAll("\n", "") + "™" + entry.getValue().getKey() + "™" + entry.getValue().getValue());
            writer.write(tempLine.toString());
            writer.write("\n");
            tempLine.setLength(0);
        }
        writer.flush();
        writer.close();
    }


    /**
     * the function reads the dictionary from the disk and writes it back to the mainDictionary varaible
     */
    private void readDictionaryFromDisk() throws IOException {
        String parhOfDictionary = this.path + "/Dictionary/Dictionary.txt";
        File file = new File(parhOfDictionary);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String line = null;
        if (mainDictionary == null) {
            mainDictionary = new HashMap<>();
        }
        while ((line = bufferedReader.readLine()) != null) {
            String[] firstSeparate = line.split("™");
            String keyDic = firstSeparate[0];
            int keyPair = Integer.parseInt(firstSeparate[1]);
            int valuePair = Integer.parseInt(firstSeparate[2]);
            Pair<Integer, Integer> tempPair = new Pair<>(keyPair, valuePair);
            mainDictionary.put(keyDic, tempPair);
        }
        bufferedReader.close();
    }


    /**
     * The function receives the parser's output and sends it to a private function to add the parser's information to the dictionary and the posting files
     *
     * @param allWords
     */
    public void buildInvertedIndexer(HashMap<String, LinkedList<Term>> allWords) throws IOException {
        fillIndexer(allWords);
    }


    /**
     * The function receives map that holds the word and her matching linklist of terms, converts it to a map that holds the word with matching list of information
     * and sends to be filled in the dictionary and posting files
     *
     * @param allWords
     */
    private void fillIndexer(HashMap<String, LinkedList<Term>> allWords) throws IOException {

        HashMap<String, LinkedList<String>> wordPostFileToUpload = new HashMap<>();
        for (Map.Entry<String, LinkedList<Term>> entry : allWords.entrySet()) {
            String word = entry.getKey();
            //if stemming is on, stem the words
            if (isStemming) {
                word = stemString(entry.getValue().get(0));
            }
            LinkedList<Term> listOfTermsInDocs = entry.getValue();

            if (listOfTermsInDocs.getFirst().getEntity() == 1) {
                allEntitiesInDictionary.add(word);
            }

            //first check if the map doesn't contain the word
            if (!wordPostFileToUpload.containsKey(word)) {
                wordPostFileToUpload.put(word, buildPostingForWord(listOfTermsInDocs));
            } else {
                // if it does, merge the two lists into one and add back to the map
                LinkedList<String> listToMerge = buildPostingForWord(listOfTermsInDocs);
                LinkedList<String> listAtDictionary = wordPostFileToUpload.get(word);
                listAtDictionary.addAll(listToMerge);
                wordPostFileToUpload.remove(word);
                wordPostFileToUpload.put(word, listAtDictionary);
            }
        }
        updateMainDictionaryAndFillPostingTree(wordPostFileToUpload);
    }

    /**
     * the function receives the temporary hashmap that holds the words and their information, adds the words to the dictionary
     * and sends the information to the treemap to be later on added to a posting-file
     *
     * @param wordsForDictionary
     */
    private void updateMainDictionaryAndFillPostingTree(HashMap<String, LinkedList<String>> wordsForDictionary) throws IOException {

        for (Map.Entry<String, LinkedList<String>> entry : wordsForDictionary.entrySet()) {

            if (mainDictionary.containsKey(entry.getKey())) {
                updateTF(entry.getKey().toLowerCase(), entry.getValue());
                Pair<Integer, Integer> oldValueOfKey = mainDictionary.get(entry.getKey());
                Pair<Integer, Integer> newValueForKey = new Pair<>(oldValueOfKey.getKey() + entry.getValue().size(), oldValueOfKey.getValue());
                mainDictionary.remove(entry.getKey());
                mainDictionary.put(entry.getKey(), newValueForKey);
                chooseTreeToFill(entry.getKey(), oldValueOfKey.getValue(), entry.getValue());
                continue;

            } else if (Character.isLowerCase(entry.getKey().charAt(0))) {
                //if the dictionary doesn't contain the word, first check if our word is in small letters and if the dictionary contains the word with capital letter
                String tempWord = Character.toUpperCase(entry.getKey().charAt(0)) + entry.getKey().substring(1);
                if (mainDictionary.containsKey(tempWord)) {
                    Pair<Integer, Integer> oldValueOfKey = mainDictionary.get(tempWord);
                    Pair<Integer, Integer> newValueForKey = new Pair<>(oldValueOfKey.getKey() + entry.getValue().size(), oldValueOfKey.getValue());
                    mainDictionary.remove(tempWord);
                    mainDictionary.put(entry.getKey(), newValueForKey);
                    updateTF(entry.getKey().toLowerCase(), entry.getValue());
                    chooseTreeToFill(entry.getKey(), oldValueOfKey.getValue(), entry.getValue());
                    continue;
                }

            } else if (Character.isUpperCase(entry.getKey().charAt(0))) {
                // if we reached here, our word starts with capital, therefore we check if the dictionary holds her with small letters
                String tempWord = entry.getKey().toLowerCase();
                if (mainDictionary.containsKey(tempWord)) {
                    updateTF(tempWord.toLowerCase(), entry.getValue());
                    Pair<Integer, Integer> oldValueOfKey = mainDictionary.get(tempWord);
                    Pair<Integer, Integer> newValueForKey = new Pair<>(oldValueOfKey.getKey() + entry.getValue().size(), oldValueOfKey.getValue());
                    mainDictionary.remove(tempWord);
                    mainDictionary.put(tempWord, newValueForKey);
                    chooseTreeToFill(entry.getKey(), oldValueOfKey.getValue(), entry.getValue());
                    continue;
                }
            }

            //if we reached here it means the word is not in the dictionary at all, and we simply place it as a newly entry
            //if we reached here then the word doesn't exist in the dictionary and we can add her
            Pair<Integer, Integer> pairForNewEntryInDictionary = new Pair<>(entry.getValue().size(), indexForMainDictionary);
            indexForMainDictionary++;
            mainDictionary.put(entry.getKey(), pairForNewEntryInDictionary);
            updateTF(entry.getKey().toLowerCase(), entry.getValue());
            chooseTreeToFill(entry.getKey(), indexForMainDictionary - 1, entry.getValue());

        }
    }


    /**
     * The function receives a word and its dictionary's data and decides in which treemap to write it by its beginning letter
     *
     * @param word
     * @param index
     * @param data
     */
    private void chooseTreeToFill(String word, int index, LinkedList<String> data) throws IOException {

        char c = word.charAt(0);
        if ((c >= 'r' && c <= 'z') || (c >= 'R' && c <= 'Z')) {
            fillPostingTree(index, data, fourthTreeToBePosted);
            if (fourthTreeToBePosted.size() > 22000) {
                fourthDisk.writeToDisk(fourthTreeToBePosted, false);
                fourthTreeToBePosted.clear();
            }
        } else if ((c >= 'd' && c <= 'k') || (c >= 'D' && c <= 'K')) {
            fillPostingTree(index, data, secondTreeToBePosted);
            if (secondTreeToBePosted.size() > 22000) {
                secondDisk.writeToDisk(secondTreeToBePosted, false);
                secondTreeToBePosted.clear();
            }
        } else if ((c >= 'l' && c <= 'q') || (c >= 'L' && c <= 'Q')) {
            fillPostingTree(index, data, thirdTreeToBePosted);
            if (thirdTreeToBePosted.size() > 22000) {
                thirdDisk.writeToDisk(thirdTreeToBePosted, false);
                thirdTreeToBePosted.clear();
            }
        } else {
            fillPostingTree(index, data, firstTreeToBePosted);
            if (firstTreeToBePosted.size() > 22000) {
                firstDisk.writeToDisk(firstTreeToBePosted, false);
                firstTreeToBePosted.clear();
            }
        }
    }


    /**
     * The function receives an index and data to be uploaded to the tree, if the tree already contains the index it will unite its data with the newly received data,
     * otherwise creates new node within the tree
     *
     * @param index
     * @param data
     */
    private void fillPostingTree(int index, LinkedList<String> data, TreeMap<Integer, LinkedList<String>> treeToBePosted) {
        //first, check if the tree exists, if not, re-create it
        //check if the tree contains the index first
        if (treeToBePosted.containsKey(index)) {
            LinkedList<String> dataInTree = treeToBePosted.get(index);
            dataInTree.addAll(data);
            treeToBePosted.replace(index, dataInTree);
        } else {
            String firstNode = index + ":";
            data.addFirst(firstNode);
            treeToBePosted.put(index, data);
        }
    }


    /**
     * The function receives a linkedlist holding different appearances of a word in different documents, creates a list of strings that holds re-arranged information about the word's appearance in the documents and returns it
     *
     * @param termList
     * @return docTitles
     */
    private LinkedList<String> buildPostingForWord(LinkedList<Term> termList) {
        String[] docsAndInfo = new String[termList.size()];
        int index = 0;
        for (Term T : termList) {
            String tempDocInfo = T.getDoc() + "," + T.getTf() + "," + T.getHeader() + "," + T.getEntity() + "," + T.getRelativeLocation() + "~";
            docsAndInfo[index] = tempDocInfo;
            index++;
        }
        return new LinkedList<>(Arrays.asList(docsAndInfo));
    }

    /**
     * this is the function which stems
     *
     * @param term
     * @return
     */
    public String stemString(Term term) {
        String word = term.getTermWord();
        if (term.getTermStarter()) {
            word = word.toLowerCase();
        }
        char[] charsArr = word.toCharArray();
        stemmer.add(charsArr, word.length());
        stemmer.stem();
        if (term.getTermStarter()) {
            return stemmer.toString().toUpperCase();
        }
        return stemmer.toString();
    }

    /**
     * sends to the VIEW the tree map it will print to the Screen
     *
     * @return a tree map of the dictionary
     */
    public LinkedList<String> showDictionary() throws IOException {
        if (termsTF == null) {
            termsTF = readTermsTfFromDisk();
        }
        TreeMap<String, Integer> tempDictionary = new TreeMap<>();
        for (HashMap.Entry<String, Integer> entry : termsTF.entrySet()) {
            tempDictionary.put(entry.getKey(), entry.getValue());
        }
        LinkedList<String> toDisplay = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : tempDictionary.entrySet()) {
            toDisplay.add(entry.getKey() + ":" + entry.getValue());
        }
        return toDisplay;
    }


    /**
     * keeps a record of the TF of each term in the dictionary
     *
     * @param term the term that we are checking
     * @param docs the string of the docs the term
     */
    private void updateTF(String term, LinkedList<String> docs) {
        for (String doc : docs) {
            String[] splitedArr = doc.split(",");
            String stringTF = splitedArr[1]; //first spot in the array holds the TF in the doc's description
            int integerTF = Integer.parseInt(stringTF);
            if (!termsTF.containsKey(term)) {
                termsTF.put(term, integerTF);
            } else {
                int oldValue = termsTF.get(term);
                int newValue = oldValue + integerTF;
                termsTF.remove(term);
                termsTF.put(term, newValue);
            }
        }
    }

    /**
     * this function writes the terms-TF to the disk
     *
     * @throws IOException
     */
    public void writeTermsTfToDisk() throws IOException {
        PrintWriter printWriter = new PrintWriter(path + "/TermsTF.txt", "UTF-8");
        for (HashMap.Entry<String, Integer> entry : termsTF.entrySet()) {
            printWriter.println(entry.getKey() + "™" + entry.getValue().toString());
        }
        printWriter.flush();
        printWriter.close();

    }


    /**
     * The function reads the terms-TF from the disk
     *
     * @return
     * @throws IOException
     */
    public HashMap<String, Integer> readTermsTfFromDisk() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path + "/TermsTF.txt"));
        HashMap<String, Integer> tempTF = new HashMap<>();
        String line = "";
        while ((line = reader.readLine()) != null) {
            String[] firstSeparate = line.split("™");
            tempTF.put(firstSeparate[0], Integer.parseInt(firstSeparate[1]));
        }
        reader.close();
        return tempTF;
    }

    /**
     * The function removes records from the dictionary that aren't real entities
     */
    private void clearWrongEntities() {
        HashSet<String> tempEntities = new HashSet<>(allEntitiesInDictionary);
        for (String word : tempEntities) {
            if (!mainDictionary.containsKey(word)) {
                allEntitiesInDictionary.remove(word);
                continue;
            }
            if (mainDictionary.get(word).getKey() <= 1) {
                mainDictionary.remove(word);
                allEntitiesInDictionary.remove(word);
                termsTF.remove(word.toLowerCase());
            }
        }
    }


    /**
     * this function clears the data from this object
     */
    public void clear() {
        mainDictionary.clear();
        allEntitiesInDictionary.clear();
        firstTreeToBePosted.clear();
        secondTreeToBePosted.clear();
        thirdTreeToBePosted.clear();
        fourthTreeToBePosted.clear();
        termsTF.clear();
    }

    //-----------NEW FUNCTIONS FOR PART B--------------

    public LinkedList<String> findTermInDictionary(String term) throws IOException {

        String termWord = term;
        char firstTermChar;
        if (mainDictionary.containsKey(termWord)) {

            firstTermChar = termWord.charAt(0);
            return getDataDocsFromPosting(firstTermChar, mainDictionary.get(termWord).getValue());

        } else if (mainDictionary.containsKey(Character.toLowerCase(termWord.charAt(0)) + termWord.substring(1))) {

            firstTermChar = Character.toLowerCase(termWord.charAt(0));
            termWord = firstTermChar + termWord.substring(1);
            return getDataDocsFromPosting(firstTermChar, mainDictionary.get(termWord).getValue());

        } else {
            termWord = Character.toUpperCase(termWord.charAt(0)) + termWord.substring(1);
            if (mainDictionary.containsKey(termWord)) {

                firstTermChar = Character.toUpperCase(termWord.charAt(0));
                return getDataDocsFromPosting(firstTermChar, mainDictionary.get(termWord).getValue());

            } else {
                return null;
            }
        }
    }


    /**
     * this function gets the term's first letter and its pointer in the dictionary, and returns an array that contains the information
     * on the docs it appears in
     * @param firstCharInTerm the first char in the term we are looking for
     * @param pointer the pointer of the term in the dictionary
     * @return the arr of information
     * @throws IOException
     */
    public LinkedList<String> getDataDocsFromPosting (char firstCharInTerm, int pointer) throws IOException {
        //here we will check to which path we need to go
        LinkedList<String> docData = new LinkedList<>();
        String path = this.path + "/Disk" + retrieveRelevantPosting(firstCharInTerm)+"/finalPosting.txt";
        File file = new File (path);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine())!=null){
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(line);
            int index = stringBuilder.indexOf(":");
            stringBuilder.delete(index,stringBuilder.length()); //need to check if the stringBuilder object is getting deleted forever
            if(Integer.parseInt(stringBuilder.toString())==pointer){
                stringBuilder = new StringBuilder();
                stringBuilder.append(line);
                stringBuilder.delete(0,index+1);
                String [] arrDocData = stringBuilder.toString().split("~");
                docData.addAll(Arrays.asList(arrDocData));
                reader.close();
                return docData;
            }
            if((pointer < Integer.parseInt(stringBuilder.toString()))){
                break;
            }
        }
        reader.close();
        return null;
    }

    /**
     * CAN BE ONLY USED IF WE KNOW THE TERM DOES EXIST IN THE DICTIONARY
     * this function gets a term and checks to which disk he belongs to
     * @param firstTermChar the first char of the term
     * @return the number of the disk
     */
    private int retrieveRelevantPosting(char firstTermChar) {

        if ((firstTermChar >= 'r' && firstTermChar <= 'z') || (firstTermChar >= 'R' && firstTermChar <= 'Z')) {
            return 4;
        }
        else if ((firstTermChar >= 'd' && firstTermChar <= 'k') || (firstTermChar >= 'D' && firstTermChar <= 'K')) {
            return 2;
        }
        else if ((firstTermChar >= 'l' && firstTermChar <= 'q') || (firstTermChar >= 'L' && firstTermChar <= 'Q')) {
            return 3;

        }
        else {
            return 1;
        }
    }



    public Indexer(String path, boolean isStemming) throws IOException {

        this.isStemming = isStemming;
        this.path = path;
        readDictionaryFromDisk();
        stemmer = new Stemmer();

    }


    /**
     * this function returns the dictionary that the indexer holds
     * @return the main dictionary
     * @throws IOException
     */
    public HashMap<String, Pair<Integer, Integer>> getMainDictionary() throws IOException {
        if(mainDictionary==null){
            readDictionaryFromDisk();
            return mainDictionary;
        }
        else{
            return mainDictionary;
        }
    }

}
