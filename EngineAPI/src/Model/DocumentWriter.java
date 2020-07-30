package Model;

import Model.Document;

import java.io.*;
import java.util.ArrayList;

public class DocumentWriter {

    private String path;
    private ArrayList<Document> docs;
    private int docsToWrite;
    private long id;

    /**
     * constructor
     * @param path the path of the folder we will write the document's file
     */
    DocumentWriter(String path, long id){
        this.path=path;
        docs = new ArrayList<>();
        this.id=id;
    }

    /**
     * this function takes a document and adds it to a list, if needed it will be written to a file
     * @param document
     * @throws IOException
     */
    public void handleDocument(Document document) throws IOException {
        docs.add(document);
        docsToWrite=docs.size();
        if(docsToWrite>1000){
            writeToDisk();
            docs = new ArrayList<>();
            docsToWrite=0;
        }
    }


    /**
     * this function writes to a disk the file
     * @throws IOException
     */
    private void writeToDisk() throws IOException {
            File tempDir = new File (path+"documents/documents");
            if(!tempDir.isDirectory()){
                new File(path + "/documents").mkdir();
            }
            File file = new File(path + "/documents/" + "documents"+id+".txt");
            FileWriter fileWriter = new FileWriter(file.getAbsolutePath(), true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            if (!file.exists()) {
                file.createNewFile();
            }
            if (docs != null) {
                for (Document doc : docs) {
                    bufferedWriter.write(doc.toString() + "\n");
                }
                bufferedWriter.flush();
                bufferedWriter.close();
            }
    }

    /**
     * this function writes the docs that are left to the disk
     * @throws IOException
     */
    public void finishWriteDocuments () throws IOException {
        writeToDisk();
    }

}
