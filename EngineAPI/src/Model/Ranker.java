package Model;

import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecModel;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

public class Ranker {

    private Indexer rankerIndexer;
    private HashMap<Term, LinkedList<String>> queryTermsAndPostings;
    private HashMap<Term, LinkedList<String>> semanticQueryTermsAndPostings;
    private HashSet<String> allRelevantDocs;
    private HashSet<Document> allDocs;
    private String path;
    private boolean isSemantic;
    private boolean isStemming;

    private double averageDocLength;

    private HashMap<String, Double> documentsBM25Score;
    private HashMap<String, Double> documentsPersonalizedScore;
    private HashMap<String, Double> documentsSemanticScore;
    private HashMap<String, String> potentialEntities;
    private DecimalFormat decimalFormat;

    /**
     * Constructor
     *
     * @param indexer
     * @param path
     * @param allDocs
     */
    public Ranker(Indexer indexer, String path, HashSet<Document> allDocs, boolean isSemantic) {

        rankerIndexer = indexer;
        this.path = path;
        queryTermsAndPostings = new HashMap<>();
        semanticQueryTermsAndPostings = new HashMap<>();
        allRelevantDocs = new HashSet<>();
        documentsBM25Score = new HashMap<>();
        documentsPersonalizedScore = new HashMap<>();
        documentsSemanticScore = new HashMap<>();
        this.allDocs = allDocs;
        calculateAverageDocumentLength(allDocs);
        decimalFormat = new DecimalFormat("#.###");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
        this.isSemantic = isSemantic;

    }


    /**
     * Constructor
     * @param indexer
     * @param path
     * @param isStemming
     */
    public Ranker(Indexer indexer, String path, boolean isStemming) {
        rankerIndexer = indexer;
        this.path = path;
        decimalFormat = new DecimalFormat("#.###");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
        this.isStemming=isStemming;
    }




    /**
     * The method receives the query's words after parsing and returns the relevant document's ID's and their rankings
     * @param parsedQuery
     * @return
     * @throws IOException
     * @throws Searcher.UnknownWordException
     */
    public HashMap<String, Double> findRelevantDocuments(ArrayList<Term> parsedQuery) throws IOException, Searcher.UnknownWordException {
        return rankRelevantDocuments(parsedQuery);
    }


    /**
     * The method receives the query's words after parsing and returns the relevant document's ID's and their ranking
     *
     * @param parsedQuery
     * @return
     */
    private HashMap<String, Double> rankRelevantDocuments(ArrayList<Term> parsedQuery) throws IOException, Searcher.UnknownWordException {

        //first, by going over each word of the query, find the matching line in the posting files if exists
        for (Term currTerm : parsedQuery) {
            LinkedList<String> temp = rankerIndexer.findTermInDictionary(currTerm.getTermWord());
            if (temp != null) {
                queryTermsAndPostings.put(currTerm, temp);
            } else {
                queryTermsAndPostings.put(currTerm, new LinkedList<>());
            }
        }

        if (isSemantic) {
            ArrayList<Term> semanticQuery = getSemanticQuery(parsedQuery);
            for (Term currTerm : semanticQuery) {

                LinkedList<String> temp = rankerIndexer.findTermInDictionary(currTerm.getTermWord());
                if (temp == null) {
                    semanticQueryTermsAndPostings.put(currTerm, new LinkedList<>());
                } else {
                    semanticQueryTermsAndPostings.put(currTerm, temp);
                }
            }
        }


        //Creating a second data-structure to hold all the documents ID's that were found in the posting files
        for (HashMap.Entry<Term, LinkedList<String>> entry : queryTermsAndPostings.entrySet()) {
            LinkedList<String> documentIds = entry.getValue();
            for (String doc : documentIds) {
                String[] parsedDoc = doc.split(",");
                allRelevantDocs.add(parsedDoc[0]);
            }
        }

        //from here we rank the documents using the BM-25 score and our personal way of ranking them into two different data structures
        getBM25Score(false);
        getTermScore();
        if (isSemantic) {
            getBM25Score(true);
        }


        //from here we create a new score for each document, based on all ranks we did before hand
        HashMap<String, Double> finalRanksForEachDocument = new HashMap<>();
        if (!isSemantic) {
            for (HashMap.Entry<String, Double> entry : documentsBM25Score.entrySet()) {
                double bm25CurrentScore = entry.getValue();
                double personalizedScore = documentsPersonalizedScore.get(entry.getKey());
                double finalScore = bm25CurrentScore * 0.65 + personalizedScore * 0.35; //todo: might need to rank differently
                finalRanksForEachDocument.put(entry.getKey(), finalScore);
            }

        } else {
            for (HashMap.Entry<String, Double> entry : documentsBM25Score.entrySet()) {
                double bm25CurrentScore = entry.getValue();
                double semanticScore = documentsSemanticScore.get(entry.getKey());
                double personalizedScore = documentsPersonalizedScore.get(entry.getKey());
                double finalScore = bm25CurrentScore * 0.4 + semanticScore * 0.4 + personalizedScore * 0.2; //todo: might need to rank differently
                finalRanksForEachDocument.put(entry.getKey(), finalScore);
            }
        }

        return finalRanksForEachDocument;
    }

    /**
     * The method receives all the documents from the secondary memory and calculates the average length of a document
     *
     * @param allDocs
     */
    private void calculateAverageDocumentLength(HashSet<Document> allDocs) {

        double numOfDocs = allDocs.size();
        double lengthOfAllDocs = 0;

        for (Document doc : allDocs) {
            lengthOfAllDocs = lengthOfAllDocs + doc.getDocLength();
        }

        averageDocLength = lengthOfAllDocs / numOfDocs;
    }


    /**
     * The method calculates IDF and returns the value for the calling method
     *
     * @param
     * @return log of tempCalculation
     */
    private double calculateIDF(double amountOfDocsTermAppearedIn) {

        double amountOfAllDocs = allRelevantDocs.size();
        double tempCalculation = amountOfAllDocs / amountOfDocsTermAppearedIn;
        return Math.log10(tempCalculation); //todo: check if needs to be log on base 2, if so check how to do it
    }


    /**
     * The method calculates the BM25 score for each document that is relevant for the current query
     *
     * @return
     */
    private void getBM25Score(boolean isScoreSemantic) {

        double k1 = 1.2; //can also be 2.0
        double b = 0.75;
        double currIDFScore;
        double termFrequencyInDoc;
        double docLength;
        double amountOfDocumentsTermAppearedIn;
        double BM25CurrentScore;
        String[] termDataFromPosting;

        for (String currDoc : allRelevantDocs) {

            ArrayList<Term> termsDocContains = getTermsInQueryAndInDoc(currDoc);
            BM25CurrentScore = 0;

            for (Term term : termsDocContains) {

                termFrequencyInDoc = 0;
                docLength = 0;
                amountOfDocumentsTermAppearedIn = 0;
                boolean flag = true;

                LinkedList<String> termPosting = queryTermsAndPostings.get(term);
                for (String currData : termPosting) {
                    if (currData.contains(currDoc)) {
                        termDataFromPosting = currData.split(",");
                        termFrequencyInDoc = termFrequencyInDoc + Integer.parseInt(termDataFromPosting[1]);

                        if (flag) {
                            amountOfDocumentsTermAppearedIn = termPosting.size();
                            for (Document doc : allDocs) {
                                if (doc.getDocName().equals(currDoc)) {
                                    docLength = doc.getDocLength();
                                    break;
                                }
                            }
                        } else {
                            amountOfDocumentsTermAppearedIn--;
                        }
                        flag = false;
                    }

                }
                currIDFScore = calculateIDF(amountOfDocumentsTermAppearedIn);
                double tempBM25 = currIDFScore * termFrequencyInDoc * (k1 + 1) / (termFrequencyInDoc + k1 * (1 - b) + b * docLength / averageDocLength);
                BM25CurrentScore = BM25CurrentScore + tempBM25;
            }
            if(!isScoreSemantic) {
                documentsBM25Score.put(currDoc, BM25CurrentScore);
            } else {
                documentsSemanticScore.put(currDoc, BM25CurrentScore);
            }
        }
    }


    /**
     * todo: tomer, please explain here the way you rank each document later
     */
    private void getTermScore() { //todo: show the function to TOMER, it changed completely

        double score;
        for (String currDoc : allRelevantDocs) {

            score = 0;
            ArrayList<Term> termsDocContains = getTermsInQueryAndInDoc(currDoc);

            for (Term term : termsDocContains) {

                LinkedList<String> termPosting = queryTermsAndPostings.get(term);

                for (String currData : termPosting) {
                    if (currData.contains(currDoc)) {
                        String[] termDataFromPosting = currData.split(",");
                        int isHeader = Integer.parseInt(termDataFromPosting[2]);
                        int relativeLocation = Integer.parseInt(termDataFromPosting[4]);
                        if (relativeLocation < 26) {
                            score = score + 0.25;
                        } else if (relativeLocation > 75) {
                            score = score + 0.15;
                        } else {
                            score = score + 0.1;
                        }
                        if (isHeader == 1) { //todo need to clean the header with CleanChar function
                            score = score + 0.75;
                        }
                        break;
                    }
                }
            }
            documentsPersonalizedScore.put(currDoc, score);
        }
    }




    /**
     * The function receives a document's ID and returns arraylist that holds each term that appears in the document
     *
     * @param document
     * @return termsForDocument
     */
    private ArrayList<Term> getTermsInQueryAndInDoc(String document) {

        ArrayList<Term> termsForDocument = new ArrayList<>();
        for (HashMap.Entry<Term, LinkedList<String>> entry : queryTermsAndPostings.entrySet()) {
            LinkedList<String> currTermList = entry.getValue();
            for (String currData : currTermList) {
                if (currData.contains(document)) {
                    termsForDocument.add(entry.getKey());
                    break;
                }
            }
        }
        return termsForDocument;
    }


    /**
     * this function gets the doc name and returns a list of the most ranked entities in this article
     *
     * @param docName the name of the doc
     * @return rankedEntities a list of the entities
     * @throws IOException
     */
    public LinkedList<String> findMostRankedEntities(String docName) throws IOException {
        LinkedList<String> rankedEntities = new LinkedList<>();
        if (potentialEntities == null) {
            readPotentialEntitiesFromDisk();
        }
        String infoOnDoc = potentialEntities.get(docName);
        rankedEntities = removeWrongEntities(infoOnDoc);

        //here we need to send the list to calculation in the ranker
        rankedEntities = getSortedEntitiesList(rankedEntities, docName);

        return rankedEntities;
    }


    /**
     * this function build a data structure of docname->and his potential entities then it returns the string of the doc we are looking for
     *
     * @throws IOException
     */
    private void readPotentialEntitiesFromDisk() throws IOException {
        potentialEntities = new HashMap<>();
        File file = new File(path + "/documents/documents.txt");
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                String[] docInfo = stringBuilder.toString().split(",");
                potentialEntities.put(docInfo[0], line);
                stringBuilder = new StringBuilder();
            }
            reader.close();
        }
    }


    /**
     * this function get the doc name, takes his entities from the disk and then for each entity check if he is in the dictionary
     *
     * @param infoOnDoc the doc name
     * @return the linked list with the real entities
     * @throws IOException
     */
    private LinkedList<String> removeWrongEntities(String infoOnDoc) throws IOException {
        LinkedList<String> realEntities = new LinkedList<>();
        HashSet<String> entitiesWithNoDuplicates = new HashSet<>();
        HashMap<String, Pair<Integer, Integer>> mainDictionary = rankerIndexer.getMainDictionary();
        String[] firstSplit = infoOnDoc.split("™"); //here we are separating the doc info from the potential entities
        String[] secSplit = firstSplit[1].split("~");
        for (String termInfo : secSplit) {
            String[] termWord = termInfo.split(",");
            if (mainDictionary.containsKey(termWord[0])) {
                entitiesWithNoDuplicates.add(termInfo);
                //realEntities.add(termInfo);
            }
        }
        for(String term : entitiesWithNoDuplicates){
            realEntities.add(term);
        }
        return realEntities;
    }

    /**
     * this function return the 5 most ranked entities in the list sorted from the most ranked to the less one
     *
     * @param allEntitiesInDoc unsorted linkedlist that may contain more than 5 entities
     * @return a sorted list with 5 elements or less
     * @throws IOException
     */
    private LinkedList<String> getSortedEntitiesList(LinkedList<String> allEntitiesInDoc, String docName) throws IOException {
        LinkedList<String> rankedEntities = new LinkedList<>();
        Model.Searcher searcher = new Model.Searcher(path,isStemming);
        this.allDocs = searcher.getDocumentsFromDisk();
        //HashMap<String, Integer> termsTf = rankerIndexer.readTermsTfFromDisk();
        PriorityQueue<Pair> pQueue = new PriorityQueue<>(new Comparator<Pair>() {
            @Override
            public int compare(Pair o1, Pair o2) {
                if ((Double) o1.getValue() - (Double) o2.getValue() < 0) {
                    return 1;
                } else if ((Double) o1.getValue() - (Double) o2.getValue() > 0) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        if (allEntitiesInDoc != null) {
            if (allEntitiesInDoc.size() > 0) {
                for (String term : allEntitiesInDoc) {
                    LinkedList<String> infoOnEntity = rankerIndexer.findTermInDictionary(term);
                    double idf = calculateIDF(infoOnEntity);
                    double tf = calculateTF(infoOnEntity, docName);
                    double rank = idf * tf;
                    Pair<String, Double> pair = new Pair<>(term, rank);
                    pQueue.add(pair);
                }
                int limit = 5;
                while (pQueue.size() > 0 && limit > 0) {
                    Pair tempPair = pQueue.poll();
                    rankedEntities.add(tempPair.getKey() + " : " + tempPair.getValue());
                    limit--;
                }
            }
        }
        return rankedEntities;
    }


    /**
     * private function that calculates the term frequency in the entire corpus by iterating on the posting file info
     *
     * @param infoOnDoc the string from the posting file
     * @return the term frequency from the corpus
     */
    private int calculateTermsFreqInCorpus(LinkedList<String> infoOnDoc) {
        int sum = 0;
        if (infoOnDoc == null) {
            return sum;
        }
        if (infoOnDoc.size() == 0) {
            return sum;
        }
        for (String doc : infoOnDoc) {
            String[] firstSplit = doc.split(",");
            sum = sum + Integer.parseInt(firstSplit[1]);
        }
        return sum;
    }


    /**
     * The method receives an array-list of terms representing the original query and returns a list of terms representing the semantic matches to the original query
     * @param parsedQuery
     * @return
     * @throws IOException
     * @throws Searcher.UnknownWordException
     */
    private ArrayList<Term> getSemanticQuery(ArrayList<Term> parsedQuery) throws IOException {

        ArrayList<Term> semanticQuery = new ArrayList<>();
        Word2VecModel model = Word2VecModel.fromTextFile(new File("./Resources/word2vec.c.output.model.txt"));
        com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();
        int numOfResultsInList = 10;


        for (Term currTerm : parsedQuery) {
            try {
                List<com.medallia.word2vec.Searcher.Match> matches = semanticSearcher.getMatches(currTerm.getTermWord(), numOfResultsInList);

                for (Searcher.Match match : matches) {
                    semanticQuery.add(new Term(match.match(), currTerm.getDoc()));
                }
            } catch (com.medallia.word2vec.Searcher.UnknownWordException e){
                //TERM NOT KNOWN TO MODEL
            }
        }

        return semanticQuery;
    }



    /**
     * this function calculates the TF value for an entity
     * @param infoOnDoc the info from the posting file
     * @param docName the name of the doc
     * @return the value of the TF
     */
    private double calculateTF (LinkedList<String> infoOnDoc, String docName){
        double sum = 0;
        if (infoOnDoc == null) {
            return sum;
        }
        if (infoOnDoc.size() == 0) {
            return sum;
        }
        for (String doc : infoOnDoc) {
            String[] firstSplit = doc.split(",");
            if(firstSplit[0].equals(docName)){
                double appearInDoc = Double.parseDouble(firstSplit[1]);
                for (Document document : allDocs){
                    if(document.getDocName().equals(docName)){
                        double length = document.getDocLength();
                        sum = appearInDoc/length;
                        return sum;
                    }
                }
            }
        }
        return sum;
    }

    /**
     * this function calculates the IDF value of each entity in the corpus
     * @param infoOnDoc the info on the entity from the posting file
     * @return sum
     */
    private double calculateIDF(LinkedList<String> infoOnDoc) {
        double sum = 0;
        if (infoOnDoc == null) {
            return sum;
        }
        if (infoOnDoc.size() == 0) {
            return sum;
        }

        else{
            double appearInCorpus = infoOnDoc.size();
            double numOfDocs = allDocs.size();
            sum = Math.log10(numOfDocs/appearInCorpus);
        }
        return sum;
    }
}
