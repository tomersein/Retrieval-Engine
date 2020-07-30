package Model;

import java.io.*;
import java.util.*;

/**
 * The class that represents the reader of the files from a directory
 */
public class ReadFile {

    private String filePath;
    private LinkedList <String> docs;

    /**
     * constructor
     * @param filePath the directory
     */
    ReadFile(String filePath){
        this.filePath=filePath;
        docs = new LinkedList<>();
    }

    /**
     * this is a default constructor
     */
    public LinkedList<String> readFiles(int start) throws IOException {
        File mainDir = new File(filePath);
        String[] subDirs = mainDir.list();
        docs.clear();
        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;
        while (start+counter < start+3) {
            if (subDirs.length - 1 > counter + start) {
                File fileDir = new File(filePath + "/" + subDirs[start + counter] + "/" + subDirs[start + counter]);
                BufferedReader reader = new BufferedReader(new FileReader(fileDir));
                try {
                    String line="";
                    while((line = reader.readLine())!=null){
                        stringBuilder.append(line+" ");
                    }
                    String [] tokens = stringBuilder.toString().split("</DOC>");
                    this.docs.addAll(Arrays.asList(tokens));

                    /*
                    FileInputStream fileInputStream = new FileInputStream(fileDir);
                    byte[] tempDocByte = new byte[(int) fileDir.length()];
                    fileInputStream.read(tempDocByte);
                    fileInputStream.close();
                    String tempDocString = new String(tempDocByte, "UTF-8");
                    String[] tokens = tempDocString.split("</DOC>");
                    this.docs.addAll(Arrays.asList(tokens));
                    */
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            counter++;
            stringBuilder = new StringBuilder();
        }
        return docs;
    }

    /**
     * The function returns a hashset that holds the stop-words in the corpus
     * @return
     * @throws FileNotFoundException
     */
    public HashSet<String> readStopWord () throws FileNotFoundException {
        HashSet<String> stopWords = new HashSet<>();
        File fileDir = new File (filePath+"/"+"stopwords.txt");
        Scanner sc = new Scanner (fileDir);
        String line="";
        while (sc.hasNextLine()){
            line=sc.nextLine();
            stopWords.add(line);
        }
        return stopWords;
    }

    //-------------FUNCTIONS FOR PART B-------------------


    /**
     * the function reads a query file and sends back a string
     * @return a string of the content of the file
     * @throws IOException
     */
    public String readQueriesFile() throws IOException {
        File file = new File (this.filePath);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while((line=reader.readLine())!=null){
            stringBuilder.append(line+" ");
        }
        stringBuilder.deleteCharAt(stringBuilder.length()-1);
        reader.close();
        return stringBuilder.toString();
    }

    /**
     * this function takes the data structure of the stop words and copies it to the posting file directory
     * @param postingFileLocation the directory
     * @param wordsSet the data structure of the stop words
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void copyStopWords(String postingFileLocation, HashSet<String> wordsSet) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter printWriter = new PrintWriter(postingFileLocation+"/stopwords.txt","UTF-8");
        for (String word : wordsSet){
            printWriter.println(word);
        }
        printWriter.flush();
        printWriter.close();
    }


}