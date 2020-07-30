package Model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Handler extends Thread implements Runnable {

    private int numOfFolder; // represents the numbers of folders in the corpus
    private String pathOfCorpus; //path of the corpus
    private String pathOfPosting; // path for the posting files
    private HashSet<String> stopWords; //holds the stop-words of the corpus
    private int toGo; //Shared resource for the threads
    private boolean isStemming; // stemming option on or not
    private ExecutorService executor;
    private Indexer indexer;
    private ReentrantLock lock1;
    private ReentrantLock lock2;
    private long startTime;
    private long endTime;

    //----PART B OBJECTS----

    private String pathToWriteQueryResults;
    private String pathToPostingFiles;
    private Searcher searcher;
    private LinkedList<String> singleQuerySortedResults;
    private HashMap<String, LinkedList<String>> multiQueriesSortedResults;
    private boolean isSemantic;
    private boolean writeToTrecFile;



    /**
     * Constructor
     *
     * @param pathOfCorpus
     * @param pathOfPosting
     * @param isStemming
     * @throws FileNotFoundException
     */
    public Handler(String pathOfCorpus, String pathOfPosting, boolean isStemming) throws FileNotFoundException, UnsupportedEncodingException {

        this.pathOfCorpus = pathOfCorpus;
        this.pathOfPosting = pathOfPosting;
        File mainDir = new File(pathOfCorpus);
        String[] subDirs = mainDir.list();
        this.numOfFolder = subDirs.length;
        this.toGo = 0;
        executor = Executors.newFixedThreadPool(3);
        ReadFile tempReader = new ReadFile(pathOfCorpus);
        this.stopWords = tempReader.readStopWord();
        this.isStemming = isStemming;
        createFolders();
        tempReader.copyStopWords(this.pathOfPosting,this.stopWords);
        indexer = new Indexer(this.pathOfPosting, this.pathOfCorpus + "\\documents", this.isStemming);
        lock1 = new ReentrantLock();
        lock2 = new ReentrantLock();

    }

    /**
     * The function sends back the indexer object
     *
     * @return
     */
    public Indexer getIndexer() {
        return indexer;
    }


    /**
     * the function creates folders when the Stemming option is on
     */
    private void createFolders() {
        File theDir;
        if (isStemming) {
            theDir = new File(this.pathOfPosting + "/WithStem/");
            this.pathOfPosting = this.pathOfPosting + "/WithStem/";
            if (!theDir.exists()) {
                theDir.mkdir();
            }
        } else {
            theDir = new File(this.pathOfPosting + "/WithOutStem/");
            this.pathOfPosting = this.pathOfPosting + "/WithOutStem/";
            if (!theDir.exists()) {
                theDir.mkdir();
            }
        }
    }


    /**
     * The function finishes building the inverted indexer
     *
     * @throws IOException
     */
    public void finish() throws IOException {
        executor = null;
        stopWords.clear();
        stopWords = null;
        int numOfUniqueTerms = indexer.finishInvertedIndexer();
        endTime = System.currentTimeMillis();
        long totalTime = (endTime - startTime) / 1000;
        File file = new File(pathOfPosting + "/documents/documents.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String line = null;
        int counter = 0;
        while ((line = bufferedReader.readLine()) != null) {
            counter++;
        }
        System.out.println("Amount of documents that were indexed: " + counter);
        System.out.println("Amount of unique terms that were identified: " + numOfUniqueTerms);
        System.out.println("Total running time: " + totalTime + " seconds");

        bufferedReader.close();

    }


    /**
     * The function begins building the inverted indexer
     */
    public void start() {
        startTime = System.currentTimeMillis();
        //The threads work on parsing the corpus together
        for (int i = 0; i < 3; i++) {
            executor.execute(() -> {
                try {
                    startReading();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        try {
            if (!executor.awaitTermination(25, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
            finish();

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * The function begins the process of building the invereted indexer, each thread enters the function, creates a parser
     * And fills the shared dictionary with the parser's data
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void startReading() throws IOException, InterruptedException {
        Parse parser = new Parse(stopWords, currentThread().getId(), pathOfPosting);
        while (true) {
            int tempNum;
            lock1.lock();
            try {
                tempNum = toGo;
                if (toGo >= numOfFolder - 1) {
                    lock1.unlock();
                    break;
                } else {
                    toGo = toGo + 3;
                    lock1.unlock();
                }
            } finally {

            }
            ReadFile reader = new ReadFile(pathOfCorpus);
            LinkedList<String> documents = reader.readFiles(tempNum);
            for (int i = 0; i < documents.size(); i++) {
                HashMap<String, LinkedList<Term>> bagOfWords = parser.parseArticles(documents.get(i));
                lock2.lock();
                try {
                    indexer.buildInvertedIndexer(bagOfWords);
                    lock2.unlock();
                } finally {

                }
            }
        }
        parser.finishWriteDocuments();
        executor.shutdown(); //shutdown executor
    }

    //-------------FUNCTIONS FOR PART B-------------------


    public Handler(boolean isStemming, String pathForPostingFiles, boolean isSemantic, boolean writeToFile, String pathForQueryResults) throws FileNotFoundException {

        this.isSemantic = isSemantic;
        this.isStemming = isStemming;
        this.pathToPostingFiles = pathForPostingFiles;
        if(isStemming) {
            this.pathToPostingFiles = pathForPostingFiles + "/withStem";
        } else {
            this.pathToPostingFiles = pathForPostingFiles + "/WithOutStem";
        }
        stopWords = new ReadFile(pathToPostingFiles).readStopWord();
        writeToTrecFile = writeToFile;
        this.pathToWriteQueryResults = pathForQueryResults;


    }


    public LinkedList<String> findRelevantDocumentsForSingleQuery(String queryString) throws IOException, com.medallia.word2vec.Searcher.UnknownWordException {

        Query query = new Query("777", queryString); // we give specific number is query since no query number is given in this option todo: might have to think about a random way to do it
        Parse parserForQuery = new Parse(stopWords, 0, pathToPostingFiles);
        searcher = new Searcher(parserForQuery, pathToPostingFiles, isStemming, isSemantic);
        HashMap<String, Double> relevantDocs = searcher.searchQuery(query);
        singleQuerySortedResults = searcher.getSortedDocsList(relevantDocs);
        if(writeToTrecFile){
            writeResultOfSingleQuery(pathToWriteQueryResults);
        }
        return singleQuerySortedResults;

    }


    public LinkedList<QueryData> findRelevantDocumentsForManyQueries(String pathToQueriesFile) throws IOException, com.medallia.word2vec.Searcher.UnknownWordException {

        Query query;
        Parse parserForQuery = new Parse(stopWords, 0, pathToPostingFiles);
        searcher = new Searcher(parserForQuery, pathToPostingFiles, isStemming, isSemantic);
        HashMap<String, LinkedList<String>> queriesAndTheirRatings = new HashMap<>();
        ReadFile queriesReader = new ReadFile(pathToQueriesFile);
        String queriesFile = queriesReader.readQueriesFile();
        String[] queriesSplit = queriesFile.split("</top>");

        for (String queryInString : queriesSplit) {

            String[] holdsDocNumAndTitle = queryInString.split("<title>");
            String[] holdsDocNum = holdsDocNumAndTitle[0].split(":");
            String queryNum = holdsDocNum[1].replaceAll(" ", ""); //holds the query's number
            String[] holdsDocTitle = holdsDocNumAndTitle[1].split("<desc>");
            String queryTitle = holdsDocTitle[0]; //holds the query itself //todo: check that works correctly with the extra spaces

            query = new Query(queryNum, queryTitle);
            HashMap<String, Double> relevantDocs = searcher.searchQuery(query);
            LinkedList<String> relevantDocsSorted = searcher.getSortedDocsList(relevantDocs);

            queriesAndTheirRatings.put(query.getId(), relevantDocsSorted);
        }
        multiQueriesSortedResults = queriesAndTheirRatings;
        if(writeToTrecFile){
            writeResultsOfMultiQueries("Path"); //todo: fix later to real path
        }

        LinkedList<QueryData> results = convertToQueryDataList(multiQueriesSortedResults);

        return results;
    }

    private LinkedList<QueryData> convertToQueryDataList (HashMap <String,LinkedList<String>> hashMap){
        LinkedList<QueryData> results = new LinkedList<>();
        for(Map.Entry<String,LinkedList<String>> entry : hashMap.entrySet()){
            LinkedList<String> docs = entry.getValue();
            for(String doc : docs){
                QueryData queryData = new QueryData(entry.getKey(),doc);
                results.add(queryData);
            }
        }
        return results;
    }




    /**
     * this function return a list of the 5 most ranked entities from a specified doc
     * @param fileName the file of the doc
     * @return the list
     * @throws IOException
     */
    public LinkedList<String> findMostRankedEntities(String fileName) throws IOException {
        LinkedList <String> strongEntites = new LinkedList<>();
        indexer = new Indexer(pathToPostingFiles,isStemming);
        Ranker ranker = new Ranker(indexer,pathToPostingFiles,isStemming);
        strongEntites = ranker.findMostRankedEntities(fileName);

        return strongEntites;
    }

    /**
     * The method receives a path and writes the results for a single query in it
     *
     * @param path
     */
    public void writeResultOfSingleQuery(String path) throws IOException {

        String pathForResults = path + "/singleResults.txt";
        File file = new File(pathForResults);
        FileOutputStream outputStream = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

        for (String result : singleQuerySortedResults) {
            String[] resultParsed = result.split(":");
            //resultParsed[1] = resultParsed[1].replaceAll(" ", "");
            resultParsed[0] = resultParsed[0].replaceAll(" ", "");

            String resultInEvacFormat = "777 0 " + resultParsed[0] + " 1 42.35 mt";  //todo: check format later, confusion in the instructions
            writer.write(resultInEvacFormat + "\n");
        }

        writer.flush();
        writer.close();

    }


    /**
     * The method receives a path and writes the results for many queries in it
     *
     * @param path
     */
    public void writeResultsOfMultiQueries(String path) throws IOException {

        String pathForResults = path + "/multiResults.txt";
        File file = new File(pathForResults);
        FileOutputStream outputStream = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

        for (HashMap.Entry<String, LinkedList<String>> entry : multiQueriesSortedResults.entrySet()) {

            LinkedList<String> entryDocs = entry.getValue();
            for (String doc : entryDocs) {
                String[] docParsed = doc.split(":");
                //resultParsed[1] = resultParsed[1].replaceAll(" ", "");
                docParsed[0] = docParsed[0].replaceAll(" ", "");
                String resultInEvacFormat = entry.getKey() + " 0 " + docParsed[0] + " 1 42.35 mt"; //todo: check format later, confusion in the instructions
                writer.write(resultInEvacFormat + "\n");
            }

        }
        writer.flush();
        writer.close();

    }


}

