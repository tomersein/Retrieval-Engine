package Model;

import Model.Document;
import Model.DocumentWriter;
import Model.Term;
import javafx.util.Pair;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

public class Parse {

    private String[] articleText;
    private String[] articleHeader;
    private String[] parsedArticle;
    private String[] documentTitle;
    //The Hash Tables
    private HashSet<String> dollarDictionaryHash;
    private HashSet<String> percentDictionaryHash;
    private HashSet<String> thousandDictionaryHash;
    private HashSet<String> millionDictionaryHash;
    private HashSet<String> billionDictionaryHash;
    private HashSet<String> monthDictionaryHash;
    private HashSet<String> monthNumberDictionaryHash;
    private HashSet<String> centimeterDictionaryHash;
    private HashSet<String> meterDictionaryHash;
    private HashSet<String> kilometerDictionaryHash;
    private HashSet<String> gramDictionaryHash;
    private HashSet<String> kilogramDictionaryHash;
    private HashSet<Character> signsDictionaryHash;
    private HashSet<String> headerWords;

    //The Arrays
    private String[] percentDictionary;
    private String[] dollarDictionary;
    private String[] thousandDictionary;
    private String[] millionDictionary;
    private String[] billionDictionary;
    private String[] monthDictionary;
    private String[] monthNumberDictionary;
    private String[] centimeterDictionary;
    private String[] meterDictionary;
    private String[] kilometerDictionary;
    private String[] gramDictionary;
    private String[] kilogramDictionary;
    private char[] signsDictionary;

    private DecimalFormat decimalFormat;
    private HashMap<String, LinkedList<Term>> allWords;
    private HashSet<String> stopWords;
    private Map<String, Integer> docData; //Holds every document's data
    private Document document;
    private DocumentWriter docWriter;
    private HashSet<Integer> markedWords; //keeps the indexes we already checked in the document
    private long sericalNumber;

    //----------PART B OBJECTS-----------

    private ArrayList<Term> queryAllWords;
    private boolean isQuery = false;
    private Query currQuery;

    private HashSet<Term> potentialEntities;

    /**
     * Constructor
     *
     * @param stopWords
     * @param threadNum
     * @param pathOfDocs
     */
    public Parse(HashSet<String> stopWords, long threadNum, String pathOfDocs) {

        allWords = new HashMap<>();
        this.sericalNumber = threadNum;
        docWriter = new DocumentWriter(pathOfDocs, sericalNumber);
        //declare the arrays
        dollarDictionary = new String[]{"dollar", "dollars", "$"};
        percentDictionary = new String[]{"percent", "percents", "percentage", "%"};
        thousandDictionary = new String[]{"thousand", "thousands"};
        millionDictionary = new String[]{"million", "millions"};
        billionDictionary = new String[]{"billion", "billions"};
        monthDictionary = new String[]{"january", "jan", "february", "feb", "march", "mar"
                , "april", "apr", "may", "june", "jun", "july", "jul", "august", "aug",
                "september", "sept", "sep", "october", "oct", "november", "nov", "december", "dec"};
        monthNumberDictionary = new String[]{"01", "01", "02", "02", "03", "03", "04", "04", "05", "06", "06",
                "07", "07", "08", "08", "09", "09", "09", "10", "10", "11", "11", "12", "12"};
        centimeterDictionary = new String[]{"centimeter", "centimeters", "cm"};
        meterDictionary = new String[]{"meter", "meters", "m"};
        kilometerDictionary = new String[]{"kilometer", "kilometers", "km"};
        gramDictionary = new String[]{"gram", "grams", "g"};
        kilogramDictionary = new String[]{"kilogram", "kilograms", "kg"};
        signsDictionary = new char[]{',', '.', ':', '!', '?', '#', '@', '&', '^', '(', ')', '[', ']', '{', '}',
                '+', '*', '/', '|', ';', '<', '>', '\n', '\t', ' ', '_', '\"', '\'', '-'};

        //declare hashes
        dollarDictionaryHash = new HashSet<>();
        percentDictionaryHash = new HashSet<>();
        thousandDictionaryHash = new HashSet<>();
        millionDictionaryHash = new HashSet<>();
        billionDictionaryHash = new HashSet<>();
        monthDictionaryHash = new HashSet<>();
        monthNumberDictionaryHash = new HashSet<>();
        centimeterDictionaryHash = new HashSet<>();
        meterDictionaryHash = new HashSet<>();
        kilometerDictionaryHash = new HashSet<>();
        gramDictionaryHash = new HashSet<>();
        kilogramDictionaryHash = new HashSet<>();
        signsDictionaryHash = new HashSet<>();
        headerWords = new HashSet<>();

        //fill the hashes
        fillHashFromArrayString(dollarDictionaryHash, dollarDictionary);
        fillHashFromArrayString(percentDictionaryHash, percentDictionary);
        fillHashFromArrayString(thousandDictionaryHash, thousandDictionary);
        fillHashFromArrayString(millionDictionaryHash, millionDictionary);
        fillHashFromArrayString(billionDictionaryHash, billionDictionary);
        fillHashFromArrayString(monthDictionaryHash, monthDictionary);
        fillHashFromArrayString(monthNumberDictionaryHash, monthNumberDictionary);
        fillHashFromArrayString(centimeterDictionaryHash, centimeterDictionary);
        fillHashFromArrayString(meterDictionaryHash, meterDictionary);
        fillHashFromArrayString(kilometerDictionaryHash, kilometerDictionary);
        fillHashFromArrayString(gramDictionaryHash, gramDictionary);
        fillHashFromArrayString(kilogramDictionaryHash, kilogramDictionary);
        fillHashFromArrayChar(signsDictionaryHash, signsDictionary);

        decimalFormat = new DecimalFormat("#.###");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
        docData = new HashMap<>();
        this.stopWords = stopWords;
        markedWords = new HashSet<>();


        potentialEntities = new HashSet<>();

    }


    private void fillHashFromArrayString(HashSet<String> set, String[] arr) {
        set.addAll(Arrays.asList(arr));
    }

    private void fillHashFromArrayChar(HashSet<Character> set, char[] arr) {
        for (int i = 0; i < arr.length; i++) {
            set.add(arr[i]);
        }
    }

    /**
     * The function several articles and returns an array of terms, *MAY CHANGE, CHECK LATER ON**
     *
     * @param article
     * @return
     */
    public HashMap<String, LinkedList<Term>> parseArticles(String article) throws IOException {
        if (allWords != null) {
            allWords.clear();
            docData.clear();
            markedWords.clear();
            potentialEntities.clear();
        }
        if (parseArticle(article)) {
            handleDocument();
        }
        return allWords;
    }

    /**
     * this function takes the information and creates a document to the article
     */
    private void handleDocument() throws IOException {
        /*
        if(documentTitle[0].equals("FBIS3-3366")){ //USE ONLY FOR CODE REVIEW DOC
            printTermsOfDoc();
        }
        */
        Pair<Integer, Integer> documentDetails = maxTermAndNumOfTerms();
        int maxTF = documentDetails.getKey();
        int uniqueTerms = documentDetails.getValue();
        int docLength = calculateDocumentLength();
        HashSet <Term> temp = new HashSet<>();
        document = new Document(documentTitle[0], maxTF, uniqueTerms, docLength, temp);
        docWriter.handleDocument(document);
    }


    /**
     * The function goes over the HashMap that holds all the terms that were created from the article and calculates the length of the article
     * by summing every term's TF
     *
     * @return
     */
    private int calculateDocumentLength() {

        int docLength = 0;
        for (HashMap.Entry<String, LinkedList<Term>> entry : allWords.entrySet()) {
            LinkedList<Term> currTermList = entry.getValue();
            for (Term tempTerm : currTermList) {
                if (tempTerm.getDoc().equals(documentTitle[0])) {
                    docLength = docLength + tempTerm.getTf();
                }
            }
        }
        return docLength;
    }


    /**
     * this function tells the Doc Writer to finish writing the documents into the disk
     *
     * @param
     */
    public void finishWriteDocuments() throws IOException {
        docWriter.finishWriteDocuments();
    }

    /**
     * The function receives an article, extracts the text out of the metadata and sends its words to be parsed by ParseWord function
     *
     * @param article
     */
    private boolean parseArticle(String article) throws IOException {
        //first we check if the document doesn't have text, if it doesn't we add only his title with zeros for its information
        if (!article.contains("<TEXT>")) {
            documentTitle = article.split("<DOCNO>");
            if (documentTitle.length > 1) {
                documentTitle = documentTitle[1].split("</DOCNO>");
                documentTitle[0] = documentTitle[0].replaceAll(" ", "");
                document = new Document(documentTitle[0], 0, 0, 0, null);
                docWriter.handleDocument(document);
            }
            return false;
        }

        String[] metaData = article.split("<TEXT>");
        if (metaData.length > 1) {
            parseHeader(metaData[0]);
            //here we will parse the TEXT part of the article
            articleText = metaData[1].split("</TEXT>"); //holds the document's text
            articleText[0] = articleText[0].replaceAll("\\<.*?\\>|\\p{Ps}|\\p{Pe}}", " ");
            parsedArticle = articleText[0].split(" "); //holds the document's text parsed by spaces
            documentTitle = metaData[0].split("<DOCNO>");
            documentTitle = documentTitle[1].split("</DOCNO>");
            documentTitle[0] = documentTitle[0].replaceAll(" ", "");
            //ArrayList<String> cleanWords = new ArrayList<>();
            for (int i = 0; i < parsedArticle.length; i++) {
                if (!markedWords.contains(i)) {
                    if (!parseWord(parsedArticle[i], i)) {
                        if (parsedArticle[i].length() > 1) {
                            //here we are cleaning the special characters from the string
                            if (!stopWords.contains(parsedArticle[i])) {
                                if (!parsedArticle[i].matches("^[a-zA-Z0-9]+$")) {
                                    String[] cleanTerms = parsedArticle[i].split("\\W");
                                    for (int j = 0; j < cleanTerms.length; j++) {
                                        handleTerm(cleanTerms[j], i);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * this function puts all of the words from the header in a hash set
     *
     * @param article
     */
    private void parseHeader(String article) {
        //first, separate the text of the article from the rest of it.
        String[] metaHeader = article.split("<HEADER>");
        if (metaHeader.length > 1) {
            //here we will parse the header of the article
            articleHeader = metaHeader[1].split("</HEADER>");
            String[] parsedHeader = articleHeader[0].split(" ");
            for (int i = 0; i < parsedHeader.length; i++) {
                if (!parsedHeader[i].isEmpty()) {
                    parsedHeader[i] = cleanChar(parsedHeader[i]);
                    headerWords.add(parsedHeader[i]);
                }
            }
        }

    }

    /**
     * The function builds a data structure that supports how many times each term appeared in a specific document
     *
     * @param wordToAdd
     */
    private void addToDocHeap(Term wordToAdd) {
        if (docData.containsKey(wordToAdd.getTermWord())) {
            docData.replace(wordToAdd.getTermWord(), docData.get(wordToAdd.getTermWord()) + wordToAdd.getTf());
        } else {
            docData.put(wordToAdd.getTermWord(), wordToAdd.getTf());
        }
    }

    /**
     * The function iterates over the document data structure and returns a pair that holds the max term frequency in the document
     * as well the amount of unique terms
     *
     * @return new pair
     */
    private Pair<Integer, Integer> maxTermAndNumOfTerms() {
        int count = 0;
        for (Map.Entry<String, Integer> entry : docData.entrySet()) {
            if (count < entry.getValue()) {
                count = entry.getValue();
            }
        }
        return new Pair<>(count, docData.size());
    }

    /**
     * this function will receive a word and catalog it by checking its nature
     *
     * @param word
     * @param index
     */
    private boolean parseWord(String word, int index) {
        if (word.length() > 1) {
            word = cleanChar(word);
        }
        if (word.isEmpty()) {
            return false;
        }
        if (word.equals("\n")) {
            return false;
        }
        if (word.contains("\n")) {
            word = word.replaceAll("\n", "");
        }
        if (word.length() == 1 && signsDictionaryHash.contains(word.charAt(0))) {
            return false;
        }
        if (!findJunkValues(word, "--") || !findJunkValues(word, "\n\n")) {
            return false;
        }
        if (checkTags(word)) {
            return false;
        }
        if (checkNotBetween(word)) {
            handleExpression(word, index);
            return true;
        }
        word = cleanChar(word);
        if (!stopWords.contains(word)) {
            //word = cleanChar(word);
            String contentOfTerm = handlePercent(word, index);
            if (contentOfTerm != null) {
                handleTerm(contentOfTerm, index);
                return true;
            }
            if (word.contains("-") || word.toLowerCase().equals("between")) {
                ArrayList<String> tempContentsOfExp = handleExpression(word, index);
                if (tempContentsOfExp != null) {
                    for (int i = 0; i < tempContentsOfExp.size(); i++) {
                        handleTerm(tempContentsOfExp.get(i), index);
                    }
                    return true;
                }
            }
            contentOfTerm = handleDate(word, index);
            if (contentOfTerm != null) {
                handleTerm(contentOfTerm, index);
                return true;
            }
            contentOfTerm = handleDollars(word, index);
            if (contentOfTerm != null) {
                handleTerm(contentOfTerm, index);
                return true;
            }
            contentOfTerm = handleDistance(word, index);
            if (contentOfTerm != null) {
                handleTerm(contentOfTerm, index);
                return true;
            }
            contentOfTerm = handleWeight(word, index);
            if (contentOfTerm != null) {
                handleTerm(contentOfTerm, index);
                return true;
            }
            //checking the number with no sign condition
            //MIGHT CHANGE LATER TO SEND TO A PRIVATE FUNCTION TO FILL THE DICTIONARIES
            contentOfTerm = parseNumberWithNoSign(word, index);
            if (contentOfTerm != null) {
                handleTerm(contentOfTerm, index);
                return true;
            }
            if (handleEntity(word, index)) {
                return true;
            }
            if (word.matches("^[a-zA-Z0-9]+$")) {
                handleTerm(word, index);
                return true;
            }
        }
        return false;
    }

    /**
     * this function checks if the word between exists, because it is also a stop word and we would like to check
     * it as an expression
     *
     * @param word
     * @return
     */
    private boolean checkNotBetween(String word) {
        if (word != null) {
            String temp = word.toLowerCase();
            if (temp.equals("between")) {
                return true;
            }
        }
        return false;
    }

    /**
     * this function removes comma in the end of the string
     *
     * @param article the string
     * @return the new string
     */
    private String cleanChar(String article) {
        boolean flag = true;
        int endIndex = article.length() - 1;
        int startIndex = 0;
        while (article.length() > 1 && flag) {
            if (signsDictionaryHash.contains(article.charAt(endIndex))) {
                article = article.substring(0, endIndex - 1);
                endIndex = article.length() - 1;
            } else {
                flag = false;
            }
        }
        flag = true;
        while (article.length() > 1 && flag) {
            if (signsDictionaryHash.contains(article.charAt(startIndex))) {
                article = article.substring(1);
                //startIndex++;
            } else {
                flag = false;
            }
        }
        if (article.contains("\n")) {
            article = article.replaceAll("\n", "");
        }
        while (article.contains("--")) {
            article = article.replaceAll("--", "-");
        }
        return article;
    }

    /**
     * this function checks if a word is a junk value like "" or --- after the parse proccess
     *
     * @param article the string we are checking
     * @return true if it is a junk value
     */
    private boolean findJunkValues(String article, String junk) {
        if (article.contains(junk)) {
            for (int i = 0; i < article.length(); i++) {
                if (article.charAt(i) != junk.charAt(0)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }


    /**
     * this function creates the term and sends it to the indexer
     *
     * @param word the string that the term will contain
     */
    private void handleTerm(String word, int index) {
        if (!stopWords.contains(word.toLowerCase()) && !checkTags(word)) {
            if (word.contains("\n")) {
                word = word.replaceAll("\n", "");
            }
            if (word.isEmpty()) {
                return;
            }
            if (word.contains("%") && word.contains("|")) {
                if (word.charAt(0) == '|' || word.charAt(0) == '"') {
                    word = word.substring(1);
                }
            }
            while (word.contains("--")) {
                word = word.replaceAll("--", "-");
            }
            Term tempTerm;
            if (!isQuery) {
                tempTerm = new Term(word, documentTitle[0]);
                int relativeLocation = calculateRelativeLocation(index);
                tempTerm.setRelativeLocation(relativeLocation);
                if (headerWords.contains(word)) {
                    tempTerm.setHeader(true);
                }
                addToDocHeap(tempTerm);
                updateAllWords(tempTerm);
            } else {
                tempTerm = new Term(word, currQuery.getId());
                queryAllWords.add(tempTerm);
            }
        }
    }


    /**
     * this function creates a term of entity
     *
     * @param word the string the term will contain
     */
    private void handleTermEntity(String word, int index) {
        if (!stopWords.contains(word.toLowerCase()) && !checkTags(word)) {
            Term tempTerm;
            if(!isQuery) {
                tempTerm = new Term(word, documentTitle[0]);
                tempTerm.setEntity(true);
                if (headerWords.contains(word)) {
                    tempTerm.setHeader(true);
                }
                int relativeLocation = calculateRelativeLocation(index);
                tempTerm.setRelativeLocation(relativeLocation);
                addToDocHeap(tempTerm);
                updateAllWords(tempTerm);
                potentialEntities.add(tempTerm); //todo: check later
            }
            else{
                tempTerm = new Term(word,currQuery.getId());
                queryAllWords.add(tempTerm);
            }
        }
    }

    /**
     * this function calculates the relative location field on the term
     *
     * @param index the index of the article
     * @return the value of the
     */
    private int calculateRelativeLocation(int index) {
        return (index * 100) / parsedArticle.length;
    }

    /**
     * The function receives a term and checks if the term has been seen before and adjusts the data structure accordingly
     *
     * @param newT
     */
    private void updateAllWords(Term newT) {
        if (allWords.containsKey(newT.getTermWord())) { //if the word had already been seen we update the relevant list
            LinkedList<Term> temp = allWords.get(newT.getTermWord());
            for (Term currT : temp) {
                if (currT.areTermsFromSameDoc(newT)) {
                    currT.setTf(currT.getTf() + newT.getTf());
                    allWords.remove((newT.getTermWord()));
                    allWords.put(newT.getTermWord(), temp);
                    return;
                }
            }
            temp.add(newT);
            allWords.remove(newT.getTermWord());
            allWords.put(newT.getTermWord(), temp);
        } else { // if the word doesn't exist we add her to the hashmap
            LinkedList<Term> temp = new LinkedList<>();
            temp.add(newT);
            allWords.put(newT.getTermWord(), temp);
        }
    }

    /**
     * this function handles numbers that have no sign after them and changes them according to their value
     *
     * @param article the string
     * @param index   the index in the text article
     */
    private String parseNumberWithNoSign(String article, int index) {
        if (article.contains(",")) {
            article = article.replaceAll(",", "");
        }
        if (tryParseInt(article) && isFraction(index)) {
            return handleFraction(article, index);
        } else if (tryParseInt(article)) {
            return handleIntWithNoSign(article, index);
        } else if (tryParseDouble(article)) {
            return handleDoubleWithNoSign(article, index);
        }
        return null;
    }


    /**
     * this function gets a string that represents an int and checks between which values it belongs
     *
     * @param article the string we are checking
     * @param index   the index of the string in the entire doc
     */
    private String handleIntWithNoSign(String article, int index) {
        int value = Integer.parseInt(article);
        if (value >= 1000 && value < 1000000) {
            article = decimalFormat.format((double) value / 1000) + "K";
        } else if (value >= 1000000 && value < 1000000000) {
            article = decimalFormat.format((double) value / 1000000) + "M";
        } else if (value >= 1000000000) {
            article = decimalFormat.format((double) value / 1000000000) + "B";
        } else {
            return handleLargeNumbers(article, index);
        }
        return article;
    }

    /**
     * this function gets a string that represents a double and checks between which values it belongs
     *
     * @param article the string we are checking
     * @param index   the index of the string in the entire doc
     */
    private String handleDoubleWithNoSign(String article, int index) {
        double value = Double.parseDouble(article);
        if (value >= 1000 && value < 1000000) {
            article = decimalFormat.format(value / 1000) + "K";
        } else if (value >= 1000000 && value < 1000000000) {
            article = decimalFormat.format(value / 1000000) + "M";
        } else if (value >= 1000000000) {
            article = decimalFormat.format(value / 1000000000) + "B";
        } else {
            handleLargeNumbers(article, index);
        }
        return article;
    }

    /**
     * this function checks if the string after a number contains a word that represents a number
     *
     * @param index the index of the word
     */
    private String handleLargeNumbers(String article, int index) {
        if (parsedArticle.length > index + 1) {
            String lowerCase = parsedArticle[index + 1].toLowerCase();
            //if (containsValue(thousandDictionary, lowerCase)) {
            if (thousandDictionaryHash.contains(lowerCase)) {
                markedWords.add(index + 1);
                article = article + "K";
            }
            //}
            else if (millionDictionaryHash.contains(lowerCase)) {
                //else if (containsValue(millionDictionary, lowerCase)) {
                markedWords.add(index + 1);
                article = article + "M";
            }
            //}
            else if (billionDictionaryHash.contains(lowerCase)) {
                //else if (containsValue(billionDictionary, lowerCase)) {
                markedWords.add(index + 1);
                article = article + "B";
            }
        }
        //}
        return article;
    }

    /**
     * this function checks if a string is an expression with the char '-'
     *
     * @param article the string we want to check
     * @param index   the index of the string in the entire doc
     * @return true if the string is an expression with the char '-'
     */
    private ArrayList<String> handleExpression(String article, int index) {
        ArrayList<String> tempContents = new ArrayList<>();
        article = cleanChar(article);
        article = article.replaceAll("\n", "");

        if (article.contains("--")) {
            return null;
        }

        if (article.contains("-")) {
            //some of the articles have "--" chars in them, therefore we'll check if there are 2 neighboring Hyphens.
            //Also checks if a term ends with a single Hyphen, therefore not an expression
            int indexOfHyphen = article.indexOf("-");
            //article.charAt(indexOfHyphen + 1) == '-') {
            if (article.length() < indexOfHyphen + 1) {
                return null;
            }

            //tempContents.add(article);
            //We'll also check if the expression contains numbers, if it does we'll add them to the bag of words

            /*
            String[] expressionSeparated = article.split("-");
            for (int i = 0; i < expressionSeparated.length; i++) {
                if (tryParseInt(expressionSeparated[i])) {
                    tempContents.add(expressionSeparated[i]);
                }
            }
            return tempContents;
            */
            //check here the expression are in alpha beit strings
            boolean flag = true;
            String[] separateHyphenString = article.split("-");
            for (int j = 0; j < separateHyphenString.length; j++) {
                if (!separateHyphenString[j].isEmpty()) {
                    separateHyphenString[j] = cleanChar(separateHyphenString[j]);

                    if (tryParseInt(separateHyphenString[j])) {
                        tempContents.add(separateHyphenString[j]);
                    }

                    if (!separateHyphenString[j].matches("^[a-zA-Z0-9]+$")) {
                        flag = false;
                        break;
                    }
                }
            }
            if (flag) {
                tempContents.add(article);
            }
            return tempContents;
        } else {
            //checking if there's an "Between number and number" expression
            article = article.toLowerCase();
            if (article.equals("between")) {
                if ((index + 1 < parsedArticle.length) && (index + 2 < parsedArticle.length) && (index + 3 < parsedArticle.length)) {
                    if ((tryParseInt(parsedArticle[index + 1]))) {
                        String temp = parsedArticle[index + 2].toLowerCase();
                        if ((temp.equals("and")) && (tryParseInt(parsedArticle[index + 3]))) {
                            article = article + " " + parsedArticle[index + 1] + " " + parsedArticle[index + 2] + " " + parsedArticle[index + 3];
                            tempContents.add(article);
                            return tempContents;
                        }
                    }
                }
            }
        }
        //if we reached this line it means the word is neither expression nor part of one
        return null;
    }


    /**
     * this function takes the first string, and checks if it is an entity.
     * if the answer is true, it will check the other strings after it and try to combine it
     *
     * @param article the string we are checking
     * @param index the index of the string in the entire doc
     */
    private boolean handleEntity(String article, int index) {
        if (isEntity(article)) {
            markedWords.add(index);

            //handleTerm(article, index);
            if (index + 1 < parsedArticle.length) {
                if (!checkGarbageValues(article, index + 1)) {
                    if(!combineEntities(article, index + 1)){
                        handleTerm(article, index);
                    }
                    return true;
                }
            }
            else{
                //if the entity is the last word in the article we will keep it
                handleTermEntity(article, index);
            }
        }
        return false;
    }

    /**
     * checks if the string is garbage value, mostly used in HANDLE ENTITIES
     *
     * @param article the string we are checking
     * @return true if it is a garbage value
     */
    private boolean checkGarbageValues(String article, int index) {
        if (article.isEmpty()) {
            markedWords.add(index);
            return true;
        } else if (!findJunkValues(article, "--") | !findJunkValues(article, "\n\n")) {
            markedWords.add(index);
            return true;
        }
        return false;
    }

    /**
     * this function runs on the parsedArticles and checks if the string starts with an upper letter, if it does, it
     * combines it into a long string and after that it will be send to the handle function
     *
     * @param index the index of the string in the doc
     * @return "" if it is not an entity, otherwise return the conmbined string
     */

    private boolean combineEntities(String article, int index) {
        //notice this is the 2nd string we are checking
        String combinedEntity = article;
        boolean isComplexEntity = false; //checks if we have added two string together so we can combine them as a term
        for (int i = index; i < parsedArticle.length; i++) {
            if (!checkGarbageValues(parsedArticle[i], i)) {
                parsedArticle[i] = cleanChar(parsedArticle[i]);
                if (isEntity(parsedArticle[i])) {
                    markedWords.add(i);

                    combinedEntity = combinedEntity + " " + parsedArticle[i];
                    isComplexEntity = true;
                }
                else {
                    break;
                }
            }
            else {
                break;

            }
        }
        if (isComplexEntity) {
            handleTermEntity(combinedEntity, index);
        }
        return isComplexEntity;
    }

    /**
     * this function checks if a string starts with an upper case
     *
     * @param article the string we want to check
     * @return the string is the string is entity
     */
    private boolean isEntity(String article) {
        if (article.length() > 1) {
            if (article.matches("^[a-zA-Z0-9]+$")) {
                if (Character.isUpperCase(article.charAt(0))) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * checks if the string contains a number with a percent
     *
     * @param article the string needed to be checked
     * @param index   the index of the string in the entire doc
     * @return true if it was a number with dollar
     */
    private boolean containsPercent(String article, int index) {
        //NEED TO CHECK BOTH START AND END
        if (article.length() > 1) {
            String tempString = article.substring(article.length() - 1);
            if (percentDictionaryHash.contains(tempString)) {
                return true;
            }
        } else if (parsedArticle.length > index + 1) {
            if (!checkGarbageValues(parsedArticle[index + 1], index + 1)) {
                parsedArticle[index + 1] = cleanChar(parsedArticle[index + 1]);
                String lowerCases = parsedArticle[index + 1].toLowerCase();
                if (percentDictionaryHash.contains(lowerCases)) {
                    //if (containsValue(percentDictionary, lowerCases)) {
                    markedWords.add(index + 1);
                    return true;
                    //}
                }
            }
        }
        return false;
    }

    /**
     * this function handles a string that contains a percent word or %
     *
     * @param article the string that is being checked
     * @param index   the index of the string in the doc
     */
    private String handlePercent(String article, int index) {
        if (containsPercent(article, index)) {
            if (!article.contains("%")) {
                return parsedArticle[index] + "%";
            } else {
                return article;

            }
        }
        return null;
    }

    /**
     * this function checks if a string is build as a format of a date
     *
     * @param article the string we are checking
     * @param index
     * @return
     */
    private String handleDate(String article, int index) {
        //CHECK THE FORMAT OF DD - MONTH
        if (tryParseInt(article)) {
            if (parsedArticle.length > index + 1) {
                if (!checkGarbageValues(parsedArticle[index + 1], index + 1)) {
                    parsedArticle[index + 1] = cleanChar(parsedArticle[index + 1]);
                    String lowerCase = parsedArticle[index + 1].toLowerCase();
                    if (monthDictionaryHash.contains(lowerCase)) {
                        //if (containsValue(monthDictionary, lowerCase)) {
                        String monthNumber = monthNumberDictionary[numberOfMonth(monthDictionary, lowerCase)];
                        int lessThan10 = Integer.parseInt(article);
                        if (lessThan10 < 10) {
                            markedWords.add(index + 1);
                            return monthNumber + "-" + "0" + parsedArticle[index];
                        }
                        markedWords.add(index + 1);
                        article = monthNumber + "-" + parsedArticle[index];
                        return article;
                    }
                }
                //}
            }
        }
        //CHECK THE FORMAT OF MM-DD
        else {
            String lowerCase = parsedArticle[index].toLowerCase();
            if (monthDictionaryHash.contains(lowerCase)) {
                //if (containsValue(monthDictionary, lowerCase)) {
                if (parsedArticle.length > index + 1) {
                    if (!checkGarbageValues(parsedArticle[index + 1], index + 1)) {
                        parsedArticle[index + 1] = cleanChar(parsedArticle[index + 1]);
                        if (isTheNumberADay(parsedArticle[index + 1])) {
                            String monthNumber = monthNumberDictionary[numberOfMonth(monthDictionary, lowerCase)];
                            article = monthNumber + "-" + parsedArticle[index + 1];
                            int lessThan10 = Integer.parseInt(parsedArticle[index + 1]);
                            if (lessThan10 < 10) {
                                markedWords.add(index + 1);
                                return monthNumber + "-" + "0" + lessThan10 + "";
                            }
                            markedWords.add(index + 1);
                            return article;
                        }

                        //CHECK THE FORMAT OF MM-YYYY CHANGE TO YYYY-MM
                        else if (isTheNumberAYear(parsedArticle[index + 1])) {
                            String monthNumber = monthNumberDictionary[numberOfMonth(monthDictionary, lowerCase)];
                            article = parsedArticle[index + 1] + "-" + monthNumber;
                            markedWords.add(index + 1);
                            return article;
                        }
                    }
                }
            }
            //}
        }
        return null;
    }

    /**
     * this function checks the number of month that we get in a string
     *
     * @param arr   they array being checked
     * @param value the value that we want to check
     * @return number of cell
     */
    private int numberOfMonth(String[] arr, String value) {
        int counter = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(value)) {
                return counter;
            } else {
                counter++;
            }
        }
        return -1;
    }

    /**
     * this function check a string that represent a NUMBER and checks if it can be a day of a month
     *
     * @param value the string that represents a number
     * @return true if it can be a date
     */
    private boolean isTheNumberADay(String value) {
        if (tryParseInt(value)) {
            int tempNumber = Integer.parseInt(value);
            if (tempNumber >= 1 && tempNumber <= 31) {
                return true;
            }
        }
        return false;
    }

    /**
     * this function check a string that represent a NUMBER and checks if it can be a year
     *
     * @param value the string that represents a number
     * @return true if it can be a year
     */
    private boolean isTheNumberAYear(String value) {
        if (tryParseInt(value)) {
            int tempNumber = Integer.parseInt(value);
            if (tempNumber >= 1000 && tempNumber <= 9999) {
                return true;
            }
        }
        return false;
    }


    /**
     * checks if the string equals to a number
     *
     * @param value a string needed to be checked
     * @return true if the string represents a number
     */
    private boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * checks if the string equals to double
     *
     * @param value a string needed to be checked
     * @return true if the string represents double
     */
    private boolean tryParseDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * generic function that helps to check if a string contains a certain value only once
     *
     * @param article      the string we want to check
     * @param checkedValue the value we want to check in the string
     * @return true if the value exists only once in the string
     */
    private boolean isContainsSingleValue(String article, String checkedValue) {
        //this counter checks that the backslash appears only once in the string
        int counter = 0;
        for (int i = 0; i < article.length(); i++) {
            String temp = article.charAt(i) + "";
            if (tryParseInt(temp)) {
                continue;
            } else if (temp.equals(checkedValue)) {
                counter++;
            } else {
                return false;
            }
        }
        if (counter == 1) {
            return true;
        }
        return false;
    }


    /**
     * this function checks if a string represents a fraction number
     *
     * @param index position of the current word
     * @return true if the string is a fraction
     */
    private boolean isFraction(int index) {
        if (parsedArticle.length > index + 1) {
            if (isContainsSingleValue(parsedArticle[index + 1], "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * this function handles a string that represents a fraction number
     *
     * @param index the index of the word in the entire doc
     */
    private String handleFraction(String article, int index) {
        markedWords.add(index + 1);
        cleanChar(parsedArticle[index + 1]);
        article = article + " " + parsedArticle[index + 1];
        return article;
    }


    /**
     * this function handles the dollars format
     *
     * @param article the string we are checking
     * @param index   the index of the string in the entire doc
     * @return the string of the value
     */
    private String handleDollars(String article, int index) {
        int intValue;
        double doubleValue;

        //a variable that keeps the number if it contains "," for the format of $ int,int,int<million
        String articleWithCommas = article;
        articleWithCommas.replaceAll("[^a-zA-Z0-9]", "");

        //we need to check if the number if double or int
        article = article.replaceAll(",", "");
        //first case we check: does the string contain a '$' sign. $price, $price million $price billion
        if (article.contains("$")) {
            return handleDollarSign(article, index, articleWithCommas);
        }
        //if the string doesn't contain $
        else {
            //price bn+dollar, price+m dollars
            if (containsChars(article, 'b', 'n')) {
                return article.substring(0, article.length() - 2) + "000 M Dollars";

            } else if (containsChar(article, 'm')) {
                return article.substring(0, article.length() - 1) + " M Dollars";
            }
            //if there's no $ sign check if the word is a number
            /*
            else if (!tryParseInt(article) && !tryParseDouble(article)) {
                return false;
            }
            */

            //from here we check numbers both int and double
            if (tryParseInt(article)) {
                intValue = Integer.parseInt(article);
            } else if (tryParseDouble(article)) {
                doubleValue = Double.parseDouble(article);
                intValue = (int) doubleValue;
            } else {
                return null;
            }
            //we check if there's a fraction within the price //price fraction dollars - ITS OUR RULE OPTIONAL
            if (isFraction(index) && (parsedArticle.length > index + 2)) {
                return handleFractionWithDollar(article, index);
            }
            //check presentations of numbers which are smaller than million
            if (intValue < 1000000) {
                if (parsedArticle.length > index + 1) {
                    return handleSmallDollarValue(article, index);
                }
            }
            //bigger than 1,000,000
            else {
                if (parsedArticle.length > index + 1) {
                    String temp = parsedArticle[index + 1].toLowerCase();
                    if (temp.equals("dollars")) {
                        return decimalFormat.format(intValue / 1000000) + " M Dollars";
                    }
                }
            }
        }
        return null;
    }


    /**
     * this function is a sub-function of handling dollars expressions, this function handles the format price$
     *
     * @param article           the string we want to check
     * @param index             the index of the string in the doc
     * @param articleWithCommas the string with commas if exists
     * @return true if the expression was from this type
     */
    private String handleDollarSign(String article, int index, String articleWithCommas) {
        int intValue;
        double doubleValue;
        article = article.replaceAll("[^a-zA-Z0-9]", "");
        if (!tryParseInt(article) && !tryParseDouble(article)) {
            return null;
        }
        if (tryParseInt(article)) {
            intValue = Integer.parseInt(article);
        } else {
            doubleValue = Double.parseDouble(article);
            intValue = (int) doubleValue;
        }
        if (intValue < 1000000) {
            if (parsedArticle.length > index + 1) {
                if (!checkGarbageValues(parsedArticle[index + 1], index + 1)) {
                    parsedArticle[index + 1] = cleanChar(parsedArticle[index + 1]);
                    if (parsedArticle[index + 1].equals("billion")) {
                        return article + "000 M Dollars";
                    } else if (parsedArticle[index + 1].equals("million")) {
                        return article + " M Dollars";
                    }
                } else {
                    return articleWithCommas + " Dollars";
                }
            }
        } else {
            return decimalFormat.format(intValue / 1000000) + " M Dollars";
        }
        return null;
    }

    /**
     * this function is a sub-function of handling dollars expressions, this function handles values with a fraction
     *
     * @param article the string we are checking
     * @param index   the index of the string in the doc
     * @return true if it matched to the pattern
     */

    private String handleFractionWithDollar(String article, int index) {
        if (!checkGarbageValues(parsedArticle[index + 2], index + 2)) {
            parsedArticle[index + 2] = cleanChar(parsedArticle[index + 2]);
            String temp = parsedArticle[index + 2].toLowerCase();
            if (temp.equals("dollars")) {
                markedWords.add(index + 1);
                markedWords.add(index + 2);
                return article + " " + parsedArticle[index + 1] + " Dollars";
            }
        }
        markedWords.add(index + 2);
        return null;
    }

    /**
     * this function is a sub-function of handling dollars expressions, this function handles values less than 1M
     *
     * @param article the string we are checking
     * @param index   the index of the string in the doc
     * @return true if it matched to the pattern
     */
    private String handleSmallDollarValue(String article, int index) {
        String nextWord = parsedArticle[index + 1].toLowerCase();
        if (!checkGarbageValues(parsedArticle[index + 1], index + 1)) {
            nextWord = cleanChar(nextWord);
            if (dollarDictionaryHash.contains(nextWord)) {
                //if (containsValue(dollarDictionary, nextWord)) {
                markedWords.add(index + 1);
                return article + " Dollars";

            }
        }
        if (parsedArticle.length > index + 2) {
            String secondNextWord = parsedArticle[index + 2].toLowerCase();
            if (!checkGarbageValues(parsedArticle[index + 2], index + 2)) {
                if (secondNextWord.equals("dollars")) {
                    if (nextWord.equals("m")) {
                        markedWords.add(index + 1);
                        markedWords.add(index + 2);
                        return article + " M Dollars";
                    } else if (nextWord.equals("bn")) {
                        markedWords.add(index + 1);
                        markedWords.add(index + 2);
                        return article + "000 M Dollars";
                    }
                }
            } else if (parsedArticle.length > index + 3) {
                String thirdNextWord = parsedArticle[index + 3].toLowerCase();
                if (!checkGarbageValues(parsedArticle[index + 3], index + 3)) {
                    thirdNextWord = cleanChar(thirdNextWord);
                    if (thirdNextWord.equals("dollars") && secondNextWord.equals("u.s.")) {
                        String measurementWord = parsedArticle[index + 1];
                        if (measurementWord.equals("million")) {
                            markedWords.add(index + 1);
                            markedWords.add(index + 2);
                            markedWords.add(index + 3);
                            return article + " M Dollars";
                        } else if (measurementWord.equals("billion")) {
                            markedWords.add(index + 1);
                            markedWords.add(index + 2);
                            markedWords.add(index + 3);
                            return article + "000 M Dollars";
                        } else if (measurementWord.equals("trillion")) {
                            markedWords.add(index + 1);
                            markedWords.add(index + 2);
                            markedWords.add(index + 3);
                            return article + "000000 M Dollars";
                        }
                    }
                }
            }
        }
        return null;
    }


    /**
     * this function helps to check if we are handling a format of numberBN and helps the handleDollar function
     *
     * @param article the string we want to check
     * @return true if it contains
     */
    private boolean containsChars(String article, char firstChar, char secondChar) {
        article = article.toLowerCase();
        if (article.length() > 1) {
            Character lastChar = article.charAt(article.length() - 1);
            Character beforeLastChar = article.charAt(article.length() - 2);
            if (beforeLastChar.equals(firstChar) && lastChar.equals(secondChar)) {
                String numPart = article.substring(0, article.length() - 2);
                if (tryParseInt(numPart) || tryParseDouble(numPart)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * this function helps to check if we are handling a format of numberM and helps the handle dollar
     *
     * @param article the string we are checking
     * @return true if it contains
     */
    private boolean containsChar(String article, char ch) {
        article = article.toLowerCase();
        if (article.length() > 0) {
            Character lastChar = article.charAt(article.length() - 1);
            if (lastChar.equals(ch)) {
                String numPart = article.substring(0, article.length() - 1);
                if (tryParseInt(numPart) || tryParseDouble(numPart)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * the main function that handles expression that represent distances
     *
     * @param article the string we are checking
     * @param index   the index of the string in the doc
     * @return true if it was handled
     */
    private String handleDistance(String article, int index) {
        if (tryParseInt(article)) {
            return handleDistanceInt(article, index);
        } else if (tryParseDouble(article)) {
            return handleDistanceDouble(article, index);
        }
        //we will check now if the number is connected to cm/m/km
        else {
            if (containsChars(article, 'k', 'm')) {
                //handle km connected
                int endIndex = article.length() - 2;
                return article.substring(0, endIndex) + " km";
            } else if (containsChars(article, 'c', 'm')) {
                //handle cm connected
                return article.substring(0, article.length() - 2) + " " + "cm";
            } else if (containsChar(article, 'm')) {
                return article.substring(0, article.length() - 1) + " m";
            }
        }
        return null;
    }

    /**
     * a sub function that helps to handle distances that the first word is an INT
     *
     * @param article the string we are checking
     * @param index   the index of the string in the doc
     * @return true if it was handled
     */
    private String handleDistanceInt(String article, int index) {
        //checks the next index to see if it is a fraction
        if (isFraction(index)) {
            if (parsedArticle.length > index + 2) {
                if (!checkGarbageValues(parsedArticle[index + 2], index + 2)) {
                    parsedArticle[index + 2] = cleanChar(parsedArticle[index + 2]);
                    if (isDistance(index + 2)) {
                        markedWords.add(index + 1);
                        markedWords.add(index + 2);
                        return article + " " + parsedArticle[index + 1] + " " + distanceCategory(index + 2);
                    }
                }
            }
        }
        //if the string is not a fraction we will check first the measurement and than convert if it is needed
        if (parsedArticle.length > index + 1) {
            if (!checkGarbageValues(parsedArticle[index + 1], index + 1)) {
                parsedArticle[index + 1] = cleanChar(parsedArticle[index + 1]);
                if (isDistance(index + 1)) {
                    int value = Integer.parseInt(article);
                    String measureDistance = distanceCategory(index + 1);
                    if (measureDistance.equals("cm") && value >= 100) {
                        markedWords.add(index + 1);
                        return decimalFormat.format((double) value / 100) + " m";
                    } else if (measureDistance.equals("m") && value >= 1000) {
                        markedWords.add(index + 1);
                        return decimalFormat.format((double) value / 1000) + " km";
                    } else {
                        markedWords.add(index + 1);
                        return value + " " + measureDistance;
                    }
                }
            }
        }
        //if the word is not a distance
        return null;
    }

    /**
     * a sub function that helps to handle distances that the first word is an DOUBLE
     *
     * @param article the string we are checking
     * @param index   the index of the string in the doc
     * @return true if it was handled
     */
    private String handleDistanceDouble(String article, int index) {
        if (index + 1 < parsedArticle.length) {
            if (!checkGarbageValues(parsedArticle[index + 1], index + 1)) {
                cleanChar(parsedArticle[index + 1]);
                if (isDistance(index + 1)) {
                    double value = Double.parseDouble(article);
                    String measurementDistance = distanceCategory(index + 1);
                    if (measurementDistance.equals("cm") && value >= 100) {
                        markedWords.add(index + 1);
                        return decimalFormat.format(value / 100) + " " + "m";
                    } else if (measurementDistance.equals("m") && value >= 1000) {
                        markedWords.add(index + 1);
                        return decimalFormat.format(value / 1000) + " " + "km";
                    } else {
                        markedWords.add(index + 1);
                        return value + " " + measurementDistance;
                    }
                }
            }
        }
        //if the word is not a distance
        return null;
    }

    /**
     * a sub-function that decides if the expression is a distance measurement
     *
     * @param index the index of the word in the doc
     * @return true if it a distance;
     */
    private boolean isDistance(int index) {
        if (index < parsedArticle.length) {
            if (parsedArticle[index] != null) {
                if (centimeterDictionaryHash.contains(parsedArticle[index]) ||
                        meterDictionaryHash.contains(parsedArticle[index]) ||
                        kilometerDictionaryHash.contains(parsedArticle[index])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * a sub-function that returns the measurement of the distance, works only if it a distance for SURE
     *
     * @param index the index of the string we want to check
     * @return the string that represents the measurement
     */
    private String distanceCategory(int index) {
        if (centimeterDictionaryHash.contains(parsedArticle[index])) {
            return "cm";
        } else if (meterDictionaryHash.contains(parsedArticle[index])) {
            return "m";
        }
        return "km";
    }


    /**
     * the main function that handles expression with weights
     *
     * @param article the string we want to check
     * @param index   the index of the string in the doc
     * @return true if it was handled as a weight string
     */
    private String handleWeight(String article, int index) {
        if (tryParseInt(article)) {
            return handleIntWeight(article, index);
        }
        if (tryParseDouble(article)) {
            return handleDoubleWeight(article, index);
        }
        //the string might be connected number+measurement
        else {
            if (containsChars(article, 'k', 'g')) {
                return article.substring(0, article.length() - 2) + " kg";
            } else if (containsChar(article, 'g')) {
                return article.substring(0, article.length() - 1) + " g";
            }
        }
        return null;
    }

    /**
     * a sub function that handles expression of weights when the value is INT
     *
     * @param article the string we want to check
     * @param index   the index of the string in the doc
     * @return true if it was handled as a weight string
     */
    private String handleIntWeight(String article, int index) {
        //if the number is connected to a fraction
        if (isFraction(index)) {
            if (parsedArticle.length > index + 2) {
                if (!checkGarbageValues(parsedArticle[index + 2], index + 2)) {
                    parsedArticle[index + 2] = cleanChar(parsedArticle[index + 2]);
                    if (isWeight(index + 2)) {
                        markedWords.add(index + 1);
                        markedWords.add(index + 2);
                        return article + " " + parsedArticle[index + 1] + " " + weightCategory(index + 2);
                    }
                }
            }
        }
        if (parsedArticle.length < index + 1) {
            if (!checkGarbageValues(parsedArticle[index + 1], index + 1)) {
                parsedArticle[index + 1] = cleanChar(parsedArticle[index + 1]);
                //if the number is not the fraction we will check the measurement and convert it if it is needed
                if (isWeight(index + 1)) {
                    int value = Integer.parseInt(article);
                    String measurementWeight = weightCategory(index + 1);
                    if (value >= 1000 && measurementWeight.equals("g")) {
                        markedWords.add(index + 1);
                        return decimalFormat.format((double) value / 1000) + " " + "kg";
                    } else {
                        markedWords.add(index + 1);
                        return value + " " + measurementWeight;
                    }
                }
            }
        }
        return null;
    }

    /**
     * a sub function that handles expression of weights when the value is DOUBLE
     *
     * @param article the string we want to check
     * @param index   the index of the string in the doc
     * @return true if it was handled as a weight string
     */
    private String handleDoubleWeight(String article, int index) {
        if (parsedArticle.length < index + 1) {
            if (!checkGarbageValues(parsedArticle[index + 1], index + 1)) {
                parsedArticle[index + 1] = cleanChar(parsedArticle[index + 1]);
                if (isWeight(index + 1)) {
                    double value = Double.parseDouble(article);
                    String measurementWeight = weightCategory(index + 1);
                    if (measurementWeight.equals("g") && value >= 1000) {
                        markedWords.add(index + 1);
                        return decimalFormat.format(value / 1000) + " kg";
                    } else {
                        markedWords.add(index + 1);
                        return value + " " + measurementWeight;
                    }
                }
            }
        }
        return null;
    }

    /**
     * sub function that decides if the string represents a weight expression
     *
     * @param index the index of the string in the doc
     * @return true if the expression is a weight
     */
    private boolean isWeight(int index) {
        if (index < parsedArticle.length) {
            if (parsedArticle[index] != null) {
                if (gramDictionaryHash.contains(parsedArticle[index]) ||
                        kilogramDictionaryHash.contains(parsedArticle[index])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * sub function that returns the kind of the weight expression
     *
     * @param index the index of the string in the doc
     * @return the string that represents the expression
     */
    private String weightCategory(int index) {
        if (gramDictionaryHash.contains(parsedArticle[index])) {
            return "g";
        }
        return "kg";
    }

    /**
     * Used for printing specific doc's terms and their frequency
     */
    private void printTermsOfDoc() {
        TreeMap<String, Integer> docAsTree = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : docData.entrySet()) {
            docAsTree.put(entry.getKey(), entry.getValue());
        }
        System.out.println(documentTitle[0]);
        for (Map.Entry<String, Integer> entry : docAsTree.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    /**
     * checks if the word is a part of a TAG
     *
     * @param word
     * @return
     */
    private boolean checkTags(String word) {
        if (word.length() > 1) {
            if (word.contains("<") || word.contains(">") || word.contains("P=10")) {
                return true;
            }
        }
        return false;
    }


    //----------NEW FUNCTIONS FOR PART B OF THE ENGINE-----------

    public ArrayList<Term> parseQuerys(Query query) {
        return parseQuery(query);
    }

    private ArrayList<Term> parseQuery(Query query) {

        queryAllWords = new ArrayList<>();
        currQuery = query;
        isQuery = true;
        parsedArticle = query.getQueryDataSplitted();
        for (int i = 0; i < parsedArticle.length; i++) {

            if(!markedWords.contains(i)) {
                parseWord(parsedArticle[i], i);
            }
        }
        isQuery = false;
        return queryAllWords;
    }

}
